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

import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public final class UrlUtil {
	public static final Path LOADER_CODE_SOURCE = getCodeSource(UrlUtil.class);

	public static Path getCodeSource(URL url, String localPath) throws UrlConversionException {
		try {
			URLConnection connection = url.openConnection();

			if (connection instanceof JarURLConnection) {
				return asPath(((JarURLConnection) connection).getJarFileURL());
			} else {
				String path = url.getPath();

				if (path.endsWith(localPath)) {
					return asPath(new URL(url.getProtocol(), url.getHost(), url.getPort(), path.substring(0, path.length() - localPath.length())));
				} else {
					throw new UrlConversionException("Could not figure out code source for file '" + localPath + "' in URL '" + url + "'!");
				}
			}
		} catch (Exception e) {
			throw new UrlConversionException(e);
		}
	}

	public static Path asPath(URL url) {
		try {
			return Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw ExceptionUtil.wrap(e);
		}
	}

	public static URL asUrl(File file) throws MalformedURLException {
		return file.toURI().toURL();
	}

	public static URL asUrl(Path path) throws MalformedURLException {
		return path.toUri().toURL();
	}

	public static Path getCodeSource(Class<?> cls) {
		CodeSource cs = cls.getProtectionDomain().getCodeSource();
		if (cs == null) return null;

		return asPath(cs.getLocation());
	}
}
