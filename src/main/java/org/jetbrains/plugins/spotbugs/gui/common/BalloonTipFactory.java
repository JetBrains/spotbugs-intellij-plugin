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
package org.jetbrains.plugins.spotbugs.gui.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.FindBugsPluginConstants;

import javax.annotation.Nullable;
import javax.swing.event.HyperlinkListener;

public final class BalloonTipFactory {

	public static void showToolWindowInfoNotifier(@NotNull final Project project, final String html) {
		showToolWindowInfoNotifier(project, html, null);
	}

	public static void showToolWindowInfoNotifier(@NotNull final Project project, final String html, @Nullable final HyperlinkListener hyperlinkListener) {
		final ToolWindowManager manager = ToolWindowManager.getInstance(project);
		manager.notifyByBalloon(FindBugsPluginConstants.TOOL_WINDOW_ID, MessageType.INFO, html, null, hyperlinkListener);
	}

	public static void showToolWindowWarnNotifier(@NotNull final Project project, final String html) {
		showToolWindowWarnNotifier(project, html, null);
	}

	public static void showToolWindowWarnNotifier(@NotNull final Project project, final String html, @Nullable final HyperlinkListener hyperlinkListener) {
		final ToolWindowManager manager = ToolWindowManager.getInstance(project);
		manager.notifyByBalloon(FindBugsPluginConstants.TOOL_WINDOW_ID, MessageType.WARNING, html, null, hyperlinkListener);
	}

	public static void showToolWindowErrorNotifier(@NotNull final Project project, final String html) {
		showToolWindowErrorNotifier(project, html, null);
	}

	public static void showToolWindowErrorNotifier(@NotNull final Project project, final String html, @Nullable final HyperlinkListener hyperlinkListener) {
		final ToolWindowManager manager = ToolWindowManager.getInstance(project);
		manager.notifyByBalloon(FindBugsPluginConstants.TOOL_WINDOW_ID, MessageType.ERROR, html, null, hyperlinkListener);
	}

	private BalloonTipFactory() {
	}
}
