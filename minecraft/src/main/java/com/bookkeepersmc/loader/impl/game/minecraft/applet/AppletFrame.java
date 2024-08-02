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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;

import com.bookkeepersmc.loader.impl.util.Arguments;

/**
 * PLEASE NOTE:
 *
 * <p>This class is originally copyrighted under Apache License 2.0
 * by the MCUpdater project (https://github.com/MCUpdater/MCU-Launcher/).
 *
 * <p>It has been adapted here for the purposes of the Fabric loader.
 */
@SuppressWarnings("serial")
public class AppletFrame extends Frame implements WindowListener {
	private AppletLauncher applet = null;

	public AppletFrame(String title, ImageIcon icon) {
		super(title);

		if (icon != null) {
			Image source = icon.getImage();
			int w = source.getWidth(null);
			int h = source.getHeight(null);

			if (w == -1) {
				w = 32;
				h = 32;
			}

			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D) image.getGraphics();
			g2d.drawImage(source, 0, 0, null);
			setIconImage(image);
			g2d.dispose();
		}

		addWindowListener(this);
	}

	public void launch(String[] args) {
		Arguments arguments = new Arguments();
		arguments.parse(args);

		String username = arguments.getOrDefault("username", "Player");
		String sessionid;

		if (arguments.containsKey("session") /* 1.6 */) {
			sessionid = arguments.get("session");
		} else if (arguments.getExtraArgs().size() == 2 /* pre 1.6 */) {
			username = arguments.getExtraArgs().get(0);
			sessionid = arguments.getExtraArgs().get(1);
		} else /* fallback */ {
			sessionid = "";
		}

		File instance = new File(arguments.getOrDefault("gameDir", "."));

		if (System.getProperty("minecraft.applet.TargetDirectory") == null) {
			System.setProperty("minecraft.applet.TargetDirectory", instance.toString());
		} else {
			instance = new File(System.getProperty("minecraft.applet.TargetDirectory"));
		}

		// 1.3 ~ 1.5 FML
		System.setProperty("minecraft.applet.WrapperClass", AppletLauncher.class.getName());

		boolean doConnect = arguments.containsKey("server") && arguments.containsKey("port");
		String host = "";
		String port = "";

		if (doConnect) {
			host = arguments.get("server");
			port = arguments.get("port");
		}

		boolean fullscreen = arguments.getExtraArgs().contains("--fullscreen");
		boolean demo = arguments.getExtraArgs().contains("--demo");
		int width = Integer.parseInt(arguments.getOrDefault("width", "854"));
		int height = Integer.parseInt(arguments.getOrDefault("height", "480"));

		applet = new AppletLauncher(
				instance,
				username, sessionid,
				host, port, doConnect,
				fullscreen, demo
				);

		for (String key : arguments.keys()) {
			applet.getParams().put("fabric.arguments." + key, arguments.get(key));
		}

		this.add(applet);
		applet.setPreferredSize(new Dimension(width, height));
		pack();
		setLocationRelativeTo(null);
		setResizable(true);
		validate();
		applet.init();
		applet.start();
		setVisible(true);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		Thread shutdownListenerThread = new Thread(new AppletForcedShutdownListener(30000L));
		shutdownListenerThread.setDaemon(true);
		shutdownListenerThread.start();

		if (applet != null) {
			applet.stop();
			applet.destroy();
		}
	}

	@Override
	public void windowOpened(WindowEvent e) { }

	@Override
	public void windowActivated(WindowEvent e) { }

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowIconified(WindowEvent e) { }

	@Override
	public void windowDeiconified(WindowEvent e) { }

	@Override
	public void windowDeactivated(WindowEvent e) { }
}
