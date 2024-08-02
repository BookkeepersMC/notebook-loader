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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.bookkeepersmc.loader.api.metadata.ModMetadata;
import com.bookkeepersmc.loader.impl.game.patch.GameTransformer;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncher;
import com.bookkeepersmc.loader.impl.util.Arguments;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;

public interface GameProvider { // name directly referenced in net.fabricmc.loader.impl.launch.knot.Knot.findEmbedddedGameProvider() and service loader records
	String getGameId();
	String getGameName();
	String getRawGameVersion();
	String getNormalizedGameVersion();
	Collection<BuiltinMod> getBuiltinMods();

	String getEntrypoint();
	Path getLaunchDirectory();
	boolean isObfuscated();
	boolean requiresUrlClassLoader();

	boolean isEnabled();
	boolean locateGame(NotebookLauncher launcher, String[] args);
	void initialize(NotebookLauncher launcher);
	GameTransformer getEntrypointTransformer();
	void unlockClassPath(NotebookLauncher launcher);
	void launch(ClassLoader loader);

	default boolean displayCrash(Throwable exception, String context) {
		return false;
	}

	Arguments getArguments();
	String[] getLaunchArguments(boolean sanitize);

	default boolean canOpenErrorGui() {
		return true;
	}

	default boolean hasAwtSupport() {
		return LoaderUtil.hasAwtSupport();
	}

	class BuiltinMod {
		public BuiltinMod(List<Path> paths, ModMetadata metadata) {
			Objects.requireNonNull(paths, "null paths");
			Objects.requireNonNull(metadata, "null metadata");

			this.paths = paths;
			this.metadata = metadata;
		}

		public final List<Path> paths;
		public final ModMetadata metadata;
	}
}
