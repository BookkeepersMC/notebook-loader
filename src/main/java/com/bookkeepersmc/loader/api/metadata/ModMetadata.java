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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bookkeepersmc.loader.api.Version;

/**
 * The metadata of a mod.
 */
public interface ModMetadata {
	/**
	 * Returns the type of the mod.
	 *
	 * <p>The types may be {@code fabric} or {@code builtin} by default.</p>
	 *
	 * @return the type of the mod
	 */
	String getType();

	// When adding getters, follow the order as presented on the wiki.
	// No defaults.

	/**
	 * Returns the mod's ID.
	 *
	 * <p>A mod's id must have only lowercase letters, digits, {@code -}, or {@code _}.</p>
	 *
	 * @return the mod's ID.
	 */
	String getId();

	/**
	 * Returns the mod's ID provides.
	 *
	 * <p>The aliases follow the same rules as ID</p>
	 *
	 * @return the mod's ID provides
	 */
	Collection<String> getProvides();

	/**
	 * Returns the mod's version.
	 */
	Version getVersion();

	/**
	 * Returns the mod's environment.
	 */
	ModEnvironment getEnvironment();

	/**
	 * Returns all of the mod's dependencies.
	 */
	Collection<ModDependency> getDependencies();

	/**
	 * Returns the mod's required dependencies, without which the Loader will terminate loading.
	 *
	 * @deprecated Use {@link #getDependencies()} and filter for {@link ModDependency.Kind#DEPENDS} instead
	 */
	@Deprecated
	default Collection<ModDependency> getDepends() {
		return getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.DEPENDS).collect(Collectors.toList());
	}

	/**
	 * Returns the mod's recommended dependencies, without which the Loader will emit a warning.
	 *
	 * @deprecated Use {@link #getDependencies()} and filter for {@link ModDependency.Kind#RECOMMENDS} instead
	 */
	@Deprecated
	default Collection<ModDependency> getRecommends() {
		return getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.RECOMMENDS).collect(Collectors.toList());
	}

	/**
	 * Returns the mod's suggested dependencies.
	 *
	 * @deprecated Use {@link #getDependencies()} and filter for {@link ModDependency.Kind#SUGGESTS} instead
	 */
	@Deprecated
	default Collection<ModDependency> getSuggests() {
		return getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.SUGGESTS).collect(Collectors.toList());
	}

	/**
	 * Returns the mod's conflicts, with which the Loader will emit a warning.
	 *
	 * @deprecated Use {@link #getDependencies()} and filter for {@link ModDependency.Kind#CONFLICTS} instead
	 */
	@Deprecated
	default Collection<ModDependency> getConflicts() {
		return getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.CONFLICTS).collect(Collectors.toList());
	}

	/**
	 * Returns the mod's conflicts, with which the Loader will terminate loading.
	 *
	 * @deprecated Use {@link #getDependencies()} and filter for {@link ModDependency.Kind#BREAKS} instead
	 */
	@Deprecated
	default Collection<ModDependency> getBreaks() {
		return getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.BREAKS).collect(Collectors.toList());
	}

	/**
	 * Returns the mod's display name.
	 */
	String getName();

	/**
	 * Returns the mod's description.
	 */
	String getDescription();

	/**
	 * Returns the mod's authors.
	 */
	Collection<Person> getAuthors();

	/**
	 * Returns the mod's contributors.
	 */
	Collection<Person> getContributors();

	/**
	 * Returns the mod's contact information.
	 */
	ContactInformation getContact();

	/**
	 * Returns the mod's licenses.
	 */
	Collection<String> getLicense();

	/**
	 * Gets the path to an icon.
	 *
	 * <p>The standard defines icons as square .PNG files, however their
	 * dimensions are not defined - in particular, they are not
	 * guaranteed to be a power of two.</p>
	 *
	 * <p>The preferred size is used in the following manner:
	 * <ul><li>the smallest image larger than or equal to the size
	 * is returned, if one is present;</li>
	 * <li>failing that, the largest image is returned.</li></ul></p>
	 *
	 * @param size the preferred size
	 * @return the icon path, if any
	 */
	Optional<String> getIconPath(int size);

	/**
	 * Returns if the mod's {@code fabric.mod.json} declares a custom value under {@code key}.
	 *
	 * @param key the key
	 * @return whether a custom value is present
	 */
	boolean containsCustomValue(String key);

	/**
	 * Returns the mod's {@code fabric.mod.json} declared custom value under {@code key}.
	 *
	 * @param key the key
	 * @return the custom value, or {@code null} if no such value is present
	 */
	/* @Nullable */
	CustomValue getCustomValue(String key);

	/**
	 * Gets all custom values defined by this mod.
	 *
	 * <p>Note this map is unmodifiable.
	 *
	 * @return a map containing the custom values this mod defines.
	 */
	Map<String, CustomValue> getCustomValues();

	/**
	 * @deprecated Use {@link #containsCustomValue} instead, this will be removed (can't expose GSON types)!
	 */
	@Deprecated
	boolean containsCustomElement(String key);
}
