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
package net.fabricmc.loader.language;

import java.io.IOException;

@Deprecated
public interface LanguageAdapter {
	enum MissingSuperclassBehavior {
		RETURN_NULL,
		CRASH
	}

	default Object createInstance(String classString, Options options) throws ClassNotFoundException, LanguageAdapterException {
		try {
			Class<?> c = JavaLanguageAdapter.getClass(classString, options);

			if (c != null) {
				return createInstance(c, options);
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new LanguageAdapterException("I/O error!", e);
		}
	}

	Object createInstance(Class<?> baseClass, Options options) throws LanguageAdapterException;

	class Options {
		private MissingSuperclassBehavior missingSuperclassBehavior;

		public MissingSuperclassBehavior getMissingSuperclassBehavior() {
			return missingSuperclassBehavior;
		}

		public static class Builder {
			private final Options options;

			private Builder() {
				options = new Options();
			}

			public static Builder create() {
				return new Builder();
			}

			public Builder missingSuperclassBehaviour(MissingSuperclassBehavior value) {
				options.missingSuperclassBehavior = value;
				return this;
			}

			public Options build() {
				return options;
			}
		}
	}
}
