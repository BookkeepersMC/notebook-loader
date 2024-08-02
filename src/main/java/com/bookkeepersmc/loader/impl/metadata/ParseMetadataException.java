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
package com.bookkeepersmc.loader.impl.metadata;

import java.util.ArrayList;
import java.util.List;

import com.bookkeepersmc.loader.impl.lib.gson.JsonReader;

@SuppressWarnings("serial")
public class ParseMetadataException extends Exception {
	private List<String> modPaths;

	public ParseMetadataException(String message) {
		super(message);
	}

	public ParseMetadataException(String message, JsonReader reader) {
		this(message + " Error was located at: " + reader.locationString());
	}

	public ParseMetadataException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ParseMetadataException(Throwable t) {
		super(t);
	}

	public void setModPaths(String modPath, List<String> modParentPaths) {
		modPaths = new ArrayList<>(modParentPaths);
		modPaths.add(modPath);
	}

	@Override
	public String getMessage() {
		String ret = "Error reading fabric.mod.json file for mod at ";

		if (modPaths == null) {
			ret += "unknown location";
		} else {
			ret += String.join(" -> ", modPaths);
		}

		String msg = super.getMessage();

		if (msg != null) {
			ret += ": "+msg;
		}

		return ret;
	}

	public static class MissingField extends ParseMetadataException {
		public MissingField(String field) {
			super(String.format("Missing required field \"%s\".", field));
		}
	}
}
