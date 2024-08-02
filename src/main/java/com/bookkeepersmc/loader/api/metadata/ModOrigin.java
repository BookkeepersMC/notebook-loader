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
package com.bookkeepersmc.loader.api.metadata;

import java.nio.file.Path;
import java.util.List;

/**
 * Representation of the various locations a mod was loaded from originally.
 *
 * <p>This location is not necessarily identical to the code source used at runtime, a mod may get copied or otherwise
 * transformed before being put on the class path. It thus mostly represents the installation and initial loading, not
 * what is being directly accessed at runtime.
 */
public interface ModOrigin {
	/**
	 * Get the kind of this origin, determines the available methods.
	 *
	 * @return mod origin kind
	 */
	Kind getKind();

	/**
	 * Get the jar or folder paths for a {@link Kind#PATH} origin.
	 *
	 * @return jar or folder paths
	 * @throws UnsupportedOperationException for incompatible kinds
	 */
	List<Path> getPaths();

	/**
	 * Get the parent mod for a {@link Kind#NESTED} origin.
	 *
	 * @return parent mod
	 * @throws UnsupportedOperationException for incompatible kinds
	 */
	String getParentModId();

	/**
	 * Get the sub-location within the parent mod for a {@link Kind#NESTED} origin.
	 *
	 * @return sub-location
	 * @throws UnsupportedOperationException for incompatible kinds
	 */
	String getParentSubLocation();

	/**
	 * Non-exhaustive list of possible {@link ModOrigin} kinds.
	 *
	 * <p>New kinds may be added in the future, use a default switch case!
	 */
	enum Kind {
		PATH, NESTED, UNKNOWN
	}
}
