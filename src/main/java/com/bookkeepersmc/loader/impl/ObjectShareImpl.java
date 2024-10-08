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
package com.bookkeepersmc.loader.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.bookkeepersmc.loader.api.ObjectShare;

final class ObjectShareImpl implements ObjectShare {
	private final Map<String, Object> values = new HashMap<>();
	private final Map<String, List<BiConsumer<String, Object>>> pendingMap = new HashMap<>();

	@Override
	public synchronized Object get(String key) {
		validateKey(key);

		return values.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		validateKey(key);
		Objects.requireNonNull(value, "null value");

		List<BiConsumer<String, Object>> pending;

		synchronized (this) {
			Object prev = values.put(key, value);
			if (prev != null) return prev; // no new entry -> can't have pending entries for it

			pending = pendingMap.remove(key);
		}

		if (pending != null) invokePending(key, value, pending);

		return null;
	}

	@Override
	public Object putIfAbsent(String key, Object value) {
		validateKey(key);
		Objects.requireNonNull(value, "null value");

		List<BiConsumer<String, Object>> pending;

		synchronized (this) {
			Object prev = values.putIfAbsent(key, value);
			if (prev != null) return prev; // no new entry -> can't have pending entries for it

			pending = pendingMap.remove(key);
		}

		if (pending != null) invokePending(key, value, pending);

		return null;
	}

	@Override
	public synchronized Object remove(String key) {
		validateKey(key);

		return values.remove(key);
	}

	@Override
	public void whenAvailable(String key, BiConsumer<String, Object> consumer) {
		validateKey(key);

		Object value;

		synchronized (this) {
			value = values.get(key);

			if (value == null) { // value doesn't exist yet, queue invocation for when it gets added
				pendingMap.computeIfAbsent(key, ignore -> new ArrayList<>()).add(consumer);
				return;
			}
		}

		// value exists already, invoke directly
		consumer.accept(key, value);
	}

	private static void validateKey(String key) {
		Objects.requireNonNull(key, "null key");

		int pos = key.indexOf(':');
		if (pos <= 0 || pos >= key.length() - 1) throw new IllegalArgumentException("invalid key, must be modid:subkey");
	}

	private static void invokePending(String key, Object value, List<BiConsumer<String, Object>> pending) {
		for (BiConsumer<String, Object> consumer : pending) {
			consumer.accept(key, value);
		}
	}
}
