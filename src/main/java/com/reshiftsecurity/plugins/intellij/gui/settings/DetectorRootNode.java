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
package com.reshiftsecurity.plugins.intellij.gui.settings;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

final class DetectorRootNode extends DetectorGroupNode {
	@NotNull
	private final Map<String, Map<String, Boolean>> enabled;

	DetectorRootNode(@NotNull final String text, @NotNull final Map<String, Map<String, Boolean>> enabled) {
		super(text);
		this.enabled = enabled;
	}

	@NotNull
	Map<String, Map<String, Boolean>> getEnabledMap() {
		return this.enabled;
	}
}
