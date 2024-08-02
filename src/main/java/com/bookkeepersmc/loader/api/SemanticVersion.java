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
package com.bookkeepersmc.loader.api;

import java.util.Optional;

import com.bookkeepersmc.loader.impl.util.version.VersionParser;

/**
 * Represents a <a href="https://semver.org/">Sematic Version</a>.
 *
 * <p>Compared to a regular {@link Version}, this type of version receives better support
 * for version comparisons in dependency notations, and is preferred.</p>
 *
 * @see Version
 */
public interface SemanticVersion extends Version {
	/**
	 * The value of {@linkplain #getVersionComponent(int) version component} that indicates
	 * a {@linkplain #hasWildcard() wildcard}.
	 */
	int COMPONENT_WILDCARD = Integer.MIN_VALUE;

	/**
	 * Returns the number of components in this version.
	 *
	 * <p>For example, {@code 1.3.x} has 3 components.</p>
	 *
	 * @return the number of components
	 */
	int getVersionComponentCount();

	/**
	 * Returns the version component at {@code pos}.
	 *
	 * <p>May return {@link #COMPONENT_WILDCARD} to indicate a wildcard component.</p>
	 *
	 * <p>If the pos exceeds the number of components, returns {@link #COMPONENT_WILDCARD}
	 * if the version {@linkplain #hasWildcard() has wildcard}; otherwise returns {@code 0}.</p>
	 *
	 * @param pos the position to check
	 * @return the version component
	 */
	int getVersionComponent(int pos);

	/**
	 * Returns the prerelease key in the version notation.
	 *
	 * <p>The prerelease key is indicated by a {@code -} before a {@code +} in
	 * the version notation.</p>
	 *
	 * @return the optional prerelease key
	 */
	Optional<String> getPrereleaseKey();

	/**
	 * Returns the build key in the version notation.
	 *
	 * <p>The build key is indicated by a {@code +} in the version notation.</p>
	 *
	 * @return the optional build key
	 */
	Optional<String> getBuildKey();

	/**
	 * Returns if a wildcard notation is present in this version.
	 *
	 * <p>A wildcard notation is a {@code x}, {@code X}, or {@code *} in the version string,
	 * such as {@code 2.5.*}.</p>
	 *
	 * @return whether this version has a wildcard notation
	 */
	boolean hasWildcard();

	/**
	 * @deprecated Use {@link #compareTo(Object)} instead
	 */
	@Deprecated
	default int compareTo(SemanticVersion o) {
		return compareTo((Version) o);
	}

	/**
	 * Parses a semantic version from a string notation.
	 *
	 * @param s the string notation of the version
	 * @return the parsed version
	 * @throws VersionParsingException if a problem arises during version parsing
	 */
	static SemanticVersion parse(String s) throws VersionParsingException {
		return VersionParser.parseSemantic(s);
	}
}
