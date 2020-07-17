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

import com.intellij.compiler.server.BuildManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.util.Alarm;
import com.intellij.util.Consumer;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.messages.MessageBusConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.EventDispatchThreadHelper;
import org.jetbrains.plugins.spotbugs.common.util.IdeaUtilImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FindBugsCompileAfterHook {

	private static final int DEFAULT_DELAY_MS = 30000;
	private static final int DELAY_MS = StringUtil.parseInt(System.getProperty("idea.findbugs.autoanalyze.delaymillis", String.valueOf(DEFAULT_DELAY_MS)), DEFAULT_DELAY_MS);
	private static final ConcurrentMap<UUID, Set<VirtualFile>> CHANGED_BY_SESSION_ID = new ConcurrentHashMap<>();
	private static final WeakHashMap<Project, DelayedExecutor> DELAYED_EXECUTOR_BY_PROJECT = new WeakHashMap<>();
	private static ChangeCollector CHANGE_COLLECTOR; // EDT thread confinement
	private static MessageBusConnection connection;

	static {
		/*
		 * Note that ProjectData is cleared before BuildManagerListener#buildStarted is invoked,
		 * so we can not use BuildManager.getInstance().getFilesChangedSinceLastCompilation(project).
		 * There is no way to get the affect/compiled files (check with source of IC-140.2285.5).
		 * As a workaround, {@link ChangeCollector} will collect the changes.
		 */
		ApplicationManager.getApplication().getMessageBus().connect().subscribe(BuildManagerListener.TOPIC, new BuildManagerListener() {

			@Override
			public void beforeBuildProcessStarted(final @NotNull Project project, final @NotNull UUID sessionId) {
			}

			@Override
			public void buildStarted(final @NotNull Project project, final @NotNull UUID sessionId, final boolean isAutomake) {
				if (isAutomake && isAfterAutoMakeEnabled(project)) {
					final Set<VirtualFile> changed = Changes.INSTANCE.getAndRemoveChanged(project);
					if (changed != null) {
						CHANGED_BY_SESSION_ID.put(sessionId, changed);
					}
				}
			}

			@Override
			public void buildFinished(final @NotNull Project project, final @NotNull UUID sessionId, final boolean isAutomake) {
				if (isAutomake) {
					final Set<VirtualFile> changed = CHANGED_BY_SESSION_ID.remove(sessionId);
					if (changed != null) {
						if (DELAY_MS <= 0) {
							initWorkerForAutoMake(project, changed);
						} else {
							synchronized (DELAYED_EXECUTOR_BY_PROJECT) {
								DelayedExecutor task = DELAYED_EXECUTOR_BY_PROJECT.get(project);
								if (task == null) {
									task = new DelayedExecutor(project);
									DELAYED_EXECUTOR_BY_PROJECT.put(project, task);
								}
								task.schedule(changed);
							}
						}
					}
				} // else do nothing
			}
		});
	}

	@SuppressFBWarnings(value = {"LI_LAZY_INIT_UPDATE_STATIC", "LI_LAZY_INIT_STATIC"}, justification = "EDT thread confinement")
	static void setAnalyzeAfterAutomake(@NotNull final Project project, final boolean enabled) {
		if (enabled) {
			Changes.INSTANCE.addListener(project);
			if (CHANGE_COLLECTOR == null) {
				CHANGE_COLLECTOR = new ChangeCollector();
				connection = project.getMessageBus().connect();
				connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(CHANGE_COLLECTOR));
			}
		} else {
			final boolean empty = Changes.INSTANCE.removeListener(project);
			if (empty) {
				if (CHANGE_COLLECTOR != null) {
					connection.disconnect();
					CHANGE_COLLECTOR = null;
				}
			}
		}
	}

	static void initWorker(final CompileContext compileContext) {
		final com.intellij.openapi.project.Project project = compileContext.getProject();
		if (null == project) { // project reload, eg: open IDEA project with unknown JRE and fix it
			return;
		}

		final WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance(project);
		if (!workspaceSettings.analyzeAfterCompile) {
			return;
		}

		final CompileScope compileScope = compileContext.getCompileScope();
		final List<VirtualFile> affectedFiles = getAffectedFiles(project, compileScope);

		new FindBugsStarter(
				project,
				"Running SpotBugs analysis for affected files...",
				ProgressStartType.RunInBackground
		) {
			@Override
			protected boolean isCompileBeforeAnalyze() {
				return false;
			}

			@Override
			protected void createCompileScope(@NotNull final CompilerManager compilerManager, @NotNull final Consumer<CompileScope> consumer) {
				throw new UnsupportedOperationException();
			}

			@Override
			protected boolean configure(@NotNull final ProgressIndicator indicator, @NotNull final FindBugsProjects projects, final boolean justCompiled) {
				return projects.addFiles(affectedFiles, false, hasTests(affectedFiles));
			}
		}.start();
	}

	@NotNull
	private static List<VirtualFile> getAffectedFiles(
			@NotNull final Project project,
			@NotNull final CompileScope compileScope
	) {
    final List<VirtualFile> ret = new ArrayList<>();
		for (final FileType fileType : IdeaUtilImpl.SUPPORTED_FILE_TYPES) {
			final VirtualFile[] files = compileScope.getFiles(fileType, true);
			for (final VirtualFile file : files) {
				// It seems that "isSourceOnly" does not work (as expected), see issue #150
				final Module module = ModuleUtilCore.findModuleForFile(file, project);
				if (module != null) {
					ret.add(file);
				}
			}
		}
		return ret;
	}

	static boolean isAfterAutoMakeEnabled(@NotNull final Project project) {
		final WorkspaceSettings settings = WorkspaceSettings.getInstance(project);
		return settings.analyzeAfterAutoMake;
	}

	private static void initWorkerForAutoMake(@NotNull final Project project, @NotNull final Collection<VirtualFile> changed) {
		ApplicationManager.getApplication().runReadAction(() -> initWorkerForAutoMakeImpl(project, changed));
	}

	private static void initWorkerForAutoMakeImpl(@NotNull final Project project, @NotNull final Collection<VirtualFile> changed) {
		EventDispatchThreadHelper.invokeLater(() -> new FindBugsStarter(
				project,
				"Running SpotBugs analysis for affected files...",
				ProgressStartType.RunInBackground
		) {
			@Override
			protected boolean isCompileBeforeAnalyze() {
				return false;
			}

			@Override
			protected void createCompileScope(@NotNull final CompilerManager compilerManager, @NotNull final Consumer<CompileScope> consumer) {
				throw new UnsupportedOperationException();
			}

			@Override
			protected boolean configure(@NotNull final ProgressIndicator indicator, @NotNull final FindBugsProjects projects, final boolean justCompiled) {
				projects.addFiles(changed, false, hasTests(changed));
				return true;
			}
		}.start());
	}

	private static class DelayedExecutor {
		private final Project _project;
		private final Alarm _alarm;
		private Set<VirtualFile> _changed;

		DelayedExecutor(@NotNull final Project project) {
			_project = project;
			_alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
		}

		void schedule(@NotNull final Set<VirtualFile> changed) {
			_alarm.cancelAllRequests();
			synchronized (this) {
				if (_changed == null) {
					_changed = changed;
				} else {
					_changed.addAll(changed);
				}
			}
			addRequest();
		}

		private void addRequest() {
			_alarm.addRequest(() -> {
				if (HeavyProcessLatch.INSTANCE.isRunning()) {
					addRequest();
				} else {
					final Set<VirtualFile> changed;
					synchronized (DelayedExecutor.this) {
						changed = _changed;
						_changed = null;
					}
					initWorkerForAutoMake(_project, changed);
				}
			}, DELAY_MS);
		}
	}
}
