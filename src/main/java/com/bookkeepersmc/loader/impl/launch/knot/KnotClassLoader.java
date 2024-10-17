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
package com.bookkeepersmc.loader.impl.launch.knot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Objects;

import com.bookkeepersmc.api.EnvType;
import com.bookkeepersmc.loader.impl.game.GameProvider;
import com.bookkeepersmc.loader.impl.launch.knot.KnotClassDelegate.ClassLoaderAccess;
import com.bookkeepersmc.loader.impl.mrj.AbstractSecureClassLoader;

// class name referenced by string constant in com.bookkeepersmc.loader.impl.util.LoaderUtil.verifyNotInTargetCl
final class KnotClassLoader extends AbstractSecureClassLoader implements ClassLoaderAccess {
	private static final class DynamicURLClassLoader extends URLClassLoader {
		private DynamicURLClassLoader(URL[] urls) {
			super(urls, new DummyClassLoader());
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}

		static {
			registerAsParallelCapable();
		}
	}

	private final DynamicURLClassLoader urlLoader;
	private final ClassLoader originalLoader;
	private final KnotClassDelegate<KnotClassLoader> delegate;

	KnotClassLoader(boolean isDevelopment, EnvType envType, GameProvider provider) {
		super("knot", new DynamicURLClassLoader(new URL[0]));
		this.originalLoader = getClass().getClassLoader();
		this.urlLoader = (DynamicURLClassLoader) getParent();
		this.delegate = new KnotClassDelegate<>(isDevelopment, envType, this, originalLoader, provider);
	}

	KnotClassDelegate<?> getDelegate() {
		return delegate;
	}

	@Override
	public URL getResource(String name) {
		Objects.requireNonNull(name);

		URL url = urlLoader.getResource(name);

		if (url == null) {
			url = originalLoader.getResource(name);
		}

		return url;
	}

	@Override
	public URL findResource(String name) {
		Objects.requireNonNull(name);

		return urlLoader.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		Objects.requireNonNull(name);

		return urlLoader.findResources(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		Objects.requireNonNull(name);

		InputStream inputStream = urlLoader.getResourceAsStream(name);

		if (inputStream == null) {
			inputStream = originalLoader.getResourceAsStream(name);
		}

		return inputStream;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Objects.requireNonNull(name);

		final Enumeration<URL> resources = urlLoader.getResources(name);

		if (!resources.hasMoreElements()) {
			return originalLoader.getResources(name);
		}

		return resources;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return delegate.loadClass(name, resolve);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return delegate.tryLoadClass(name, false);
	}

	@Override
	public void addUrlFwd(URL url) {
		urlLoader.addURL(url);
	}

	@Override
	public URL findResourceFwd(String name) {
		return urlLoader.findResource(name);
	}

	@Override
	public Package getPackageFwd(String name) {
		return super.getPackage(name);
	}

	@Override
	public Package definePackageFwd(String name, String specTitle, String specVersion, String specVendor,
			String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
		return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
	}

	@Override
	public Object getClassLoadingLockFwd(String name) {
		return super.getClassLoadingLock(name);
	}

	@Override
	public Class<?> findLoadedClassFwd(String name) {
		return super.findLoadedClass(name);
	}

	@Override
	public Class<?> defineClassFwd(String name, byte[] b, int off, int len, CodeSource cs) {
		return super.defineClass(name, b, off, len, cs);
	}

	@Override
	public void resolveClassFwd(Class<?> cls) {
		super.resolveClass(cls);
	}

	static {
		registerAsParallelCapable();
	}
}
