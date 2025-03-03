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
import org.jetbrains.plugins.spotbugs.android.AndroidUtil;
import org.jetbrains.plugins.spotbugs.gui.common.NotificationUtil;
import org.jetbrains.plugins.spotbugs.gui.settings.ModuleConfigurableImpl;
import org.jetbrains.plugins.spotbugs.gui.settings.ProjectConfigurableImpl;
import org.jetbrains.plugins.spotbugs.plugins.Plugins;

import java.util.HashSet;
import java.util.Set;

public final class PluginSuggestion {

	private static final String NOTIFICATION_GROUP_ID_PLUGIN_SUGGESTION = "SpotBugs: Plugin Suggestion";
	private static final NotificationGroup NOTIFICATION_GROUP_PLUGIN_SUGGESTION = new NotificationGroup(NOTIFICATION_GROUP_ID_PLUGIN_SUGGESTION, NotificationDisplayType.STICKY_BALLOON, false);

	public static void suggestPlugins(@NotNull final Project project) {
		final ProjectSettings settings = ProjectSettings.getInstance(project);
		if (!NotificationUtil.isGroupEnabled(NOTIFICATION_GROUP_ID_PLUGIN_SUGGESTION)) {
			return;
		}
		if (isAndroidFindbugsPluginEnabled(settings)) {
			return;
		}
		final Set<Suggestion> suggestions = collectSuggestions(project, settings);
		if (!suggestions.isEmpty()) {
			showSuggestions(project, suggestions);
		}
	}

	private static boolean isAndroidFindbugsPluginEnabled(@NotNull final AbstractSettings settings) {
		return isPluginEnabled(Plugins.AndroidFindbugs.id, settings);
	}

	private static boolean isPluginEnabled(@NotNull final String pluginId, @NotNull final AbstractSettings settings) {
		for (final PluginSettings pluginSettings : settings.plugins) {
			if (pluginId.equals(pluginSettings.id)) {
				if (pluginSettings.enabled) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("UnusedParameters") // LATER: enabled plugin - think of bundled and user plugins
	private static void enablePlugin(
			@NotNull final Project project,
			@NotNull final Module module,
			final boolean moduleSettingsOverrideProjectSettings
	) {

		if (moduleSettingsOverrideProjectSettings) {
			ModuleConfigurableImpl.show(module);
		} else {
			ProjectConfigurableImpl.show(project);
		}
	}

	private static void showSuggestions(
			@NotNull final Project project,
			@NotNull final Set<Suggestion> suggestions
	) {

		final StringBuilder sb = new StringBuilder();
		for (final Suggestion suggestion : suggestions) {
			sb.append("&nbsp;&nbsp; <a href='").append(suggestion.pluginId).append("'>").append("Enable '").append(suggestion.name).append("'</a>");
			if (suggestion.moduleSettingsOverrideProjectSettings) {
				sb.append(" for module '").append(suggestion.module.getName()).append("'");
			}
			sb.append("<br>");
		}
		sb.append("<br><a href='").append(AbstractSuggestionNotificationListener.A_HREF_DISABLE_ANCHOR).append("'>Disable Suggestion</a>");

		NOTIFICATION_GROUP_PLUGIN_SUGGESTION.createNotification(
				"SpotBugs Plugin Suggestion",
				sb.toString(),
				NotificationType.INFORMATION,
				new AbstractSuggestionNotificationListener(project, NOTIFICATION_GROUP_ID_PLUGIN_SUGGESTION) {
					@Override
					protected void linkClicked(@NotNull final Notification notification, String description) {
						Suggestion suggestion = suggestions.iterator().next();
						for (final Suggestion s : suggestions) {
							if (suggestion.pluginId.equals(description)) {
								suggestion = s;
								break;
							}
						}
						enablePlugin(project, suggestion.module, suggestion.moduleSettingsOverrideProjectSettings);
						if (suggestions.size() == 1) {
							notification.hideBalloon();
						}
					}
				}
		).setImportant(false).notify(project);
	}

	@NotNull
	private static Set<Suggestion> collectSuggestions(@NotNull final Project project, @NotNull final ProjectSettings settings) {
		final Set<Suggestion> ret = new HashSet<>();
		collectSuggestionsByModules(project, settings, ret);
		return ret;
	}

	private static void collectSuggestionsByModules(
			@NotNull final Project project,
			@NotNull final ProjectSettings settings,
			@NotNull final Set<Suggestion> suggestions
	) {

		final ModuleManager moduleManager = ModuleManager.getInstance(project);
		final Module[] modules = moduleManager.getModules();
		for (final Module module : modules) {
			collectSuggestionsByFacets(settings, module, suggestions);
		}
	}

	private static void collectSuggestionsByFacets(
			@NotNull final ProjectSettings projectSettings,
			@NotNull final Module module,
			@NotNull final Set<Suggestion> suggestions
	) {

		final FacetManager facetManager = FacetManager.getInstance(module);
		if (facetManager == null) {
			return;
		}
		final var facets = facetManager.getAllFacets();
		for (final var facet : facets) {
			var facetTypeId = facet.getTypeId();
			if (facetTypeId != null) {

				if (!isAndroidFindbugsPluginEnabled(projectSettings)) {
					final ModuleSettings moduleSettings = ModuleSettings.getInstance(module);
					if (!moduleSettings.overrideProjectSettings || !isAndroidFindbugsPluginEnabled(moduleSettings)) {
						if (AndroidUtil.isAndroidFacetType(facetTypeId)) {
							suggestions.add(new Suggestion(Plugins.AndroidFindbugs.id, "Android FindBugs", module, moduleSettings.overrideProjectSettings));
						}
					}
				}

			}
		}
	}

	private record Suggestion(
			@NotNull String pluginId,
			@NotNull String name,
			@NotNull Module module,
			boolean moduleSettingsOverrideProjectSettings
	) {

		@Override
		public boolean equals(@Nullable final Object o) {
			return this == o || !(o == null || getClass() != o.getClass()) && pluginId.equals(((Suggestion) o).pluginId);
		}

		@Override
		public int hashCode() {
			return pluginId.hashCode();
		}
	}
}
