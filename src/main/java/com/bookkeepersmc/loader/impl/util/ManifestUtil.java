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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

public final class ManifestUtil {
	public static Manifest readManifest(Class<?> cls) throws IOException, URISyntaxException {
		CodeSource cs = cls.getProtectionDomain().getCodeSource();
		if (cs == null) return null;

		URL url = cs.getLocation();
		if (url == null) return null;

		return readManifest(url);
	}

	private static Manifest readManifest(URL codeSourceUrl) throws IOException, URISyntaxException {
		Path path = UrlUtil.asPath(codeSourceUrl);

		if (Files.isDirectory(path)) {
			return readManifestFromBasePath(path);
		} else {
			URLConnection connection = new URL("jar:" + codeSourceUrl.toString() + "!/").openConnection();

			if (connection instanceof JarURLConnection) {
				return ((JarURLConnection) connection).getManifest();
			}

			try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(path, false)) {
				return readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
			}
		}
	}

	public static Manifest readManifest(Path codeSource) throws IOException {
		if (Files.isDirectory(codeSource)) {
			return readManifestFromBasePath(codeSource);
		} else {
			try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(codeSource, false)) {
				return readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
			}
		}
	}

	public static Manifest readManifestFromBasePath(Path basePath) throws IOException {
		Path path = basePath.resolve("META-INF").resolve("MANIFEST.MF");
		if (!Files.exists(path)) return null;

		try (InputStream stream = Files.newInputStream(path)) {
			return new Manifest(stream);
		}
	}

	public static String getManifestValue(Manifest manifest, Name name) {
		return manifest.getMainAttributes().getValue(name);
	}

	public static List<URL> getClassPath(Manifest manifest, Path baseDir) throws MalformedURLException {
		String cp = ManifestUtil.getManifestValue(manifest, Name.CLASS_PATH);
		if (cp == null) return null;

		StringTokenizer tokenizer = new StringTokenizer(cp);
		List<URL> ret = new ArrayList<>();
		URL context = UrlUtil.asUrl(baseDir);

		while (tokenizer.hasMoreElements()) {
			ret.add(new URL(context, tokenizer.nextToken()));
		}

		return ret;
	}
}
