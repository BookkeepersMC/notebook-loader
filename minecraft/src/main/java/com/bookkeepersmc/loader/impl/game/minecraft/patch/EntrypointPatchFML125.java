
/*
 * Copyright (c) 2024 BookkeepersMC under the MIT License
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
package com.bookkeepersmc.loader.impl.game.minecraft.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import com.bookkeepersmc.loader.impl.game.patch.GamePatch;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncher;
import com.bookkeepersmc.loader.impl.launch.knot.Knot;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public class EntrypointPatchFML125 extends GamePatch {
	private static final String FROM = ModClassLoader_125_FML.class.getName();
	private static final String TO = "cpw.mods.fml.common.ModClassLoader";
	private static final String FROM_INTERNAL = FROM.replace('.', '/');
	private static final String TO_INTERNAL = "cpw/mods/fml/common/ModClassLoader";

	@Override
	public void process(NotebookLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		if (classSource.apply(TO) != null
				&& classSource.apply("cpw.mods.fml.relauncher.FMLRelauncher") == null) {
			if (!(launcher instanceof Knot)) {
				throw new RuntimeException("1.2.5 FML patch only supported on Knot!");
			}

			Log.debug(LogCategory.GAME_PATCH, "Detected 1.2.5 FML - Knotifying ModClassLoader...");

			// ModClassLoader_125_FML isn't in the game's class path, so it's loaded from the launcher's class path instead
			ClassNode patchedClassLoader = new ClassNode();

			try (InputStream stream = launcher.getResourceAsStream(LoaderUtil.getClassFileName(FROM))) {
				if (stream != null) {
					ClassReader patchedClassLoaderReader = new ClassReader(stream);
					patchedClassLoaderReader.accept(patchedClassLoader, 0);
				} else {
					throw new IOException("Could not find class " + FROM + " in the launcher classpath while transforming ModClassLoader");
				}
			} catch (IOException e) {
				throw new RuntimeException("An error occurred while reading class " + FROM + " while transforming ModClassLoader", e);
			}

			ClassNode remappedClassLoader = new ClassNode();

			patchedClassLoader.accept(new ClassRemapper(remappedClassLoader, new Remapper() {
				@Override
				public String map(String internalName) {
					return FROM_INTERNAL.equals(internalName) ? TO_INTERNAL : internalName;
				}
			}));

			classEmitter.accept(remappedClassLoader);
		}
	}
}
