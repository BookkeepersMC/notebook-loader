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
package net.fabricmc.loader.launch.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.NotebookLoader;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncher;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.util.UrlUtil;

/**
 * @deprecated Internal API, do not use
 */
@Deprecated
public class FabricLauncherBase implements FabricLauncher {
	private final NotebookLauncher parent = NotebookLauncherBase.getLauncher();

	public static Class<?> getClass(String className) throws ClassNotFoundException {
		return Class.forName(className, true, getLauncher().getTargetClassLoader());
	}

	public static FabricLauncher getLauncher() {
		return new FabricLauncherBase();
	}

	@Override
	public void propose(URL url) {
		parent.addToClassPath(UrlUtil.asPath(url));
	}

	@Override
	public EnvType getEnvironmentType() {
		return NotebookLoader.getInstance().getEnvironmentType();
	}

	@Override
	public boolean isClassLoaded(String name) {
		return parent.isClassLoaded(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return parent.getResourceAsStream(name);
	}

	@Override
	public ClassLoader getTargetClassLoader() {
		return parent.getTargetClassLoader();
	}

	@Override
	public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		return parent.getClassByteArray(name, runTransformers);
	}

	@Override
	public boolean isDevelopment() {
		return NotebookLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Collection<URL> getLoadTimeDependencies() {
		List<URL> ret = new ArrayList<>();

		for (Path path : parent.getClassPath()) {
			try {
				ret.add(UrlUtil.asUrl(path));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		return ret;
	}
}
