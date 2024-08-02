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

import com.bookkeepersmc.loader.api.NotebookLoader;

/**
 * Entrypoint getting invoked just before launching the game.
 *
 * <p><b>Avoid interfering with the game from this!</b> Accessing anything needs careful consideration to avoid
 * interfering with its own initialization or otherwise harming its state. It is recommended to implement this interface
 * on its own class to avoid running static initializers too early, e.g. because they were referenced in field or method
 * signatures in the same class.
 *
 * <p>The entrypoint is exposed with {@code preLaunch} key in the mod json and runs for any environment. It usually
 * executes several seconds before the {@code main}/{@code client}/{@code server} entrypoints.
 *
 * @see NotebookLoader#getEntrypointContainers(String, Class)
 */
@FunctionalInterface
public interface PreLaunchEntrypoint {
	/**
	 * Runs the entrypoint.
	 */
	void onPreLaunch();
}
