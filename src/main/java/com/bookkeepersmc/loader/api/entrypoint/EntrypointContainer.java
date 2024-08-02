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
package com.bookkeepersmc.loader.api.entrypoint;

import com.bookkeepersmc.loader.api.ModContainer;
import com.bookkeepersmc.loader.api.NotebookLoader;

/**
 * A container holding both an entrypoint instance and the {@link ModContainer} which has provided the entrypoint.
 *
 * @param <T> The type of the entrypoint
 * @see NotebookLoader#getEntrypointContainers(String, Class)
 */
public interface EntrypointContainer<T> {
	/**
	 * Returns the entrypoint instance. It will be constructed the first time you call this method.
	 */
	T getEntrypoint();

	/**
	 * Returns the mod that provided this entrypoint.
	 */
	ModContainer getProvider();
}
