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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Arguments {
	// set the game version for the builtin game mod/dependencies, bypassing auto-detection
	public static final String GAME_VERSION = SystemProperties.GAME_VERSION;
	// additional mods to load (path separator separated paths, @ prefix for meta-file with each line referencing an actual file)
	public static final String ADD_MODS = SystemProperties.ADD_MODS;

	private final Map<String, String> values;
	private final List<String> extraArgs;

	public Arguments() {
		values = new LinkedHashMap<>();
		extraArgs = new ArrayList<>();
	}

	public Collection<String> keys() {
		return values.keySet();
	}

	public List<String> getExtraArgs() {
		return Collections.unmodifiableList(extraArgs);
	}

	public boolean containsKey(String key) {
		return values.containsKey(key);
	}

	public String get(String key) {
		return values.get(key);
	}

	public String getOrDefault(String key, String value) {
		return values.getOrDefault(key, value);
	}

	public void put(String key, String value) {
		values.put(key, value);
	}

	public void addExtraArg(String value) {
		extraArgs.add(value);
	}

	public void parse(String[] args) {
		parse(Arrays.asList(args));
	}

	public void parse(List<String> args) {
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);

			if (arg.startsWith("--") && i < args.size() - 1) {
				String value = args.get(i + 1);

				if (value.startsWith("--")) {
					// Give arguments that have no value an empty string.
					value = "";
				} else {
					i += 1;
				}

				values.put(arg.substring(2), value);
			} else {
				extraArgs.add(arg);
			}
		}
	}

	public String[] toArray() {
		String[] newArgs = new String[values.size() * 2 + extraArgs.size()];
		int i = 0;

		for (String s : values.keySet()) {
			newArgs[i++] = "--" + s;
			newArgs[i++] = values.get(s);
		}

		for (String s : extraArgs) {
			newArgs[i++] = s;
		}

		return newArgs;
	}

	public String remove(String s) {
		return values.remove(s);
	}
}
