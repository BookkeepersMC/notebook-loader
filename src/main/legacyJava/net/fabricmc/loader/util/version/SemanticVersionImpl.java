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
package net.fabricmc.loader.util.version;

import java.util.Optional;

import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;

/**
 * @deprecated Internal API, do not use
 */
@Deprecated
public class SemanticVersionImpl implements SemanticVersion {
	private final SemanticVersion parent;

	protected SemanticVersionImpl() {
		parent = null;
	}

	public SemanticVersionImpl(String version, boolean storeX) throws VersionParsingException {
		parent = SemanticVersion.parse(version);
	}

	@Override
	public int getVersionComponentCount() {
		return parent.getVersionComponentCount();
	}

	@Override
	public int getVersionComponent(int pos) {
		return parent.getVersionComponent(pos);
	}

	@Override
	public Optional<String> getPrereleaseKey() {
		return parent.getPrereleaseKey();
	}

	@Override
	public Optional<String> getBuildKey() {
		return parent.getBuildKey();
	}

	@Override
	public String getFriendlyString() {
		return parent.getFriendlyString();
	}

	@Override
	public boolean equals(Object o) {
		return parent.equals(o);
	}

	@Override
	public int hashCode() {
		return parent.hashCode();
	}

	@Override
	public String toString() {
		return parent.toString();
	}

	@Override
	public boolean hasWildcard() {
		return parent.hasWildcard();
	}

	public boolean equalsComponentsExactly(SemanticVersionImpl other) {
		for (int i = 0; i < Math.max(getVersionComponentCount(), other.getVersionComponentCount()); i++) {
			if (getVersionComponent(i) != other.getVersionComponent(i)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int compareTo(Version o) {
		return parent.compareTo(o);
	}
}
