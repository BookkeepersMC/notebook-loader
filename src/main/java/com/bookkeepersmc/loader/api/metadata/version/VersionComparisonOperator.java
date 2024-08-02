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
package com.bookkeepersmc.loader.api.metadata.version;

import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.impl.util.version.SemanticVersionImpl;

public enum VersionComparisonOperator {
	// order is important to match the longest substring (e.g. try >= before >)
	GREATER_EQUAL(">=", true, false) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) >= 0;
		}

		@Override
		public SemanticVersion minVersion(SemanticVersion version) {
			return version;
		}
	},
	LESS_EQUAL("<=", false, true) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) <= 0;
		}

		@Override
		public SemanticVersion maxVersion(SemanticVersion version) {
			return version;
		}
	},
	GREATER(">", false, false) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) > 0;
		}

		@Override
		public SemanticVersion minVersion(SemanticVersion version) {
			return version;
		}
	},
	LESS("<", false, false) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) < 0;
		}

		@Override
		public SemanticVersion maxVersion(SemanticVersion version) {
			return version;
		}
	},
	EQUAL("=", true, true) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) == 0;
		}

		@Override
		public SemanticVersion minVersion(SemanticVersion version) {
			return version;
		}

		@Override
		public SemanticVersion maxVersion(SemanticVersion version) {
			return version;
		}
	},
	SAME_TO_NEXT_MINOR("~", true, false) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) >= 0
					&& a.getVersionComponent(0) == b.getVersionComponent(0)
					&& a.getVersionComponent(1) == b.getVersionComponent(1);
		}

		@Override
		public SemanticVersion minVersion(SemanticVersion version) {
			return version;
		}

		@Override
		public SemanticVersion maxVersion(SemanticVersion version) {
			return new SemanticVersionImpl(new int[] { version.getVersionComponent(0), version.getVersionComponent(1) + 1 }, "", null);
		}
	},
	SAME_TO_NEXT_MAJOR("^", true, false) {
		@Override
		public boolean test(SemanticVersion a, SemanticVersion b) {
			return a.compareTo((Version) b) >= 0
					&& a.getVersionComponent(0) == b.getVersionComponent(0);
		}

		@Override
		public SemanticVersion minVersion(SemanticVersion version) {
			return version;
		}

		@Override
		public SemanticVersion maxVersion(SemanticVersion version) {
			return new SemanticVersionImpl(new int[] { version.getVersionComponent(0) + 1 }, "", null);
		}
	};

	private final String serialized;
	private final boolean minInclusive;
	private final boolean maxInclusive;

	VersionComparisonOperator(String serialized, boolean minInclusive, boolean maxInclusive) {
		this.serialized = serialized;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}

	public final String getSerialized() {
		return serialized;
	}

	public final boolean isMinInclusive() {
		return minInclusive;
	}

	public final boolean isMaxInclusive() {
		return maxInclusive;
	}

	public final boolean test(Version a, Version b) {
		if (a instanceof SemanticVersion && b instanceof SemanticVersion) {
			return test((SemanticVersion) a, (SemanticVersion) b);
		} else if (minInclusive || maxInclusive) {
			return a.getFriendlyString().equals(b.getFriendlyString());
		} else {
			return false;
		}
	}

	public abstract boolean test(SemanticVersion a, SemanticVersion b);

	public SemanticVersion minVersion(SemanticVersion version) {
		return null;
	}

	public SemanticVersion maxVersion(SemanticVersion version) {
		return null;
	}
}
