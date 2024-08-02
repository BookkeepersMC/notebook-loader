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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.version.VersionParser;

public final class VersionOverrides {
	private final Map<String, Version> replacements = new HashMap<>();

	public VersionOverrides() {
		String property = System.getProperty(SystemProperties.DEBUG_REPLACE_VERSION);
		if (property == null) return;

		for (String entry : property.split(",")) {
			int pos = entry.indexOf(":");
			if (pos <= 0 || pos >= entry.length() - 1) throw new RuntimeException("invalid version replacement entry: "+entry);

			String id = entry.substring(0, pos);
			String rawVersion = entry.substring(pos + 1);
			Version version;

			try {
				version = VersionParser.parse(rawVersion, false);
			} catch (VersionParsingException e) {
				throw new RuntimeException(String.format("Invalid replacement version for mod %s: %s", id, rawVersion), e);
			}

			replacements.put(id, version);
		}
	}

	public void apply(LoaderModMetadata metadata) {
		if (replacements.isEmpty()) return;

		Version replacement = replacements.get(metadata.getId());

		if (replacement != null) {
			metadata.setVersion(replacement);
		}
	}

	public Collection<String> getAffectedModIds() {
		return replacements.keySet();
	}
}
