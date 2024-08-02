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
package com.bookkeepersmc.loader.impl.game.minecraft.applet;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.bookkeepersmc.loader.impl.game.minecraft.Hooks;
import com.bookkeepersmc.loader.impl.launch.NotebookLauncherBase;

/**
 * PLEASE NOTE:
 *
 * <p>This class is originally copyrighted under Apache License 2.0
 * by the MCUpdater project (https://github.com/MCUpdater/MCU-Launcher/).
 *
 * <p>It has been adapted here for the purposes of the Fabric loader.
 */
@SuppressWarnings("serial")
public class AppletLauncher extends Applet implements AppletStub {
	public static File gameDir;

	private final Map<String, String> params;
	private Applet mcApplet;
	private boolean active;

	public AppletLauncher(File instance, String username, String sessionid, String host, String port, boolean doConnect, boolean fullscreen, boolean demo) {
		gameDir = instance;

		params = new HashMap<>();
		params.put("username", username);
		params.put("sessionid", sessionid);
		params.put("stand-alone", "true");

		if (doConnect) {
			params.put("server", host);
			params.put("port", port);
		}

		params.put("fullscreen", Boolean.toString(fullscreen)); //Required param for vanilla. Forge handles the absence gracefully.
		params.put("demo", Boolean.toString(demo));

		try {
			mcApplet = (Applet) NotebookLauncherBase.getLauncher()
					.getTargetClassLoader()
					.loadClass(Hooks.appletMainClass)
					.getDeclaredConstructor()
					.newInstance();

			//noinspection ConstantConditions
			if (mcApplet == null) {
				throw new RuntimeException("Could not instantiate MinecraftApplet - is null?");
			}

			this.add(mcApplet, "Center");
		} catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, String> getParams() {
		return params;
	}

	// 1.3 ~ 1.5 FML
	public void replace(Applet applet) {
		this.mcApplet = applet;
		init();

		if (active) {
			start();
			validate();
		}
	}

	@Override
	public void appletResize(int width, int height) {
		mcApplet.resize(width, height);
	}

	@Override
	public void resize(int width, int height) {
		mcApplet.resize(width, height);
	}

	@Override
	public void resize(Dimension dim) {
		mcApplet.resize(dim);
	}

	@Override
	public String getParameter(String name) {
		String value = params.get(name);
		if (value != null) return value;

		try {
			return super.getParameter(name);
		} catch (Exception ignored) {
			// ignored
		}

		return null;
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	@Override
	public void init() {
		mcApplet.setStub(this);
		mcApplet.setSize(getWidth(), getHeight());
		setLayout(new BorderLayout());
		this.add(mcApplet, "Center");
		mcApplet.init();
	}

	@Override
	public void start() {
		mcApplet.start();
		active = true;
	}

	@Override
	public void stop() {
		mcApplet.stop();
		active = false;
	}

	/**
	 * Minecraft 0.30 checks for "minecraft.net" or "www.minecraft.net" being
	 * the applet hosting location, as an anti-rehosting measure. Of course,
	 * being ran stand-alone, it's not actually "hosted" anywhere.
	 *
	 * <p>The side effect of not providing the correct URL here is all levels,
	 * loaded or generated, being set to null.
	 */
	private URL getMinecraftHostingUrl() {
		try {
			return new URL("http://www.minecraft.net/game");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public URL getCodeBase() {
		return getMinecraftHostingUrl();
	}

	@Override
	public URL getDocumentBase() {
		return getMinecraftHostingUrl();
	}

	@Override
	public void setVisible(boolean flag) {
		super.setVisible(flag);
		mcApplet.setVisible(flag);
	}
}
