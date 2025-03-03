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
package org.jetbrains.plugins.spotbugs.android;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.core.AbstractSuggestionNotificationListener;
import org.jetbrains.plugins.spotbugs.core.ModuleSettings;
import org.jetbrains.plugins.spotbugs.core.ProjectSettings;
import org.jetbrains.plugins.spotbugs.gui.settings.ModuleConfigurableImpl;
import org.jetbrains.plugins.spotbugs.gui.settings.ProjectConfigurableImpl;

public final class RFilerFilterSuggestion {

	private static final String NOTIFICATION_GROUP_ID_SUGGESTION = "SpotBugs: R-File Filter Suggestion";
	private static final NotificationGroup NOTIFICATION_GROUP_PLUGIN_SUGGESTION = new NotificationGroup(NOTIFICATION_GROUP_ID_SUGGESTION, NotificationDisplayType.STICKY_BALLOON, false);

	@NotNull
	private final Project project;

	public RFilerFilterSuggestion(@NotNull final Project project) {
		this.project = project;
	}

	public void suggest() {
		final ProjectSettings projectSettings = ProjectSettings.getInstance(project);
		boolean projectShow = false;
		for (final Module module : ModuleManager.getInstance(project).getModules()) {
			for (final Facet<?> facet : FacetManager.getInstance(module).getAllFacets()) {
				if (AndroidUtil.isAndroidFacetType(facet.getTypeId())) {
					final ModuleSettings moduleSettings = ModuleSettings.getInstance(module);
					if (moduleSettings.overrideProjectSettings) {
						if (moduleSettings.excludeFilterFiles.isEmpty()) {
							showSuggestion(module);
						}
					} else if (!projectShow) {
						if (projectSettings.excludeFilterFiles.isEmpty()) {
							projectShow = true;
							showSuggestion(null);
						}
					}
				}
			}
		}
	}

	private void showSuggestion(@Nullable final Module module) {
		final StringBuilder sb = new StringBuilder();
		sb.append("<a href=add>").append("Add R.class File Filter").append("</a>");
		if (module != null) {
			sb.append(" for module '").append(module.getName()).append("'");
		}
		sb.append("<br><br><a href='").append(AbstractSuggestionNotificationListener.A_HREF_DISABLE_ANCHOR).append("'>Disable Suggestion</a>");

		NOTIFICATION_GROUP_PLUGIN_SUGGESTION.createNotification(
				"SpotBugs R-Filter Suggestion",
				sb.toString(),
				NotificationType.INFORMATION,
				new AbstractSuggestionNotificationListener(project, NOTIFICATION_GROUP_ID_SUGGESTION) {
					@Override
					protected void linkClicked(@NotNull final Notification notification, String description) {
						if (module != null) {
							ModuleConfigurableImpl.showFileFilterAndAddRFilerFilter(module);
						} else {
							ProjectConfigurableImpl.showFileFilterAndAddRFilerFilter(project);
						}
						notification.hideBalloon();
					}
				}
		).setImportant(false).notify(project);
	}
}
