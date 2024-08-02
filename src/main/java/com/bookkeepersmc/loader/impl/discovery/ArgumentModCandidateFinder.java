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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.util.Arguments;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public class ArgumentModCandidateFinder implements ModCandidateFinder {
	private final boolean requiresRemap;

	public ArgumentModCandidateFinder(boolean requiresRemap) {
		this.requiresRemap = requiresRemap;
	}

	@Override
	public void findCandidates(ModCandidateConsumer out) {
		String list = System.getProperty(SystemProperties.ADD_MODS);
		if (list != null) addMods(list, "system property", out);

		list = NotebookLoaderImpl.INSTANCE.getGameProvider().getArguments().remove(Arguments.ADD_MODS);
		if (list != null) addMods(list, "argument", out);
	}

	private void addMods(String list, String source, ModCandidateConsumer out) {
		for (String pathStr : list.split(File.pathSeparator)) {
			if (pathStr.isEmpty()) continue;

			if (pathStr.startsWith("@")) {
				Path path = Paths.get(pathStr.substring(1));

				if (!Files.isRegularFile(path)) {
					Log.warn(LogCategory.DISCOVERY, "Skipping missing/invalid %s provided mod list file %s", source, path);
					continue;
				}

				try (BufferedReader reader = Files.newBufferedReader(path)) {
					String fileSource = String.format("%s file %s", source, path);
					String line;

					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.isEmpty()) continue;

						addMod(line, fileSource, out);
					}
				} catch (IOException e) {
					throw new RuntimeException(String.format("Error reading %s provided mod list file %s", source, path), e);
				}
			} else {
				addMod(pathStr, source, out);
			}
		}
	}

	private void addMod(String pathStr, String source, ModCandidateConsumer out) {
		Path path = LoaderUtil.normalizePath(Paths.get(pathStr));

		if (!Files.exists(path)) { // missing
			Log.warn(LogCategory.DISCOVERY, "Skipping missing %s provided mod path %s", source, path);
		} else if (Files.isDirectory(path)) { // directory for extracted mod (in-dev usually) or jars (like mods, but recursive)
			if (isHidden(path)) {
				Log.warn(LogCategory.DISCOVERY, "Ignoring hidden %s provided mod path %s", source, path);
				return;
			}

			if (Files.exists(path.resolve("notebook.mod.json"))) { // extracted mod
				out.accept(path, requiresRemap);
			} else { // dir containing jars
				try {
					List<String> skipped = new ArrayList<>();

					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (DirectoryModCandidateFinder.isValidFile(file)) {
								out.accept(file, requiresRemap);
							} else {
								skipped.add(path.relativize(file).toString());
							}

							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							if (isHidden(dir)) {
								return FileVisitResult.SKIP_SUBTREE;
							} else {
								return FileVisitResult.CONTINUE;
							}
						}
					});

					if (!skipped.isEmpty()) {
						Log.warn(LogCategory.DISCOVERY, "Incompatible files in %s provided mod directory %s (non-jar or hidden): %s", source, path, String.join(", ", skipped));
					}
				} catch (IOException e) {
					Log.warn(LogCategory.DISCOVERY, "Error processing %s provided mod path %s: %s", source, path, e);
				}
			}
		} else { // single file
			if (!DirectoryModCandidateFinder.isValidFile(path)) {
				Log.warn(LogCategory.DISCOVERY, "Incompatible file in %s provided mod path %s (non-jar or hidden)", source, path);
			} else {
				out.accept(path, requiresRemap);
			}
		}
	}

	private static boolean isHidden(Path path) {
		try {
			return path.getFileName().toString().startsWith(".") || Files.isHidden(path);
		} catch (IOException e) {
			Log.warn(LogCategory.DISCOVERY, "Error determining whether %s is hidden: %s", path, e);
			return true;
		}
	}
}
