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

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public class DirectoryModCandidateFinder implements ModCandidateFinder {
	private final Path path;
	private final boolean requiresRemap;

	public DirectoryModCandidateFinder(Path path, boolean requiresRemap) {
		this.path = path;
		this.requiresRemap = requiresRemap;
	}

	@Override
	public void findCandidates(ModCandidateConsumer out) {
		if (!Files.exists(path)) {
			try {
				Files.createDirectory(path);
			} catch (IOException e) {
				throw new RuntimeException("Could not create directory " + path, e);
			}
		}

		if (!Files.isDirectory(path)) {
			throw new RuntimeException(path + " is not a directory!");
		}

		try {
			Files.walkFileTree(this.path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (isValidFile(file)) {
						out.accept(file, requiresRemap);
					}

					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Exception while searching for mods in '" + path + "'!", e);
		}
	}

	static boolean isValidFile(Path path) {
		/*
		 * We only propose a file as a possible mod in the following scenarios:
		 * General: Must be a jar file
		 *
		 * Some OSes Generate metadata so consider the following because of OSes:
		 * UNIX: Exclude if file is hidden; this occurs when starting a file name with `.`
		 * MacOS: Exclude hidden + startsWith "." since Mac OS names their metadata files in the form of `.mod.jar`
		 */

		if (!Files.isRegularFile(path)) return false;

		try {
			if (Files.isHidden(path)) return false;
		} catch (IOException e) {
			Log.warn(LogCategory.DISCOVERY, "Error checking if file %s is hidden", path, e);
			return false;
		}

		String fileName = path.getFileName().toString();

		return fileName.endsWith(".jar") && !fileName.startsWith(".");
	}
}
