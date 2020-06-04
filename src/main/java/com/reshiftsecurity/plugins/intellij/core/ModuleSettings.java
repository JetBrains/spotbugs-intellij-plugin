/*
 * Copyright 2020 Reshift Security Intellij plugin contributors
 *
 * This file is part of Reshift Security Intellij plugin.
 *
 * Reshift Security Intellij plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Reshift Security Intellij plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Reshift Security Intellij plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.reshiftsecurity.plugins.intellij.core;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
		name = "ReshiftSecurity-Intellij",
		storages = @Storage(file = StoragePathMacros.MODULE_FILE)
)
public final class ModuleSettings extends AbstractSettings implements PersistentStateComponent<ModuleSettings> {

	public boolean overrideProjectSettings;

	@Nullable
	@Override
	public ModuleSettings getState() {
		return this;
	}

	@Override
	public void loadState(@NotNull final ModuleSettings state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	public static ModuleSettings getInstance(@NotNull final Module module) {
		return ModuleServiceManager.getService(module, ModuleSettings.class);
	}
}
