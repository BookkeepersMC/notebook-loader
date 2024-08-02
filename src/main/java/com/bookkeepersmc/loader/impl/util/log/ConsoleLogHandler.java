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

import java.io.PrintWriter;
import java.io.StringWriter;

public class ConsoleLogHandler implements LogHandler {
	private static final LogLevel MIN_STDERR_LEVEL = LogLevel.ERROR;
	private static final LogLevel MIN_STDOUT_LEVEL = LogLevel.getDefault();

	@Override
	public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		String formatted = formatLog(time, level, category, msg, exc);

		if (level.isLessThan(MIN_STDERR_LEVEL)) {
			System.out.print(formatted);
		} else {
			System.err.print(formatted);
		}
	}

	protected static String formatLog(long time, LogLevel level, LogCategory category, String msg, Throwable exc) {
		String ret = String.format("[%tT] [%s] [%s/%s]: %s%n", time, level.name(), category.context, category.name, msg);

		if (exc != null) {
			StringWriter writer = new StringWriter(ret.length() + 500);

			try (PrintWriter pw = new PrintWriter(writer, false)) {
				pw.print(ret);
				exc.printStackTrace(pw);
			}

			ret = writer.toString();
		}

		return ret;
	}

	@Override
	public boolean shouldLog(LogLevel level, LogCategory category) {
		return !level.isLessThan(MIN_STDOUT_LEVEL);
	}

	@Override
	public void close() { }
}
