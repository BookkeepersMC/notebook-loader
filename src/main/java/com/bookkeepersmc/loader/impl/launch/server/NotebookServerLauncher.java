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
package com.bookkeepersmc.loader.impl.launch.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.bookkeepersmc.loader.impl.launch.knot.KnotServer;
import com.bookkeepersmc.loader.impl.util.LoaderUtil;
import com.bookkeepersmc.loader.impl.util.SystemProperties;

public class NotebookServerLauncher {
	private static final ClassLoader parentLoader = NotebookServerLauncher.class.getClassLoader();
	private static String mainClass = KnotServer.class.getName();

	public static void main(String[] args) {
		URL propUrl = parentLoader.getResource("fabric-server-launch.properties");

		if (propUrl != null) {
			Properties properties = new Properties();

			try (InputStreamReader reader = new InputStreamReader(propUrl.openStream(), StandardCharsets.UTF_8)) {
				properties.load(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (properties.containsKey("launch.mainClass")) {
				mainClass = properties.getProperty("launch.mainClass");
			}
		}

		boolean dev = Boolean.parseBoolean(System.getProperty(SystemProperties.DEVELOPMENT, "false"));

		if (!dev) {
			try {
				setup(args);
			} catch (Exception e) {
				throw new RuntimeException("Failed to setup Fabric server environment!", e);
			}
		}

		try {
			Class<?> c = Class.forName(mainClass);
			MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class)).invokeExact(args);
		} catch (Throwable e) {
			throw new RuntimeException("An exception occurred when launching the server!", e);
		}
	}

	private static void setup(String... runArguments) throws IOException {
		if (System.getProperty(SystemProperties.GAME_JAR_PATH) == null) {
			System.setProperty(SystemProperties.GAME_JAR_PATH, getServerJarPath());
		}

		Path serverJar = LoaderUtil.normalizePath(Paths.get(System.getProperty(SystemProperties.GAME_JAR_PATH)));

		if (!Files.exists(serverJar)) {
			System.err.println("The Minecraft server .JAR is missing (" + serverJar + ")!");
			System.err.println();
			System.err.println("Fabric's server-side launcher expects the server .JAR to be provided.");
			System.err.println("You can edit its location in fabric-server-launcher.properties.");
			System.err.println();
			System.err.println("Without the official Minecraft server .JAR, Fabric Loader cannot launch.");
			throw new RuntimeException("Missing game jar at " + serverJar);
		}
	}

	private static String getServerJarPath() throws IOException {
		// Pre-load "fabric-server-launcher.properties"
		Path propertiesFile = Paths.get("fabric-server-launcher.properties");
		Properties properties = new Properties();

		if (Files.exists(propertiesFile)) {
			try (Reader reader = Files.newBufferedReader(propertiesFile)) {
				properties.load(reader);
			}
		}

		// Most popular Minecraft server hosting platforms do not allow
		// passing arbitrary arguments to the server .JAR. Meanwhile,
		// Mojang's default server filename is "server.jar" as of
		// a few versions... let's use this.
		if (!properties.containsKey("serverJar")) {
			properties.put("serverJar", "server.jar");

			try (Writer writer = Files.newBufferedWriter(propertiesFile)) {
				properties.store(writer, null);
			}
		}

		return (String) properties.get("serverJar");
	}
}
