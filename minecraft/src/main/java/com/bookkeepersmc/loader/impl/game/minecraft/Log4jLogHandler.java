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
package com.bookkeepersmc.loader.impl.game.minecraft;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import com.bookkeepersmc.loader.api.Version;
import com.bookkeepersmc.loader.api.VersionParsingException;
import com.bookkeepersmc.loader.impl.util.ManifestUtil;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;
import com.bookkeepersmc.loader.impl.util.log.LogHandler;
import com.bookkeepersmc.loader.impl.util.log.LogLevel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

public final class Log4jLogHandler implements LogHandler {
	@Override
	public boolean shouldLog(LogLevel level, LogCategory category) {
		return getLogger(category).isEnabled(translateLogLevel(level));
	}

	@Override
	public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		// TODO: suppress console log output if wasSuppressed is false to avoid duplicate output
		getLogger(category).log(translateLogLevel(level), msg, exc);
	}

	private static Logger getLogger(LogCategory category) {
		Logger ret = (Logger) category.data;

		if (ret == null) {
			category.data = ret = LogManager.getLogger(category.toString());
		}

		return ret;
	}

	private static Level translateLogLevel(LogLevel level) {
		// can't use enum due to it generating a nested class, which would have to be on the same class loader as Log4jLogHandler
		if (level == LogLevel.ERROR) return Level.ERROR;
		if (level == LogLevel.WARN) return Level.WARN;
		if (level == LogLevel.INFO) return Level.INFO;
		if (level == LogLevel.DEBUG) return Level.DEBUG;
		if (level == LogLevel.TRACE) return Level.TRACE;

		throw new IllegalArgumentException("unknown log level: "+level);
	}

	@Override
	public void close() { }

	static {
		if (needsLookupRemoval()) {
			patchJndi();
		} else {
			Log.debug(LogCategory.GAME_PROVIDER, "Log4J2 JNDI removal is unnecessary");
		}
	}

	private static boolean needsLookupRemoval() {
		Manifest manifest;

		try {
			manifest = ManifestUtil.readManifest(LogManager.class);
		} catch (IOException | URISyntaxException e) {
			Log.warn(LogCategory.GAME_PROVIDER, "Can't read Log4J2 Manifest", e);
			return true;
		}

		if (manifest == null) return true;

		String title = ManifestUtil.getManifestValue(manifest, Name.IMPLEMENTATION_TITLE);
		if (title == null || !title.toLowerCase(Locale.ENGLISH).contains("log4j")) return true;

		String version = ManifestUtil.getManifestValue(manifest, Name.IMPLEMENTATION_VERSION);
		if (version == null) return true;

		try {
			return Version.parse(version).compareTo(Version.parse("2.16")) < 0; // 2.15+ doesn't lookup by default, but we patch anything up to 2.16 just in case
		} catch (VersionParsingException e) {
			Log.warn(LogCategory.GAME_PROVIDER, "Can't parse Log4J2 Manifest version %s", version, e);
			return true;
		}
	}

	private static void patchJndi() {
		LoggerContext context = LogManager.getContext(false);

		try {
			context.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class).invoke(context, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName().equals("config")) {
						removeSubstitutionLookups(true);
					}
				}
			});
		} catch (Exception e) {
			Log.warn(LogCategory.GAME_PROVIDER, "Can't register Log4J2 PropertyChangeListener: %s", e.toString());
		}

		removeSubstitutionLookups(false);
	}

	private static void removeSubstitutionLookups(boolean ignoreMissing) {
		// strip the jndi lookup and then all over lookups from the active org.apache.logging.log4j.core.lookup.Interpolator instance's lookups map

		try {
			LoggerContext context = LogManager.getContext(false);
			if (context.getClass().getName().equals("org.apache.logging.log4j.simple.SimpleLoggerContext")) return; // -> no log4j core

			Object config = context.getClass().getMethod("getConfiguration").invoke(context);
			Object substitutor = config.getClass().getMethod("getStrSubstitutor").invoke(config);
			Object varResolver = substitutor.getClass().getMethod("getVariableResolver").invoke(substitutor);
			if (varResolver == null) return;

			boolean removed = false;

			for (Field field : varResolver.getClass().getDeclaredFields()) {
				if (Map.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					@SuppressWarnings("unchecked")
					Map<String, ?> map = (Map<String, ?>) field.get(varResolver);

					if (map.remove("jndi") != null) {
						map.clear();
						removed = true;
						break;
					}
				}
			}

			if (!removed) {
				if (ignoreMissing) return;
				throw new RuntimeException("couldn't find JNDI lookup entry");
			}

			Log.debug(LogCategory.GAME_PROVIDER, "Removed Log4J2 substitution lookups");
		} catch (Exception e) {
			Log.warn(LogCategory.GAME_PROVIDER, "Can't remove Log4J2 JNDI substitution Lookup: %s", e.toString());
		}
	}
}
