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
package org.jetbrains.plugins.spotbugs.gui.toolwindow.view;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.diagnostic.IdeMessagePanel;
import com.intellij.notification.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.*;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.*;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.*;
import org.jetbrains.plugins.spotbugs.common.*;
import org.jetbrains.plugins.spotbugs.common.util.FindBugsUtil;
import org.jetbrains.plugins.spotbugs.core.FindBugsResult;
import org.jetbrains.plugins.spotbugs.gui.common.*;
import org.jetbrains.plugins.spotbugs.messages.*;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

@SuppressWarnings({"HardCodedStringLiteral", "AnonymousInnerClass", "AnonymousInnerClassMayBeStatic"})
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SE_BAD_FIELD"})
public final class ToolWindowPanel extends JPanel implements AnalysisStateListener, Disposable {

	static final String NOTIFICATION_GROUP_ID_ANALYSIS_FINISHED = "SpotBugs.AnalysisFinished";
	private static final Logger LOGGER = Logger.getInstance(ToolWindowPanel.class.getName());

	private static final String A_HREF_MORE_ANCHOR = "#more";
	private static final String A_HREF_ERROR_ANCHOR = "#error";

	private static final String DEFAULT_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.4) (LEAF name=right weight=0.6))";
	private static final String PREVIEW_LAYOUT_DEF = "(ROW (LEAF name=left weight=0.3) (LEAF name=middle weight=0.4) (LEAF weight=0.3 name=right))";

	private final Project _project;
	private BugTreePanel _bugTreePanel;
	private transient BugDetailsComponents _bugDetailsComponents;
	private ComponentListener _componentListener;
	private MultiSplitPane _multiSplitPane;

	private boolean _previewEnabled;
	private boolean _isPreviewLayoutEnabled;
	private transient PreviewPanel _previewPanel;
	private FindBugsResult result;

	public ToolWindowPanel(@NotNull final Project project) {
		_project = project;
		installListeners();
		initGui();
		MessageBusManager.subscribeAnalysisState(project, this, this);
		MessageBusManager.subscribe(project, this, ClearListener.TOPIC, () -> {
			ToolWindowPanel.this.clear();
			DaemonCodeAnalyzer.getInstance(_project).restart();
		});
		MessageBusManager.subscribe(project, this, NewBugListener.TOPIC, (bug, analyzedClassCount) -> {
			_bugTreePanel.addNode(bug);
			_bugTreePanel.updateRootNode(analyzedClassCount);
		});
	}

	private void initGui() {
		setLayout(new NDockLayout());
		setBorder(JBUI.Borders.empty(1));

		final ActionGroup actionGroupLeft = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_LEFT);
		final ActionToolbar toolbarLeft1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupLeft, false);

		final ActionGroup actionGroupRight = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_RIGHT);
		final ActionToolbar toolbarRight1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupRight, false);

		final ActionGroup actionGroupNavigation = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_NAVIGATION);
		final ActionToolbar toolbarNavigation1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupNavigation, false);

		final ActionGroup actionGroupUtils = (ActionGroup) ActionManager.getInstance().getAction(FindBugsPluginConstants.ACTION_GROUP_UTILS);
		final ActionToolbar toolbarUtils1 = ActionManager.getInstance().createActionToolbar(FindBugsPluginConstants.TOOL_WINDOW_ID, actionGroupUtils, false);


		final Component toolbarLeft = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Analysis...", SwingConstants.VERTICAL, toolbarLeft1, true);
		final Component toolbarRight = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Grouping...", SwingConstants.VERTICAL, toolbarRight1, true);
		final Component toolbarNavigation = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Navigation...", SwingConstants.VERTICAL, toolbarNavigation1, true);
		final Component toolbarUtils = new ActionToolbarContainer(FindBugsPluginConstants.TOOL_WINDOW_ID + ": Utils...", SwingConstants.VERTICAL, toolbarUtils1, true);


		add(toolbarLeft, NDockLayout.WEST);
		add(toolbarRight, NDockLayout.WEST);
		add(toolbarNavigation, NDockLayout.WEST);
		add(toolbarUtils, NDockLayout.WEST);

		updateLayout(false);

		add(getMultiSplitPane(), NDockLayout.CENTER);
	}

	private MultiSplitPane getMultiSplitPane() {
		if (_multiSplitPane == null) {
			_multiSplitPane = new MultiSplitPane();
			_multiSplitPane.setContinuousLayout(true);
		}
		return _multiSplitPane;
	}

	private void _updateMultiSplitLayout(final String layoutDef) {
		final MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel(layoutDef);
		final MultiSplitLayout multiSplitLayout = getMultiSplitPane().getMultiSplitLayout();
		multiSplitLayout.setDividerSize(3);
		multiSplitLayout.setModel(modelRoot);
		multiSplitLayout.setFloatingDividers(true);
	}

	private void updateLayout(final boolean enablePreviewLayout) {
		EventDispatchThreadHelper.checkEDT();
		if (!_isPreviewLayoutEnabled && enablePreviewLayout) {
			_updateMultiSplitLayout(PREVIEW_LAYOUT_DEF);
			getMultiSplitPane().add(getBugTreePanel(), "left");
			_multiSplitPane.add(getPreviewPanel().getComponent(), "middle");
			getMultiSplitPane().add(getBugDetailsComponents().getBugDetailsSplitPane(), "right");
			_isPreviewLayoutEnabled = true;

		} else if (!enablePreviewLayout) {
			getPreviewPanel().release();

			_updateMultiSplitLayout(DEFAULT_LAYOUT_DEF);
			getMultiSplitPane().add(getBugTreePanel(), "left");
			getMultiSplitPane().add(getBugDetailsComponents().getBugDetailsSplitPane(), "right");

			if (getPreviewPanel().getEditor() != null) {
				resizeSplitNodes(ToolWindowPanel.this);
			}
			_previewPanel = null;
			_isPreviewLayoutEnabled = false;
		}
	}

	private PreviewPanel getPreviewPanel() {
		if (_previewPanel == null) {
			_previewPanel = new PreviewPanel();
		}
		return _previewPanel;
	}

	void setPreviewEditor(@Nullable final Editor editor, final PsiFile psiFile) {
		updateLayout(true);
		if (editor != null) {
			getPreviewPanel().add(editor, psiFile);
		} else {
			getPreviewPanel().clear();
		}
		resizeSplitNodes(this);
	}

	public void setPreviewEnabled(final boolean enabled) {
		updateLayout(false);
		_previewEnabled = enabled;
		getBugTreePanel().setPreviewEnabled(enabled);
	}

	public boolean isPreviewEnabled() {
		return _previewEnabled;
	}

	public Project getProject() {
		return _project;
	}

	public FindBugsResult getResult() {
		return result;
	}

	public Map<PsiFile, List<ExtendedProblemDescriptor>> getProblems() {
		return _bugTreePanel.getProblems();
	}

	private void installListeners() {
		if (_componentListener == null) {
			_componentListener = createComponentListener();
		}
		addComponentListener(_componentListener);
	}

	@Override
	public void analysisStarted() {
		EditorFactory.getInstance().refreshAllEditors();
		DaemonCodeAnalyzer.getInstance(_project).restart();
		updateLayout(false);
		clear();
	}

	@Override
	public void analysisAborting() {
	}

	@Override
	public void analysisAborted() {
		_bugTreePanel.setResult(null);
	}

	@Override
	public void analysisFinished(@NotNull final FindBugsResult result, @Nullable final Throwable error) {
		_bugTreePanel.setResult(result);
		final Integer analyzedClassCount = result.getAnalyzedClassCount();
		_bugTreePanel.updateRootNode(analyzedClassCount);
		_bugTreePanel.getBugTree().validate();
		final int numAnalysedClasses = analyzedClassCount != null ? analyzedClassCount : 0;

		final StringBuilder message = new StringBuilder()
				.append("Found ")
				.append(_bugTreePanel.getGroupModel().getBugCount())
				.append(" bugs in ")
				.append(numAnalysedClasses)
				.append(numAnalysedClasses > 1 ? " classes" : " class");

		this.result = result;

		final NotificationType notificationType;
		if (numAnalysedClasses == 0) {
			notificationType = NotificationType.WARNING;
			message.append("&nbsp; (no class files found <a href='").append(A_HREF_MORE_ANCHOR).append("'>more...</a>)<br/>");
		} else {
			notificationType = NotificationType.INFORMATION;
			message.append("&nbsp;<a href='").append(A_HREF_MORE_ANCHOR).append("'>more...</a><br/>");
		}
		message.append("<font size='10px'>using ").append(VersionManager.getFullVersion()).append(" with SpotBugs version ").append(FindBugsUtil.getFindBugsFullVersion()).append("</font><br/>");

		if (error != null) {
			final boolean findBugsError = FindBugsUtil.isFindBugsError(error);
			final String impl;
			if (findBugsError) {
				impl = "SpotBugs";
			} else {
				impl = FindBugsPluginConstants.PLUGIN_NAME;
			}
			String errorText = "An " + impl + " error occurred.";

			IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(_project);
			final StatusBar statusBar = ideFrame == null ? null : ideFrame.getStatusBar();
			final StatusBarWidget widget = statusBar == null ? null : statusBar.getWidget(IdeMessagePanel.FATAL_ERROR);
			IdeMessagePanel ideMessagePanel = null; // openFatals like ErrorNotifier
			if (widget instanceof IdeMessagePanel) {
				ideMessagePanel = (IdeMessagePanel) widget;
				errorText = "<a href='" + A_HREF_ERROR_ANCHOR + "'>" + errorText + "</a>";
			}

			message.append(String.format("%s The results of the analysis might be incomplete.", errorText));
			LOGGER.error(error);
			// use balloon because error should never disabled
			BalloonTipFactory.showToolWindowErrorNotifier(_project, message.toString(), new BalloonErrorListenerImpl(ToolWindowPanel.this, result, ideMessagePanel));
		} else {
			NotificationGroupManager.getInstance()
					.getNotificationGroup(NOTIFICATION_GROUP_ID_ANALYSIS_FINISHED)
					.createNotification(
							VersionManager.getName() + ": Analysis Finished", message.toString(), notificationType)
					.setImportant(false)
					.setListener(new NotificationListenerImpl(ToolWindowPanel.this, result))
					.addAction(ActionManager.getInstance().getAction("SpotBugs.DisableNotificationAction"))
					.notify(_project);
		}

		EditorFactory.getInstance().refreshAllEditors();
		DaemonCodeAnalyzer.getInstance(_project).restart();
	}

	private ComponentListener createComponentListener() {
		return new ToolWindowComponentAdapter(this);
	}

	private void resizeSplitNodes(final Component component) {
		final int width = component.getWidth();
		final int height = component.getHeight();
		if (isPreviewEnabled() && getPreviewPanel().getEditor() != null) {
			_bugDetailsComponents.adaptSize((int) (width * 0.3), height);
			_bugTreePanel.adaptSize((int) (width * 0.3), height);
		} else {
			_bugDetailsComponents.adaptSize((int) (width * 0.6), height);
			_bugTreePanel.adaptSize((int) (width * 0.4), height);
		}
		getPreviewPanel().adaptSize((int) (width * 0.4), height);
		_multiSplitPane.validate();
	}

	@NotNull
	public BugTreePanel getBugTreePanel() {
		if (_bugTreePanel == null) {
			_bugTreePanel = new BugTreePanel(this, _project);
		}
		return _bugTreePanel;
	}

	public BugDetailsComponents getBugDetailsComponents() {
		if (_bugDetailsComponents == null) {
			_bugDetailsComponents = new BugDetailsComponents(this);
		}
		return _bugDetailsComponents;
	}

	private void clear() {
		result = null;
		_bugTreePanel.clear();
		_bugTreePanel.updateRootNode(null);
	}

	@Override
	public void dispose() {
		if (_previewPanel != null) {
			Disposer.dispose(_previewPanel);
			_previewPanel = null;
		}
	}

	@Nullable
	public static ToolWindow getWindow(@NotNull final Project project) {
		return ToolWindowManager.getInstance(project).getToolWindow(FindBugsPluginConstants.TOOL_WINDOW_ID);
	}

	public static void showWindow(@NotNull final Project project) {
		final ToolWindow toolWindow = getWindow(project);
		if (toolWindow == null) {
			throw new IllegalStateException("No FindBugs ToolWindow");
		}
		showWindow(toolWindow);
	}

	public static void showWindow(@NotNull final ToolWindow toolWindow) {
		if (!toolWindow.isActive() && toolWindow.isAvailable()) {
			toolWindow.show(null);
		}
	}

	@Nullable
	public static ToolWindowPanel getInstance(@NotNull final Project project) {
		return getInstance(getWindow(project));
	}

	@Nullable
	public static ToolWindowPanel getInstance(@Nullable final ToolWindow toolWindow) {
		if (toolWindow == null) {
			return null;
		}
		final ContentManager contentManager = toolWindow.getContentManager();
		final Content[] contents = contentManager.getContents();
		if (contents.length == 0) {
			return null;
		}
		final JComponent component = contents[0].getComponent();
		if (component instanceof ToolWindowPanel) { // could be a JLabel
			return (ToolWindowPanel) component;
		}
		return null;
	}

	private static class ToolWindowComponentAdapter extends ComponentAdapter {

		private final ToolWindowPanel _toolWindowPanel;

		private ToolWindowComponentAdapter(final ToolWindowPanel toolWindowPanel) {
			_toolWindowPanel = toolWindowPanel;
		}

		@Override
		public void componentShown(final ComponentEvent e) {
			super.componentShown(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}

		@Override
		public void componentResized(final ComponentEvent e) {
			super.componentResized(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}

		@Override
		public void componentMoved(final ComponentEvent e) {
			super.componentMoved(e);
			final Component component = e.getComponent();
			_toolWindowPanel.resizeSplitNodes(component);
		}
	}

	private static abstract class AbstractListenerImpl {

		protected final FindBugsResult result;
		final ToolWindowPanel _toolWindowPanel;

		private AbstractListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsResult result) {
			this.result = result;
			_toolWindowPanel = toolWindowPanel;
		}

		final void openAnalysisRunDetailsDialog() {
			final int bugCount = _toolWindowPanel.getBugTreePanel().getGroupModel().getBugCount();
			final DialogBuilder dialog = AnalysisRunDetailsDialog.create(_toolWindowPanel.getProject(), bugCount, result.getAnalyzedClassCountSafe(), result);
			dialog.showModal(false);
		}
	}

	private static class NotificationListenerImpl extends AbstractListenerImpl implements NotificationListener {

		private NotificationListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsResult result) {
			super(toolWindowPanel, result);
		}

		@Override
		public void hyperlinkUpdate(@NotNull final Notification notification, @NotNull final HyperlinkEvent e) {
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
				final String desc = e.getDescription();
				if (desc.equals(A_HREF_MORE_ANCHOR)) {
					openAnalysisRunDetailsDialog();
				}
			}
		}

		@Override
		public String toString() {
			return "NotifierHyperlinkListener" +
					"{result=" + result +
					", _toolWindowPanel=" + _toolWindowPanel +
					'}';
		}
	}

	private static class BalloonErrorListenerImpl extends AbstractListenerImpl implements HyperlinkListener {

		private final IdeMessagePanel _ideMessagePanel;

		private BalloonErrorListenerImpl(@NotNull final ToolWindowPanel toolWindowPanel, final FindBugsResult result, @Nullable final IdeMessagePanel ideMessagePanel) {
			super(toolWindowPanel, result);
			_ideMessagePanel = ideMessagePanel;
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
				final String desc = e.getDescription();
				if (desc.equals(A_HREF_MORE_ANCHOR)) {
					openAnalysisRunDetailsDialog();
				} else if (desc.equals(A_HREF_ERROR_ANCHOR) && _ideMessagePanel != null) {
					_ideMessagePanel.openErrorsDialog(null);
				}
			}
		}
	}
}
