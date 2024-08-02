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
import com.bookkeepersmc.loader.api.metadata.ModMetadata;
import com.bookkeepersmc.loader.api.metadata.Person;
import com.bookkeepersmc.loader.impl.metadata.AbstractModMetadata;
import com.bookkeepersmc.loader.impl.metadata.EntrypointMetadata;
import com.bookkeepersmc.loader.impl.metadata.LoaderModMetadata;
import com.bookkeepersmc.loader.impl.metadata.NestedJarEntry;

class BuiltinMetadataWrapper extends AbstractModMetadata implements LoaderModMetadata {
	private final ModMetadata parent;
	private Version version;
	private Collection<ModDependency> dependencies;

	BuiltinMetadataWrapper(ModMetadata parent) {
		this.parent = parent;

		version = parent.getVersion();
		dependencies = parent.getDependencies();
	}

	@Override
	public String getType() {
		return parent.getType();
	}

	@Override
	public String getId() {
		return parent.getId();
	}

	@Override
	public Collection<String> getProvides() {
		return parent.getProvides();
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public void setVersion(Version version) {
		this.version = version;
	}

	@Override
	public ModEnvironment getEnvironment() {
		return parent.getEnvironment();
	}

	@Override
	public Collection<ModDependency> getDependencies() {
		return dependencies;
	}

	@Override
	public void setDependencies(Collection<ModDependency> dependencies) {
		this.dependencies = Collections.unmodifiableCollection(dependencies);
	}

	@Override
	public String getName() {
		return parent.getName();
	}

	@Override
	public String getDescription() {
		return parent.getDescription();
	}

	@Override
	public Collection<Person> getAuthors() {
		return parent.getAuthors();
	}

	@Override
	public Collection<Person> getContributors() {
		return parent.getContributors();
	}

	@Override
	public ContactInformation getContact() {
		return parent.getContact();
	}

	@Override
	public Collection<String> getLicense() {
		return parent.getLicense();
	}

	@Override
	public Optional<String> getIconPath(int size) {
		return parent.getIconPath(size);
	}

	@Override
	public boolean containsCustomValue(String key) {
		return parent.containsCustomValue(key);
	}

	@Override
	public CustomValue getCustomValue(String key) {
		return parent.getCustomValue(key);
	}

	@Override
	public Map<String, CustomValue> getCustomValues() {
		return parent.getCustomValues();
	}

	@Override
	public int getSchemaVersion() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Map<String, String> getLanguageAdapterDefinitions() {
		return Collections.emptyMap();
	}

	@Override
	public Collection<NestedJarEntry> getJars() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getMixinConfigs(EnvType type) {
		return Collections.emptyList();
	}

	@Override
	public String getAccessWidener() {
		return null;
	}

	@Override
	public boolean loadsInEnvironment(EnvType type) {
		return true;
	}

	@Override
	public Collection<String> getOldInitializers() {
		return Collections.emptyList();
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
}
