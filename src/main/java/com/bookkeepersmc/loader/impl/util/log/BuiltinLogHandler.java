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
package com.bookkeepersmc.loader.impl.util.log;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;

/**
 * Default LogHandler until Log is initialized.
 *
 * <p>The log handler has the following properties:
 * - log to stdout for anything but LogLevel.ERROR
 * - log to stderr for LogLevel.ERROR
 * - option to relay previous log output to another log handler if requested through Log.init
 * - dumps previous log output to a log file if not closed/relayed yet
 */
final class BuiltinLogHandler extends ConsoleLogHandler {
	private static final String DEFAULT_LOG_FILE = "notebook.log";

	private boolean configured;
	private boolean enableOutput;
	private List<ReplayEntry> buffer = new ArrayList<>();
	private final Thread shutdownHook;

	BuiltinLogHandler() {
		shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	@Override
	public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		boolean output;

		synchronized (this) {
			if (enableOutput) {
				output = true;
			} else if (level.isLessThan(LogLevel.ERROR)) {
				output = false;
			} else {
				startOutput();
				output = true;
			}

			if (buffer != null) {
				buffer.add(new ReplayEntry(time, level, category, msg, exc));
			}
		}

		if (output) super.log(time, level, category, msg, exc, fromReplay, wasSuppressed);
	}

	private void startOutput() {
		if (enableOutput) return;

		if (buffer != null) {
			for (int i = 0; i < buffer.size(); i++) { // index based loop to tolerate replay producing log output by itself
				ReplayEntry entry = buffer.get(i);
				super.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, true);
			}
		}

		enableOutput = true;
	}

	@Override
	public void close() {
		Thread shutdownHook = this.shutdownHook;

		if (shutdownHook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			} catch (IllegalStateException e) {
				// ignore
			}
		}
	}

	synchronized void configure(boolean buffer, boolean output) {
		if (!buffer && !output) throw new IllegalArgumentException("can't both disable buffering and the output");

		if (output) {
			startOutput();
		} else {
			enableOutput = false;
		}

		if (buffer) {
			if (this.buffer == null) this.buffer = new ArrayList<>();
		} else {
			this.buffer = null;
		}

		configured = true;
	}

	synchronized void finishConfig() {
		if (!configured) configure(false, true);
	}

	synchronized boolean replay(LogHandler target) {
		if (buffer == null || buffer.isEmpty()) return false;

		for (int i = 0; i < buffer.size(); i++) { // index based loop to tolerate replay producing log output by itself
			ReplayEntry entry = buffer.get(i);
			target.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, !enableOutput);
		}

		return true;
	}

	private static final class ReplayEntry {
		ReplayEntry(long time, LogLevel level, LogCategory category, String msg, Throwable exc) {
			this.time = time;
			this.level = level;
			this.category = category;
			this.msg = msg;
			this.exc = exc;
		}

		final long time;
		final LogLevel level;
		final LogCategory category;
		final String msg;
		final Throwable exc;
	}

	private final class ShutdownHook extends Thread {
		ShutdownHook() {
			super("BuiltinLogHandler shutdown hook");
		}

		@Override
		public void run() {
			synchronized (BuiltinLogHandler.this) {
				if (buffer == null || buffer.isEmpty()) return;

				if (!enableOutput) {
					enableOutput = true;

					for (int i = 0; i < buffer.size(); i++) { // index based loop to tolerate replay producing log output by itself
						ReplayEntry entry = buffer.get(i);
						BuiltinLogHandler.super.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, true);
					}
				}

				String fileName = System.getProperty(SystemProperties.LOG_FILE, DEFAULT_LOG_FILE);
				if (fileName.isEmpty()) return;

				try {
					Path file = LoaderUtil.normalizePath(Paths.get(fileName));
					Files.createDirectories(file.getParent());

					try (Writer writer = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
						for (int i = 0; i < buffer.size(); i++) { // index based loop to tolerate replay producing log output by itself
							ReplayEntry entry = buffer.get(i);
							writer.write(formatLog(entry.time, entry.level, entry.category, entry.msg, entry.exc));
						}
					}
				} catch (IOException e) {
					System.err.printf("Error saving log: %s", e);
				}
			}
		}
	}
}
