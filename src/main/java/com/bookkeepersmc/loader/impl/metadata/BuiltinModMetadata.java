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
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.api.metadata.ContactInformation;
import com.bookkeepersmc.loader.api.metadata.CustomValue;
import com.bookkeepersmc.loader.api.metadata.ModDependency;
import com.bookkeepersmc.loader.api.metadata.ModEnvironment;
import com.bookkeepersmc.loader.api.metadata.ModMetadata;
import com.bookkeepersmc.loader.api.metadata.Person;

public final class BuiltinModMetadata extends AbstractModMetadata {
	private final String id;
	private final Version version;
	private final ModEnvironment environment;
	private final String name;
	private final String description;
	private final Collection<Person> authors;
	private final Collection<Person> contributors;
	private final ContactInformation contact;
	private final Collection<String> license;
	private final NavigableMap<Integer, String> icons;
	private final Collection<ModDependency> dependencies;

	private BuiltinModMetadata(String id, Version version,
			ModEnvironment environment,
			String name, String description,
			Collection<Person> authors, Collection<Person> contributors,
			ContactInformation contact,
			Collection<String> license,
			NavigableMap<Integer, String> icons,
			Collection<ModDependency> dependencies) {
		this.id = id;
		this.version = version;
		this.environment = environment;
		this.name = name;
		this.description = description;
		this.authors = Collections.unmodifiableCollection(authors);
		this.contributors = Collections.unmodifiableCollection(contributors);
		this.contact = contact;
		this.license = Collections.unmodifiableCollection(license);
		this.icons = icons;
		this.dependencies = Collections.unmodifiableCollection(dependencies);
	}

	@Override
	public String getType() {
		return TYPE_BUILTIN;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Collection<String> getProvides() {
		return Collections.emptyList();
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public ModEnvironment getEnvironment() {
		return environment;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Collection<Person> getAuthors() {
		return authors;
	}

	@Override
	public Collection<Person> getContributors() {
		return contributors;
	}

	@Override
	public ContactInformation getContact() {
		return contact;
	}

	@Override
	public Collection<String> getLicense() {
		return license;
	}

	@Override
	public Optional<String> getIconPath(int size) {
		if (icons.isEmpty()) return Optional.empty();

		Integer key = size;
		Entry<Integer, String> ret = icons.ceilingEntry(key);
		if (ret == null) ret = icons.lastEntry();

		return Optional.of(ret.getValue());
	}

	@Override
	public Collection<ModDependency> getDependencies() {
		return dependencies;
	}

	@Override
	public boolean containsCustomValue(String key) {
		return false;
	}

	@Override
	public CustomValue getCustomValue(String key) {
		return null;
	}

	@Override
	public Map<String, CustomValue> getCustomValues() {
		return Collections.emptyMap();
	}

	public static class Builder {
		private final String id;
		private final Version version;
		private ModEnvironment environment = ModEnvironment.UNIVERSAL;
		private String name;
		private String description = "";
		private final Collection<Person> authors = new ArrayList<>();
		private final Collection<Person> contributors = new ArrayList<>();
		private ContactInformation contact = ContactInformation.EMPTY;
		private final Collection<String> license = new ArrayList<>();
		private final NavigableMap<Integer, String> icons = new TreeMap<>();
		private final Collection<ModDependency> dependencies = new ArrayList<>();

		public Builder(String id, String version) {
			this.name = this.id = id;

			try {
				this.version = Version.parse(version);
			} catch (VersionParsingException e) {
				throw new RuntimeException(e);
			}
		}

		public Builder setEnvironment(ModEnvironment environment) {
			this.environment = environment;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder addAuthor(String name, Map<String, String> contactMap) {
			this.authors.add(createPerson(name, contactMap));
			return this;
		}

		public Builder addContributor(String name, Map<String, String> contactMap) {
			this.contributors.add(createPerson(name, contactMap));
			return this;
		}

		public Builder setContact(ContactInformation contact) {
			this.contact = contact;
			return this;
		}

		public Builder addLicense(String license) {
			this.license.add(license);
			return this;
		}

		public Builder addIcon(int size, String path) {
			this.icons.put(size, path);
			return this;
		}

		public Builder addDependency(ModDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public ModMetadata build() {
			return new BuiltinModMetadata(id, version, environment, name, description, authors, contributors, contact, license, icons, dependencies);
		}

		private static Person createPerson(String name, Map<String, String> contactMap) {
			return new Person() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public ContactInformation getContact() {
					return contact;
				}

				private final ContactInformation contact = contactMap.isEmpty() ? ContactInformation.EMPTY : new ContactInformationImpl(contactMap);
			};
		}
	}
}
