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
package net.fabricmc.loader.language;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.api.NotebookLoader;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;

import org.objectweb.asm.ClassReader;

@Deprecated
public class JavaLanguageAdapter implements LanguageAdapter {
	private static boolean canApplyInterface(String itfString) throws IOException {
		// TODO: Be a bit more involved
		switch (itfString) {
		case "com/bookkeepersmc/api/ClientModInitializer":
			if (NotebookLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
				return false;
			}

			break;
		case "com/bookkeepersmc/api/DedicatedServerModInitializer":
			if (NotebookLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				return false;
			}
		}

		InputStream stream = NotebookLauncherBase.getLauncher().getResourceAsStream(LoaderUtil.getClassFileName(itfString));
		if (stream == null) return false;

		ClassReader reader = new ClassReader(stream);

		for (String s : reader.getInterfaces()) {
			if (!canApplyInterface(s)) {
				stream.close();
				return false;
			}
		}

		stream.close();
		return true;
	}

	public static Class<?> getClass(String className, Options options) throws ClassNotFoundException, IOException {
		InputStream stream = NotebookLauncherBase.getLauncher().getResourceAsStream(LoaderUtil.getClassFileName(className));
		if (stream == null) throw new ClassNotFoundException("Could not find or load class " + className);

		ClassReader reader = new ClassReader(stream);

		for (String s : reader.getInterfaces()) {
			if (!canApplyInterface(s)) {
				switch (options.getMissingSuperclassBehavior()) {
				case RETURN_NULL:
					stream.close();
					return null;
				case CRASH:
				default:
					stream.close();
					throw new ClassNotFoundException("Could not find or load class " + s);
				}
			}
		}

		stream.close();
		return NotebookLauncherBase.getClass(className);
	}

	@Override
	public Object createInstance(Class<?> modClass, Options options) throws LanguageAdapterException {
		try {
			Constructor<?> constructor = modClass.getDeclaredConstructor();
			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new LanguageAdapterException("Could not find constructor for class " + modClass.getName() + "!", e);
		} catch (IllegalAccessException e) {
			throw new LanguageAdapterException("Could not access constructor of class " + modClass.getName() + "!", e);
		} catch (InvocationTargetException | IllegalArgumentException | InstantiationException e) {
			throw new LanguageAdapterException("Could not instantiate class " + modClass.getName() + "!", e);
		}
	}
}
