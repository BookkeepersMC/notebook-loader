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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class Localization {
	public static final ResourceBundle BUNDLE = createBundle("net.fabricmc.loader.Messages", Locale.getDefault());
	public static final ResourceBundle ROOT_LOCALE_BUNDLE = createBundle("net.fabricmc.loader.Messages", Locale.ROOT);

	public static String format(String key, Object... args) {
		String pattern = BUNDLE.getString(key);

		if (args.length == 0) {
			return pattern;
		} else {
			return MessageFormat.format(pattern, args);
		}
	}

	public static String formatRoot(String key, Object... args) {
		String pattern = ROOT_LOCALE_BUNDLE.getString(key);

		if (args.length == 0) {
			return pattern;
		} else {
			return MessageFormat.format(pattern, args);
		}
	}

	private static ResourceBundle createBundle(String name, Locale locale) {
		if (System.getProperty("java.version", "").startsWith("1.")) { // below java 9
			return ResourceBundle.getBundle(name, locale, new ResourceBundle.Control() {
				@Override
				public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
						throws IllegalAccessException, InstantiationException, IOException {
					if (format.equals("java.properties")) {
						InputStream is = loader.getResourceAsStream(toResourceName(toBundleName(baseName, locale), "properties"));

						if (is != null) {
							try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
								return new PropertyResourceBundle(reader);
							}
						}
					}

					return super.newBundle(baseName, locale, format, loader, reload);
				};
			});
		} else { // java 9 and later
			return ResourceBundle.getBundle(name, locale);
		}
	}
}
