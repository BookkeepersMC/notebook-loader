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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bookkeepersmc.loader.impl.lib.gson.JsonReader;
import com.bookkeepersmc.loader.impl.lib.gson.JsonToken;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public final class ModMetadataParser {
	public static final int LATEST_VERSION = 1;
	/**
	 * Keys that will be ignored by any mod metadata parser.
	 */
	public static final Set<String> IGNORED_KEYS = Collections.singleton("$schema");

	// Per the ECMA-404 (www.ecma-international.org/publications/files/ECMA-ST/ECMA-404.pdf), the JSON spec does not prohibit duplicate keys.
	// For all intents and purposes of replicating the logic of Gson's fromJson before we have migrated to JsonReader, duplicate keys will replace previous entries.
	public static LoaderModMetadata parseMetadata(InputStream is, String modPath, List<String> modParentPaths,
			VersionOverrides versionOverrides, DependencyOverrides depOverrides, boolean isDevelopment) throws ParseMetadataException {
		try {
			LoaderModMetadata ret = readModMetadata(is, isDevelopment);

			versionOverrides.apply(ret);
			depOverrides.apply(ret);

			MetadataVerifier.verify(ret, isDevelopment);

			return ret;
		} catch (ParseMetadataException e) {
			e.setModPaths(modPath, modParentPaths);
			throw e;
		} catch (Throwable t) {
			ParseMetadataException e = new ParseMetadataException(t);
			e.setModPaths(modPath, modParentPaths);
			throw e;
		}
	}

	private static LoaderModMetadata readModMetadata(InputStream is, boolean isDevelopment) throws IOException, ParseMetadataException {
		// So some context:
		// Per the json specification, ordering of fields is not typically enforced.
		// Furthermore we cannot guarantee the `schemaVersion` is the first field in every `fabric.mod.json`
		//
		// To work around this, we do the following:
		// Try to read first field
		// If the first field is the schemaVersion, read the file normally.
		//
		// If the first field is not the schema version, fallback to a more exhaustive check.
		// Read the rest of the file, looking for the `schemaVersion` field.
		// If we find the field, cache the value
		// If there happens to be another `schemaVersion` that has a differing value, then fail.
		// At the end, if we find no `schemaVersion` then assume the `schemaVersion` is 0
		// Re-read the JSON file.
		int schemaVersion = 0;

		try (JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			reader.setRewindEnabled(true);

			if (reader.peek() != JsonToken.BEGIN_OBJECT) {
				throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object", reader);
			}

			reader.beginObject();

			boolean firstField = true;

			while (reader.hasNext()) {
				// Try to read the schemaVersion
				String key = reader.nextName();

				if (key.equals("schemaVersion")) {
					if (reader.peek() != JsonToken.NUMBER) {
						throw new ParseMetadataException("\"schemaVersion\" must be a number.", reader);
					}

					schemaVersion = reader.nextInt();

					if (firstField) {
						reader.setRewindEnabled(false);
						// Finish reading the metadata
						LoaderModMetadata ret = readModMetadata(reader, schemaVersion);
						reader.endObject();

						return ret;
					}

					// schemaVersion found, but after some content -> start over to parse all data with the detected version
					break;
				} else {
					reader.skipValue();
				}

				if (!IGNORED_KEYS.contains(key)) {
					firstField = false;
				}
			}

			// Slow path, schema version wasn't specified early enough, re-read with detected/inferred version

			reader.rewind();
			reader.setRewindEnabled(false);

			reader.beginObject();
			LoaderModMetadata ret = readModMetadata(reader, schemaVersion);
			reader.endObject();

			if (isDevelopment) {
				Log.warn(LogCategory.METADATA, "\"fabric.mod.json\" from mod %s did not have \"schemaVersion\" as first field.", ret.getId());
			}

			return ret;
		}
	}

	private static LoaderModMetadata readModMetadata(JsonReader reader, int schemaVersion) throws IOException, ParseMetadataException {
		switch (schemaVersion) {
		case 1:
			return V1ModMetadataParser.parse(reader);
		case 0:
			return V0ModMetadataParser.parse(reader);
		default:
			if (schemaVersion > 0) {
				throw new ParseMetadataException(String.format("This version of fabric-loader doesn't support the newer schema version of \"%s\""
						+ "\nPlease update fabric-loader to be able to read this.", schemaVersion));
			}

			throw new ParseMetadataException(String.format("Invalid/Unsupported schema version \"%s\" was found", schemaVersion));
		}
	}

	static void logWarningMessages(String id, List<ParseWarning> warnings) {
		if (warnings.isEmpty()) return;

		final StringBuilder message = new StringBuilder();

		message.append(String.format("The mod \"%s\" contains invalid entries in its mod json:", id));

		for (ParseWarning warning : warnings) {
			message.append(String.format("\n- %s \"%s\" at line %d column %d",
					warning.getReason(), warning.getKey(), warning.getLine(), warning.getColumn()));
		}

		Log.warn(LogCategory.METADATA, message.toString());
	}

	private ModMetadataParser() {
	}
}
