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
package org.jetbrains.plugins.spotbugs.gui.preferences;

import com.intellij.openapi.util.text.StringUtil;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.common.FindBugsPluginConstants;
import org.jetbrains.plugins.spotbugs.common.util.FileUtilFb;
import org.jetbrains.plugins.spotbugs.core.AbstractSettings;
import org.jetbrains.plugins.spotbugs.core.PluginSettings;
import org.jetbrains.plugins.spotbugs.core.WorkspaceSettings;
import org.jetbrains.plugins.spotbugs.plugins.AbstractPluginLoaderLegacy;
import org.jetbrains.plugins.spotbugs.plugins.Plugins;
import org.jetbrains.plugins.spotbugs.preferences.PersistencePreferencesBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LegacyAbstractSettingsConverter {

	private static final String PROPERTIES_PREFIX = "property.";
	private static final String RUN_ANALYSIS_IN_BACKGROUND = PROPERTIES_PREFIX + "runAnalysisInBackground";
	private static final String ANALYSIS_EFFORT_LEVEL = PROPERTIES_PREFIX + "analysisEffortLevel";
	private static final String MIN_PRIORITY_TO_REPORT = PROPERTIES_PREFIX + "minPriorityToReport";
	private static final String TOOLWINDOW_TO_FRONT = PROPERTIES_PREFIX + "toolWindowToFront";
	private static final String COMPILE_BEFORE_ANALYZE = PROPERTIES_PREFIX + "compileBeforeAnalyse";
	private static final String ANALYZE_AFTER_COMPILE = PROPERTIES_PREFIX + "analyzeAfterCompile";
	private static final String ANALYZE_AFTER_AUTOMAKE = PROPERTIES_PREFIX + "analyzeAfterAutoMake";
	private static final String IMPORT_FILE_PATH = PROPERTIES_PREFIX + "importedFilePath";
	private static final String EXPORT_BASE_DIR = PROPERTIES_PREFIX + "exportBaseDir";
	private static final String EXPORT_CREATE_ARCHIVE_DIR = PROPERTIES_PREFIX + "exportCreateArchiveDir";
	private static final String EXPORT_AS_HTML = PROPERTIES_PREFIX + "exportAsHtml";
	private static final String EXPORT_AS_XML = PROPERTIES_PREFIX + "exportAsXml";
	private static final String EXPORT_OPEN_BROWSER = PROPERTIES_PREFIX + "exportOpenBrowser";
	private static final String TOOLWINDOW_SCROLL_TO_SOURCE = PROPERTIES_PREFIX + "toolWindowScrollToSource";
	private static final String TOOLWINDOW_EDITOR_PREVIEW = PROPERTIES_PREFIX + "toolWindowEditorPreview";
	private static final String TOOLWINDOW_GROUP_BY = PROPERTIES_PREFIX + "toolWindowGroupBy";
	private static final String ANNOTATION_SUPPRESS_WARNING_CLASS = PROPERTIES_PREFIX + "annotationSuppressWarningsClass";
	private static final String ANNOTATION_GUTTER_ICON_ENABLED = PROPERTIES_PREFIX + "annotationGutterIconEnabled";
	private static final String ANNOTATION_TEXT_RAGE_MARKUP_ENABLED = PROPERTIES_PREFIX + "annotationTextRangeMarkupEnabled";

	private LegacyAbstractSettingsConverter() {
	}

	public static void applyTo(
			@NotNull final PersistencePreferencesBean from,
			@NotNull final AbstractSettings to,
			@Nullable final WorkspaceSettings toWorkspace,
			@NotNull final String importFilePathKey
	) {
		applyBasePreferencesTo(from, to, toWorkspace, importFilePathKey);
		applyBugCategoriesTo(from, to);
		applyFileFiltersTo(from, to);
		applyDetectorsAndPluginsTo(from, to);
	}

	private static void applyBasePreferencesTo(
			@NotNull final PersistencePreferencesBean from,
			@NotNull final AbstractSettings to,
			@Nullable final WorkspaceSettings toWorkspace,
			@NotNull final String importFilePathKey
	) {

		final Map<String, String> p = from.getBasePreferences();
		if (p == null || p.isEmpty()) {
			return;
		}

		to.analysisEffort = asString(p.get(ANALYSIS_EFFORT_LEVEL), to.analysisEffort);
		to.minPriority = asString(p.get(MIN_PRIORITY_TO_REPORT), to.minPriority);
		to.suppressWarningsClassName = asString(p.get(ANNOTATION_SUPPRESS_WARNING_CLASS), to.suppressWarningsClassName);

		if (toWorkspace != null) {
			toWorkspace.compileBeforeAnalyze = asBoolean(p.get(COMPILE_BEFORE_ANALYZE), toWorkspace.compileBeforeAnalyze);
			toWorkspace.analyzeAfterCompile = asBoolean(p.get(ANALYZE_AFTER_COMPILE), toWorkspace.analyzeAfterCompile);
			toWorkspace.analyzeAfterAutoMake = asBoolean(p.get(ANALYZE_AFTER_AUTOMAKE), toWorkspace.analyzeAfterAutoMake);
			toWorkspace.runInBackground = asBoolean(p.get(RUN_ANALYSIS_IN_BACKGROUND), toWorkspace.runInBackground);

			final String importFilePath = FileUtilFb.toSystemIndependentName(asString(p.get(IMPORT_FILE_PATH), null));
			if (!StringUtil.isEmptyOrSpaces(importFilePath)) {
				toWorkspace.importFilePath.put(importFilePathKey, importFilePath);
			}
			toWorkspace.exportBugCollectionDirectory = asString(p.get(EXPORT_BASE_DIR), toWorkspace.exportBugCollectionDirectory);
			toWorkspace.exportBugCollectionAsHtml = asBoolean(p.get(EXPORT_AS_HTML), toWorkspace.exportBugCollectionAsHtml);
			toWorkspace.exportBugCollectionAsXml = asBoolean(p.get(EXPORT_AS_XML), toWorkspace.exportBugCollectionAsXml);
			toWorkspace.exportBugCollectionCreateSubDirectory = asBoolean(p.get(EXPORT_CREATE_ARCHIVE_DIR), toWorkspace.exportBugCollectionCreateSubDirectory);
			toWorkspace.openExportedHtmlBugCollectionInBrowser = asBoolean(p.get(EXPORT_OPEN_BROWSER), toWorkspace.openExportedHtmlBugCollectionInBrowser);

			toWorkspace.toolWindowToFront = asBoolean(p.get(TOOLWINDOW_TO_FRONT), toWorkspace.toolWindowToFront);
			toWorkspace.toolWindowScrollToSource = asBoolean(p.get(TOOLWINDOW_SCROLL_TO_SOURCE), toWorkspace.toolWindowScrollToSource);
			toWorkspace.toolWindowEditorPreview = asBoolean(p.get(TOOLWINDOW_EDITOR_PREVIEW), toWorkspace.toolWindowEditorPreview);
			toWorkspace.toolWindowGroupBy = asString(p.get(TOOLWINDOW_GROUP_BY), toWorkspace.toolWindowGroupBy);

			toWorkspace.annotationGutterIcon = asBoolean(p.get(ANNOTATION_GUTTER_ICON_ENABLED), toWorkspace.annotationGutterIcon);
			toWorkspace.annotationTextRangeMarkup = asBoolean(p.get(ANNOTATION_TEXT_RAGE_MARKUP_ENABLED), toWorkspace.annotationTextRangeMarkup);
		}
	}

	private static void applyBugCategoriesTo(@NotNull final PersistencePreferencesBean from, @NotNull final AbstractSettings to) {
		final Map<String, String> bugCategories = from.getBugCategories();
		if (bugCategories == null || bugCategories.isEmpty()) {
			return;
		}
		for (final String category : new String[]{
				"BAD_PRACTICE",
				"MALICIOUS_CODE",
				"CORRECTNESS",
				"PERFORMANCE",
				"SECURITY",
				"STYLE",
				"EXPERIMENTAL",
				"MT_CORRECTNESS",
				"I18N"
		}) {
			if (bugCategories.containsKey(category)) {
				final String value = bugCategories.get(category);
				if (!StringUtil.isEmptyOrSpaces(value) && !Boolean.valueOf(value)) {
					to.hiddenBugCategory.add(category);
				}
			}
		}
	}

	private static void applyFileFiltersTo(@NotNull final PersistencePreferencesBean from, @NotNull final AbstractSettings to) {
		final List<String> includeFilters = from.getIncludeFilters();
		if (includeFilters != null && !includeFilters.isEmpty()) {
			for (final String includeFilter : includeFilters) {
				to.includeFilterFiles.put(includeFilter, true);
			}
		}
		final List<String> excludeFilters = from.getExcludeFilters();
		if (excludeFilters != null && !excludeFilters.isEmpty()) {
			for (final String excludeFilter : excludeFilters) {
				to.excludeFilterFiles.put(excludeFilter, true);
			}
		}
		final List<String> excludeBugsFiles = from.getExcludeBaselineBugs();
		if (excludeBugsFiles != null && !excludeBugsFiles.isEmpty()) {
			for (final String excludeBugsFile : excludeBugsFiles) {
				to.excludeBugsFiles.put(excludeBugsFile, true);
			}
		}
	}

	private static void applyDetectorsAndPluginsTo(@NotNull final PersistencePreferencesBean from, @NotNull final AbstractSettings to) {
		final LegacyPluginLoaderImpl legacyPluginLoader = new LegacyPluginLoaderImpl();
		legacyPluginLoader.load(
				from.getPlugins(),
				from.getDisabledUserPluginIds(),
				from.getEnabledBundledPluginIds(),
				from.getDisabledBundledPluginIds()
		);

		final Map<String, Map<String, Boolean>> detectorDefaultEnabled = new HashMap<>();
		for (final DetectorFactory detector : DetectorFactoryCollection.instance().getFactories()) {
			detectorDefaultEnabled.computeIfAbsent(detector.getPlugin().getPluginId(), k -> new HashMap<>())
					.put(detector.getShortName(), detector.isDefaultEnabled());
		}

		// core detectors
		apply(from.getDetectors(), to.detectors, detectorDefaultEnabled.get(FindBugsPluginConstants.FINDBUGS_CORE_PLUGIN_ID));

		// bundled plugins and detectors
		for (final String bundledPluginId : new String[]{
				Plugins.AndroidFindbugs.id,
				Plugins.fb_contrib.id,
				Plugins.findsecbugs_plugin.id
		}) {
			final boolean enabled = from.getEnabledBundledPluginIds() != null && from.getEnabledBundledPluginIds().contains(bundledPluginId);
			final PluginSettings pluginSettings = new PluginSettings();
			pluginSettings.id = bundledPluginId;
			pluginSettings.enabled = enabled;
			pluginSettings.bundled = true;
			apply(from.getDetectors(), pluginSettings.detectors, detectorDefaultEnabled.get(bundledPluginId));
			if (enabled || !pluginSettings.detectors.isEmpty()) {
				to.plugins.add(pluginSettings);
			}
		}

		// user plugins and detectors
		final List<String> plugins = from.getPlugins();
		if (plugins != null && !plugins.isEmpty()) {
			for (final String pluginUrl : plugins) {
				final String pluginId = legacyPluginLoader.userPluginIdByPluginUrl.get(pluginUrl);
				if (!StringUtil.isEmptyOrSpaces(pluginId)) {
					final PluginSettings pluginSettings = new PluginSettings();
					pluginSettings.id = pluginId;
					pluginSettings.enabled = from.getDisabledUserPluginIds() == null || !from.getDisabledUserPluginIds().contains(pluginId);
					pluginSettings.url = pluginUrl;
					apply(from.getDetectors(), pluginSettings.detectors, detectorDefaultEnabled.get(pluginId));
					to.plugins.add(pluginSettings);
				}
			}
		}
	}

	private static void apply(
			@Nullable final Map<String, String> from,
			@NotNull final Map<String, Boolean> to,
			@Nullable final Map<String, Boolean> defaults
	) {
		if (from == null || defaults == null) {
			return;
		}
		for (final Map.Entry<String, String> fromEntry : from.entrySet()) {
			if (!StringUtil.isEmptyOrSpaces(fromEntry.getValue()) && !StringUtil.isEmptyOrSpaces(fromEntry.getKey())) {
				final boolean enabled = Boolean.parseBoolean(fromEntry.getValue());
				final Boolean defaultEnabled = defaults.get(fromEntry.getKey());
				if (defaultEnabled != null) { // otherwise detector does not belong to the plugin
					final boolean apply = defaultEnabled != enabled;
					if (apply) {
						to.put(fromEntry.getKey(), enabled);
					}
				}
			}
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private static boolean asBoolean(@Nullable final String value, final boolean defaultValue) {
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)) {
			return false;
		}
		return defaultValue;
	}

	private static String asString(@Nullable final String value, final String defaultValue) {
		if (StringUtil.isEmptyOrSpaces(value)) {
			return defaultValue;
		}
		return value;
	}

	private static class LegacyPluginLoaderImpl extends AbstractPluginLoaderLegacy {
		@NotNull
		private final Map<String, String> userPluginIdByPluginUrl;

		LegacyPluginLoaderImpl() {
			super(false);
			userPluginIdByPluginUrl = new HashMap<>();
		}

		@Override
		protected void seenUserPlugin(@NotNull final String pluginUrl, Plugin plugin) {
			userPluginIdByPluginUrl.put(pluginUrl, plugin.getPluginId());
		}
	}
}
