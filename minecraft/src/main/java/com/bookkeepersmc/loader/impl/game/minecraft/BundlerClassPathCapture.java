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
package com.bookkeepersmc.loader.impl.game.minecraft;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

public final class BundlerClassPathCapture {
	static final CompletableFuture<URL[]> FUTURE = new CompletableFuture<>();

	public static void main(String[] args) { // invoked by the bundler on a thread
		try {
			URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
			URL[] urls = cl.getURLs();

			URL asmUrl = cl.findResource("org/objectweb/asm/ClassReader.class");

			if (asmUrl != null && (asmUrl = getJarUrl(asmUrl)) != null) {
				for (int i = 0; i < urls.length; i++) {
					if (urls[i].equals(asmUrl)) {
						URL[] newUrls = new URL[urls.length - 1];
						System.arraycopy(urls, 0, newUrls, 0, i);
						System.arraycopy(urls, i + 1, newUrls, i, urls.length - i - 1);
						urls = newUrls;
						break;
					}
				}
			}

			FUTURE.complete(urls);
		} catch (Throwable t) {
			FUTURE.completeExceptionally(t);
		}
	}

	/**
	 * Transform jar:file url to its outer file url.
	 *
	 * <p>jar:file:/path/to.jar!/pkg/Cls.class -> file:/path/to.jar
	 */
	private static URL getJarUrl(URL url) throws IOException {
		URLConnection connection = url.openConnection();

		if (connection instanceof JarURLConnection) {
			return ((JarURLConnection) connection).getJarFileURL();
		}

		return null;
	}
}
