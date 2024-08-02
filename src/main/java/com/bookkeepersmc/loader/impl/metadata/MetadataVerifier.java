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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.impl.discovery.ModCandidate;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public final class MetadataVerifier {
	private static final Pattern MOD_ID_PATTERN = Pattern.compile("[a-z][a-z0-9-_]{1,63}");

	public static ModCandidate verifyIndev(ModCandidate mod, boolean isDevelopment) {
		if (isDevelopment) {
			try {
				MetadataVerifier.verify(mod.getMetadata(), isDevelopment);
			} catch (ParseMetadataException e) {
				e.setModPaths(mod.getLocalPath(), Collections.emptyList());
				throw new RuntimeException("Invalid mod metadata", e);
			}
		}

		return mod;
	}

	static void verify(LoaderModMetadata metadata, boolean isDevelopment) throws ParseMetadataException {
		checkModId(metadata.getId(), "mod id");

		for (String providesDecl : metadata.getProvides()) {
			checkModId(providesDecl, "provides declaration");
		}

		// TODO: verify mod id and version decls in deps

		if (isDevelopment) {
			if (metadata.getSchemaVersion() < ModMetadataParser.LATEST_VERSION) {
				Log.warn(LogCategory.METADATA, "Mod %s uses an outdated schema version: %d < %d", metadata.getId(), metadata.getSchemaVersion(), ModMetadataParser.LATEST_VERSION);
			}
		}

		if (!(metadata.getVersion() instanceof SemanticVersion)) {
			String version = metadata.getVersion().getFriendlyString();
			VersionParsingException exc;

			try {
				SemanticVersion.parse(version);
				exc = null;
			} catch (VersionParsingException e) {
				exc = e;
			}

			if (exc != null) {
				Log.warn(LogCategory.METADATA, "Mod %s uses the version %s which isn't compatible with Loader's extended semantic version format (%s), SemVer is recommended for reliably evaluating dependencies and prioritizing newer version",
						metadata.getId(), version, exc.getMessage());
			}

			metadata.emitFormatWarnings();
		}
	}

	private static void checkModId(String id, String name) throws ParseMetadataException {
		if (MOD_ID_PATTERN.matcher(id).matches()) return;

		List<String> errorList = new ArrayList<>();

		// A more useful error list for MOD_ID_PATTERN
		if (id.isEmpty()) {
			errorList.add("is empty!");
		} else {
			if (id.length() == 1) {
				errorList.add("is only a single character! (It must be at least 2 characters long)!");
			} else if (id.length() > 64) {
				errorList.add("has more than 64 characters!");
			}

			char first = id.charAt(0);

			if (first < 'a' || first > 'z') {
				errorList.add("starts with an invalid character '" + first + "' (it must be a lowercase a-z - uppercase isn't allowed anywhere in the ID)");
			}

			Set<Character> invalidChars = null;

			for (int i = 1; i < id.length(); i++) {
				char c = id.charAt(i);

				if (c == '-' || c == '_' || ('0' <= c && c <= '9') || ('a' <= c && c <= 'z')) {
					continue;
				}

				if (invalidChars == null) {
					invalidChars = new HashSet<>();
				}

				invalidChars.add(c);
			}

			if (invalidChars != null) {
				StringBuilder error = new StringBuilder("contains invalid characters: ");
				error.append(invalidChars.stream().map(value -> "'" + value + "'").collect(Collectors.joining(", ")));
				errorList.add(error.append("!").toString());
			}
		}

		assert !errorList.isEmpty();

		StringWriter sw = new StringWriter();

		try (PrintWriter pw = new PrintWriter(sw)) {
			pw.printf("Invalid %s %s:", name, id);

			if (errorList.size() == 1) {
				pw.printf(" It %s", errorList.get(0));
			} else {
				for (String error : errorList) {
					pw.printf("\n\t- It %s", error);
				}
			}
		}

		throw new ParseMetadataException(sw.toString());
	}
}
