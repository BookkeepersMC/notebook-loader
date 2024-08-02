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
package com.bookkeepersmc.loader.impl;

import java.util.Collection;
import java.util.function.Supplier;

import com.bookkeepersmc.loader.api.MappingResolver;

public class LazyMappingResolver implements MappingResolver {
	private final Supplier<MappingResolver> delegateSupplier;
	private final String currentRuntimeNamespace;

	private MappingResolver delegate = null;

	LazyMappingResolver(Supplier<MappingResolver> delegateSupplier, String currentRuntimeNamespace) {
		this.delegateSupplier = delegateSupplier;
		this.currentRuntimeNamespace = currentRuntimeNamespace;
	}

	private MappingResolver getDelegate() {
		if (delegate == null) {
			delegate = delegateSupplier.get();
		}

		return delegate;
	}

	@Override
	public Collection<String> getNamespaces() {
		return getDelegate().getNamespaces();
	}

	@Override
	public String getCurrentRuntimeNamespace() {
		return currentRuntimeNamespace;
	}

	@Override
	public String mapClassName(String namespace, String className) {
		if (namespace.equals(currentRuntimeNamespace)) {
			// Skip loading the mappings if the namespace is the same as the current runtime namespace
			return className;
		}

		return getDelegate().mapClassName(namespace, className);
	}

	@Override
	public String unmapClassName(String targetNamespace, String className) {
		return getDelegate().unmapClassName(targetNamespace, className);
	}

	@Override
	public String mapFieldName(String namespace, String owner, String name, String descriptor) {
		if (namespace.equals(currentRuntimeNamespace)) {
			// Skip loading the mappings if the namespace is the same as the current runtime namespace
			return name;
		}

		return getDelegate().mapFieldName(namespace, owner, name, descriptor);
	}

	@Override
	public String mapMethodName(String namespace, String owner, String name, String descriptor) {
		if (namespace.equals(currentRuntimeNamespace)) {
			// Skip loading the mappings if the namespace is the same as the current runtime namespace
			return name;
		}

		return getDelegate().mapMethodName(namespace, owner, name, descriptor);
	}
}
