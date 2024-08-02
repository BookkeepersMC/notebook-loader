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
package com.bookkeepersmc.loader.impl.metadata;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.bookkeepersmc.loader.api.metadata.ModOrigin;

public final class ModOriginImpl implements ModOrigin {
	private final Kind kind;
	private List<Path> paths;
	private String parentModId;
	private String parentSubLocation;

	public ModOriginImpl() {
		this.kind = Kind.UNKNOWN;
	}

	public ModOriginImpl(List<Path> paths) {
		this.kind = Kind.PATH;
		this.paths = paths;
	}

	public ModOriginImpl(String parentModId, String parentSubLocation) {
		this.kind = Kind.NESTED;
		this.parentModId = parentModId;
		this.parentSubLocation = parentSubLocation;
	}

	@Override
	public Kind getKind() {
		return kind;
	}

	@Override
	public List<Path> getPaths() {
		if (kind != Kind.PATH) throw new UnsupportedOperationException("kind "+kind.name()+" doesn't have paths");

		return paths;
	}

	@Override
	public String getParentModId() {
		if (kind != Kind.NESTED) throw new UnsupportedOperationException("kind "+kind.name()+" doesn't have a parent mod");

		return parentModId;
	}

	@Override
	public String getParentSubLocation() {
		if (kind != Kind.NESTED) throw new UnsupportedOperationException("kind "+kind.name()+" doesn't have a parent sub-location");

		return parentSubLocation;
	}

	@Override
	public String toString() {
		switch (getKind()) {
		case PATH:
			return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
		case NESTED:
			return String.format("%s:%s", parentModId, parentSubLocation);
		default:
			return "unknown";
		}
	}
}
