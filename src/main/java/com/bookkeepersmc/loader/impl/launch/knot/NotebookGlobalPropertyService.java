/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bookkeepersmc.loader.impl.launch.knot;

import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class NotebookGlobalPropertyService implements IGlobalPropertyService {
	@Override
	public IPropertyKey resolveKey(String name) {
		return new MixinStringPropertyKey(name);
	}

	private String keyString(IPropertyKey key) {
		return ((MixinStringPropertyKey) key).key;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key) {
		return (T) NotebookLauncherBase.getProperties().get(keyString(key));
	}

	@Override
	public void setProperty(IPropertyKey key, Object value) {
		NotebookLauncherBase.getProperties().put(keyString(key), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key, T defaultValue) {
		return (T) NotebookLauncherBase.getProperties().getOrDefault(keyString(key), defaultValue);
	}

	@Override
	public String getPropertyString(IPropertyKey key, String defaultValue) {
		Object o = NotebookLauncherBase.getProperties().get(keyString(key));
		return o != null ? o.toString() : defaultValue;
	}
}
