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

package org.jetbrains.plugins.spotbugs.gui.toolwindow.view

import com.intellij.CommonBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationDisplayType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.spotbugs.gui.common.NotificationUtil
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader

internal class DisableAnalysisFinishedNotificationAction : NotificationAction(ResourcesLoader.getString("disable.notification")) {

    override fun actionPerformed(
        e: AnActionEvent,
        notification: Notification,
    ) {
        val notificationGroupName = ResourcesLoader.getString("notification.group.analysis.finished")
        val title = "SpotBugs Analysis Finished Notification"
        val message = """
            |Notification will be disabled for all projects.
            |Settings | Appearance & Behavior | Notifications | $notificationGroupName
            |can be used to configure the notification.
        """.trimMargin()

        MessageDialogBuilder.yesNo(title, message)
            .yesText(ResourcesLoader.getString("disable.notification"))
            .noText(CommonBundle.getCancelButtonText())
            .icon(Messages.getWarningIcon())
            .ask(e.project)
            .let { result ->
                when (result) {
                    true -> {
                        NotificationUtil.getNotificationsConfigurationImpl().changeSettings(
                            /* groupId = */ ToolWindowPanel.NOTIFICATION_GROUP_ID_ANALYSIS_FINISHED,
                            /* displayType = */ NotificationDisplayType.NONE,
                            /* shouldLog = */ false,
                            /* shouldReadAloud = */ false,
                        )
                        notification.expire()
                    }

                    false -> notification.hideBalloon()
                }
            }
    }
}
