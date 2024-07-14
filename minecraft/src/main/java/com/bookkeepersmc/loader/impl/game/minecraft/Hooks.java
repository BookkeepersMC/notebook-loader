/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bookkeepersmc.loader.impl.game.minecraft;

import java.io.File;

import com.bookkeepersmc.api.ClientModInitializer;
import com.bookkeepersmc.api.DedicatedServerModInitializer;
import com.bookkeepersmc.api.ModInitializer;
import com.bookkeepersmc.loader.impl.FabricLoaderImpl;
import com.bookkeepersmc.loader.impl.util.log.Log;
import com.bookkeepersmc.loader.impl.util.log.LogCategory;

public final class Hooks {
	public static final String INTERNAL_NAME = Hooks.class.getName().replace('.', '/');

	public static String appletMainClass;

	public static final String FABRIC = "fabric";
	public static final String VANILLA = "vanilla";

	public static String insertBranding(final String brand) {
		if (brand == null || brand.isEmpty()) {
			Log.warn(LogCategory.GAME_PROVIDER, "Null or empty branding found!", new IllegalStateException());
			return FABRIC;
		}

		return VANILLA.equals(brand) ? FABRIC : brand + ',' + FABRIC;
	}

	public static void startClient(File runDir, Object gameInstance) {
		if (runDir == null) {
			runDir = new File(".");
		}

		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
		loader.prepareModInit(runDir.toPath(), gameInstance);
		loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
		loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
	}

	public static void startServer(File runDir, Object gameInstance) {
		if (runDir == null) {
			runDir = new File(".");
		}

		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
		loader.prepareModInit(runDir.toPath(), gameInstance);
		loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
		loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
	}

	public static void setGameInstance(Object gameInstance) {
		FabricLoaderImpl.INSTANCE.setGameInstance(gameInstance);
	}
}
