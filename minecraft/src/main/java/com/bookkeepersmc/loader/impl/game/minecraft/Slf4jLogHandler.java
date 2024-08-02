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
package com.bookkeepersmc.loader.impl.game.minecraft;

import com.bookkeepersmc.loader.impl.util.log.LogCategory;
import com.bookkeepersmc.loader.impl.util.log.LogHandler;
import com.bookkeepersmc.loader.impl.util.log.LogLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4jLogHandler implements LogHandler {
	@Override
	public boolean shouldLog(LogLevel level, LogCategory category) {
		Logger logger = getLogger(category);

		switch (level) {
		case ERROR: return logger.isErrorEnabled();
		case WARN: return logger.isWarnEnabled();
		case INFO: return logger.isInfoEnabled();
		case DEBUG: return logger.isDebugEnabled();
		case TRACE: return logger.isTraceEnabled();
		}

		throw new IllegalArgumentException("unknown level: "+level);
	}

	@Override
	public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		Logger logger = getLogger(category);

		if (msg == null) {
			if (exc == null) return;
			msg = "Exception";
		}

		switch (level) {
		case ERROR:
			if (exc == null) {
				logger.error(msg);
			} else {
				logger.error(msg, exc);
			}

			break;
		case WARN:
			if (exc == null) {
				logger.warn(msg);
			} else {
				logger.warn(msg, exc);
			}

			break;
		case INFO:
			if (exc == null) {
				logger.info(msg);
			} else {
				logger.info(msg, exc);
			}

			break;
		case DEBUG:
			if (exc == null) {
				logger.debug(msg);
			} else {
				logger.debug(msg, exc);
			}

			break;
		case TRACE:
			if (exc == null) {
				logger.trace(msg);
			} else {
				logger.trace(msg, exc);
			}

			break;
		default:
			throw new IllegalArgumentException("unknown level: "+level);
		}
	}

	private static Logger getLogger(LogCategory category) {
		Logger ret = (Logger) category.data;

		if (ret == null) {
			category.data = ret = LoggerFactory.getLogger(category.toString());
		}

		return ret;
	}

	@Override
	public void close() { }
}
