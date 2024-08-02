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
package com.bookkeepersmc.loader.impl.launch.knot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.jar.Manifest;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.impl.game.GameProvider;

interface KnotClassLoaderInterface {
	@SuppressWarnings("resource")
	static KnotClassLoaderInterface create(boolean useCompatibility, boolean isDevelopment, EnvType envType, GameProvider provider) {
		if (useCompatibility) {
			return new KnotCompatibilityClassLoader(isDevelopment, envType, provider).getDelegate();
		} else {
			return new KnotClassLoader(isDevelopment, envType, provider).getDelegate();
		}
	}

	void initializeTransformers();

	ClassLoader getClassLoader();

	void addCodeSource(Path path);
	void setAllowedPrefixes(Path codeSource, String... prefixes);
	void setValidParentClassPath(Collection<Path> codeSources);

	Manifest getManifest(Path codeSource);

	boolean isClassLoaded(String name);
	Class<?> loadIntoTarget(String name) throws ClassNotFoundException;

	byte[] getRawClassBytes(String name) throws IOException;
	byte[] getPreMixinClassBytes(String name);
}
