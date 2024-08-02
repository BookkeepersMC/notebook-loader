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

import com.bookkeepersmc.loader.impl.util.Localization;

@SuppressWarnings("serial")
public final class FormattedException extends RuntimeException {
	private final String mainText;
	private String translatedText;

	public FormattedException(String mainText, String message) {
		super(message);

		this.mainText = mainText;
	}

	public FormattedException(String mainText, String format, Object... args) {
		super(String.format(format, args));

		this.mainText = mainText;
	}

	public FormattedException(String mainText, String message, Throwable cause) {
		super(message, cause);

		this.mainText = mainText;
	}

	public FormattedException(String mainText, Throwable cause) {
		super(cause);

		this.mainText = mainText;
	}

	public static FormattedException ofLocalized(String key, String message) {
		return new FormattedException(Localization.formatRoot(key), message).addTranslation(key);
	}

	public static FormattedException ofLocalized(String key, String format, Object... args) {
		return new FormattedException(Localization.formatRoot(key), format, args).addTranslation(key);
	}

	public static FormattedException ofLocalized(String key, String message, Throwable cause) {
		return new FormattedException(Localization.formatRoot(key), message, cause).addTranslation(key);
	}

	public static FormattedException ofLocalized(String key, Throwable cause) {
		return new FormattedException(Localization.formatRoot(key), cause).addTranslation(key);
	}

	public String getMainText() {
		return mainText;
	}

	public String getDisplayedText() {
		return translatedText == null || translatedText.equals(mainText) ? mainText : translatedText + " (" + mainText + ")";
	}

	private FormattedException addTranslation(String key) {
		this.translatedText = Localization.format(key);
		return this;
	}
}
