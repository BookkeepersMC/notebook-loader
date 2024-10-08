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
package com.bookkeepersmc.loader.impl.game;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.impl.FormattedException;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.launch.MappingConfiguration;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncher;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.UrlConversionException;
import com.bookkeepersmc.loader.impl.util.UrlUtil;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;
import com.bookkeepersmc.loader.impl.util.mappings.TinyRemapperMappingsHelper;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public final class GameProviderHelper {
	private GameProviderHelper() { }

	public static Path getCommonGameJar() {
		return getGameJar(SystemProperties.GAME_JAR_PATH);
	}

	public static Path getEnvGameJar(EnvType env) {
		return getGameJar(env == EnvType.CLIENT ? SystemProperties.GAME_JAR_PATH_CLIENT : SystemProperties.GAME_JAR_PATH_SERVER);
	}

	private static Path getGameJar(String property) {
		String val = System.getProperty(property);
		if (val == null) return null;

		Path path = Paths.get(val);
		if (!Files.exists(path)) throw new RuntimeException("Game jar "+path+" ("+LoaderUtil.normalizePath(path)+") configured through "+property+" system property doesn't exist");

		return LoaderUtil.normalizeExistingPath(path);
	}

	public static Optional<Path> getSource(ClassLoader loader, String filename) {
		URL url;

		if ((url = loader.getResource(filename)) != null) {
			try {
				return Optional.of(UrlUtil.getCodeSource(url, filename));
			} catch (UrlConversionException e) {
				// TODO: Point to a logger
				e.printStackTrace();
			}
		}

		return Optional.empty();
	}

	public static List<Path> getSources(ClassLoader loader, String filename) {
		try {
			Enumeration<URL> urls = loader.getResources(filename);
			List<Path> paths = new ArrayList<>();

			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();

				try {
					paths.add(UrlUtil.getCodeSource(url, filename));
				} catch (UrlConversionException e) {
					// TODO: Point to a logger
					e.printStackTrace();
				}
			}

			return paths;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public static FindResult findFirst(List<Path> paths, Map<Path, ZipFile> zipFiles, boolean isClassName, String... names) {
		for (String name : names) {
			String file = isClassName ? LoaderUtil.getClassFileName(name) : name;

			for (Path path : paths) {
				if (Files.isDirectory(path)) {
					if (Files.exists(path.resolve(file))) {
						return new FindResult(name, path);
					}
				} else {
					ZipFile zipFile = zipFiles.get(path);

					if (zipFile == null) {
						try {
							zipFile = new ZipFile(path.toFile());
							zipFiles.put(path, zipFile);
						} catch (IOException e) {
							throw new RuntimeException("Error reading "+path, e);
						}
					}

					if (zipFile.getEntry(file) != null) {
						return new FindResult(name, path);
					}
				}
			}
		}

		return null;
	}

	public static final class FindResult {
		public final String name;
		public final Path path;

		FindResult(String name, Path path) {
			this.name = name;
			this.path = path;
		}
	}

	private static boolean emittedInfo = false;

	public static Map<String, Path> deobfuscate(Map<String, Path> inputFileMap, String gameId, String gameVersion, Path gameDir, NotebookLauncher launcher) {
		Log.debug(LogCategory.GAME_REMAP, "Requesting deobfuscation of %s", inputFileMap);

		if (launcher.isDevelopment()) { // in-dev is already deobfuscated
			return inputFileMap;
		}

		MappingConfiguration mappingConfig = launcher.getMappingConfiguration();

		if (!mappingConfig.matches(gameId, gameVersion)) {
			String mappingsGameId = mappingConfig.getGameId();
			String mappingsGameVersion = mappingConfig.getGameVersion();

			throw new FormattedException("Incompatible mappings",
					String.format("Supplied mappings for %s %s are incompatible with %s %s, this is likely caused by launcher misbehavior",
							(mappingsGameId != null ? mappingsGameId : "(unknown)"),
							(mappingsGameVersion != null ? mappingsGameVersion : "(unknown)"),
							gameId,
							gameVersion));
		}

		String targetNamespace = mappingConfig.getTargetNamespace();
		List<String> namespaces = mappingConfig.getNamespaces();

		if (namespaces == null) {
			Log.debug(LogCategory.GAME_REMAP, "No mappings, using input files");
			return inputFileMap;
		}

		if (!namespaces.contains(targetNamespace)) {
			Log.debug(LogCategory.GAME_REMAP, "Missing namespace in mappings, using input files");
			return inputFileMap;
		}

		Path deobfJarDir = getDeobfJarDir(gameDir, gameId, gameVersion);
		List<Path> inputFiles = new ArrayList<>(inputFileMap.size());
		List<Path> outputFiles = new ArrayList<>(inputFileMap.size());
		List<Path> tmpFiles = new ArrayList<>(inputFileMap.size());
		Map<String, Path> ret = new HashMap<>(inputFileMap.size());
		boolean anyMissing = false;

		for (Map.Entry<String, Path> entry : inputFileMap.entrySet()) {
			String name = entry.getKey();
			Path inputFile = entry.getValue();
			// TODO: allow versioning mappings?
			String deobfJarFilename = String.format("%s-%s.jar", name, targetNamespace);
			Path outputFile = deobfJarDir.resolve(deobfJarFilename);
			Path tmpFile = deobfJarDir.resolve(deobfJarFilename + ".tmp");

			if (Files.exists(tmpFile)) { // previous unfinished remap attempt
				Log.warn(LogCategory.GAME_REMAP, "Incomplete remapped file found! This means that the remapping process failed on the previous launch. If this persists, make sure to let us at Fabric know!");

				try {
					Files.deleteIfExists(outputFile);
					Files.deleteIfExists(tmpFile);
				} catch (IOException e) {
					throw new RuntimeException("can't delete incompletely remapped files", e);
				}
			}

			inputFiles.add(inputFile);
			outputFiles.add(outputFile);
			tmpFiles.add(tmpFile);
			ret.put(name, outputFile);

			if (!anyMissing && !Files.exists(outputFile)) {
				anyMissing = true;
			}
		}

		if (!anyMissing) {
			Log.debug(LogCategory.GAME_REMAP, "Remapped files exist already, reusing them");
			return ret;
		}

		Log.debug(LogCategory.GAME_REMAP, "Fabric mapping file detected, applying...");

		if (!emittedInfo) {
			Log.info(LogCategory.GAME_REMAP, "Fabric is preparing JARs on first launch, this may take a few seconds...");
			emittedInfo = true;
		}

		try {
			Files.createDirectories(deobfJarDir);
			deobfuscate0(inputFiles, outputFiles, tmpFiles, mappingConfig.getMappings(), targetNamespace, launcher);
		} catch (IOException e) {
			throw new RuntimeException("error remapping game jars "+inputFiles, e);
		}

		return ret;
	}

	private static Path getDeobfJarDir(Path gameDir, String gameId, String gameVersion) {
		Path ret = gameDir.resolve(NotebookLoaderImpl.CACHE_DIR_NAME).resolve(NotebookLoaderImpl.REMAPPED_JARS_DIR_NAME);
		StringBuilder versionDirName = new StringBuilder();

		if (!gameId.isEmpty()) {
			versionDirName.append(gameId);
		}

		if (!gameVersion.isEmpty()) {
			if (versionDirName.length() > 0) versionDirName.append('-');
			versionDirName.append(gameVersion);
		}

		if (versionDirName.length() > 0) versionDirName.append('-');
		versionDirName.append(NotebookLoaderImpl.VERSION);

		return ret.resolve(versionDirName.toString().replaceAll("[^\\w\\-\\. ]+", "_"));
	}

	private static void deobfuscate0(List<Path> inputFiles, List<Path> outputFiles, List<Path> tmpFiles, MappingTree mappings, String targetNamespace, NotebookLauncher launcher) throws IOException {
		TinyRemapper remapper = TinyRemapper.newRemapper()
				.withMappings(TinyRemapperMappingsHelper.create(mappings, "official", targetNamespace))
				.rebuildSourceFilenames(true)
				.build();

		Set<Path> depPaths = new HashSet<>();

		if (System.getProperty(SystemProperties.DEBUG_DEOBFUSCATE_WITH_CLASSPATH) != null) {
			for (Path path : launcher.getClassPath()) {
				if (!inputFiles.contains(path)) {
					depPaths.add(path);

					Log.debug(LogCategory.GAME_REMAP, "Appending '%s' to remapper classpath", path);
					remapper.readClassPathAsync(path);
				}
			}
		}

		List<OutputConsumerPath> outputConsumers = new ArrayList<>(inputFiles.size());
		List<InputTag> inputTags = new ArrayList<>(inputFiles.size());

		try {
			for (int i = 0; i < inputFiles.size(); i++) {
				Path inputFile = inputFiles.get(i);
				Path tmpFile = tmpFiles.get(i);

				InputTag inputTag = remapper.createInputTag();
				OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(tmpFile)
						// force jar despite the .tmp extension
						.assumeArchive(true)
						.build();

				outputConsumers.add(outputConsumer);
				inputTags.add(inputTag);

				outputConsumer.addNonClassFiles(inputFile, NonClassCopyMode.FIX_META_INF, remapper);
				remapper.readInputsAsync(inputTag, inputFile);
			}

			for (int i = 0; i < inputFiles.size(); i++) {
				remapper.apply(outputConsumers.get(i), inputTags.get(i));
			}
		} finally {
			for (OutputConsumerPath outputConsumer : outputConsumers) {
				outputConsumer.close();
			}

			remapper.finish();
		}

		// Minecraft doesn't tend to check if a ZipFileSystem is already present,
		// so we clean up here.

		depPaths.addAll(tmpFiles);

		for (Path p : depPaths) {
			try {
				p.getFileSystem().close();
			} catch (Exception e) {
				// pass
			}

			try {
				FileSystems.getFileSystem(new URI("jar:" + p.toUri())).close();
			} catch (Exception e) {
				// pass
			}
		}

		List<Path> missing = new ArrayList<>();

		for (int i = 0; i < inputFiles.size(); i++) {
			Path inputFile = inputFiles.get(i);
			Path tmpFile = tmpFiles.get(i);
			Path outputFile = outputFiles.get(i);

			boolean found;

			try (JarFile jar = new JarFile(tmpFile.toFile())) {
				found = jar.stream().anyMatch((e) -> e.getName().endsWith(".class"));
			}

			if (!found) {
				missing.add(inputFile);
				Files.delete(tmpFile);
			} else {
				Files.move(tmpFile, outputFile);
			}
		}

		if (!missing.isEmpty()) {
			throw new RuntimeException("Generated deobfuscated JARs contain no classes: "+missing);
		}
	}
}
