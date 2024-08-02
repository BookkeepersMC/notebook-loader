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
package com.bookkeepersmc.loader.api.metadata;

import java.util.Map;

/**
 * Represents a custom value in the {@code fabric.mod.json}.
 */
public interface CustomValue {
	/**
	 * Returns the type of the value.
	 */
	CvType getType();

	/**
	 * Returns this value as an {@link CvType#OBJECT}.
	 *
	 * @return this value
	 * @throws ClassCastException if this value is not an object
	 */
	CvObject getAsObject();

	/**
	 * Returns this value as an {@link CvType#ARRAY}.
	 *
	 * @return this value
	 * @throws ClassCastException if this value is not an array
	 */
	CvArray getAsArray();

	/**
	 * Returns this value as a {@link CvType#STRING}.
	 *
	 * @return this value
	 * @throws ClassCastException if this value is not a string
	 */
	String getAsString();

	/**
	 * Returns this value as a {@link CvType#NUMBER}.
	 *
	 * @return this value
	 * @throws ClassCastException if this value is not a number
	 */
	Number getAsNumber();

	/**
	 * Returns this value as a {@link CvType#BOOLEAN}.
	 *
	 * @return this value
	 * @throws ClassCastException if this value is not a boolean
	 */
	boolean getAsBoolean();

	/**
	 * Represents an {@link CvType#OBJECT} value.
	 */
	interface CvObject extends CustomValue, Iterable<Map.Entry<String, CustomValue>> {
		/**
		 * Returns the number of key-value pairs within this object value.
		 */
		int size();

		/**
		 * Returns whether a {@code key} is present within this object value.
		 *
		 * @param key the key to check
		 * @return whether the key is present
		 */
		boolean containsKey(String key);

		/**
		 * Gets the value associated with a {@code key} within this object value.
		 *
		 * @param key the key to check
		 * @return the value associated, or {@code null} if no such value is present
		 */
		CustomValue get(String key);
	}

	/**
	 * Represents an {@link CvType#ARRAY} value.
	 */
	interface CvArray extends CustomValue, Iterable<CustomValue> {
		/**
		 * Returns the number of values within this array value.
		 */
		int size();

		/**
		 * Gets the value at {@code index} within this array value.
		 *
		 * @param index the index of the value
		 * @return the value associated
		 * @throws IndexOutOfBoundsException if the index is not within {{@link #size()}}
		 */
		CustomValue get(int index);
	}

	/**
	 * The possible types of a custom value.
	 */
	enum CvType {
		OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL;
	}
}
