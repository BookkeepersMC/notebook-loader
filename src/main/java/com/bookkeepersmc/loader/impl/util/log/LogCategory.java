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
package com.bookkeepersmc.loader.impl.util.log;

public final class LogCategory {
	public static final LogCategory DISCOVERY = create("Discovery");
	public static final LogCategory ENTRYPOINT = create("Entrypoint");
	public static final LogCategory GAME_PATCH = create("GamePatch");
	public static final LogCategory GAME_PROVIDER = create("GameProvider");
	public static final LogCategory GAME_REMAP = create("GameRemap");
	public static final LogCategory GENERAL = create();
	public static final LogCategory KNOT = create("Knot");
	public static final LogCategory LIB_CLASSIFICATION = create("LibClassify");
	public static final LogCategory LOG = create("Log");
	public static final LogCategory MAPPINGS = create("Mappings");
	public static final LogCategory METADATA = create("Metadata");
	public static final LogCategory MOD_REMAP = create("ModRemap");
	public static final LogCategory MIXIN = create("Mixin");
	public static final LogCategory RESOLUTION = create("Resolution");
	public static final LogCategory TEST = create("Test");

	public static final String SEPARATOR = "/";

	public final String context;
	public final String name;
	public Object data;

	public static LogCategory create(String... names) {
		return new LogCategory(Log.NAME, names);
	}

	/**
	 * Create a log category for external uses, no API guarantees!
	 */
	public static LogCategory createCustom(String context, String... names) {
		return new LogCategory(context, names);
	}

	private LogCategory(String context, String[] names) {
		this.context = context;
		this.name = String.join(SEPARATOR, names);
	}

	@Override
	public String toString() {
		return name.isEmpty() ? context : context+SEPARATOR+name;
	}
}
