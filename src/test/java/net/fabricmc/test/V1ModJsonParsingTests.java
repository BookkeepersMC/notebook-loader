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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.metadata.CustomValue;
import com.bookkeepersmc.loader.impl.metadata.DependencyOverrides;
import com.bookkeepersmc.loader.impl.metadata.LoaderModMetadata;
import com.bookkeepersmc.loader.impl.metadata.ModMetadataParser;
import com.bookkeepersmc.loader.impl.metadata.ParseMetadataException;
import com.bookkeepersmc.loader.impl.metadata.VersionOverrides;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V1ModJsonParsingTests {
	private static Path testLocation;
	private static Path specPath;
	private static Path errorPath;

	@BeforeAll
	public static void setupPaths() {
		testLocation = new File(System.getProperty("user.dir"))
				.toPath()
				.resolve("src")
				.resolve("test")
				.resolve("resources")
				.resolve("testing")
				.resolve("parsing")
				.resolve("v1");

		specPath = testLocation.resolve("spec");
		errorPath = testLocation.resolve("error");
	}

	/*
	 * Spec compliance tests
	 */

	@Test
	@DisplayName("Test required values")
	public void testRequiredValues() throws IOException, ParseMetadataException {
		// Required fields
		final LoaderModMetadata metadata = parseMetadata(specPath.resolve("required.json"));
		assertNotNull(metadata, "Failed to read mod metadata!");
		validateRequiredValues(metadata);

		// Required fields in different order to verify we don't have ordering issues
		final LoaderModMetadata reversedMetadata = parseMetadata(specPath.resolve("required_reversed.json"));
		assertNotNull(reversedMetadata, "Failed to read mod metadata!");
		validateRequiredValues(reversedMetadata);
	}

	@Test
	@DisplayName("Read custom values")
	public void customValues() throws IOException, ParseMetadataException {
		final LoaderModMetadata metadata = parseMetadata(specPath.resolve("custom_values.json"));

		final Map<String, CustomValue> customValues = metadata.getCustomValues();
		// Should be 6 elements in custom values map
		assertEquals(6, customValues.size(), "Incorrectly read \"custom\", expected 6 elements but found " + customValues.size());

		// Boolean
		final CustomValue zero = customValues.get("zero");
		assertEquals(CustomValue.CvType.BOOLEAN, zero.getType(), "Custom value \"zero\" was not a boolean type. Found " + zero.getType());

		// Null
		final CustomValue one = customValues.get("one");
		assertEquals(CustomValue.CvType.NULL, one.getType(), "Custom value \"one\" was not a null type. Found " + one.getType());

		// Number - Int
		final CustomValue two = customValues.get("two");
		assertEquals(CustomValue.CvType.NUMBER, two.getType(), "Custom value \"two\" was not a number type. Found " + one.getType());
		assertEquals(2, two.getAsNumber().intValue());

		// Number - Decimal
		final CustomValue three = customValues.get("three");
		assertEquals(CustomValue.CvType.NUMBER, three.getType(), "Custom value \"three\" was not a number type. Found " + one.getType());
		assertEquals(3.3D, three.getAsNumber().doubleValue());

		// Array
		final CustomValue four = customValues.get("four");
		assertEquals(CustomValue.CvType.ARRAY, four.getType(), "Custom value \"four\" was not an array type. Found " + one.getType());
		assertEquals(3, four.getAsArray().size(), "Custom value \"four\" was expected to have 3 values in array but found " + four.getAsArray().size());

		// String in array
		final CustomValue.CvArray fourAsArray = four.getAsArray();
		final CustomValue five = fourAsArray.get(0);
		assertEquals(CustomValue.CvType.STRING, five.getType(), "Custom value \"five\" within \"four\" was not a string type. Found " + one.getType());

		// Object
		final CustomValue eight = customValues.get("eight");
		assertEquals(CustomValue.CvType.OBJECT, eight.getType(), "Custom value \"four\" was not an object type. Found " + one.getType());

		final CustomValue.CvObject eightAsObject = eight.getAsObject();
		assertEquals(2, eightAsObject.size(), "Custom value \"eight\" was expected to have 2 values in object but found " + eightAsObject.size());
	}

	@Test
	@DisplayName("Test example 1")
	public void example1() throws IOException, ParseMetadataException {
		parseMetadata(specPath.resolve("example_1.json"));
	}

	private void validateRequiredValues(LoaderModMetadata metadata) {
		final int schemaVersion = metadata.getSchemaVersion();
		assertEquals(1, metadata.getSchemaVersion(), String.format("Parsed JSON file had schema version %s, expected \"1\"", schemaVersion));

		final String id = metadata.getId();
		assertEquals("v1-parsing-test", id, String.format("Mod id \"%s\" was found, expected \"v1-parsing-test\"", id));

		final String friendlyString = metadata.getVersion().getFriendlyString();
		assertEquals("1.0.0-SNAPSHOT", friendlyString, String.format("Version \"%s\" was found, expected \"1.0.0-SNAPSHOT\"", friendlyString));

		assertTrue(metadata.getVersion() instanceof SemanticVersion, "Parsed version was not a semantic version, expected a semantic version");
	}

	@Test
	@DisplayName("Long test file")
	public void testLongFile() throws IOException, ParseMetadataException {
		final LoaderModMetadata modMetadata = parseMetadata(specPath.resolve("long.json"));

		if (!modMetadata.getAccessWidener().equals("examplemod.accessWidener")) {
			throw new RuntimeException("Incorrect access widener entry");
		}

		if (modMetadata.getDependencies().isEmpty()) {
			throw new RuntimeException("Incorrect amount of dependencies");
		}
	}

	@Test
	@DisplayName("Any icon size test file")
	public void testAnyIconSizeFile() throws IOException, ParseMetadataException {
		final LoaderModMetadata metadata = parseMetadata(specPath.resolve("any_icon_size.json"));

		validateIconPath(metadata, 32, 64);
		validateIconPath(metadata, 64, 64);
		validateIconPath(metadata, 128, 64);
	}

	@Test
	@DisplayName("No icon test file")
	public void testNoIconFile() throws IOException, ParseMetadataException {
		final LoaderModMetadata metadata = parseMetadata(specPath.resolve("no_icon.json"));
		assertEquals(Optional.empty(), metadata.getIconPath(32));
	}

	@Test
	@DisplayName("Icon sizes test file")
	public void testIconSizesFile() throws IOException, ParseMetadataException {
		final LoaderModMetadata metadata = parseMetadata(specPath.resolve("icon_sizes.json"));

		validateIconPath(metadata, 32, 64);
		validateIconPath(metadata, 64, 64);
		validateIconPath(metadata, 128, 128);
		validateIconPath(metadata, 256, 256);
		validateIconPath(metadata, 512, 256);
	}

	private void validateIconPath(LoaderModMetadata metadata, int preferredSize, int expectedSize) {
		Optional<String> expected = Optional.of("assets/testing/icon-" + expectedSize + ".png");
		Optional<String> actual = metadata.getIconPath(preferredSize);

		assertEquals(expected, actual, "Found " + actual.orElse("no icon") + " for preferred size " + preferredSize + " instead of expected size " + expectedSize);
	}

	/*
	 * Spec violation tests
	 */

	@Test
	public void verifyMissingVersionFails() {
		// Missing version should throw an exception
		assertThrows(ParseMetadataException.MissingField.class, () -> {
			parseMetadata(errorPath.resolve("missing_version.json"));
		}, "Missing version exception was not caught");
	}

	@Test
	public void validateDuplicateSchemaVersionMismatchFails() {
		assertThrows(ParseMetadataException.class, () -> {
			parseMetadata(errorPath.resolve("missing_version.json"));
		}, "Parser did not fail when the duplicate \"schemaVersion\" mismatches");
	}

	/*
	 * Warning tests
	 */

	@Test
	public void testWarnings() { }

	private static LoaderModMetadata parseMetadata(Path path) throws IOException, ParseMetadataException {
		try (InputStream is = Files.newInputStream(path)) {
			return ModMetadataParser.parseMetadata(is, "dummy", Collections.emptyList(), new VersionOverrides(), new DependencyOverrides(Paths.get("randomMissing")), false);
		}
	}
}
