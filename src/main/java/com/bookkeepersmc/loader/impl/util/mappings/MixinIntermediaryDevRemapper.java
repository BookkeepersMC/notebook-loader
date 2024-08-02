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
package com.bookkeepersmc.loader.impl.util.mappings;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.fabricmc.mappingio.tree.MappingTree;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public class MixinIntermediaryDevRemapper extends MixinRemapper {
	private static final String ambiguousName = "<ambiguous>"; // dummy value for ambiguous mappings - needs querying with additional owner and/or desc info

	private final Set<String> allPossibleClassNames = new HashSet<>();
	private final Map<String, String> nameFieldLookup = new HashMap<>();
	private final Map<String, String> nameMethodLookup = new HashMap<>();
	private final Map<String, String> nameDescFieldLookup = new HashMap<>();
	private final Map<String, String> nameDescMethodLookup = new HashMap<>();

	public MixinIntermediaryDevRemapper(MappingTree mappings, String from, String to) {
		super(mappings, mappings.getNamespaceId(from), mappings.getNamespaceId(to));

		for (MappingTree.ClassMapping classDef : mappings.getClasses()) {
			allPossibleClassNames.add(classDef.getName(from));
			allPossibleClassNames.add(classDef.getName(to));

			putMemberInLookup(fromId, toId, classDef.getFields(), nameFieldLookup, nameDescFieldLookup);
			putMemberInLookup(fromId, toId, classDef.getMethods(), nameMethodLookup, nameDescMethodLookup);
		}
	}

	private <T extends MappingTree.MemberMapping> void putMemberInLookup(int from, int to, Collection<T> descriptored, Map<String, String> nameMap, Map<String, String> nameDescMap) {
		for (T field : descriptored) {
			String nameFrom = field.getName(from);
			String descFrom = field.getDesc(from);
			String nameTo = field.getName(to);

			String prev = nameMap.putIfAbsent(nameFrom, nameTo);

			if (prev != null && prev != ambiguousName && !prev.equals(nameTo)) {
				nameDescMap.put(nameFrom, ambiguousName);
			}

			String key = getNameDescKey(nameFrom, descFrom);
			prev = nameDescMap.putIfAbsent(key, nameTo);

			if (prev != null && prev != ambiguousName && !prev.equals(nameTo)) {
				nameDescMap.put(key, ambiguousName);
			}
		}
	}

	private void throwAmbiguousLookup(String type, String name, String desc) {
		throw new RuntimeException("Ambiguous Mixin: " + type + " lookup " + name + " " + desc+" is not unique");
	}

	private String mapMethodNameInner(String owner, String name, String desc) {
		String result = super.mapMethodName(owner, name, desc);

		if (result.equals(name)) {
			String otherClass = unmap(owner);
			return super.mapMethodName(otherClass, name, unmapDesc(desc));
		} else {
			return result;
		}
	}

	private String mapFieldNameInner(String owner, String name, String desc) {
		String result = super.mapFieldName(owner, name, desc);

		if (result.equals(name)) {
			String otherClass = unmap(owner);
			return super.mapFieldName(otherClass, name, unmapDesc(desc));
		} else {
			return result;
		}
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		// handle unambiguous values early
		if (owner == null || allPossibleClassNames.contains(owner)) {
			String newName;

			if (desc == null) {
				newName = nameMethodLookup.get(name);
			} else {
				newName = nameDescMethodLookup.get(getNameDescKey(name, desc));
			}

			if (newName != null) {
				if (newName == ambiguousName) {
					if (owner == null) {
						throwAmbiguousLookup("method", name, desc);
					}
				} else {
					return newName;
				}
			} else if (owner == null) {
				return name;
			} else {
				// FIXME: this kind of namespace mixing shouldn't happen..
				// TODO: this should not repeat more than once
				String unmapOwner = unmap(owner);
				String unmapDesc = unmapDesc(desc);

				if (!unmapOwner.equals(owner) || !unmapDesc.equals(desc)) {
					return mapMethodName(unmapOwner, name, unmapDesc);
				} else {
					// take advantage of the fact allPossibleClassNames
					// and nameDescLookup cover all sets; if none are present,
					// we don't have a mapping for it.
					return name;
				}
			}
		}

		ClassInfo classInfo = ClassInfo.forName(map(owner));

		if (classInfo == null) { // unknown class?
			return name;
		}

		Queue<ClassInfo> queue = new ArrayDeque<>();

		do {
			String ownerO = unmap(classInfo.getName());
			String s;

			if (!(s = mapMethodNameInner(ownerO, name, desc)).equals(name)) {
				return s;
			}

			if (classInfo.getSuperName() != null && !classInfo.getSuperName().startsWith("java/")) {
				ClassInfo cSuper = classInfo.getSuperClass();

				if (cSuper != null) {
					queue.add(cSuper);
				}
			}

			for (String itf : classInfo.getInterfaces()) {
				if (itf.startsWith("java/")) {
					continue;
				}

				ClassInfo cItf = ClassInfo.forName(itf);

				if (cItf != null) {
					queue.add(cItf);
				}
			}
		} while ((classInfo = queue.poll()) != null);

		return name;
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		// handle unambiguous values early
		if (owner == null || allPossibleClassNames.contains(owner)) {
			String newName = nameDescFieldLookup.get(getNameDescKey(name, desc));

			if (newName != null) {
				if (newName == ambiguousName) {
					if (owner == null) {
						throwAmbiguousLookup("field", name, desc);
					}
				} else {
					return newName;
				}
			} else if (owner == null) {
				return name;
			} else {
				// FIXME: this kind of namespace mixing shouldn't happen..
				// TODO: this should not repeat more than once
				String unmapOwner = unmap(owner);
				String unmapDesc = unmapDesc(desc);

				if (!unmapOwner.equals(owner) || !unmapDesc.equals(desc)) {
					return mapFieldName(unmapOwner, name, unmapDesc);
				} else {
					// take advantage of the fact allPossibleClassNames
					// and nameDescLookup cover all sets; if none are present,
					// we don't have a mapping for it.
					return name;
				}
			}
		}

		ClassInfo c = ClassInfo.forName(map(owner));

		while (c != null) {
			String nextOwner = unmap(c.getName());
			String s = mapFieldNameInner(nextOwner, name, desc);

			if (!s.equals(name)) {
				return s;
			}

			if (c.getSuperName() == null || c.getSuperName().startsWith("java/")) {
				break;
			}

			c = c.getSuperClass();
		}

		return name;
	}

	private static String getNameDescKey(String name, String descriptor) {
		return name+ ";;" + descriptor;
	}
}
