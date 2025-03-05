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

package org.jetbrains.plugins.spotbugs.core

import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import org.jetbrains.plugins.spotbugs.android.RFilerFilterSuggestion
import org.jetbrains.plugins.spotbugs.core.FindBugsCompileAfterHook.initWorker
import org.jetbrains.plugins.spotbugs.gui.preferences.LegacyProjectSettingsConverter

internal class SpotBugsPostStartupActivity: ProjectActivity {

    override suspend fun execute(project: Project) {
        val connection = project.messageBus.connect().apply {
            subscribe(CompilerTopics.COMPILATION_STATUS, object : CompilationStatusListener {
                override fun compilationFinished(
                    aborted: Boolean,
                    errors: Int,
                    warnings: Int,
                    compileContext: CompileContext,
                ) {
                    if (aborted || errors != 0) return
                    // note that this is not invoked when auto make trigger compilation
                    initWorker(compileContext)
                }
            })
        }

        if (FindBugsCompileAfterHook.isAfterAutoMakeEnabled(project)) {
            FindBugsCompileAfterHook.setAnalyzeAfterAutomake(project, true)
        }

        PluginSuggestion.suggestPlugins(project)
        RFilerFilterSuggestion(project).suggest()

        LegacyProjectSettingsConverter.convertSettings(project)

        Disposer.register(project.service<SpotBugsPluginDisposable>()) {
            connection.disconnect()
            FindBugsCompileAfterHook.setAnalyzeAfterAutomake(project, false)
        }
    }
}
