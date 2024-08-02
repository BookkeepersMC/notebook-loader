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
package net.fabricmc.test;

import java.util.function.Predicate;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.impl.util.version.SemanticVersionImpl;
import com.bookkeepersmc.loader.impl.util.version.VersionPredicateParser;

import org.jetbrains.annotations.Nullable;

public class VersionParsingTests {
	private static Exception tryParseSemantic(String s, boolean storeX) {
		try {
			new SemanticVersionImpl(s, storeX);
			return null;
		} catch (VersionParsingException e) {
			return e;
		}
	}

	private static void testTrue(@Nullable Exception b) {
		if (b != null) {
			throw new RuntimeException("Test failed!", b);
		}
	}

	private static void testFalse(@Nullable Exception b) {
		if (b == null) {
			throw new RuntimeException("Test failed!");
		}
	}

	private static void testTrue(boolean b) {
		if (!b) {
			throw new RuntimeException("Test failed!");
		}
	}

	private static void testFalse(boolean b) {
		if (b) {
			throw new RuntimeException("Test failed!");
		}
	}

	public static void main(String[] args) throws Exception {
		// Test: Semantic version creation.
		testTrue(tryParseSemantic("0.3.5", false));
		testTrue(tryParseSemantic("0.3.5-beta.2", false));
		testTrue(tryParseSemantic("0.3.5-alpha.6+build.120", false));
		testTrue(tryParseSemantic("0.3.5+build.3000", false));
		testFalse(tryParseSemantic("0.0.-1", false));
		testFalse(tryParseSemantic("0." + ((long) Integer.MAX_VALUE + 1) + ".0", false));
		testFalse(tryParseSemantic("0.-1.0", false));
		testFalse(tryParseSemantic("-1.0.0", false));
		testFalse(tryParseSemantic("", false));
		testFalse(tryParseSemantic("0.0.a", false));
		testFalse(tryParseSemantic("0.a.0", false));
		testFalse(tryParseSemantic("a.0.0", false));
		testFalse(tryParseSemantic("x", true));
		testTrue(tryParseSemantic("2.x", true));
		testTrue(tryParseSemantic("2.x.x", true));
		testTrue(tryParseSemantic("2.X", true));
		testTrue(tryParseSemantic("2.*", true));
		testFalse(tryParseSemantic("2.x.1", true));
		testFalse(tryParseSemantic("2.*.1", true));
		testFalse(tryParseSemantic("2.x-alpha.1", true));
		testFalse(tryParseSemantic("2.*-alpha.1", true));
		testFalse(tryParseSemantic("*-alpha.1", true));
		testFalse(tryParseSemantic("2.x", false));
		testFalse(tryParseSemantic("2.X", false));
		testFalse(tryParseSemantic("2.*", false));

		// Test: Semantic version creation (spec).
		testTrue(tryParseSemantic("1.0.0-0.3.7", false));
		testTrue(tryParseSemantic("1.0.0-x.7.z.92", false));
		testTrue(tryParseSemantic("1.0.0+20130313144700", false));
		testTrue(tryParseSemantic("1.0.0-beta+exp.sha.5114f85", false));

		// Test: comparator range with pre-releases.
		{
			Predicate<Version> predicate = VersionPredicateParser.parse(">=0.3.1-beta.2 <0.4.0");
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.2.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.4+build.125", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.4.0-alpha.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.4-beta.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.11", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.4.0", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse(">=0.3.1-beta.2 <0.4.0-");
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.2.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.4+build.125", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.4-beta.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.11", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.4.0-alpha.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.4.0", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse(">=1.4-");
			testTrue(predicate.test(new SemanticVersionImpl("1.4-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.4+build.125", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.4.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.5", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3-alpha.1", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("<1.4");
			testTrue(predicate.test(new SemanticVersionImpl("1.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3-alpha.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.4-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4+build.125", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("<1.4-");
			testTrue(predicate.test(new SemanticVersionImpl("1.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3-alpha.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4+build.125", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4", false)));
		}

		// Test: pre-release parts
		{
			Predicate<Version> predicate = VersionPredicateParser.parse(">=0.3.1-beta.8.d.10");
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.9", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.11", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.8.e", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.8.d.10", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.9.d.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.final", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.1-beta.-final-", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.7", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.8.d", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.8.a", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-alpha.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.3.1-beta.8.8", false)));
		}

		// Test: x-range. "a.b.x" = ">=a.b.0- <a.(b+1).0-" (same major+minor, pre allowed)
		{
			Predicate<Version> predicate = VersionPredicateParser.parse("1.3.x");
			testTrue(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.0-alpha.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.99", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.9-rc.6", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.4.0-alpha.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0", false)));
		}

		// Test: smaller x-range. "a.x" = ">=a.0.0- <(a+1).0.0-" (same major, pre allowed)
		{
			Predicate<Version> predicate = VersionPredicateParser.parse("2.x");
			testTrue(predicate.test(new SemanticVersionImpl("2.0.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("2.0.0-alpha.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("2.9.0-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("2.2.4", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.99.99", false)));
			testFalse(predicate.test(new SemanticVersionImpl("3.0.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("3.0.0-alpha.1", false)));
		}

		// Test: tilde-ranges. "~a" = ">=a <(a[0]).(a[1]+1).0-" (at least a, same major+minor)
		{
			Predicate<Version> predicate = VersionPredicateParser.parse("~1.2.3");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-alpha.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-rc.7", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.2.0", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("~1.2");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.1-alpha.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.6", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.1.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.0-rc.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0-alpha.3", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("~1.2-");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.1-alpha.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.6", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.0-rc.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.1.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0-alpha.3", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("~1");
			testTrue(predicate.test(new SemanticVersionImpl("1.0.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.0.4", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.9.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.1.5", false)));
			testFalse(predicate.test(new SemanticVersionImpl("3.0.5", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("~1.2.3-beta.2");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.2.1", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.11", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-rc.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-alpha.4", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-beta.1", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-beta.1.9", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-alpha.4", false)));
		}

		// Test: caret-range. "^a" = ">=a <(a[0]+1).0.0-" (at least a, same major)
		{
			Predicate<Version> predicate = VersionPredicateParser.parse("^1.2.3");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("^0.2.3");
			testTrue(predicate.test(new SemanticVersionImpl("0.2.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.2.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.2.8-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("0.3.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.2.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.2.3-rc.8", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.0", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("^1.2.3-beta.2");
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.2", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-beta.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3-rc.7", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.3", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.3.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-alpha.4", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.2.3-alpha.4", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("^1");
			testTrue(predicate.test(new SemanticVersionImpl("1.0.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.99.99", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.9.6", false)));
			testFalse(predicate.test(new SemanticVersionImpl("1.0.0-rc.5", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0-beta.2", false)));
		}

		{
			Predicate<Version> predicate = VersionPredicateParser.parse("^1-");
			testTrue(predicate.test(new SemanticVersionImpl("1.0.0", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.0.0-rc.5", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.99.99", false)));
			testTrue(predicate.test(new SemanticVersionImpl("1.2.4-beta.2", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.9.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("0.9.0-rc.5", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0", false)));
			testFalse(predicate.test(new SemanticVersionImpl("2.0.0-beta.2", false)));
		}
	}
}
