/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bookkeepersmc.loader.impl.gui;

import java.awt.GraphicsEnvironment;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.game.GameProvider;
import com.bookkeepersmc.loader.impl.gui.NotebookStatusTree.NotebookBasicButtonType;
import com.bookkeepersmc.loader.impl.gui.NotebookStatusTree.NotebookStatusTab;
import com.bookkeepersmc.loader.impl.gui.NotebookStatusTree.NotebookTreeWarningLevel;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.Localization;
import com.bookkeepersmc.loader.impl.util.UrlUtil;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

/** The main entry point for all fabric-based stuff. */
public final class NotebookGuiEntry {
	/** Opens the given {@link NotebookStatusTree} in a new swing window.
	 *
	 * @throws Exception if something went wrong while opening the window. */
	public static void open(NotebookStatusTree tree) throws Exception {
		GameProvider provider = NotebookLoaderImpl.INSTANCE.tryGetGameProvider();

		if (provider == null && LoaderUtil.hasAwtSupport()
				|| provider != null && provider.hasAwtSupport()) {
			NotebookMainWindow.open(tree, true);
		} else {
			openForked(tree);
		}
	}

	private static void openForked(NotebookStatusTree tree) throws IOException, InterruptedException {
		Path javaBinDir = LoaderUtil.normalizePath(Paths.get(System.getProperty("java.home"), "bin"));
		String[] executables = { "javaw.exe", "java.exe", "java" };
		Path javaPath = null;

		for (String executable : executables) {
			Path path = javaBinDir.resolve(executable);

			if (Files.isRegularFile(path)) {
				javaPath = path;
				break;
			}
		}

		if (javaPath == null) throw new RuntimeException("can't find java executable in "+javaBinDir);

		Process process = new ProcessBuilder(javaPath.toString(), "-Xmx100M", "-cp", UrlUtil.LOADER_CODE_SOURCE.toString(), NotebookGuiEntry.class.getName())
				.redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();

		final Thread shutdownHook = new Thread(process::destroy);

		Runtime.getRuntime().addShutdownHook(shutdownHook);

		try (DataOutputStream os = new DataOutputStream(process.getOutputStream())) {
			tree.writeTo(os);
		}

		int rVal = process.waitFor();

		Runtime.getRuntime().removeShutdownHook(shutdownHook);

		if (rVal != 0) throw new IOException("subprocess exited with code "+rVal);
	}

	public static void main(String[] args) throws Exception {
		NotebookStatusTree tree = new NotebookStatusTree(new DataInputStream(System.in));
		NotebookMainWindow.open(tree, true);
		System.exit(0);
	}

	/** @param exitAfter If true then this will call {@link System#exit(int)} after showing the gui, otherwise this will
	 *            return normally. */
	public static void displayCriticalError(Throwable exception, boolean exitAfter) {
		Log.error(LogCategory.GENERAL, "A critical error occurred", exception);

		displayError(Localization.format("gui.error.header"), exception, exitAfter);
	}

	public static void displayError(String mainText, Throwable exception, boolean exitAfter) {
		displayError(mainText, exception, tree -> {
			StringWriter error = new StringWriter();
			error.append(mainText);

			if (exception != null) {
				error.append(System.lineSeparator());
				exception.printStackTrace(new PrintWriter(error));
			}

			tree.addButton(Localization.format("gui.button.copyError"), NotebookBasicButtonType.CLICK_MANY).withClipboard(error.toString());
		}, exitAfter);
	}

	public static void displayError(String mainText, Throwable exception, Consumer<NotebookStatusTree> treeCustomiser, boolean exitAfter) {
		GameProvider provider = NotebookLoaderImpl.INSTANCE.tryGetGameProvider();

		if (!GraphicsEnvironment.isHeadless() && (provider == null || provider.canOpenErrorGui())) {
			String title = "Notebook Loader " + NotebookLoaderImpl.VERSION;
			NotebookStatusTree tree = new NotebookStatusTree(title, mainText);
			NotebookStatusTab crashTab = tree.addTab(Localization.format("gui.tab.crash"));

			if (exception != null) {
				crashTab.node.addCleanedException(exception);
			} else {
				crashTab.node.addMessage(Localization.format("gui.error.missingException"), NotebookTreeWarningLevel.NONE);
			}

			// Maybe add an "open mods folder" button?
			// or should that be part of the main tree's right-click menu?
			tree.addButton(Localization.format("gui.button.exit"), NotebookBasicButtonType.CLICK_ONCE).makeClose();
			treeCustomiser.accept(tree);

			try {
				open(tree);
			} catch (Exception e) {
				if (exitAfter) {
					Log.warn(LogCategory.GENERAL, "Failed to open the error gui!", e);
				} else {
					throw new RuntimeException("Failed to open the error gui!", e);
				}
			}
		}

		if (exitAfter) {
			System.exit(1);
		}
	}
}
