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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.metadata.ModDependency;
import com.bookkeepersmc.loader.api.metadata.ModDependency.Kind;
import com.bookkeepersmc.loader.impl.discovery.ModSolver.InactiveReason;
import com.bookkeepersmc.loader.impl.metadata.ModDependencyImpl;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class ModResolver {
	public static List<ModCandidate> resolve(Collection<ModCandidate> candidates, EnvType envType, Map<String, Set<ModCandidate>> envDisabledMods) throws ModResolutionException {
		long startTime = System.nanoTime();
		List<ModCandidate> result = findCompatibleSet(candidates, envType, envDisabledMods);

		long endTime = System.nanoTime();
		Log.debug(LogCategory.RESOLUTION, "Mod resolution time: %.1f ms", (endTime - startTime) * 1e-6);

		return result;
	}

	private static List<ModCandidate> findCompatibleSet(Collection<ModCandidate> candidates, EnvType envType, Map<String, Set<ModCandidate>> envDisabledMods) throws ModResolutionException {
		// sort all mods by priority and group by id

		List<ModCandidate> allModsSorted = new ArrayList<>(candidates);
		Map<String, List<ModCandidate>> modsById = new LinkedHashMap<>(); // linked to ensure consistent execution

		ModPrioSorter.sort(allModsSorted, modsById);

		// soften positive deps from schema 0 or 1 mods on mods that are present but disabled for the current env
		// this is a workaround necessary due to many mods declaring deps that are unsatisfiable in some envs and loader before 0.12x not verifying them properly

		for (ModCandidate mod : allModsSorted) {
			if (mod.getMetadata().getSchemaVersion() >= 2) continue;

			for (ModDependency dep : mod.getMetadata().getDependencies()) {
				if (!dep.getKind().isPositive() || dep.getKind() == Kind.SUGGESTS) continue; // no positive dep or already suggests
				if (!(dep instanceof ModDependencyImpl)) continue; // can't modify dep kind
				if (modsById.containsKey(dep.getModId())) continue; // non-disabled match available

				Collection<ModCandidate> disabledMatches = envDisabledMods.get(dep.getModId());
				if (disabledMatches == null) continue; // no disabled id matches

				for (ModCandidate m : disabledMatches) {
					if (dep.matches(m.getVersion())) { // disabled version match -> remove dep
						((ModDependencyImpl) dep).setKind(Kind.SUGGESTS);
						break;
					}
				}
			}
		}

		// preselect mods, check for builtin mod collisions

		List<ModCandidate> preselectedMods = new ArrayList<>();

		for (List<ModCandidate> mods : modsById.values()) {
			ModCandidate builtinMod = null;

			for (ModCandidate mod : mods) {
				if (mod.isBuiltin()) {
					builtinMod = mod;
					break;
				}
			}

			if (builtinMod == null) continue;

			if (mods.size() > 1) {
				mods.remove(builtinMod);
				throw new ModResolutionException("Mods share ID with builtin mod %s: %s", builtinMod, mods);
			}

			preselectedMods.add(builtinMod);
		}

		Map<String, ModCandidate> selectedMods = new HashMap<>(allModsSorted.size());
		List<ModCandidate> uniqueSelectedMods = new ArrayList<>(allModsSorted.size());

		for (ModCandidate mod : preselectedMods) {
			preselectMod(mod, allModsSorted, modsById, selectedMods, uniqueSelectedMods);
		}

		// solve

		ModSolver.Result result;

		try {
			result = ModSolver.solve(allModsSorted, modsById,
					selectedMods, uniqueSelectedMods);
		} catch (ContradictionException | TimeoutException e) {
			throw new ModResolutionException("Solving failed", e);
		}

		if (!result.success) {
			Log.warn(LogCategory.RESOLUTION, "Mod resolution failed");
			Log.info(LogCategory.RESOLUTION, "Immediate reason: %s%n", result.immediateReason);
			Log.info(LogCategory.RESOLUTION, "Reason: %s%n", result.reason);
			if (!envDisabledMods.isEmpty()) Log.info(LogCategory.RESOLUTION, "%s environment disabled: %s%n", envType.name(), envDisabledMods.keySet());

			if (result.fix == null) {
				Log.info(LogCategory.RESOLUTION, "No fix?");
			} else {
				Log.info(LogCategory.RESOLUTION, "Fix: add %s, remove %s, replace [%s]%n",
						result.fix.modsToAdd,
						result.fix.modsToRemove,
						result.fix.modReplacements.entrySet().stream().map(e -> String.format("%s -> %s", e.getValue(), e.getKey())).collect(Collectors.joining(", ")));

				for (Collection<ModCandidate> mods : envDisabledMods.values()) {
					for (ModCandidate m : mods) {
						result.fix.inactiveMods.put(m, InactiveReason.WRONG_ENVIRONMENT);
					}
				}
			}

			throw new ModResolutionException("Some of your mods are incompatible with the game or each other!%s",
					ResultAnalyzer.gatherErrors(result, selectedMods, modsById, envDisabledMods, envType));
		}

		uniqueSelectedMods.sort(Comparator.comparing(ModCandidate::getId));

		// clear cached data and inbound refs for unused mods, set minNestLevel for used non-root mods to max, queue root mods

		Queue<ModCandidate> queue = new ArrayDeque<>();

		for (ModCandidate mod : allModsSorted) {
			if (selectedMods.get(mod.getId()) == mod) { // mod is selected
				if (!mod.resetMinNestLevel()) { // -> is root
					queue.add(mod);
				}
			} else {
				mod.clearCachedData();

				for (ModCandidate m : mod.getNestedMods()) {
					m.getParentMods().remove(mod);
				}

				for (ModCandidate m : mod.getParentMods()) {
					m.getNestedMods().remove(mod);
				}
			}
		}

		// recompute minNestLevel (may have changed due to parent associations having been dropped by the above step)

		{
			ModCandidate mod;

			while ((mod = queue.poll()) != null) {
				for (ModCandidate child : mod.getNestedMods()) {
					if (child.updateMinNestLevel(mod)) {
						queue.add(child);
					}
				}
			}
		}

		String warnings = ResultAnalyzer.gatherWarnings(uniqueSelectedMods, selectedMods,
				envDisabledMods, envType);

		if (warnings != null) {
			Log.warn(LogCategory.RESOLUTION, "Warnings were found!%s", warnings);
		}

		return uniqueSelectedMods;
	}

	static void preselectMod(ModCandidate mod, List<ModCandidate> allModsSorted, Map<String, List<ModCandidate>> modsById,
			Map<String, ModCandidate> selectedMods, List<ModCandidate> uniqueSelectedMods) throws ModResolutionException {
		selectMod(mod, selectedMods, uniqueSelectedMods);

		allModsSorted.removeAll(modsById.remove(mod.getId()));

		for (String provided : mod.getProvides()) {
			allModsSorted.removeAll(modsById.remove(provided));
		}
	}

	static void selectMod(ModCandidate mod, Map<String, ModCandidate> selectedMods, List<ModCandidate> uniqueSelectedMods) throws ModResolutionException {
		ModCandidate prev = selectedMods.put(mod.getId(), mod);
		if (prev != null) throw new ModResolutionException("duplicate mod %s", mod.getId());

		for (String provided : mod.getProvides()) {
			prev = selectedMods.put(provided, mod);
			if (prev != null) throw new ModResolutionException("duplicate mod %s", provided);
		}

		uniqueSelectedMods.add(mod);
	}
}
