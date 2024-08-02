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
package com.bookkeepersmc.loader.impl.util;

import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public final class ExceptionUtil {
	private static final boolean THROW_DIRECTLY = System.getProperty(SystemProperties.DEBUG_THROW_DIRECTLY) != null;

	public static <T extends Throwable> T gatherExceptions(Throwable exc, T prev, Function<Throwable, T> mainExcFactory) throws T {
		exc = unwrap(exc);

		if (THROW_DIRECTLY) throw mainExcFactory.apply(exc);

		if (prev == null) {
			return mainExcFactory.apply(exc);
		} else if (exc != prev) {
			for (Throwable t : prev.getSuppressed()) {
				if (exc.equals(t)) return prev;
			}

			prev.addSuppressed(exc);
		}

		return prev;
	}

	public static RuntimeException wrap(Throwable exc) {
		if (exc instanceof RuntimeException) return (RuntimeException) exc;

		exc = unwrap(exc);
		if (exc instanceof RuntimeException) return (RuntimeException) exc;

		return new WrappedException(exc);
	}

	private static Throwable unwrap(Throwable exc) {
		if (exc instanceof WrappedException
				|| exc instanceof UncheckedIOException
				|| exc instanceof ExecutionException
				|| exc instanceof CompletionException) {
			Throwable ret = exc.getCause();
			if (ret != null) return unwrap(ret);
		}

		return exc;
	}

	@SuppressWarnings("serial")
	public static final class WrappedException extends RuntimeException {
		public WrappedException(Throwable cause) {
			super(cause);
		}
	}
}
