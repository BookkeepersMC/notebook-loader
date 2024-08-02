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
package com.bookkeepersmc.loader.api;

/**
 * Represents an exception that arises when obtaining entrypoints.
 *
 * @see NotebookLoader#getEntrypointContainers(String, Class)
 */
@SuppressWarnings("serial")
public class EntrypointException extends RuntimeException {
	private final String key;

	/**
	 * @deprecated For internal use only, to be removed!
	 */
	@Deprecated
	public EntrypointException(String key, Throwable cause) {
		super("Exception while loading entries for entrypoint '" + key + "'!", cause);
		this.key = key;
	}

	/**
	 * @deprecated For internal use only, use regular exceptions!
	 */
	@Deprecated
	public EntrypointException(String key, String causingMod, Throwable cause) {
		super("Exception while loading entries for entrypoint '" + key + "' provided by '" + causingMod + "'", cause);
		this.key = key;
	}

	/**
	 * @deprecated For internal use only, to be removed!
	 */
	@Deprecated
	public EntrypointException(String s) {
		super(s);
		this.key = "";
	}

	/**
	 * @deprecated For internal use only, to be removed!
	 */
	@Deprecated
	public EntrypointException(Throwable t) {
		super(t);
		this.key = "";
	}

	/**
	 * Returns the key of entrypoint in which the exception arose.
	 *
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
}
