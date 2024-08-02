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

import java.util.Collection;
import java.util.List;

import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.impl.util.version.VersionIntervalImpl;

/**
 * Representation of a version interval, closed or open.
 *
 * <p>The represented version interval is contiguous between its lower and upper limit, disjoint intervals are built
 * using collections of {@link VersionInterval}. Empty intervals may be represented by {@code null} or any interval
 * @code (x,x)} with x being a non-{@code null} version and both endpoints being exclusive.
 */
public interface VersionInterval {
	VersionInterval INFINITE = new VersionIntervalImpl(null, false, null, false);

	/**
	 * Get whether the interval uses {@link SemanticVersion} compatible bounds.
	 *
	 * @return True if both bounds are open (null), {@link SemanticVersion} instances or a combination of both, false otherwise.
	 */
	boolean isSemantic();

	/**
	 * Get the lower limit of the version interval.
	 *
	 * @return Version's lower limit or null if none, inclusive depending on {@link #isMinInclusive()}
	 */
	Version getMin();

	/**
	 * Get whether the lower limit of the version interval is inclusive.
	 *
	 * @return True if inclusive, false otherwise
	 */
	boolean isMinInclusive();

	/**
	 * Get the upper limit of the version interval.
	 *
	 * @return Version's upper limit or null if none, inclusive depending on {@link #isMaxInclusive()}
	 */
	Version getMax();

	/**
	 * Get whether the upper limit of the version interval is inclusive.
	 *
	 * @return True if inclusive, false otherwise
	 */
	boolean isMaxInclusive();

	default VersionInterval and(VersionInterval o) {
		return and(this, o);
	}

	default List<VersionInterval> or(Collection<VersionInterval> o) {
		return or(o, this);
	}

	default List<VersionInterval> not() {
		return not(this);
	}

	/**
	 * Compute the intersection between two version intervals.
	 */
	static VersionInterval and(VersionInterval a, VersionInterval b) {
		return VersionIntervalImpl.and(a, b);
	}

	/**
	 * Compute the intersection between two potentially disjoint of version intervals.
	 */
	static List<VersionInterval> and(Collection<VersionInterval> a, Collection<VersionInterval> b) {
		return VersionIntervalImpl.and(a, b);
	}

	/**
	 * Compute the union between multiple version intervals.
	 */
	static List<VersionInterval> or(Collection<VersionInterval> a, VersionInterval b) {
		return VersionIntervalImpl.or(a, b);
	}

	static List<VersionInterval> not(VersionInterval interval) {
		return VersionIntervalImpl.not(interval);
	}

	static List<VersionInterval> not(Collection<VersionInterval> intervals) {
		return VersionIntervalImpl.not(intervals);
	}
}
