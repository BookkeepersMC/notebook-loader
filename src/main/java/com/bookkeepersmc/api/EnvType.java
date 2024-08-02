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
package com.bookkeepersmc.api;

/**
 * Represents a type of environment.
 *
 * <p>A type of environment is a jar file in a <i>Minecraft</i> version's json file's {@code download}
 * subsection, including the {@code client.jar} and the {@code server.jar}.</p>
 *
 * @see Environment
 * @see EnvironmentInterface
 */
public enum EnvType {
	/**
	 * Represents the client environment type, in which the {@code client.jar} for a
	 * <i>Minecraft</i> version is the main game jar.
	 *
	 * <p>A client environment type has all client logic (client rendering and integrated
	 * server logic), the data generator logic, and dedicated server logic. It encompasses
	 * everything that is available on the {@linkplain #SERVER server environment type}.</p>
	 */
	CLIENT,
	/**
	 * Represents the server environment type, in which the {@code server.jar} for a
	 * <i>Minecraft</i> version is the main game jar.
	 *
	 * <p>A server environment type has the dedicated server logic and data generator
	 * logic, which are all included in the {@linkplain #CLIENT client environment type}.
	 * However, the server environment type has its libraries embedded compared to the
	 * client.</p>
	 */
	SERVER
}
