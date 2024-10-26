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
package com.bookkeepersmc.loader.impl.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.api.metadata.ModDependency;
import com.bookkeepersmc.loader.api.metadata.ModDependency.Kind;
import com.bookkeepersmc.loader.api.metadata.version.VersionInterval;
import com.bookkeepersmc.loader.impl.ModContainerImpl;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.launch.knot.MixinServiceKnot;
import com.bookkeepersmc.loader.impl.launch.knot.MixinServiceKnotBootstrap;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;
import com.bookkeepersmc.loader.impl.util.mappings.MixinIntermediaryDevRemapper;

import net.fabricmc.mappingio.tree.MappingTree;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

public final class NotebookMixinBootstrap {
	private NotebookMixinBootstrap() { }

	private static boolean initialized = false;

	public static void init(EnvType side, NotebookLoaderImpl loader) {
		if (initialized) {
			throw new RuntimeException("FabricMixinBootstrap has already been initialized!");
		}

		System.setProperty("mixin.bootstrapService", MixinServiceKnotBootstrap.class.getName());
		System.setProperty("mixin.service", MixinServiceKnot.class.getName());

		MixinBootstrap.init();

		if (NotebookLauncherBase.getLauncher().isDevelopment()) {
			MappingConfiguration mappingConfiguration = NotebookLauncherBase.getLauncher().getMappingConfiguration();
			MappingTree mappings = mappingConfiguration.getMappings();

			if (mappings != null) {
				List<String> namespaces = new ArrayList<>(mappings.getDstNamespaces());
				namespaces.add(mappings.getSrcNamespace());

				if (namespaces.contains("intermediary") && namespaces.contains(mappingConfiguration.getTargetNamespace())) {
					System.setProperty("mixin.env.remapRefMap", "true");

					try {
						MixinIntermediaryDevRemapper remapper = new MixinIntermediaryDevRemapper(mappings, "intermediary", mappingConfiguration.getTargetNamespace());
						MixinEnvironment.getDefaultEnvironment().getRemappers().add(remapper);
						Log.info(LogCategory.MIXIN, "Loaded development mappings for mixin remapper!");
					} catch (Exception e) {
						Log.error(LogCategory.MIXIN, "Development environment setup error - the game will probably crash soon!");
						e.printStackTrace();
					}
				}
			}
		}

		Map<String, ModContainerImpl> configToModMap = new HashMap<>();

		for (ModContainerImpl mod : loader.getModsInternal()) {
			for (String config : mod.getMetadata().getMixinConfigs(side)) {
				ModContainerImpl prev = configToModMap.putIfAbsent(config, mod);
				if (prev != null) throw new RuntimeException(String.format("Non-unique Mixin config name %s used by the mods %s and %s", config, prev.getMetadata().getId(), mod.getMetadata().getId()));

				try {
					Mixins.addConfiguration(config);
				} catch (Throwable t) {
					throw new RuntimeException(String.format("Error parsing Mixin config %s for mod %s", config, mod.getMetadata().getId()), t);
				}
			}
		}

		for (Config config : Mixins.getConfigs()) {
			ModContainerImpl mod = configToModMap.get(config.getName());
			if (mod == null) continue;
		}

		try {
			IMixinConfig.class.getMethod("decorate", String.class, Object.class);
			MixinConfigDecorator.apply(configToModMap);
		} catch (NoSuchMethodException e) {
			Log.info(LogCategory.MIXIN, "Detected old Mixin version without config decoration support");
		}

		initialized = true;
	}

	private static final class MixinConfigDecorator {
		private static final List<LoaderMixinVersionEntry> versions = new ArrayList<>();

		static {
			// maximum loader version and bundled fabric mixin version, DESCENDING ORDER, LATEST FIRST
			// loader versions with new mixin versions need to be added here

			// TODO: change to notebook
			addVersion("0.2.0", FabricUtil.COMPATIBILITY_0_14_0);
		}

		static void apply(Map<String, ModContainerImpl> configToModMap) {
			for (Config rawConfig : Mixins.getConfigs()) {
				ModContainerImpl mod = configToModMap.get(rawConfig.getName());
				if (mod == null) continue;

				IMixinConfig config = rawConfig.getConfig();
				config.decorate(FabricUtil.KEY_MOD_ID, mod.getMetadata().getId());
				config.decorate(FabricUtil.KEY_COMPATIBILITY, getMixinCompat(mod));
			}
		}

		private static int getMixinCompat(ModContainerImpl mod) {
			// infer from loader dependency by determining the least relevant loader version the mod accepts
			// AND any loader deps

			List<VersionInterval> reqIntervals = Collections.singletonList(VersionInterval.INFINITE);

			for (ModDependency dep : mod.getMetadata().getDependencies()) {
				if (dep.getModId().equals("notebookloader") || dep.getModId().equals("notebook-loader")) {
					if (dep.getKind() == Kind.DEPENDS) {
						reqIntervals = VersionInterval.and(reqIntervals, dep.getVersionIntervals());
					} else if (dep.getKind() == Kind.BREAKS) {
						reqIntervals = VersionInterval.and(reqIntervals, VersionInterval.not(dep.getVersionIntervals()));
					}
				}
			}

			if (reqIntervals.isEmpty()) throw new IllegalStateException("mod "+mod+" is incompatible with every loader version?"); // shouldn't get there

			Version minLoaderVersion = reqIntervals.get(0).getMin(); // it is sorted, to 0 has the absolute lower bound

			if (minLoaderVersion != null) { // has a lower bound
				for (LoaderMixinVersionEntry version : versions) {
					if (minLoaderVersion.compareTo(version.loaderVersion) >= 0) { // lower bound is >= current version
						return version.mixinVersion;
					}
				}
			}

			return FabricUtil.COMPATIBILITY_0_9_2;
		}

		private static void addVersion(String minLoaderVersion, int mixinCompat) {
			try {
				versions.add(new LoaderMixinVersionEntry(SemanticVersion.parse(minLoaderVersion), mixinCompat));
			} catch (VersionParsingException e) {
				throw new RuntimeException(e);
			}
		}

		private static final class LoaderMixinVersionEntry {
			final SemanticVersion loaderVersion;
			final int mixinVersion;

			LoaderMixinVersionEntry(SemanticVersion loaderVersion, int mixinVersion) {
				this.loaderVersion = loaderVersion;
				this.mixinVersion = mixinVersion;
			}
		}
	}
}
