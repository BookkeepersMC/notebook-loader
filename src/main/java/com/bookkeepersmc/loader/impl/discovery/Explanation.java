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
package com.bookkeepersmc.loader.impl.discovery;

import com.bookkeepersmc.loader.api.metadata.ModDependency;

class Explanation implements Comparable<Explanation> {
	private static int nextCmpId;

	final ErrorKind error;
	final ModCandidateImpl mod;
	final ModDependency dep;
	final String data;
	private final int cmpId;

	Explanation(ErrorKind error, ModCandidateImpl mod) {
		this(error, mod, null, null);
	}

	Explanation(ErrorKind error, ModCandidateImpl mod, ModDependency dep) {
		this(error, mod, dep, null);
	}

	Explanation(ErrorKind error, String data) {
		this(error, null, data);
	}

	Explanation(ErrorKind error, ModCandidateImpl mod, String data) {
		this(error, mod, null, data);
	}

	private Explanation(ErrorKind error, ModCandidateImpl mod, ModDependency dep, String data) {
		this.error = error;
		this.mod = mod;
		this.dep = dep;
		this.data = data;
		this.cmpId = nextCmpId++;
	}

	@Override
	public int compareTo(Explanation o) {
		return Integer.compare(cmpId, o.cmpId);
	}

	@Override
	public String toString() {
		if (mod == null) {
			return String.format("%s %s", error, data);
		} else if (dep == null) {
			return String.format("%s %s", error, mod);
		} else {
			return String.format("%s %s %s", error, mod, dep);
		}
	}

	enum ErrorKind {
		/**
		 * Positive hard dependency (depends) from a preselected mod.
		 */
		PRESELECT_HARD_DEP(true),
		/**
		 * Positive soft dependency (recommends) from a preselected mod.
		 */
		PRESELECT_SOFT_DEP(true),
		/**
		 * Negative hard dependency (breaks) from a preselected mod.
		 */
		PRESELECT_NEG_HARD_DEP(true),
		/**
		 * Force loaded due to being preselected.
		 */
		PRESELECT_FORCELOAD(false),
		/**
		 * Positive hard dependency (depends) from a mod with incompatible preselected candidate.
		 */
		HARD_DEP_INCOMPATIBLE_PRESELECTED(true),
		/**
		 * Positive hard dependency (depends) from a mod with no matching candidate.
		 */
		HARD_DEP_NO_CANDIDATE(true),
		/**
		 * Positive hard dependency (depends) from a mod.
		 */
		HARD_DEP(true),
		/**
		 * Positive soft dependency (recommends) from a mod.
		 */
		SOFT_DEP(true),
		/**
		 * Negative hard dependency (breaks) from a mod.
		 */
		NEG_HARD_DEP(true),
		/**
		 * Force loaded if the parent is loaded due to LoadType ALWAYS.
		 */
		NESTED_FORCELOAD(false),
		/**
		 * Dependency of a nested mod on its parent mods.
		 */
		NESTED_REQ_PARENT(false),
		/**
		 * Force loaded due to LoadType ALWAYS as a singular root mod.
		 */
		ROOT_FORCELOAD_SINGLE(false),
		/**
		 * Force loaded due to LoadType ALWAYS and containing root mods.
		 */
		ROOT_FORCELOAD(false),
		/**
		 * Requirement to load at most one mod per id (including provides).
		 */
		UNIQUE_ID(false);

		final boolean isDependencyError;

		ErrorKind(boolean isDependencyError) {
			this.isDependencyError = isDependencyError;
		}
	}
}
