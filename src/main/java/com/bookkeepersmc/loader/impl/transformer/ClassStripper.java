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
package com.bookkeepersmc.loader.impl.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Strips the specified interfaces, fields and methods from a class.
 */
public class ClassStripper extends ClassVisitor {
	private final Collection<String> stripInterfaces;
	private final Collection<String> stripFields;
	private final Collection<String> stripMethods;

	public ClassStripper(int api, ClassVisitor classVisitor, Collection<String> stripInterfaces, Collection<String> stripFields, Collection<String> stripMethods) {
		super(api, classVisitor);
		this.stripInterfaces = stripInterfaces;
		this.stripFields = stripFields;
		this.stripMethods = stripMethods;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (!this.stripInterfaces.isEmpty()) {
			List<String> interfacesList = new ArrayList<>();

			for (String itf : interfaces) {
				if (!this.stripInterfaces.contains(itf)) {
					interfacesList.add(itf);
				}
			}

			interfaces = interfacesList.toArray(new String[0]);
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (stripFields.contains(name + descriptor)) return null;
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (stripMethods.contains(name + descriptor)) return null;
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
