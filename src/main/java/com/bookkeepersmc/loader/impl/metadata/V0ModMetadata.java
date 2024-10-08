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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.metadata.ContactInformation;
import com.bookkeepersmc.loader.api.metadata.CustomValue;
import com.bookkeepersmc.loader.api.metadata.ModDependency;
import com.bookkeepersmc.loader.api.metadata.ModEnvironment;
import com.bookkeepersmc.loader.api.metadata.Person;

final class V0ModMetadata extends AbstractModMetadata implements LoaderModMetadata {
	private static final Mixins EMPTY_MIXINS = new Mixins(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	// Required
	private final String id;
	private Version version;

	// Optional (Environment)
	private Collection<ModDependency> dependencies;
	private final String languageAdapter = "net.fabricmc.loader.language.JavaLanguageAdapter"; // TODO: Constants class?
	private final Mixins mixins;
	private final ModEnvironment environment; // REMOVEME: Replacing Side in old metadata with this
	private final String initializer;
	private final Collection<String> initializers;

	// Optional (metadata)
	private final String name;
	private final String description;
	private final Collection<Person> authors;
	private final Collection<Person> contributors;
	private final ContactInformation links;
	private final String license;

	V0ModMetadata(String id, Version version, Collection<ModDependency> dependencies, Mixins mixins, ModEnvironment environment, String initializer, Collection<String> initializers,
			String name, String description, Collection<Person> authors, Collection<Person> contributors, ContactInformation links, String license) {
		this.id = id;
		this.version = version;
		this.dependencies = Collections.unmodifiableCollection(dependencies);

		if (mixins == null) {
			this.mixins = V0ModMetadata.EMPTY_MIXINS;
		} else {
			this.mixins = mixins;
		}

		this.environment = environment;
		this.initializer = initializer;
		this.initializers = Collections.unmodifiableCollection(initializers);
		this.name = name;

		if (description == null) {
			this.description = "";
		} else {
			this.description = description;
		}

		this.authors = Collections.unmodifiableCollection(authors);
		this.contributors = Collections.unmodifiableCollection(contributors);
		this.links = links;
		this.license = license;
	}

	@Override
	public int getSchemaVersion() {
		return 0;
	}

	@Override
	public String getType() {
		return TYPE_NOTEBOOK_MOD;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public Collection<String> getProvides() {
		return Collections.emptyList();
	}

	@Override
	public Version getVersion() {
		return this.version;
	}

	@Override
	public void setVersion(Version version) {
		this.version = version;
	}

	@Override
	public ModEnvironment getEnvironment() {
		return this.environment;
	}

	@Override
	public boolean loadsInEnvironment(EnvType type) {
		return this.environment.matches(type);
	}

	@Override
	public Collection<ModDependency> getDependencies() {
		return dependencies;
	}

	@Override
	public void setDependencies(Collection<ModDependency> dependencies) {
		this.dependencies = Collections.unmodifiableCollection(dependencies);
	}

	// General metadata

	@Override
	public String getName() {
		if (this.name != null && this.name.isEmpty()) {
			return this.id;
		}

		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public Collection<Person> getAuthors() {
		return this.authors;
	}

	@Override
	public Collection<Person> getContributors() {
		return this.contributors;
	}

	@Override
	public ContactInformation getContact() {
		return this.links;
	}

	@Override
	public Collection<String> getLicense() {
		return Collections.singleton(this.license);
	}

	@Override
	public Optional<String> getIconPath(int size) {
		// honor Mod Menu's de-facto standard
		return Optional.of("assets/" + getId() + "/icon.png");
	}

	@Override
	public String getOldStyleLanguageAdapter() {
		return this.languageAdapter;
	}

	@Override
	public Map<String, CustomValue> getCustomValues() {
		return Collections.emptyMap();
	}

	@Override
	public boolean containsCustomValue(String key) {
		return false;
	}

	@Override
	public CustomValue getCustomValue(String key) {
		return null;
	}

	// Internals

	@Override
	public Map<String, String> getLanguageAdapterDefinitions() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<NestedJarEntry> getJars() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getOldInitializers() {
		if (this.initializer != null) {
			return Collections.singletonList(this.initializer);
		} else if (!this.initializers.isEmpty()) {
			return this.initializers;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<EntrypointMetadata> getEntrypoints(String type) {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getEntrypointKeys() {
		return Collections.emptyList();
	}

	@Override
	public void emitFormatWarnings() { }

	@Override
	public Collection<String> getMixinConfigs(EnvType type) {
		List<String> mixinConfigs = new ArrayList<>(this.mixins.common);

		switch (type) {
		case CLIENT:
			mixinConfigs.addAll(this.mixins.client);
			break;
		case SERVER:
			mixinConfigs.addAll(this.mixins.server);
			break;
		}

		return mixinConfigs;
	}

	@Override
	public String getAccessWidener() {
		return null; // intentional null
	}

	static final class Mixins {
		final Collection<String> client;
		final Collection<String> common;
		final Collection<String> server;

		Mixins(Collection<String> client, Collection<String> common, Collection<String> server) {
			this.client = Collections.unmodifiableCollection(client);
			this.common = Collections.unmodifiableCollection(common);
			this.server = Collections.unmodifiableCollection(server);
		}
	}
}
