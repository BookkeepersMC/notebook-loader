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
import java.util.Collections;
import java.util.HashSet;

import com.bookkeepersmc.loader.api.MappingResolver;

import net.fabricmc.mappingio.tree.MappingTree;

class MappingResolverImpl implements MappingResolver {
	private final MappingTree mappings;
	private final String targetNamespace;
	private final int targetNamespaceId;

	MappingResolverImpl(MappingTree mappings, String targetNamespace) {
		this.mappings = mappings;
		this.targetNamespace = targetNamespace;
		this.targetNamespaceId = mappings.getNamespaceId(targetNamespace);
	}

	@Override
	public Collection<String> getNamespaces() {
		HashSet<String> namespaces = new HashSet<>(mappings.getDstNamespaces());
		namespaces.add(mappings.getSrcNamespace());
		return Collections.unmodifiableSet(namespaces);
	}

	@Override
	public String getCurrentRuntimeNamespace() {
		return targetNamespace;
	}

	@Override
	public String mapClassName(String namespace, String className) {
		if (className.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
		}

		return replaceSlashesWithDots(mappings.mapClassName(replaceDotsWithSlashes(className), mappings.getNamespaceId(namespace), targetNamespaceId));
	}

	@Override
	public String unmapClassName(String namespace, String className) {
		if (className.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
		}

		return replaceSlashesWithDots(mappings.mapClassName(replaceDotsWithSlashes(className), targetNamespaceId, mappings.getNamespaceId(namespace)));
	}

	@Override
	public String mapFieldName(String namespace, String owner, String name, String descriptor) {
		if (owner.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
		}

		MappingTree.FieldMapping field = mappings.getField(replaceDotsWithSlashes(owner), name, descriptor, mappings.getNamespaceId(namespace));
		return field == null ? name : field.getName(targetNamespaceId);
	}

	@Override
	public String mapMethodName(String namespace, String owner, String name, String descriptor) {
		if (owner.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
		}

		MappingTree.MethodMapping method = mappings.getMethod(replaceDotsWithSlashes(owner), name, descriptor, mappings.getNamespaceId(namespace));
		return method == null ? name : method.getName(targetNamespaceId);
	}

	private static String replaceSlashesWithDots(String cname) {
		return cname.replace('/', '.');
	}

	private static String replaceDotsWithSlashes(String cname) {
		return cname.replace('.', '/');
	}
}
