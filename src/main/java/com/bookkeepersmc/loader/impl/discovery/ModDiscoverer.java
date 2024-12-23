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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.SemanticVersion;
import com.bookkeepersmc.loader.api.metadata.ModMetadata;
import com.bookkeepersmc.loader.impl.FormattedException;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;
import com.bookkeepersmc.loader.impl.discovery.ModCandidateFinder.ModCandidateConsumer;
import com.bookkeepersmc.loader.impl.game.GameProvider.BuiltinMod;
import com.bookkeepersmc.loader.impl.metadata.BuiltinModMetadata;
import com.bookkeepersmc.loader.impl.metadata.DependencyOverrides;
import com.bookkeepersmc.loader.impl.metadata.LoaderModMetadata;
import com.bookkeepersmc.loader.impl.metadata.MetadataVerifier;
import com.bookkeepersmc.loader.impl.metadata.ModMetadataParser;
import com.bookkeepersmc.loader.impl.metadata.NestedJarEntry;
import com.bookkeepersmc.loader.impl.metadata.ParseMetadataException;
import com.bookkeepersmc.loader.impl.metadata.VersionOverrides;
import com.bookkeepersmc.loader.impl.util.ExceptionUtil;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public final class ModDiscoverer {
	private final VersionOverrides versionOverrides;
	private final DependencyOverrides depOverrides;
	private final List<ModCandidateFinder> candidateFinders = new ArrayList<>();
	private final EnvType envType = NotebookLoaderImpl.INSTANCE.getEnvironmentType();
	private final Map<Long, ModScanTask> jijDedupMap = new ConcurrentHashMap<>(); // avoids reading the same jar twice
	private final List<NestedModInitData> nestedModInitDatas = Collections.synchronizedList(new ArrayList<>()); // breaks potential cycles from deduplication
	private final List<Path> nonNotebookMods = Collections.synchronizedList(new ArrayList<>());

	public ModDiscoverer(VersionOverrides versionOverrides, DependencyOverrides depOverrides) {
		this.versionOverrides = versionOverrides;
		this.depOverrides = depOverrides;
	}

	public void addCandidateFinder(ModCandidateFinder f) {
		candidateFinders.add(f);
	}

	public List<ModCandidateImpl> discoverMods(NotebookLoaderImpl loader, Map<String, Set<ModCandidateImpl>> envDisabledModsOut) throws ModResolutionException {
		long startTime = System.nanoTime();
		ForkJoinPool pool = new ForkJoinPool();
		Set<Path> processedPaths = new HashSet<>(); // suppresses duplicate paths
		List<Future<ModCandidateImpl>> futures = new ArrayList<>();

		ModCandidateConsumer taskSubmitter = (paths, requiresRemap) -> {
			List<Path> pendingPaths = new ArrayList<>(paths.size());

			for (Path path : paths) {
				assert path.equals(LoaderUtil.normalizeExistingPath(path));

				if (processedPaths.add(path)) {
					pendingPaths.add(path);
				}
			}

			if (!pendingPaths.isEmpty()) {
				futures.add(pool.submit(new ModScanTask(pendingPaths, requiresRemap)));
			}
		};

		for (ModCandidateFinder finder : candidateFinders) {
			finder.findCandidates(taskSubmitter);
		}

		List<ModCandidateImpl> candidates = new ArrayList<>();

		// add builtin mods
		for (BuiltinMod mod : loader.getGameProvider().getBuiltinMods()) {
			if (!(mod.metadata.getVersion() instanceof SemanticVersion)) {
				String error = String.format("%s uses the non-semantic version %s, which doesn't support range comparisons and may cause mod dependencies against it to fail unexpectedly. Consider updating Fabric Loader or explicitly specifying the game version with the fabric.gameVersion system property.",
						mod.metadata.getId(), mod.metadata.getVersion());

				if (loader.isDevelopmentEnvironment()) { // fail hard in-dev
					throw new FormattedException("Invalid game version", error);
				} else {
					Log.warn(LogCategory.GENERAL, error);
				}
			}

			ModCandidateImpl candidate = ModCandidateImpl.createBuiltin(mod, versionOverrides, depOverrides);
			candidates.add(MetadataVerifier.verifyIndev(candidate, loader.isDevelopmentEnvironment()));
		}

		// Add the current Java version
		candidates.add(MetadataVerifier.verifyIndev(createJavaMod(), loader.isDevelopmentEnvironment()));

		ModResolutionException exception = null;

		int timeout = Integer.getInteger(SystemProperties.DEBUG_DISCOVERY_TIMEOUT, 60);
		if (timeout <= 0) timeout = Integer.MAX_VALUE;

		try {
			pool.shutdown();

			pool.awaitTermination(timeout, TimeUnit.SECONDS);

			for (Future<ModCandidateImpl> future : futures) {
				if (!future.isDone()) {
					throw new TimeoutException();
				}

				try {
					ModCandidateImpl candidate = future.get();
					if (candidate != null) candidates.add(candidate);
				} catch (ExecutionException e) {
					exception = ExceptionUtil.gatherExceptions(e, exception, exc -> new ModResolutionException("Mod discovery failed!", exc));
				}
			}

			for (NestedModInitData data : nestedModInitDatas) {
				for (Future<ModCandidateImpl> future : data.futures) {
					if (!future.isDone()) {
						throw new TimeoutException();
					}

					try {
						ModCandidateImpl candidate = future.get();
						if (candidate != null) data.target.add(candidate);
					} catch (ExecutionException e) {
						exception = ExceptionUtil.gatherExceptions(e, exception, exc -> new ModResolutionException("Mod discovery failed!", exc));
					}
				}
			}
		} catch (TimeoutException e) {
			throw new FormattedException("Mod discovery took too long!",
					"Analyzing the mod folder contents took longer than %d seconds. This may be caused by unusually slow hardware, pathological antivirus interference or other issues. The timeout can be changed with the system property %s (-D%<s=<desired timeout in seconds>).",
					timeout, SystemProperties.DEBUG_DISCOVERY_TIMEOUT);
		} catch (InterruptedException e) {
			throw new FormattedException("Mod discovery interrupted!", e);
		}

		if (exception != null) {
			throw exception;
		}

		// get optional set of disabled mod ids
		Set<String> disabledModIds = findDisabledModIds();

		// gather all mods (root+nested), initialize parent data

		Set<ModCandidateImpl> ret = Collections.newSetFromMap(new IdentityHashMap<>(candidates.size() * 2));
		Queue<ModCandidateImpl> queue = new ArrayDeque<>(candidates);
		ModCandidateImpl mod;

		while ((mod = queue.poll()) != null) {
			if (mod.getMetadata().loadsInEnvironment(envType)) {
				if (disabledModIds.contains(mod.getId())) {
					Log.info(LogCategory.DISCOVERY, "Skipping disabled mod %s", mod.getId());
					continue;
				}

				if (!ret.add(mod)) continue;

				for (ModCandidateImpl child : mod.getNestedMods()) {
					if (child.addParent(mod)) {
						queue.add(child);
					}
				}
			} else {
				envDisabledModsOut.computeIfAbsent(mod.getId(), ignore -> Collections.newSetFromMap(new IdentityHashMap<>())).add(mod);
			}
		}

		long endTime = System.nanoTime();

		Log.debug(LogCategory.DISCOVERY, "Mod discovery time: %.1f ms", (endTime - startTime) * 1e-6);

		return new ArrayList<>(ret);
	}

	public List<Path> getNonNotebookMods() {
		return Collections.unmodifiableList(nonNotebookMods);
	}

	// retrieve set of disabled mod ids from system property
	private static Set<String> findDisabledModIds() {
		String modIdList = System.getProperty(SystemProperties.DISABLE_MOD_IDS);

		if (modIdList == null) {
			return Collections.emptySet();
		}

		Set<String> disabledModIds = Arrays.stream(modIdList.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
		Log.debug(LogCategory.DISCOVERY, "Disabled mod ids: %s", disabledModIds);
		return disabledModIds;
	}

	private ModCandidateImpl createJavaMod() {
		ModMetadata metadata = new BuiltinModMetadata.Builder("java", System.getProperty("java.specification.version").replaceFirst("^1\\.", ""))
				.setName(System.getProperty("java.vm.name"))
				.build();
		BuiltinMod builtinMod = new BuiltinMod(Collections.singletonList(Paths.get(System.getProperty("java.home"))), metadata);

		return ModCandidateImpl.createBuiltin(builtinMod, versionOverrides, depOverrides);
	}

	@SuppressWarnings("serial")
	final class ModScanTask extends RecursiveTask<ModCandidateImpl> {
		private final List<Path> paths;
		private final String localPath;
		private final RewindableInputStream is;
		private final long hash;
		private final boolean requiresRemap;
		private final List<String> parentPaths;

		ModScanTask(List<Path> paths, boolean requiresRemap) {
			this(paths, null, null, -1, requiresRemap, Collections.emptyList());
		}

		private ModScanTask(List<Path> paths, String localPath, RewindableInputStream is, long hash,
				boolean requiresRemap, List<String> parentPaths) {
			this.paths = paths;
			this.localPath = localPath != null ? localPath : paths.get(0).toString();
			this.is = is;
			this.hash = hash;
			this.requiresRemap = requiresRemap;
			this.parentPaths = parentPaths;
		}

		@Override
		protected ModCandidateImpl compute() {
			if (is != null) { // nested jar
				try {
					return computeJarStream();
				} catch (ParseMetadataException e) { // already contains all context
					throw ExceptionUtil.wrap(e);
				} catch (Throwable t) {
					throw new RuntimeException(String.format("Error analyzing nested jar %s from %s: %s", localPath, parentPaths, t), t);
				}
			} else { // regular classes-dir or jar
				try {
					for (Path path : paths) {
						final ModCandidateImpl candidate;

						if (Files.isDirectory(path)) {
							candidate = computeDir(path);
						} else {
							candidate = computeJarFile(path);
						}

						if (candidate != null) {
							return candidate;
						}
					}
				} catch (ParseMetadataException e) { // already contains all context
					throw ExceptionUtil.wrap(e);
				} catch (Throwable t) {
					throw new RuntimeException(String.format("Error analyzing %s: %s", paths, t), t);
				}

				return null;
			}
		}

		private ModCandidateImpl computeDir(Path path) throws IOException, ParseMetadataException {
			Path notebook = path.resolve("notebook.mod.json");
			if (!Files.exists(notebook)) return null;

			LoaderModMetadata metadata;

			try (InputStream is = Files.newInputStream(notebook)) {
				metadata = parseMetadata(is, path.toString());
			}

			return ModCandidateImpl.createPlain(paths, metadata, requiresRemap, Collections.emptyList());
		}

		private ModCandidateImpl computeJarFile(Path path) throws IOException, ParseMetadataException {
			try (ZipFile zf = new ZipFile(path.toFile())) {
				ZipEntry entry = zf.getEntry("notebook.mod.json");

				if (entry == null) {
					nonNotebookMods.add(path);
					return null;
				}

				LoaderModMetadata metadata;

				try (InputStream is = zf.getInputStream(entry)) {
					metadata = parseMetadata(is, localPath);
				}

				if (!metadata.loadsInEnvironment(envType)) {
					return ModCandidateImpl.createPlain(paths, metadata, requiresRemap, Collections.emptyList());
				}

				List<ModScanTask> nestedModTasks;

				if (metadata.getJars().isEmpty()) {
					nestedModTasks = Collections.emptyList();
				} else {
					Set<NestedJarEntry> nestedJarPaths = new HashSet<>(metadata.getJars());

					nestedModTasks = computeNestedMods(new ZipEntrySource() {
						@Override
						public ZipEntry getNextEntry() throws IOException {
							while (jarIt.hasNext()) {
								NestedJarEntry jar = jarIt.next();
								ZipEntry ret = zf.getEntry(jar.getFile());

								if (isValidNestedJarEntry(ret)) {
									currentEntry = ret;
									jarIt.remove();
									return ret;
								}
							}

							currentEntry = null;
							return null;
						}

						@Override
						public RewindableInputStream getInputStream() throws IOException {
							try (InputStream is = zf.getInputStream(currentEntry)) {
								return new RewindableInputStream(is);
							}
						}

						private final Iterator<NestedJarEntry> jarIt = nestedJarPaths.iterator();
						private ZipEntry currentEntry;
					});

					if (!nestedJarPaths.isEmpty() && NotebookLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
						Log.warn(LogCategory.METADATA, "Mod %s %s references missing nested jars: %s", metadata.getId(), metadata.getVersion(), nestedJarPaths);
					}
				}

				List<ModCandidateImpl> nestedMods;

				if (nestedModTasks.isEmpty()) {
					nestedMods = Collections.emptyList();
				} else {
					nestedMods = new ArrayList<>();
					nestedModInitDatas.add(new NestedModInitData(nestedModTasks, nestedMods));
				}

				return ModCandidateImpl.createPlain(paths, metadata, requiresRemap, nestedMods);
			}
		}

		private ModCandidateImpl computeJarStream() throws IOException, ParseMetadataException {
			LoaderModMetadata metadata = null;
			ZipEntry entry;

			try (ZipInputStream zis = new ZipInputStream(is)) {
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().equals("notebook.mod.json")) {
						metadata = parseMetadata(zis, localPath);
						break;
					}
				}
			}

			if (metadata == null) return null;

			if (!metadata.loadsInEnvironment(envType)) {
				return ModCandidateImpl.createNested(localPath, hash, metadata, requiresRemap, Collections.emptyList());
			}

			Collection<NestedJarEntry> nestedJars = metadata.getJars();
			List<ModScanTask> nestedModTasks;

			if (nestedJars.isEmpty()) {
				nestedModTasks = Collections.emptyList();
			} else {
				Set<String> nestedJarPaths = new HashSet<>(nestedJars.size());

				for (NestedJarEntry nestedJar : nestedJars) {
					nestedJarPaths.add(nestedJar.getFile());
				}

				is.rewind();

				try (ZipInputStream zis = new ZipInputStream(is)) {
					nestedModTasks = computeNestedMods(new ZipEntrySource() {
						@Override
						public ZipEntry getNextEntry() throws IOException {
							if (nestedJarPaths.isEmpty()) return null;

							ZipEntry ret;

							while ((ret = zis.getNextEntry()) != null) {
								if (isValidNestedJarEntry(ret) && nestedJarPaths.remove(ret.getName())) {
									is = new RewindableInputStream(zis); // reads the entry, which completes the ZipEntry with any trailing header data
									return ret;
								}
							}

							return null;
						}

						@Override
						public RewindableInputStream getInputStream() throws IOException {
							return is;
						}

						private RewindableInputStream is;
					});
				}

				if (!nestedJarPaths.isEmpty() && NotebookLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
					Log.warn(LogCategory.METADATA, "Mod %s %s references missing nested jars: %s", metadata.getId(), metadata.getVersion(), nestedJarPaths);
				}
			}

			List<ModCandidateImpl> nestedMods;

			if (nestedModTasks.isEmpty()) {
				nestedMods = Collections.emptyList();
			} else {
				nestedMods = new ArrayList<>();
				nestedModInitDatas.add(new NestedModInitData(nestedModTasks, nestedMods));
			}

			ModCandidateImpl ret = ModCandidateImpl.createNested(localPath, hash, metadata, requiresRemap, nestedMods);
			ret.setData(is.getBuffer());

			return ret;
		}

		private List<ModScanTask> computeNestedMods(ZipEntrySource entrySource) throws IOException {
			List<String> parentPaths = new ArrayList<>(this.parentPaths.size() + 1);
			parentPaths.addAll(this.parentPaths);
			parentPaths.add(localPath);

			List<ModScanTask> tasks = new ArrayList<>(5);
			ModScanTask localTask = null;
			ZipEntry entry;

			while ((entry = entrySource.getNextEntry()) != null) {
				long hash = ModCandidateImpl.hash(entry);
				ModScanTask task = jijDedupMap.get(hash);

				if (task == null) {
					task = new ModScanTask(null, entry.getName(), entrySource.getInputStream(), hash, requiresRemap, parentPaths);
					ModScanTask prev = jijDedupMap.putIfAbsent(hash, task);

					if (prev != null) {
						task = prev;
					} else if (localTask == null) { // don't fork first task, leave it for this thread
						localTask = task;
					} else {
						task.fork();
					}
				}

				tasks.add(task);
			}

			if (tasks.isEmpty()) return Collections.emptyList();

			if (localTask != null) localTask.invoke();

			return tasks;
		}

		private LoaderModMetadata parseMetadata(InputStream is, String localPath) throws ParseMetadataException {
			return ModMetadataParser.parseMetadata(is, localPath, parentPaths, versionOverrides, depOverrides, NotebookLoaderImpl.INSTANCE.isDevelopmentEnvironment());
		}
	}

	private static boolean isValidNestedJarEntry(ZipEntry entry) {
		return entry != null && !entry.isDirectory() && entry.getName().endsWith(".jar");
	}

	private interface ZipEntrySource {
		ZipEntry getNextEntry() throws IOException;
		RewindableInputStream getInputStream() throws IOException;
	}

	private static final class RewindableInputStream extends InputStream {
		private final ByteBuffer buffer;
		private int pos;

		RewindableInputStream(InputStream parent) throws IOException { // no parent.close()
			buffer = readMod(parent);

			assert buffer.hasArray() && buffer.arrayOffset() == 0 && buffer.position() == 0;
		}

		public ByteBuffer getBuffer() {
			return buffer;
		}

		public void rewind() {
			pos = 0;
		}

		@Override
		public int read() throws IOException {
			if (pos >= buffer.limit()) {
				return -1;
			} else {
				return buffer.get(pos++) & 0xff;
			}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int rem = buffer.limit() - pos;

			if (rem <= 0) {
				return -1;
			} else {
				len = Math.min(len, rem);
				System.arraycopy(buffer.array(), pos, b, off, len);
				pos += len;

				return len;
			}
		}
	}

	static ByteBuffer readMod(InputStream is) throws IOException {
		int available = is.available();
		boolean availableGood = available > 1;
		byte[] buffer = new byte[availableGood ? available : 30_000];
		int offset = 0;
		int len;

		while ((len = is.read(buffer, offset, buffer.length - offset)) >= 0) {
			offset += len;

			if (offset == buffer.length) {
				if (availableGood) {
					int val = is.read();
					if (val < 0) break;

					availableGood = false;
					buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, 30_000));
					buffer[offset++] = (byte) val;
				} else {
					buffer = Arrays.copyOf(buffer, buffer.length * 2);
				}
			}
		}

		return ByteBuffer.wrap(buffer, 0, offset);
	}

	private static class NestedModInitData {
		final List<? extends Future<ModCandidateImpl>> futures;
		final List<ModCandidateImpl> target;

		NestedModInitData(List<? extends Future<ModCandidateImpl>> futures, List<ModCandidateImpl> target) {
			this.futures = futures;
			this.target = target;
		}
	}
}
