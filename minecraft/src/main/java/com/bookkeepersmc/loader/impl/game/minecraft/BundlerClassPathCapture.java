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

package com.bookkeepersmc.loader.impl.game.minecraft;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;

public final class BundlerClassPathCapture {
	static final CompletableFuture<URL[]> FUTURE = new CompletableFuture<>();

	public static void main(String[] args) { // invoked by the bundler on a thread
		try {
			URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
			FUTURE.complete(cl.getURLs());
		} catch (Throwable t) {
			FUTURE.completeExceptionally(t);
		}
	}
}
