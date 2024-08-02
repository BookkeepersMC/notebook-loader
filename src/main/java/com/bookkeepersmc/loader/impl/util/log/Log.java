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

import java.util.Arrays;
import java.util.IllegalFormatException;

public final class Log {
	public static final String NAME = "Notebook";
	private static final boolean CHECK_FOR_BRACKETS = true;

	private static LogHandler handler = new BuiltinLogHandler();

	public static void init(LogHandler handler) {
		if (handler == null) throw new NullPointerException("null log handler");

		LogHandler oldHandler = Log.handler;

		if (oldHandler instanceof BuiltinLogHandler) {
			((BuiltinLogHandler) oldHandler).replay(handler);
		}

		Log.handler = handler;
		oldHandler.close();
	}

	/**
	 * Configure builtin log handler.
	 *
	 * @param buffer whether to buffer log messages for later replaying
	 * @param output whether to output log messages directly
	 */
	public static void configureBuiltin(boolean buffer, boolean output) {
		LogHandler handler = Log.handler;

		if (handler instanceof BuiltinLogHandler) {
			((BuiltinLogHandler) handler).configure(buffer, output);
		}
	}

	/**
	 * Finish configuring builtin log handler, using defaults if unconfigured.
	 */
	public static void finishBuiltinConfig() {
		LogHandler handler = Log.handler;

		if (handler instanceof BuiltinLogHandler) {
			((BuiltinLogHandler) handler).finishConfig();
		}
	}

	public static void error(LogCategory category, String format, Object... args) {
		logFormat(LogLevel.ERROR, category, format, args);
	}

	public static void error(LogCategory category, String msg) {
		log(LogLevel.ERROR, category, msg);
	}

	public static void error(LogCategory category, String msg, Throwable exc) {
		log(LogLevel.ERROR, category, msg, exc);
	}

	public static void warn(LogCategory category, String format, Object... args) {
		logFormat(LogLevel.WARN, category, format, args);
	}

	public static void warn(LogCategory category, String msg) {
		log(LogLevel.WARN, category, msg);
	}

	public static void warn(LogCategory category, String msg, Throwable exc) {
		log(LogLevel.WARN, category, msg, exc);
	}

	public static void info(LogCategory category, String format, Object... args) {
		logFormat(LogLevel.INFO, category, format, args);
	}

	public static void info(LogCategory category, String msg) {
		log(LogLevel.INFO, category, msg);
	}

	public static void info(LogCategory category, String msg, Throwable exc) {
		log(LogLevel.INFO, category, msg, exc);
	}

	public static void debug(LogCategory category, String format, Object... args) {
		logFormat(LogLevel.DEBUG, category, format, args);
	}

	public static void debug(LogCategory category, String msg) {
		log(LogLevel.DEBUG, category, msg);
	}

	public static void debug(LogCategory category, String msg, Throwable exc) {
		log(LogLevel.DEBUG, category, msg, exc);
	}

	public static void trace(LogCategory category, String format, Object... args) {
		logFormat(LogLevel.TRACE, category, format, args);
	}

	public static void trace(LogCategory category, String msg) {
		log(LogLevel.TRACE, category, msg);
	}

	public static void trace(LogCategory category, String msg, Throwable exc) {
		log(LogLevel.TRACE, category, msg, exc);
	}

	public static void log(LogLevel level, LogCategory category, String msg) {
		LogHandler handler = Log.handler;
		if (handler.shouldLog(level, category)) log(handler, level, category, msg, null);
	}

	public static void log(LogLevel level, LogCategory category, String msg, Throwable exc) {
		LogHandler handler = Log.handler;
		if (handler.shouldLog(level, category)) log(handler, level, category, msg, exc);
	}

	public static void logFormat(LogLevel level, LogCategory category, String format, Object... args) {
		LogHandler handler = Log.handler;
		if (!handler.shouldLog(level, category)) return;

		String msg;
		Throwable exc;

		if (args.length == 0) {
			//assert getRequiredArgs(format.toString()) == 0;

			msg = format;
			exc = null;
		} else {
			if (CHECK_FOR_BRACKETS) {
				if (format.indexOf("{}") != -1) throw new IllegalArgumentException("log message containing {}: "+format);
			}

			Object lastArg = args[args.length - 1];
			Object[] newArgs;

			if (lastArg instanceof Throwable && getRequiredArgs(format) < args.length) {
				exc = (Throwable) lastArg;
				newArgs = Arrays.copyOf(args, args.length - 1);
			} else {
				exc = null;
				newArgs = args;
			}

			assert getRequiredArgs(format) == newArgs.length;

			try {
				msg = String.format(format, newArgs);
			} catch (IllegalFormatException e) {
				msg = "Format error: fmt=["+format+"] args="+Arrays.toString(args);
				warn(LogCategory.LOG, "Invalid format string.", e);
			}
		}

		log(handler, level, category, msg, exc);
	}

	private static int getRequiredArgs(String format) {
		int ret = 0;
		int minRet = 0;
		boolean wasPct = false;

		for (int i = 0, max = format.length(); i < max; i++) {
			char c = format.charAt(i);

			if (c == '%') {
				wasPct = !wasPct;
			} else if (wasPct) {
				wasPct = false;

				if (c == 'n' || c == '<') { // not %n or %<x
					continue;
				}

				if (c >= '0' && c <= '9') { // abs indexing %12$
					int start = i;

					while (i + 1 < format.length()
							&& (c = format.charAt(i + 1)) >= '0' && c <= '9') {
						i++;
					}

					if (i + 1 < format.length() && format.charAt(i + 1) == '$') {
						i++;
						minRet = Math.max(minRet, Integer.parseInt(format.substring(start, i)) + 1);
						continue;
					} else {
						i = start;
					}
				}

				ret++;
			}
		}

		return Math.max(ret, minRet);
	}

	private static void log(LogHandler handler, LogLevel level, LogCategory category, String msg, Throwable exc) {
		handler.log(System.currentTimeMillis(), level, category, msg.trim(), exc, false, false);
	}

	public static boolean shouldLog(LogLevel level, LogCategory category) {
		return handler.shouldLog(level, category);
	}
}
