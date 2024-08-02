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
import java.util.function.Predicate;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.impl.util.version.VersionPredicateParser;

public interface VersionPredicate extends Predicate<Version> {
	/**
	 * Get all terms that have to be satisfied for this predicate to match.
	 *
	 * @return Required predicate terms, empty if anything matches
	 */
	Collection<? extends PredicateTerm> getTerms();

	/**
	 * Get the version interval representing the matched versions.
	 *
	 * @return Covered version interval or null if nothing
	 */
	VersionInterval getInterval();

	interface PredicateTerm {
		VersionComparisonOperator getOperator();
		Version getReferenceVersion();
	}

	static VersionPredicate parse(String predicate) throws VersionParsingException {
		return VersionPredicateParser.parse(predicate);
	}

	static Collection<VersionPredicate> parse(Collection<String> predicates) throws VersionParsingException {
		return VersionPredicateParser.parse(predicates);
	}
}
