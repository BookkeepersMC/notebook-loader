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

public final class StringUtil {
	public static String capitalize(String s) {
		if (s.isEmpty()) return s;

		int pos;

		for (pos = 0; pos < s.length(); pos++) {
			if (Character.isLetterOrDigit(s.codePointAt(pos))) {
				break;
			}
		}

		if (pos == s.length()) return s;

		int cp = s.codePointAt(pos);
		int cpUpper = Character.toUpperCase(cp);
		if (cpUpper == cp) return s;

		StringBuilder ret = new StringBuilder(s.length());
		ret.append(s, 0, pos);
		ret.appendCodePoint(cpUpper);
		ret.append(s, pos + Character.charCount(cp), s.length());

		return ret.toString();
	}

	public static String[] splitNamespaced(String s, String defaultNamespace) {
		int i = s.indexOf(':');

		if (i >= 0) {
			return new String[] { s.substring(0, i), s.substring(i + 1) };
		} else {
			return new String[] { defaultNamespace, s };
		}
	}

	public static String wrapLines(String str, int limit) {
		if (str.length() < limit) return str;

		StringBuilder sb = new StringBuilder(str.length() + 20);
		int lastSpace = -1;
		int len = 0;

		for (int i = 0, max = str.length(); i <= max; i++) {
			char c = i < max ? str.charAt(i) : ' ';

			if (c == '\r') {
				// ignore
			} else if (c == '\n') {
				lastSpace = sb.length();
				sb.append(c);
				len = 0;
			} else if (Character.isWhitespace(c)) {
				if (len > limit && lastSpace >= 0) {
					sb.setCharAt(lastSpace, '\n');
					len = sb.length() - lastSpace - 1;
				}

				if (i == max) break;

				if (len >= limit) {
					lastSpace = -1;
					sb.append('\n');
					len = 0;
				} else {
					lastSpace = sb.length();
					sb.append(c);
					len++;
				}
			} else if (c == '"' || c == '\'') {
				int next = str.indexOf(c, i + 1) + 1;
				if (next <= 0) next = str.length();
				sb.append(str, i, next);
				len += next - i;
				i = next - 1;
			} else {
				sb.append(c);
				len++;
			}
		}

		return sb.toString();
	}
}
