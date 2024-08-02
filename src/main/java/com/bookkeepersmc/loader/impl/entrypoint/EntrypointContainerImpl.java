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
package com.bookkeepersmc.loader.impl.entrypoint;

import com.bookkeepersmc.loader.api.EntrypointException;
import com.bookkeepersmc.loader.api.ModContainer;
import com.bookkeepersmc.loader.api.entrypoint.EntrypointContainer;

public final class EntrypointContainerImpl<T> implements EntrypointContainer<T> {
	private final String key;
	private final Class<T> type;
	private final EntrypointStorage.Entry entry;
	private T instance;

	/**
	 * Create EntrypointContainer with lazy init.
	 */
	public EntrypointContainerImpl(String key, Class<T> type, EntrypointStorage.Entry entry) {
		this.key = key;
		this.type = type;
		this.entry = entry;
	}

	/**
	 * Create EntrypointContainer without lazy init.
	 */
	public EntrypointContainerImpl(EntrypointStorage.Entry entry, T instance) {
		this.key = null;
		this.type = null;
		this.entry = entry;
		this.instance = instance;
	}

	@SuppressWarnings("deprecation")
	@Override
	public synchronized T getEntrypoint() {
		if (instance == null) {
			try {
				instance = entry.getOrCreate(type);
				assert instance != null;
			} catch (Exception ex) {
				throw new EntrypointException(key, getProvider().getMetadata().getId(), ex);
			}
		}

		return instance;
	}

	@Override
	public ModContainer getProvider() {
		return entry.getModContainer();
	}
}
