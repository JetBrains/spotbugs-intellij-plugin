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
package org.jetbrains.plugins.spotbugs.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import edu.umd.cs.findbugs.SortedBugCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.EventDispatchThreadHelper;
import org.jetbrains.plugins.spotbugs.common.ExportErrorType;
import org.jetbrains.plugins.spotbugs.common.util.ErrorUtil;
import org.jetbrains.plugins.spotbugs.common.util.FileUtilFb;
import org.jetbrains.plugins.spotbugs.core.FindBugsProject;
import org.jetbrains.plugins.spotbugs.core.FindBugsResult;
import org.jetbrains.plugins.spotbugs.core.FindBugsState;
import org.jetbrains.plugins.spotbugs.core.WorkspaceSettings;
import org.jetbrains.plugins.spotbugs.gui.export.ExportBugCollectionDialog;
import org.jetbrains.plugins.spotbugs.gui.toolwindow.view.ToolWindowPanel;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public final class ExportBugCollection extends AbstractAction {

	@Override
	void updateImpl(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		boolean enable = false;
		if (state.isIdle()) {
			final ToolWindowPanel panel = ToolWindowPanel.getInstance(toolWindow);
			if (panel != null) {
				final FindBugsResult result = panel.getResult();
				enable = result != null && !result.isBugCollectionEmpty();
			}
		}

		e.getPresentation().setEnabled(enable);
		e.getPresentation().setVisible(true);
	}

	@Override
	void actionPerformedImpl(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		final ToolWindowPanel panel = ToolWindowPanel.getInstance(toolWindow);
		if (panel == null) {
			return;
		}

		final ExportBugCollectionDialog dialog = new ExportBugCollectionDialog(project);
		dialog.reset();
		if (!dialog.showAndGet()) {
			return;
		}
		dialog.apply();

		final WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance(project);
		final String exportDir = workspaceSettings.exportBugCollectionDirectory;
		final boolean exportXml = workspaceSettings.exportBugCollectionAsXml;
		final boolean exportHtml = workspaceSettings.exportBugCollectionAsHtml;
		final boolean createSubDir = workspaceSettings.exportBugCollectionCreateSubDirectory;
		final boolean openInBrowser = workspaceSettings.openExportedHtmlBugCollectionInBrowser;

		final File exportDirPath = new File(exportDir);
		ExportErrorType errorType = ExportErrorType.from(exportDirPath);
		if (errorType != null) {
			showError(errorType.getText(exportDirPath));
			actionPerformedImpl(e, project, toolWindow, state);
			return;
		}

		final FindBugsResult result = panel.getResult();

		new Task.Backgroundable(project, ResourcesLoader.getString("export.progress.title"), false) {
			@Override
			public void run(@NotNull final ProgressIndicator indicator) {
				try {
					FileUtilFb.mkdirs(exportDirPath);
					File finalExportDir = exportDirPath;
					final String currentTime = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH).format(new Date());
					if (createSubDir) {
						final String dirName = "spotbugs-result-" + project.getName() + "_" + currentTime;
						finalExportDir = new File(exportDirPath, dirName);
						FileUtilFb.mkdirs(finalExportDir);
					}
					final boolean multiModule = result.getResults().size() > 1;

					for (final Map.Entry<edu.umd.cs.findbugs.Project, SortedBugCollection> entry : result.getResults().entrySet()) {
						final String fileName;
						if (createSubDir) {
							if (multiModule && entry.getKey() instanceof FindBugsProject) {
								fileName = ((FindBugsProject) entry.getKey()).getModule().getName();
							} else {
								fileName = "result";
							}
						} else {
							fileName = "spotbugs-result-" + entry.getKey().getProjectName() + "_" + currentTime;
						}
						exportImpl(
								entry.getValue(),
								finalExportDir,
								fileName,
								exportXml,
								exportHtml,
								openInBrowser
						);
					}
				} catch (final Exception e) {
					throw ErrorUtil.toUnchecked(e);
				}
			}
		}.queue();
	}

	private void exportImpl(
			@NotNull final SortedBugCollection bugCollection,
			@NotNull final File exportDir,
			@NotNull final String fileName,
			final boolean exportXml,
			final boolean exportHtml,
			final boolean openInBrowser
	) throws Exception {
		final boolean withMessages = bugCollection.getWithMessages();
		try {
			bugCollection.setWithMessages(true);
			if (exportXml) {
				final File xml = new File(exportDir, fileName + ".xml");
				XmlBugCollectionExporter xmlExporter = new XmlBugCollectionExporter();
				xmlExporter.export(bugCollection, xml);
			}
			if (exportHtml) {
				final File html = new File(exportDir, fileName + ".html");
				HtmlBugCollectionExporter htmlExporter = new HtmlBugCollectionExporter();
				htmlExporter.export(bugCollection, html);
				if (openInBrowser) {
					openInBrowser(html);
				}
			}
		} finally {
			bugCollection.setWithMessages(withMessages);
		}
	}

	private static void openInBrowser(@NotNull final File file) {
		EventDispatchThreadHelper.invokeLater(() -> BrowserUtil.browse(file.toURI()));
	}

	private static void showError(@NotNull final String message) {
		EventDispatchThreadHelper.invokeLater(
				() -> Messages.showErrorDialog(
						message, StringUtil.capitalizeWords(ResourcesLoader.getString("export.title"), true)));
	}
}
