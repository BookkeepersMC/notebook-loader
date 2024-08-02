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
package com.bookkeepersmc.loader.api;

import com.bookkeepersmc.loader.impl.util.DefaultLanguageAdapter;

/**
 * Creates instances of objects from custom notations.
 *
 * <p>It enables obtaining of other JVM languages' objects with custom instantiation logic.</p>
 *
 * <p>A language adapter is defined as so in {@code fabric.mod.json}:
 * <pre><blockquote>
 *   "languageAdapter": {
 *     "&lt;a key&gt;": "&lt;the binary name of the language adapter class&gt;"
 *   }
 * </blockquote></pre>
 * Multiple keys can be present in the {@code languageAdapter} section.</p>
 *
 * <p>In the declaration, the language adapter is referred by its <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">binary name</a>,
 * such as {@code "mypackage.MyClass$Inner"}. It must have a no-argument public constructor for the Loader to instantiate.</p>
 *
 * <p>The {@code default} language adapter from Fabric Loader can accept {@code value} as follows:
 * <ul>
 *   <li>A fully qualified reference to a class, in <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">
 *   binary name</a>, such as {@code package.MyClass$Inner}, where the class has a public no-argument constructor
 *   and {@code type} is assignable from the class.
 *
 *   <p>An example of an entrypoint class
 *   <pre><blockquote>
 *     package net.fabricmc.example;
 *     import net.fabricmc.api.ModInitializer;
 *     public class ExampleMod implements ModInitializer {
 *       public ExampleMod() {} // the constructor must be public no-argument
 *       {@literal @}Override
 *       public void onInitialize() {}
 *     }
 *   </blockquote></pre>
 *   You would declare {@code "net.fabricmc.example.ExampleMod"}.</p>
 *
 *   <p>For each entrypoint reference, a new instance of the class is created.
 * 	 If this class implements two separate entrypoints, there will be two distinct
 * 	 instances of this class in two entrypoint containers.</p>
 * 	 </li>
 *
 *   <li>A fully qualified reference to a class in binary name followed by {@code ::} and a
 *   field name. The field must be static, and {@code type} must be assignable from
 *   the field's class.
 *
 *   <p>An example of an entrypoint field
 *   <pre><blockquote>
 *     package net.fabricmc.example;
 *     import net.fabricmc.api.ModInitializer;
 *     public final class ExampleMod implements ModInitializer {
 *       public static final ExampleMod INSTANCE = new ExampleMod();
 *
 *       private ExampleMod() {} // Doesn't need to be instantiable by loader
 *
 *       {@literal @}Override
 *       public void onInitialize() {}
 *     }
 *   </blockquote></pre>
 *   You would declare {@code "net.fabricmc.example.ExampleMod::INSTANCE"}.</p>
 *   </li>
 *
 *   <li>A fully qualified reference to a class in binary name followed by {@code ::} and a
 *   method name. The method must be capable to implement {@code type} as a
 *   method reference. If the method is not static, the class must have an
 *   accessible no-argument constructor for the Loader to create an instance.
 *
 *   <p>An example of an entrypoint method
 *   <pre><blockquote>
 *     package net.fabricmc.example;
 *     public final class ExampleMod {
 *       private ExampleMod() {} // doesn't need to be instantiable by others if method is static
 *
 *       public static void init() {}
 *     }
 *   </blockquote></pre>
 *   You would declare {@code "net.fabricmc.example.ExampleMod::init"}.</p>
 *   </li>
 * </ul>
 */
public interface LanguageAdapter {
	/**
	 * Get an instance of the default language adapter.
	 */
	static LanguageAdapter getDefault() {
		return DefaultLanguageAdapter.INSTANCE;
	}

	/**
	 * Creates an object of {@code type} from an arbitrary string declaration.
	 *
	 * @param mod   the mod which the object is from
	 * @param value the string declaration of the object
	 * @param type  the type that the created object must be an instance of
	 * @param <T>   the type
	 * @return the created object
	 * @throws LanguageAdapterException if a problem arises during creation, such as an invalid declaration
	 */
	<T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException;
}
