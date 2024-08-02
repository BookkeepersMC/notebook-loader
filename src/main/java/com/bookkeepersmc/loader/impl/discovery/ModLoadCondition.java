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
package com.bookkeepersmc.loader.impl.discovery;

/**
 * Conditions for whether to load a mod.
 *
 * <p>These apply only after the unique ID and nesting requirements are met. If a mod ID is shared by multiple mod
 * candidates only one of them will load. Mods nested within another mod only load if the encompassing mod loads.
 *
 * <p>Each condition encompasses all conditions after it in enum declaration order. For example {@link #IF_POSSIBLE}
 * also loads if the conditions for {@link #IF_RECOMMENDED} or {@link #IF_NEEDED} are met.
 */
public enum ModLoadCondition {
	/**
	 * Load always, triggering an error if that is not possible.
	 *
	 * <p>This is the default for root mods (typically those in the mods folder).
	 */
	ALWAYS,
	/**
	 * Load whenever there is nothing contradicting.
	 *
	 * <p>This is the default for nested mods.
	 */
	IF_POSSIBLE,
	/**
	 * Load if the mod is recommended by another loading mod.
	 */
	IF_RECOMMENDED,
	/**
	 * Load if another loading mod requires the mod.
	 */
	IF_NEEDED;
}
