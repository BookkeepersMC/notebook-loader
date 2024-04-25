
/*
 * Copyright (c) 2024 BookkeepersMC under the MIT License
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
package com.bookkeepersmc.loader.impl.game.minecraft.patch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.util.UrlUtil;

import com.bookkeepersmc.loader.impl.game.minecraft.MinecraftGameProvider;

/**
 * Wrapper class replacing pre-1.3 FML's ModClassLoader (which relies on
 * URLClassLoader implementation details - no longer applicable in Java 9+)
 * with an implementation effectively wrapping Knot.
 */
public class ModClassLoader_125_FML extends URLClassLoader {
	private URL[] localUrls;

	public ModClassLoader_125_FML() {
		super(new URL[0], NotebookLauncherBase.getLauncher().getTargetClassLoader());
		localUrls = new URL[0];
	}

	@Override
	protected void addURL(URL url) {
		NotebookLauncherBase.getLauncher().addToClassPath(UrlUtil.asPath(url));

		URL[] newLocalUrls = new URL[localUrls.length + 1];
		System.arraycopy(localUrls, 0, newLocalUrls, 0, localUrls.length);
		newLocalUrls[localUrls.length] = url;
		localUrls = newLocalUrls;
	}

	@Override
	public URL[] getURLs() {
		return localUrls;
	}

	@Override
	public URL findResource(final String name) {
		return getParent().getResource(name);
	}

	@Override
	public Enumeration<URL> findResources(final String name) throws IOException {
		return getParent().getResources(name);
	}

	/**
	 * This is used to add mods to the classpath.
	 * @param file The mod file.
	 * @throws MalformedURLException If the File->URL transformation fails.
	 */
	public void addFile(File file) throws MalformedURLException {
		try {
			addURL(UrlUtil.asUrl(file));
		} catch (MalformedURLException e) {
			throw new MalformedURLException(e.getMessage());
		}
	}

	/**
	 * This is used to find the Minecraft .JAR location.
	 *
	 * @return The "parent source" file.
	 */
	public File getParentSource() {
		return ((MinecraftGameProvider) NotebookLoaderImpl.INSTANCE.getGameProvider()).getGameJar().toFile();
	}

	/**
	 * @return The "parent source" files array.
	 */
	public File[] getParentSources() {
		return new File[] { getParentSource() };
	}

	static {
		registerAsParallelCapable();
	}
}
