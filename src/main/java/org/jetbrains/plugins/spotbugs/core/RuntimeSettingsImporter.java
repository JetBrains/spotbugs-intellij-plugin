/*
 * Copyright 2020 SpotBugs plugin contributors
 *
 * This file is part of IntelliJ SpotBugs plugin.
 *
 * IntelliJ SpotBugs plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * IntelliJ SpotBugs plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ SpotBugs plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.jetbrains.plugins.spotbugs.core;

import com.intellij.notification.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.common.EventDispatchThreadHelper;
import org.jetbrains.plugins.spotbugs.common.util.IoUtil;
import org.jetbrains.plugins.spotbugs.gui.settings.ModuleConfigurableImpl;
import org.jetbrains.plugins.spotbugs.gui.settings.ProjectConfigurableImpl;
import org.jetbrains.plugins.spotbugs.gui.settings.SettingsImporter;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

final class RuntimeSettingsImporter {

	private static final Logger LOGGER = Logger.getInstance(RuntimeSettingsImporter.class);

	private RuntimeSettingsImporter() {
	}

	static boolean importSettings(
			@NotNull final Project project,
			@NotNull final Module module,
			@NotNull final AbstractSettings settings,
			@NotNull final String filePath,
			@NotNull final String importFilePathKey
	) {

		final File file = new File(filePath);
		if (!file.exists()) {
			showImportPreferencesWarning(project, module, ResourcesLoader.getString("analysis.error.importSettings.notExists", filePath));
			return false;
		}
		if (!file.isFile()) {
			showImportPreferencesWarning(project, module, ResourcesLoader.getString("analysis.error.importSettings.noFile", filePath));
			return false;
		}
		if (!file.canRead()) {
			showImportPreferencesWarning(project, module, ResourcesLoader.getString("analysis.error.importSettings.notReadable", filePath));
			return false;
		}

		try {
			final FileInputStream input = new FileInputStream(file);
			try {
				final WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance(project);
				final Map<String, String> importFilePath = new HashMap<>(workspaceSettings.importFilePath);
				final boolean success = new SettingsImporter(project) {
					@Override
					protected void handleError(@NotNull final String title, @NotNull final String message) {
						showImportPreferencesWarning(project, module, title, message);
					}
				}.doImport(input, settings, importFilePathKey);
				workspaceSettings.importFilePath = importFilePath; // restore current
				return success;
			} finally {
				IoUtil.safeClose(input);
			}
		} catch (final Exception e) {
			final String msg = ResourcesLoader.getString("analysis.error.importSettings.fatal", filePath, e.getMessage());
			LOGGER.error(msg, e);
			showImportPreferencesWarning(project, module, msg);
			return false;
		}
	}

	private static void showImportPreferencesWarning(@NotNull final Project project, @NotNull final Module module, @NotNull String message) {
		showImportPreferencesWarning(project, module, null, message);
	}

	private static void showImportPreferencesWarning(
			@NotNull final Project project,
			@NotNull final Module module,
			@Nullable final String title,
			@NotNull final String message
	) {

		EventDispatchThreadHelper.invokeLater(() -> showImportPreferencesWarningImpl(project, module, title, message));
	}

	private static void showImportPreferencesWarningImpl(
			@NotNull final Project project,
			@NotNull final Module module,
			@Nullable String title,
			@NotNull String message
	) {

		final boolean overrideProjectSettings = ModuleSettings.getInstance(module).overrideProjectSettings;
		if (StringUtil.isEmptyOrSpaces(title)) {
			title = ResourcesLoader.getString("analysis.error.importSettings.title");
		}
		NotificationGroupManager.getInstance()
				.getNotificationGroup("SpotBugs.AnalyzeError")
				.createNotification(title, message, NotificationType.ERROR)
				.addAction(NotificationAction.create(ResourcesLoader.getString("edit.settings"), (e, notification) -> {
					notification.hideBalloon();
					if (overrideProjectSettings) {
						ModuleConfigurableImpl.showShare(module);
					} else {
						ProjectConfigurableImpl.showShare(project);
					}
				}))
				.notify(project);
	}
}
