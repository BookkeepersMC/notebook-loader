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
package net.fabricmc.loader.impl.junit;

import java.util.Locale;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.impl.launch.knot.Knot;
import com.bookkeepersmc.loader.impl.util.SystemProperties;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public class FabricLoaderLauncherSessionListener implements LauncherSessionListener {
	static {
		System.setProperty(SystemProperties.DEVELOPMENT, "true");
		System.setProperty(SystemProperties.UNIT_TEST, "true");
	}

	private final Knot knot;
	private final ClassLoader classLoader;

	private ClassLoader launcherSessionClassLoader;

	public FabricLoaderLauncherSessionListener() {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader originalClassLoader = currentThread.getContextClassLoader();

		// parse the test environment type, defaults to client
		final EnvType envType = EnvType.valueOf(System.getProperty(SystemProperties.SIDE, EnvType.CLIENT.name()).toUpperCase(Locale.ROOT));

		try {
			knot = new Knot(envType);
			classLoader = knot.init(new String[]{});
		} finally {
			// Knot.init sets the context class loader, revert it back for now.
			currentThread.setContextClassLoader(originalClassLoader);
		}
	}

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		final Thread currentThread = Thread.currentThread();
		launcherSessionClassLoader = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader(classLoader);
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		final Thread currentThread = Thread.currentThread();
		currentThread.setContextClassLoader(launcherSessionClassLoader);
	}
}
