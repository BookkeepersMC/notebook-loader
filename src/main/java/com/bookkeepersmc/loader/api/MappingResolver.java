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

import java.util.Collection;

/**
 * Helper class for performing mapping resolution.
 *
 * <p><strong>Note</strong>: The target namespace (the one being mapped to) for mapping (or the
 * source one for unmapping) is always implied to be the one Loader is
 * currently operating in.</p>
 *
 * <p>All the {@code className} used in this resolver are in <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">binary names</a>,
 * such as {@code "mypackage.MyClass$Inner"}.</p>
 *
 * @since 0.4.1
 */
public interface MappingResolver {
	/**
	 * Get the list of all available mapping namespaces in the loaded instance.
	 *
	 * @return The list of all available namespaces.
	 */
	Collection<String> getNamespaces();

	/**
	 * Get the current namespace being used at runtime.
	 *
	 * @return the runtime namespace
	 */
	String getCurrentRuntimeNamespace();

	/**
	 * Map a class name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided class name
	 * @param className the provided binary class name
	 * @return the mapped class name, or {@code className} if no such mapping is present
	 */
	String mapClassName(String namespace, String className);

	/**
	 * Unmap a class name to the mapping currently used at runtime.
	 *
	 * @param targetNamespace The target namespace for unmapping.
	 * @param className the provided binary class name of the mapping form currently used at runtime
	 * @return the mapped class name, or {@code className} if no such mapping is present
	 */
	String unmapClassName(String targetNamespace, String className);

	/**
	 * Map a field name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided field name and descriptor
	 * @param owner the binary name of the owner class of the field
	 * @param name the name of the field
	 * @param descriptor the descriptor of the field
	 * @return the mapped field name, or {@code name} if no such mapping is present
	 */
	String mapFieldName(String namespace, String owner, String name, String descriptor);

	/**
	 * Map a method name to the mapping currently used at runtime.
	 *
	 * @param namespace the namespace of the provided method name and descriptor
	 * @param owner the binary name of the owner class of the method
	 * @param name the name of the method
	 * @param descriptor the descriptor of the method
	 * @return the mapped method name, or {@code name} if no such mapping is present
	 */
	String mapMethodName(String namespace, String owner, String name, String descriptor);
}
