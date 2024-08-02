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
package net.fabricmc.test;

import com.bookkeepersmc.loader.impl.util.Arguments;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgumentParsingTests {
	@Test
	public void parseNormal() {
		Arguments arguments = new Arguments();
		arguments.parse(new String[]{"--clientId", "123", "--xuid", "abc", "--versionType", "release"});
		arguments.put("versionType", "Fabric");

		Assertions.assertEquals(arguments.keys().size(), 3);
		Assertions.assertEquals(arguments.get("xuid"), "abc");
		Assertions.assertEquals(arguments.get("versionType"), "Fabric");
	}

	@Test
	public void parseMissing() {
		Arguments arguments = new Arguments();
		arguments.parse(new String[]{"--clientId", "123", "--xuid", "--versionType", "release"});
		arguments.put("versionType", "Fabric");

		Assertions.assertEquals(arguments.keys().size(), 3);
		Assertions.assertEquals(arguments.get("xuid"), "");
		Assertions.assertEquals(arguments.get("versionType"), "Fabric");
	}
}
