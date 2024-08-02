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
import java.util.Collections;
import java.util.List;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.api.metadata.ModDependency;
import com.bookkeepersmc.loader.api.metadata.version.VersionInterval;
import com.bookkeepersmc.loader.api.metadata.version.VersionPredicate;

public final class ModDependencyImpl implements ModDependency {
	private Kind kind;
	private final String modId;
	private final List<String> matcherStringList;
	private final Collection<VersionPredicate> ranges;

	public ModDependencyImpl(Kind kind, String modId, List<String> matcherStringList) throws VersionParsingException {
		this.kind = kind;
		this.modId = modId;
		this.matcherStringList = matcherStringList;
		this.ranges = VersionPredicate.parse(this.matcherStringList);
	}

	@Override
	public Kind getKind() {
		return kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

	@Override
	public String getModId() {
		return this.modId;
	}

	@Override
	public boolean matches(Version version) {
		for (VersionPredicate predicate : ranges) {
			if (predicate.test(version)) return true;
		}

		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ModDependency)) return false;

		ModDependency o = (ModDependency) obj;

		return kind == o.getKind()
				&& modId.equals(o.getModId())
				&& ranges.equals(o.getVersionRequirements());
	}

	@Override
	public int hashCode() {
		return (kind.ordinal() * 31 + modId.hashCode()) * 257 + ranges.hashCode();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("{");
		builder.append(kind.getKey());
		builder.append(' ');
		builder.append(this.modId);
		builder.append(" @ [");

		for (int i = 0; i < matcherStringList.size(); i++) {
			if (i > 0) {
				builder.append(" || ");
			}

			builder.append(matcherStringList.get(i));
		}

		builder.append("]}");
		return builder.toString();
	}

	@Override
	public Collection<VersionPredicate> getVersionRequirements() {
		return ranges;
	}

	@Override
	public List<VersionInterval> getVersionIntervals() {
		List<VersionInterval> ret = Collections.emptyList();

		for (VersionPredicate predicate : ranges) {
			ret = VersionInterval.or(ret, predicate.getInterval());
		}

		return ret;
	}
}
