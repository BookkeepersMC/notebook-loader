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
package com.bookkeepersmc.loader.api.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.metadata.version.VersionInterval;
import com.bookkeepersmc.loader.api.metadata.version.VersionPredicate;

/**
 * Represents a dependency.
 */
public interface ModDependency {
	/**
	 * Get the kind of dependency.
	 */
	Kind getKind();

	/**
	 * Returns the ID of the mod to check.
	 */
	String getModId();

	/**
	 * Returns if the version fulfills this dependency's version requirement.
	 *
	 * @param version the version to check
	 */
	boolean matches(Version version);

	/**
	 * Returns a representation of the dependency's version requirements.
	 *
	 * @return representation of the dependency's version requirements
	 */
	Collection<VersionPredicate> getVersionRequirements();

	/**
	 * Returns the version intervals covered by the dependency's version requirements.
	 *
	 * <p>There may be multiple because the allowed range may not be consecutive.
	 */
	List<VersionInterval> getVersionIntervals();

	enum Kind {
		DEPENDS("depends", true, false),
		RECOMMENDS("recommends", true, true),
		SUGGESTS("suggests", true, true),
		CONFLICTS("conflicts", false, true),
		BREAKS("breaks", false, false);

		private static final Map<String, Kind> map = createMap();
		private final String key;
		private final boolean positive;
		private final boolean soft;

		Kind(String key, boolean positive, boolean soft) {
			this.key = key;
			this.positive = positive;
			this.soft = soft;
		}

		/**
		 * Get the key for the dependency as used by fabric.mod.json (v1+) and dependency overrides.
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Get whether the dependency is positive, encouraging the inclusion of a mod instead of negative/discouraging.
		 */
		public boolean isPositive() {
			return positive;
		}

		/**
		 * Get whether it is a soft dependency, allowing the mod to still load if the dependency is unmet.
		 */
		public boolean isSoft() {
			return soft;
		}

		/**
		 * Parse a dependency kind from its key as provided by {@link #getKey}.
		 */
		public static Kind parse(String key) {
			return map.get(key);
		}

		private static Map<String, Kind> createMap() {
			Kind[] values = values();
			Map<String, Kind> ret = new HashMap<>(values.length);

			for (Kind kind : values) {
				ret.put(kind.key, kind);
			}

			return ret;
		}
	}
}
