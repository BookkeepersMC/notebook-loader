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
package net.fabricmc.loader;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.bookkeepersmc.loader.api.NotebookLoader;
import com.bookkeepersmc.loader.impl.NotebookLoaderImpl;

/**
 * The main class for mod loading operations.
 *
 * @deprecated Use {@link net.fabricmc.loader.api.FabricLoader}
 */
@Deprecated
public abstract class FabricLoader implements NotebookLoader {
	/**
	 * @deprecated Use {@link net.fabricmc.loader.api.FabricLoader#getInstance()} where possible,
	 * report missing areas as an issue.
	 */
	@Deprecated
	public static final FabricLoader INSTANCE = NotebookLoaderImpl.InitHelper.get();

	public File getModsDirectory() {
		return getModsDirectory0().toFile();
	}

	@Override
	public abstract <T> List<T> getEntrypoints(String key, Class<T> type);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Collection<ModContainer> getModContainers() {
		return (Collection) getAllMods();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<ModContainer> getMods() {
		return (List) getAllMods();
	}

	protected abstract Path getModsDirectory0();
}
