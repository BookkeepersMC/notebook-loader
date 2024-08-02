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
package net.fabricmc.test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.bookkeepersmc.api.ModInitializer;
import com.bookkeepersmc.loader.api.NotebookLoader;
import com.bookkeepersmc.loader.api.entrypoint.PreLaunchEntrypoint;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public class TestMod implements PreLaunchEntrypoint, ModInitializer {
	/**
	 * Entrypoint implementation for preLaunch.
	 *
	 * <p>Warning: This should normally be in a separate class from later entrypoints to avoid accidentally loading
	 * and/or initializing game classes. This is just trivial test code not meant for production use.
	 */
	@Override
	public void onPreLaunch() {
		if (TestMod.class.getClassLoader() != NotebookLauncherBase.getLauncher().getTargetClassLoader()) {
			throw new IllegalStateException("invalid class loader: "+TestMod.class.getClassLoader());
		}

		Log.info(LogCategory.TEST, "In preLaunch (cl %s)", TestMod.class.getClassLoader());
	}

	@Override
	public void onInitialize() {
		if (TestMod.class.getClassLoader() != NotebookLauncherBase.getLauncher().getTargetClassLoader()) {
			throw new IllegalStateException("invalid class loader: "+TestMod.class.getClassLoader());
		}

		Log.info(LogCategory.TEST, "**************************");
		Log.info(LogCategory.TEST, "Hello from Fabric");
		Log.info(LogCategory.TEST, "**************************");

		Set<CustomEntry> testingInits = new LinkedHashSet<>(NotebookLoader.getInstance().getEntrypoints("test:testing", CustomEntry.class));
		Log.info(LogCategory.TEST, "Found %d testing inits", testingInits.size());
		Log.info(LogCategory.TEST, testingInits.stream().map(CustomEntry::describe).collect(Collectors.joining(", ")));
	}
}
