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
package com.bookkeepersmc.loader.impl.game;

import java.net.URL;
import java.nio.file.Path;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.impl.util.UrlConversionException;
import com.bookkeepersmc.loader.impl.util.UrlUtil;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.util.CheckClassAdapter;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.spongepowered.asm.launch.MixinBootstrap;

enum LoaderLibrary {
	FABRIC_LOADER(UrlUtil.LOADER_CODE_SOURCE),
	MAPPING_IO(MappingTree.class),
	SPONGE_MIXIN(MixinBootstrap.class),
	TINY_REMAPPER(TinyRemapper.class),
	ACCESS_WIDENER(AccessWidener.class),
	ASM(ClassReader.class),
	ASM_ANALYSIS(Analyzer.class),
	ASM_COMMONS(Remapper.class),
	ASM_TREE(ClassNode.class),
	ASM_UTIL(CheckClassAdapter.class),
	SAT4J_CORE(ContradictionException.class),
	SAT4J_PB(SolverFactory.class),
	SERVER_LAUNCH("fabric-server-launch.properties", EnvType.SERVER), // installer generated jar to run setup loader's class path
	SERVER_LAUNCHER("com/bookkeepersmc/installer/ServerLauncher.class", EnvType.SERVER), // installer based launch-through method
	JUNIT_API("org/junit/jupiter/api/Test.class", null),
	JUNIT_PLATFORM_ENGINE("org/junit/platform/engine/TestEngine.class", null),
	JUNIT_PLATFORM_LAUNCHER("org/junit/platform/launcher/core/LauncherFactory.class", null),
	JUNIT_JUPITER("org/junit/jupiter/engine/JupiterTestEngine.class", null),
	FABRIC_LOADER_JUNIT("net/fabricmc/loader/impl/junit/FabricLoaderLauncherSessionListener.class", null),

	// Logging libraries are only loaded from the platform CL when running as a unit test.
	LOG4J_API("org/apache/logging/log4j/LogManager.class", true),
	LOG4J_CORE("META-INF/services/org.apache.logging.log4j.spi.Provider", true),
	LOG4J_CONFIG("log4j2.xml", true),
	LOG4J_PLUGIN_3("net/minecrell/terminalconsole/util/LoggerNamePatternSelector.class", true),
	SLF4J_API("org/slf4j/Logger.class", true);

	final Path path;
	final EnvType env;
	final boolean junitRunOnly;

	LoaderLibrary(Class<?> cls) {
		this(UrlUtil.getCodeSource(cls));
	}

	LoaderLibrary(Path path) {
		if (path == null) throw new RuntimeException("missing loader library "+name());

		this.path = path;
		this.env = null;
		this.junitRunOnly = false;
	}

	LoaderLibrary(String file, EnvType env) {
		this(file, env, false);
	}

	LoaderLibrary(String file, EnvType env, boolean junitRunOnly) {
		URL url = LoaderLibrary.class.getClassLoader().getResource(file);

		try {
			this.path = url != null ? UrlUtil.getCodeSource(url, file) : null;
			this.env = env;
		} catch (UrlConversionException e) {
			throw new RuntimeException(e);
		}

		this.junitRunOnly = junitRunOnly;
	}

	LoaderLibrary(String path, boolean loggerLibrary) {
		this(path, null, loggerLibrary);
	}

	boolean isApplicable(EnvType env, boolean junitRun) {
		return (this.env == null || this.env == env)
				&& (!junitRunOnly || junitRun);
	}
}
