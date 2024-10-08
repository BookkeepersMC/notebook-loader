/*
 * Copyright (c) 2024 BookkeepersMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.bookkeepersmc.loader.impl.discovery;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import com.bookkeepersmc.loader.impl.FormattedException;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncher;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.util.FileSystemUtil;
import com.bookkeepersmc.loader.impl.util.ManifestUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;
import com.bookkeepersmc.loader.impl.util.mappings.TinyRemapperMappingsHelper;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import org.objectweb.asm.commons.Remapper;

public final class RuntimeModRemapper {
	private static final String REMAP_TYPE_MANIFEST_KEY = "Fabric-Loom-Mixin-Remap-Type";
	private static final String REMAP_TYPE_STATIC = "static";

	public static void remap(Collection<ModCandidateImpl> modCandidates, Path tmpDir, Path outputDir) {
		List<ModCandidateImpl> modsToRemap = new ArrayList<>();
		Set<InputTag> remapMixins = new HashSet<>();

		for (ModCandidateImpl mod : modCandidates) {
			if (mod.getRequiresRemap()) {
				modsToRemap.add(mod);
			}
		}

		if (modsToRemap.isEmpty()) return;

		NotebookLauncher launcher = NotebookLauncherBase.getLauncher();

		TinyRemapper remapper = TinyRemapper.newRemapper()
				.withMappings(TinyRemapperMappingsHelper.create(launcher.getMappingConfiguration().getMappings(), "intermediary", launcher.getTargetNamespace()))
				.renameInvalidLocals(false)
				.extension(new MixinExtension(remapMixins::contains))
				.build();

		try {
			remapper.readClassPathAsync(getRemapClasspath().toArray(new Path[0]));
		} catch (IOException e) {
			throw new RuntimeException("Failed to populate remap classpath", e);
		}

		Map<ModCandidateImpl, RemapInfo> infoMap = new HashMap<>();

		try {
			for (ModCandidateImpl mod : modsToRemap) {
				RemapInfo info = new RemapInfo();
				infoMap.put(mod, info);

				InputTag tag = remapper.createInputTag();
				info.tag = tag;

				if (mod.hasPath()) {
					List<Path> paths = mod.getPaths();
					if (paths.size() != 1) throw new UnsupportedOperationException("multiple path for "+mod);

					info.inputPath = paths.get(0);
				} else {
					info.inputPath = mod.copyToDir(tmpDir, true);
					info.inputIsTemp = true;
				}

				if (requiresMixinRemap(info.inputPath)) {
					remapMixins.add(tag);
				}

				info.outputPath = outputDir.resolve(mod.getDefaultFileName());
				Files.deleteIfExists(info.outputPath);

				remapper.readInputsAsync(tag, info.inputPath);
			}

			//Done in a 2nd loop as we need to make sure all the inputs are present before remapping
			for (ModCandidateImpl mod : modsToRemap) {
				RemapInfo info = infoMap.get(mod);
				OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(info.outputPath).build();

				FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(info.inputPath, false);

				if (delegate.get() == null) {
					throw new RuntimeException("Could not open JAR file " + info.inputPath.getFileName() + " for NIO reading!");
				}

				Path inputJar = delegate.get().getRootDirectories().iterator().next();
				outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);

				info.outputConsumerPath = outputConsumer;

				remapper.apply(outputConsumer, info.tag);
			}

			//Done in a 3rd loop as this can happen when the remapper is doing its thing.
			for (ModCandidateImpl mod : modsToRemap) {
				RemapInfo info = infoMap.get(mod);

				String accessWidener = mod.getMetadata().getAccessWidener();

				if (accessWidener != null) {
					info.accessWidenerPath = accessWidener;

					try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.inputPath, false)) {
						FileSystem fs = jarFs.get();
						info.accessWidener = remapAccessWidener(Files.readAllBytes(fs.getPath(accessWidener)), remapper.getRemapper());
					} catch (Throwable t) {
						throw new RuntimeException("Error remapping access widener for mod '"+mod.getId()+"'!", t);
					}
				}
			}

			remapper.finish();

			for (ModCandidateImpl mod : modsToRemap) {
				RemapInfo info = infoMap.get(mod);

				info.outputConsumerPath.close();

				if (info.accessWidenerPath != null) {
					try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.outputPath, false)) {
						FileSystem fs = jarFs.get();

						Files.delete(fs.getPath(info.accessWidenerPath));
						Files.write(fs.getPath(info.accessWidenerPath), info.accessWidener);
					}
				}

				mod.setPaths(Collections.singletonList(info.outputPath));
			}
		} catch (Throwable t) {
			remapper.finish();

			for (RemapInfo info : infoMap.values()) {
				if (info.outputPath == null) {
					continue;
				}

				try {
					Files.deleteIfExists(info.outputPath);
				} catch (IOException e) {
					Log.warn(LogCategory.MOD_REMAP, "Error deleting failed output jar %s", info.outputPath, e);
				}
			}

			throw new FormattedException("Failed to remap mods!", t);
		} finally {
			for (RemapInfo info : infoMap.values()) {
				try {
					if (info.inputIsTemp) Files.deleteIfExists(info.inputPath);
				} catch (IOException e) {
					Log.warn(LogCategory.MOD_REMAP, "Error deleting temporary input jar %s", info.inputIsTemp, e);
				}
			}
		}
	}

	private static byte[] remapAccessWidener(byte[] input, Remapper remapper) {
		AccessWidenerWriter writer = new AccessWidenerWriter();
		AccessWidenerRemapper remappingDecorator = new AccessWidenerRemapper(writer, remapper, "intermediary", "named");
		AccessWidenerReader accessWidenerReader = new AccessWidenerReader(remappingDecorator);
		accessWidenerReader.read(input, "intermediary");
		return writer.write();
	}

	private static List<Path> getRemapClasspath() throws IOException {
		String remapClasspathFile = System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE);

		if (remapClasspathFile == null) {
			throw new RuntimeException("No remapClasspathFile provided");
		}

		String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

		return Arrays.stream(content.split(File.pathSeparator))
				.map(Paths::get)
				.collect(Collectors.toList());
	}

	private static boolean requiresMixinRemap(Path inputPath) throws IOException, URISyntaxException {
		final Manifest manifest = ManifestUtil.readManifest(inputPath);
		final Attributes mainAttributes = manifest.getMainAttributes();

		return REMAP_TYPE_STATIC.equalsIgnoreCase(mainAttributes.getValue(REMAP_TYPE_MANIFEST_KEY));
	}

	private static class RemapInfo {
		InputTag tag;
		Path inputPath;
		Path outputPath;
		boolean inputIsTemp;
		OutputConsumerPath outputConsumerPath;
		String accessWidenerPath;
		byte[] accessWidener;
	}
}
