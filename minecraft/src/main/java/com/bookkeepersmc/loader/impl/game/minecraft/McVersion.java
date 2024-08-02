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
package com.bookkeepersmc.loader.impl.game.minecraft;

import java.util.OptionalInt;

public final class McVersion {
	/**
	 * The id from version.json, if available.
	 */
	private final String id;
	/**
	 * The name from version.json, if available.
	 */
	private final String name;
	/**
	 * The raw version, such as {@code 18w21a}.
	 *
	 * <p>This is derived from the version.json's id and name fields if available, otherwise through other sources.
	 */
	private final String raw;
	/**
	 * The normalized version.
	 *
	 * <p>This is usually compliant with Semver and
	 * contains release and pre-release information.
	 */
	private final String normalized;
	private final OptionalInt classVersion;

	private McVersion(String id, String name, String raw, String release, OptionalInt classVersion) {
		this.id = id;
		this.name = name;
		this.raw = raw;
		this.normalized = McVersionLookup.normalizeVersion(raw, release);
		this.classVersion = classVersion;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getRaw() {
		return this.raw;
	}

	public String getNormalized() {
		return this.normalized;
	}

	public OptionalInt getClassVersion() {
		return this.classVersion;
	}

	@Override
	public String toString() {
		return String.format("McVersion{id=%s, name=%s, raw=%s, normalized=%s, classVersion=%s}",
				id, name, raw, normalized, classVersion);
	}

	public static final class Builder {
		private String id; // id as in version.json
		private String name; // name as in version.json
		private String version; // derived from version.json's id and name or other sources
		private String release; // mc release (major.minor)
		private OptionalInt classVersion = OptionalInt.empty();

		// Setters
		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setVersion(String name) {
			this.version = name;
			return this;
		}

		public Builder setRelease(String release) {
			this.release = release;
			return this;
		}

		public Builder setClassVersion(int classVersion) {
			this.classVersion = OptionalInt.of(classVersion);
			return this;
		}

		// Complex setters
		public Builder setNameAndRelease(String name) {
			return setVersion(name)
					.setRelease(McVersionLookup.getRelease(name));
		}

		public Builder setFromFileName(String name) {
			// strip extension
			int pos = name.lastIndexOf('.');
			if (pos > 0) name = name.substring(0, pos);

			return setNameAndRelease(name);
		}

		public McVersion build() {
			return new McVersion(this.id, this.name, this.version, this.release, this.classVersion);
		}
	}
}
