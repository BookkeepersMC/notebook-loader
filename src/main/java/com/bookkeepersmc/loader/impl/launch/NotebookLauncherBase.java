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
package com.bookkeepersmc.loader.impl.launch;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import com.bookkeepersmc.loader.impl.FormattedException;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.game.GameProvider;
import com.bookkeepersmc.loader.impl.gui.NotebookGuiEntry;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.asm.mixin.MixinEnvironment;

public abstract class NotebookLauncherBase implements NotebookLauncher {
	private static boolean mixinReady;
	private static Map<String, Object> properties;
	private static NotebookLauncher launcher;
	private static MappingConfiguration mappingConfiguration = new MappingConfiguration();

	protected NotebookLauncherBase() {
		setLauncher(this);
	}

	public static Class<?> getClass(String className) throws ClassNotFoundException {
		return Class.forName(className, true, getLauncher().getTargetClassLoader());
	}

	@Override
	public MappingConfiguration getMappingConfiguration() {
		return mappingConfiguration;
	}

	protected static void setProperties(Map<String, Object> propertiesA) {
		if (properties != null && properties != propertiesA) {
			throw new RuntimeException("Duplicate setProperties call!");
		}

		properties = propertiesA;
	}

	@VisibleForTesting
	public static void setLauncher(NotebookLauncher launcherA) {
		if (launcher != null && launcher != launcherA) {
			throw new RuntimeException("Duplicate setLauncher call!");
		}

		launcher = launcherA;
	}

	public static NotebookLauncher getLauncher() {
		return launcher;
	}

	public static Map<String, Object> getProperties() {
		return properties;
	}

	protected static void handleFormattedException(FormattedException exc) {
		Throwable actualExc = exc.getMessage() != null ? exc : exc.getCause();
		Log.error(LogCategory.GENERAL, exc.getMainText(), actualExc);

		GameProvider gameProvider = NotebookLoaderImpl.INSTANCE.tryGetGameProvider();

		if (gameProvider == null || !gameProvider.displayCrash(actualExc, exc.getDisplayedText())) {
			NotebookGuiEntry.displayError(exc.getDisplayedText(), actualExc, true);
		} else {
			System.exit(1);
		}

		throw new AssertionError("exited");
	}

	protected static void setupUncaughtExceptionHandler() {
		Thread mainThread = Thread.currentThread();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				try {
					if (e instanceof FormattedException) {
						handleFormattedException((FormattedException) e);
					} else {
						String mainText = String.format("Uncaught exception in thread \"%s\"", t.getName());
						Log.error(LogCategory.GENERAL, mainText, e);

						GameProvider gameProvider = NotebookLoaderImpl.INSTANCE.tryGetGameProvider();

						if (Thread.currentThread() == mainThread
								&& (gameProvider == null || !gameProvider.displayCrash(e, mainText))) {
							NotebookGuiEntry.displayError(mainText, e, false);
						}
					}
				} catch (Throwable e2) { // just in case
					e.addSuppressed(e2);

					try {
						e.printStackTrace();
					} catch (Throwable e3) {
						PrintWriter pw = new PrintWriter(new FileOutputStream(FileDescriptor.err));
						e.printStackTrace(pw);
						pw.flush();
					}
				}
			}
		});
	}

	protected static void finishMixinBootstrapping() {
		if (mixinReady) {
			throw new RuntimeException("Must not call FabricLauncherBase.finishMixinBootstrapping() twice!");
		}

		try {
			Method m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
			m.setAccessible(true);
			m.invoke(null, MixinEnvironment.Phase.INIT);
			m.invoke(null, MixinEnvironment.Phase.DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		mixinReady = true;
	}

	public static boolean isMixinReady() {
		return mixinReady;
	}
}
