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
package com.bookkeepersmc.loader.impl.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class LoaderUtil {
	public static String getClassFileName(String className) {
		return className.replace('.', '/').concat(".class");
	}

	public static Path normalizePath(Path path) {
		if (Files.exists(path)) {
			return normalizeExistingPath(path);
		} else {
			return path.toAbsolutePath().normalize();
		}
	}

	public static Path normalizeExistingPath(Path path) {
		try {
			return path.toRealPath();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void verifyNotInTargetCl(Class<?> cls) {
		if (cls.getClassLoader().getClass().getName().equals("com.bookkeepersmc.loader.impl.launch.knot.KnotClassLoader")) {
			// This usually happens when fabric loader has been added to the target class loader. This is a bad state.
			// Such additions may be indirect, a JAR can use the Class-Path manifest attribute to drag additional
			// libraries with it, likely recursively.
			throw new IllegalStateException("trying to load "+cls.getName()+" from target class loader");
		}
	}

	public static void verifyClasspath() {
		try {
			final List<URL> resources = Collections.list(LoaderUtil.class.getClassLoader().getResources("com/bookkeepersmc/loader/api/NotebookLoader.class"));

			if (resources.size() != 1) {
				// This usually happens when fabric loader has been added to the classpath more than once.
				throw new IllegalStateException("duplicate loader classes found on classpath: " + resources.stream().map(URL::toString).collect(Collectors.joining(", ")));
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to get resources", e);
		}
	}

	public static boolean hasMacOs() {
		return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac");
	}

	public static boolean hasAwtSupport() {
		if (hasMacOs()) {
			// check for JAVA_STARTED_ON_FIRST_THREAD_<pid> which is set if -XstartOnFirstThread is used
			// -XstartOnFirstThread is incompatible with AWT (force enables embedded mode)
			for (String key : System.getenv().keySet()) {
				if (key.startsWith("JAVA_STARTED_ON_FIRST_THREAD_")) return false;
			}
		}

		return true;
	}
}
