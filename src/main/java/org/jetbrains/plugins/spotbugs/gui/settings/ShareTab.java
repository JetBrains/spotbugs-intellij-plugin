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
package org.jetbrains.plugins.spotbugs.gui.settings;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.common.util.FileUtilFb;
import org.jetbrains.plugins.spotbugs.core.WorkspaceSettings;
import org.jetbrains.plugins.spotbugs.gui.common.HAlignment;
import org.jetbrains.plugins.spotbugs.gui.common.VAlignment;
import org.jetbrains.plugins.spotbugs.gui.common.VerticalFlowLayout;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ShareTab extends JPanel implements SettingsOwner<WorkspaceSettings>, Disposable {

	private static final Logger LOGGER = Logger.getInstance(ShareTab.class);

	@NotNull
	private final Project project;

	@Nullable
	private final String importFilePathKey;

	private final JLabel description;
	private final HyperlinkLabel link;
	private final LabeledComponent<TextFieldWithBrowseButton> importPathLabel;

	ShareTab(@NotNull final Project project, @Nullable final Module module) {
		super(new VerticalFlowLayout(HAlignment.Left, VAlignment.Top, 0, 0, true, false));
		this.project = project;
		importFilePathKey = module != null ? module.getName() : WorkspaceSettings.PROJECT_IMPORT_FILE_PATH_KEY;

		description = new JLabel("<html>" + ResourcesLoader.getString("share.description"));
		description.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		description.setIcon(MessageType.INFO.getDefaultIcon());
		description.setDisabledIcon(MessageType.INFO.getDefaultIcon());

		final String url = ResourcesLoader.getString("share.url");
		link = new HyperlinkLabel(); // LATER: HotspotPainter (Search) does not work with HyperlinkLabel
		link.setHyperlinkText(url);
		link.setHyperlinkTarget(url);

		final JPanel linkPane = new JPanel(new FlowLayout(FlowLayout.LEFT, MessageType.INFO.getDefaultIcon().getIconWidth() + 5, 0));
		linkPane.add(link);

		final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(XmlFileType.INSTANCE);
		final FileTextField field = FileChooserFactory.getInstance().createFileTextField(descriptor, this);
		final TextFieldWithBrowseButton importPath = new TextFieldWithBrowseButton(field.getField());
		importPath.addBrowseFolderListener(
				null,
				descriptor.withTitle(ResourcesLoader.getString("settings.choose.title"))
						.withDescription(ResourcesLoader.getString("settings.choose.description"))
		);
		importPathLabel = new LabeledComponent<>();
		importPathLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		importPathLabel.setComponent(importPath);
		importPathLabel.setLabelLocation(BorderLayout.WEST);
		importPathLabel.setText(ResourcesLoader.getString("share.file.title"));

		add(description);
		add(linkPane);
		add(importPathLabel);
	}

	@Nullable
	private String getImportFilePath() {
		String ret = importPathLabel.getComponent().getText();
		if (StringUtil.isEmptyOrSpaces(ret)) {
			return null;
		}
		return FileUtilFb.toSystemIndependentName(ret.trim());
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		description.setEnabled(enabled);
		link.setEnabled(enabled);
		importPathLabel.setEnabled(enabled);
	}

	@Override
	public boolean isModified(@NotNull final WorkspaceSettings settings) {
		return !StringUtil.equals(settings.importFilePath.get(importFilePathKey), getImportFilePath());
	}

	@Override
	public void apply(@NotNull final WorkspaceSettings settings) throws ConfigurationException {
		final String filePath = getImportFilePath();
		if (filePath != null) {
			final File file = new File(filePath);
			if (!file.exists()) {
				throw new ConfigurationException(ResourcesLoader.getString("error.path.exists", filePath));
			}
			if (!file.isFile()) {
				throw new ConfigurationException(ResourcesLoader.getString("error.file.type", filePath));
			}
			if (!file.canRead()) {
				throw new ConfigurationException(ResourcesLoader.getString("error.file.readable", filePath));
			}
		}
		if (StringUtil.isEmptyOrSpaces(filePath)) {
			settings.importFilePath.remove(importFilePathKey);
		} else {
			settings.importFilePath.put(importFilePathKey, filePath);
		}
		removeOrphanEntries(settings.importFilePath);
	}

	@Override
	public void reset(@NotNull final WorkspaceSettings settings) {
		importPathLabel.getComponent().setText(FileUtilFb.toSystemDependentName(settings.importFilePath.get(importFilePathKey)));
	}

	private void removeOrphanEntries(@NotNull final Map<String, String> importFilePath) {
    final Set<String> moduleNames = new HashSet<>();
		for (final Module module : ModuleManager.getInstance(project).getModules()) {
			moduleNames.add(module.getName());
		}
		for (final String moduleName : new HashSet<>(importFilePath.keySet())) {
			if (!StringUtil.equals(WorkspaceSettings.PROJECT_IMPORT_FILE_PATH_KEY, moduleName)) {
				if (!moduleNames.contains(moduleName)) {
					String path = importFilePath.remove(moduleName);
					LOGGER.warn(String.format(
							"Remove importFilePath (%s) from settings because module with name '%s' does not exist", path, moduleName));
				}
			}
		}
	}

	@Override
	public void dispose() {
	}

	void requestFocusOnImportFile() {
		IdeFocusManager.findInstance().doWhenFocusSettlesDown(
				() -> IdeFocusManager.findInstance().requestFocus(importPathLabel.getComponent().getTextField(), true));
	}

	@NotNull
	static String getSearchPath() {
		return ResourcesLoader.getString("settings.share");
	}

	@NotNull
	static String[] getSearchResourceKey() {
		return new String[]{
				"share.description",
				"share.file.title",
				"share.url"
		};
	}
}
