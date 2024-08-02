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
package com.bookkeepersmc.loader.impl.util;

public final class SystemProperties {
	// whether fabric loader is running in a development environment / mode, affects class path mod discovery, remapping, logging, ...
	public static final String DEVELOPMENT = "notebook.development";
	public static final String SIDE = "notebook.side";
	// skips the embedded MC game provider, letting ServiceLoader-provided ones take over
	public static final String SKIP_MC_PROVIDER = "notebook.skipMcProvider";
	// game jar paths for common/client/server, replaces lookup from class path if present, env specific takes precedence
	public static final String GAME_JAR_PATH = "notebook.gameJarPath";
	public static final String GAME_JAR_PATH_CLIENT = "notebook.gameJarPath.client";
	public static final String GAME_JAR_PATH_SERVER = "notebook.gameJarPath.server";
	// set the game version for the builtin game mod/dependencies, bypassing auto-detection
	public static final String GAME_VERSION = "notebook.gameVersion";
	// fallback log file for the builtin log handler (dumped on exit if not replaced with another handler)
	public static final String LOG_FILE = "notebook.log.file";
	// minimum log level for builtin log handler
	public static final String LOG_LEVEL = "notebook.log.level";
	// a path to a directory to replace the default mod search directory
	public static final String MODS_FOLDER = "notebook.modsFolder";
	// additional mods to load (path separator separated paths, @ prefix for meta-file with each line referencing an actual file)
	public static final String ADD_MODS = "notebook.addMods";
	// a comma-separated list of mod ids to disable, even if they're discovered. mostly useful for unit testing.
	public static final String DISABLE_MOD_IDS = "notebook.debug.disableModIds";
	// file containing the class path for in-dev runtime mod remapping
	public static final String REMAP_CLASSPATH_FILE = "notebook.remapClasspathFile";
	// class path groups to map multiple class path entries to a mod (paths separated by path separator, groups by double path separator)
	public static final String PATH_GROUPS = "notebook.classPathGroups";
	// system level libraries, matching code sources will not be assumed to be part of the game or mods and remain on the system class path (paths separated by path separator)
	public static final String SYSTEM_LIBRARIES = "notebook.systemLibraries";
	// throw exceptions from entrypoints, discovery etc. directly instead of gathering and attaching as suppressed
	public static final String DEBUG_THROW_DIRECTLY = "notebook.debug.throwDirectly";
	// logs library classification activity
	public static final String DEBUG_LOG_LIB_CLASSIFICATION = "notebook.debug.logLibClassification";
	// logs class loading
	public static final String DEBUG_LOG_CLASS_LOAD = "notebook.debug.logClassLoad";
	// logs class loading errors to uncover caught exceptions without adequate logging
	public static final String DEBUG_LOG_CLASS_LOAD_ERRORS = "notebook.debug.logClassLoadErrors";
	// logs class transformation errors to uncover caught exceptions without adequate logging
	public static final String DEBUG_LOG_TRANSFORM_ERRORS = "notebook.debug.logTransformErrors";
	// disables system class path isolation, allowing bogus lib accesses (too early, transient jars)
	public static final String DEBUG_DISABLE_CLASS_PATH_ISOLATION = "notebook.debug.disableClassPathIsolation";
	// disables mod load order shuffling to be the same in-dev as in production
	public static final String DEBUG_DISABLE_MOD_SHUFFLE = "notebook.debug.disableModShuffle";
	// workaround for bad load order dependencies
	public static final String DEBUG_LOAD_LATE = "notebook.debug.loadLate";
	// override the mod discovery timeout, unit in seconds, <= 0 to disable
	public static final String DEBUG_DISCOVERY_TIMEOUT = "notebook.debug.discoveryTimeout";
	// override the mod resolution timeout, unit in seconds, <= 0 to disable
	public static final String DEBUG_RESOLUTION_TIMEOUT = "notebook.debug.resolutionTimeout";
	// replace mod versions (modA:versionA,modB:versionB,...)
	public static final String DEBUG_REPLACE_VERSION = "notebook.debug.replaceVersion";
	// deobfuscate the game jar with the classpath
	public static final String DEBUG_DEOBFUSCATE_WITH_CLASSPATH = "notebook.debug.deobfuscateWithClasspath";
	// whether fabric loader is running in a unit test, this affects logging classpath setup
	public static final String UNIT_TEST = "notebook.unitTest";

	private SystemProperties() {
	}
}
