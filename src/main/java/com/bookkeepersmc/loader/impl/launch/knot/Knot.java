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
package com.bookkeepersmc.loader.impl.launch.knot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.entrypoint.PreLaunchEntrypoint;
import com.bookkeepersmc.loader.impl.FormattedException;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.game.GameProvider;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.launch.NotebookMixinBootstrap;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.UrlUtil;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public final class Knot extends NotebookLauncherBase {
	private static final boolean IS_DEVELOPMENT = Boolean.parseBoolean(System.getProperty(SystemProperties.DEVELOPMENT, "false"));

	protected Map<String, Object> properties = new HashMap<>();

	private KnotClassLoaderInterface classLoader;
	private EnvType envType;
	private final List<Path> classPath = new ArrayList<>();
	private GameProvider provider;
	private boolean unlocked;

	public static void launch(String[] args, EnvType type) {
		setupUncaughtExceptionHandler();

		try {
			Knot knot = new Knot(type);
			ClassLoader cl = knot.init(args);

			if (knot.provider == null) {
				throw new IllegalStateException("Game provider was not initialized! (Knot#init(String[]))");
			}

			knot.provider.launch(cl);
		} catch (FormattedException e) {
			handleFormattedException(e);
		}
	}

	public Knot(EnvType type) {
		this.envType = type;
	}

	public ClassLoader init(String[] args) {
		setProperties(properties);

		// configure fabric vars
		if (envType == null) {
			String side = System.getProperty(SystemProperties.SIDE);
			if (side == null) throw new RuntimeException("Please specify side or use a dedicated Knot!");

			switch (side.toLowerCase(Locale.ROOT)) {
			case "client":
				envType = EnvType.CLIENT;
				break;
			case "server":
				envType = EnvType.SERVER;
				break;
			default:
				throw new RuntimeException("Invalid side provided: must be \"client\" or \"server\"!");
			}
		}

		classPath.clear();

		List<String> missing = null;
		List<String> unsupported = null;

		for (String cpEntry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (cpEntry.equals("*") || cpEntry.endsWith(File.separator + "*")) {
				if (unsupported == null) unsupported = new ArrayList<>();
				unsupported.add(cpEntry);
				continue;
			}

			Path path = Paths.get(cpEntry);

			if (!Files.exists(path)) {
				if (missing == null) missing = new ArrayList<>();
				missing.add(cpEntry);
				continue;
			}

			classPath.add(LoaderUtil.normalizeExistingPath(path));
		}

		if (unsupported != null) Log.warn(LogCategory.KNOT, "Knot does not support wildcard class path entries: %s - the game may not load properly!", String.join(", ", unsupported));
		if (missing != null) Log.warn(LogCategory.KNOT, "Class path entries reference missing files: %s - the game may not load properly!", String.join(", ", missing));

		provider = createGameProvider(args);
		Log.finishBuiltinConfig();
		Log.info(LogCategory.GAME_PROVIDER, "Loading %s %s with Notebook %s", provider.getGameName(), provider.getRawGameVersion(), NotebookLoaderImpl.VERSION);

		// Setup classloader
		// TODO: Provide KnotCompatibilityClassLoader in non-exclusive-Fabric pre-1.13 environments?
		boolean useCompatibility = provider.requiresUrlClassLoader() || Boolean.parseBoolean(System.getProperty("fabric.loader.useCompatibilityClassLoader", "false"));
		classLoader = KnotClassLoaderInterface.create(useCompatibility, isDevelopment(), envType, provider);
		ClassLoader cl = classLoader.getClassLoader();

		provider.initialize(this);

		Thread.currentThread().setContextClassLoader(cl);

		NotebookLoaderImpl loader = NotebookLoaderImpl.INSTANCE;
		loader.setGameProvider(provider);
		loader.load();
		loader.freeze();

		NotebookLoaderImpl.INSTANCE.loadAccessWideners();

		NotebookMixinBootstrap.init(getEnvironmentType(), loader);
		NotebookLauncherBase.finishMixinBootstrapping();

		classLoader.initializeTransformers();

		provider.unlockClassPath(this);
		unlocked = true;

		try {
			loader.invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
		} catch (RuntimeException e) {
			throw FormattedException.ofLocalized("exception.initializerFailure", e);
		}

		return cl;
	}

	private GameProvider createGameProvider(String[] args) {
		// fast path with direct lookup

		GameProvider embeddedGameProvider = findEmbedddedGameProvider();

		if (embeddedGameProvider != null
				&& embeddedGameProvider.isEnabled()
				&& embeddedGameProvider.locateGame(this, args)) {
			return embeddedGameProvider;
		}

		// slow path with service loader

		List<GameProvider> failedProviders = new ArrayList<>();

		for (GameProvider provider : ServiceLoader.load(GameProvider.class)) {
			if (!provider.isEnabled()) continue; // don't attempt disabled providers and don't include them in the error report

			if (provider != embeddedGameProvider // don't retry already failed provider
					&& provider.locateGame(this, args)) {
				return provider;
			}

			failedProviders.add(provider);
		}

		// nothing found

		String msg;

		if (failedProviders.isEmpty()) {
			msg = "No game providers present on the class path!";
		} else if (failedProviders.size() == 1) {
			msg = String.format("%s game provider couldn't locate the game! "
					+ "The game may be absent from the class path, lacks some expected files, suffers from jar "
					+ "corruption or is of an unsupported variety/version.",
					failedProviders.get(0).getGameName());
		} else {
			msg = String.format("None of the game providers (%s) were able to locate their game!",
					failedProviders.stream().map(GameProvider::getGameName).collect(Collectors.joining(", ")));
		}

		Log.error(LogCategory.GAME_PROVIDER, msg);

		throw new RuntimeException(msg);
	}

	/**
	 * Find game provider embedded into the Fabric Loader jar, best effort.
	 *
	 * <p>This is faster than going through service loader because it only looks at a single jar.
	 */
	private static GameProvider findEmbedddedGameProvider() {
		try {
			Path flPath = UrlUtil.getCodeSource(Knot.class);
			if (flPath == null || !flPath.getFileName().toString().endsWith(".jar")) return null; // not a jar

			try (ZipFile zf = new ZipFile(flPath.toFile())) {
				ZipEntry entry = zf.getEntry("META-INF/services/com.bookkeepersmc.loader.impl.game.GameProvider"); // same file as used by service loader
				if (entry == null) return null;

				try (InputStream is = zf.getInputStream(entry)) {
					byte[] buffer = new byte[100];
					int offset = 0;
					int len;

					while ((len = is.read(buffer, offset, buffer.length - offset)) >= 0) {
						offset += len;
						if (offset == buffer.length) buffer = Arrays.copyOf(buffer, buffer.length * 2);
					}

					String content = new String(buffer, 0, offset, StandardCharsets.UTF_8).trim();
					if (content.indexOf('\n') >= 0) return null; // potentially more than one entry -> bail out

					int pos = content.indexOf('#');
					if (pos >= 0) content = content.substring(0, pos).trim();

					if (!content.isEmpty()) {
						return (GameProvider) Class.forName(content).getConstructor().newInstance();
					}
				}
			}

			return null;
		} catch (IOException | ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getTargetNamespace() {
		// TODO: Won't work outside of Yarn
		return IS_DEVELOPMENT ? "named" : "intermediary";
	}

	@Override
	public List<Path> getClassPath() {
		return classPath;
	}

	@Override
	public void addToClassPath(Path path, String... allowedPrefixes) {
		Log.debug(LogCategory.KNOT, "Adding " + path + " to classpath.");

		classLoader.setAllowedPrefixes(path, allowedPrefixes);
		classLoader.addCodeSource(path);
	}

	@Override
	public void setAllowedPrefixes(Path path, String... prefixes) {
		classLoader.setAllowedPrefixes(path, prefixes);
	}

	@Override
	public void setValidParentClassPath(Collection<Path> paths) {
		classLoader.setValidParentClassPath(paths);
	}

	@Override
	public EnvType getEnvironmentType() {
		return envType;
	}

	@Override
	public boolean isClassLoaded(String name) {
		return classLoader.isClassLoaded(name);
	}

	@Override
	public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
		return classLoader.loadIntoTarget(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return classLoader.getClassLoader().getResourceAsStream(name);
	}

	@Override
	public ClassLoader getTargetClassLoader() {
		KnotClassLoaderInterface classLoader = this.classLoader;

		return classLoader != null ? classLoader.getClassLoader() : null;
	}

	@Override
	public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		if (!unlocked) throw new IllegalStateException("early getClassByteArray access");

		if (runTransformers) {
			return classLoader.getPreMixinClassBytes(name);
		} else {
			return classLoader.getRawClassBytes(name);
		}
	}

	@Override
	public Manifest getManifest(Path originPath) {
		return classLoader.getManifest(originPath);
	}

	@Override
	public boolean isDevelopment() {
		return IS_DEVELOPMENT;
	}

	@Override
	public String getEntrypoint() {
		return provider.getEntrypoint();
	}

	public static void main(String[] args) {
		new Knot(null).init(args);
	}

	static {
		LoaderUtil.verifyNotInTargetCl(Knot.class);

		if (IS_DEVELOPMENT) {
			LoaderUtil.verifyClasspath();
		}
	}
}
