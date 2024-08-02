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

import java.util.function.BiConsumer;

/**
 * Object share for inter-mod communication, obtainable through {@link NotebookLoader#getObjectShare}.
 *
 * <p>The share allows mods to exchange data without directly referencing each other. This makes simple interaction
 * easier by eliminating any compile- or run-time dependencies if the shared value type is independent of the mod
 * (only Java/game/Fabric types like collections, primitives, String, Consumer, Function, ...).
 *
 * <p>Active interaction is possible as well since the shared values can be arbitrary Java objects. For example
 * exposing a {@code Runnable} or {@code Function} allows the "API" user to directly invoke some program logic.
 *
 * <p>It is required to prefix the share key with the mod id like {@code mymod:someProperty}. Mods should not
 * modify entries by other mods. The share is thread safe.
 */
public interface ObjectShare {
	/**
	 * Get the value for a specific key.
	 *
	 * <p>Java 16 introduced a convenient syntax for type safe queries that combines null check, type check and cast:
	 * <pre>
	 * if (FabricLoader.getInstance().getObjectShare().get("someMod:someValue") instanceof String value) {
	 *   // use value here
	 * }
	 * </pre>
	 *
	 * <p>A generic type still needs a second unchecked cast due to erasure:
	 * <pre>
	 * if (FabricLoader.getInstance().getObjectShare().get("mymod:fuel") instanceof Consumer{@code<?>} c) {
	 *   ((Consumer{@code<ItemStack>}) c).accept(someStack);
	 * }
	 * </pre>
	 *
	 * <p>Consider using {@link #whenAvailable} instead if the value may not be available yet. The mod load order is
	 * undefined, so entries that are added during the same load phase should be queried in a later phase or be handled
	 * through {@link whenAvailable}.
	 *
	 * @param key key to query, format {@code modid:subkey}
	 * @return value associated with the key or null if none
	 */
	Object get(String key);

	/**
	 * Request being notified when a key/value becomes available.
	 *
	 * <p>This is primarily intended to resolve load order issues, when there is no good time to call {@link get}.
	 *
	 * <p>If there is already a value associated with the {@code key}, the consumer will be invoked directly, otherwise
	 * when one of the {@code put} methods adds a value for the key. The invocation happens on the thread calling
	 * {@link #whenAvailable} or on whichever thread calls {@code put} with the same {@code key}.
	 *
	 * <p>The request will only act once, not if the value changes again.
	 *
	 * <p>Example use:
	 * <pre>
	 * FabricLoader.getInstance().getObjectShare().whenAvailable("someMod:someValue", (k, v) -> {
	 *   if (v instanceof String value) {
	 *     // use value
	 *   }
	 * });
	 * </pre>
	 *
	 * @param key key to react upon, format {@code modid:subkey}
	 * @paran consumer consumer receiving the key/value pair: key first, value second
	 */
	void whenAvailable(String key, BiConsumer<String, Object> consumer);

	/**
	 * Set the value for a specific key.
	 *
	 * @param key key to add a value for, format {@code modid:subkey}
	 * @param value value to add, must not be null
	 * @return previous value associated with the key, null if none
	 */
	Object put(String key, Object value);

	/**
	 * Set the value for a specific key if there isn't one yet.
	 *
	 * <p>This is an atomic operation, thus thread safe contrary to using get+put.
	 *
	 * @param key key to add a value for, format {@code modid:subkey}
	 * @param value value to add, must not be null
	 * @return previous value associated with the key, null if none and thus the entry changed
	 */
	Object putIfAbsent(String key, Object value);

	/**
	 * Remove the value for a specific key.
	 *
	 * @param key key to remove the value for, format {@code modid:subkey}
	 * @return previous value associated with the key, null if none
	 */
	Object remove(String key);
}
