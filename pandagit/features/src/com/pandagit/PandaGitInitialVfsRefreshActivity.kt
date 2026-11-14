// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.pandagit

import com.intellij.openapi.project.InitialVfsRefreshService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ex.isIndexingActivitiesSuppressedSync

/**
 * Starts the initial VFS refresh that a suppressed scan never starts.
 *
 * `UnindexedFilesScanner.scan` calls `InitialVfsRefreshService.scheduleInitialVfsRefresh` as its last step. A PandaGit
 * project suppresses that scan, so the step never runs and the service never completes.
 *
 * Three callers wait for that service, and without it they wait forever:
 * - `GitUntrackedFilesHolder` cannot list a file that the repository does not track.
 * - `VcsIgnoreManagerImpl` cannot read the ignore rules.
 * - `VcsRootScanner` cannot report a broken root.
 *
 * This activity runs the same asynchronous refresh that the scan runs in the IDE path, so the wait ends and the three
 * callers continue. The service ignores a second call, so the work happens once.
 */
internal class PandaGitInitialVfsRefreshActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!isIndexingActivitiesSuppressedSync(project)) {
      return
    }
    project.getService(InitialVfsRefreshService::class.java).scheduleInitialVfsRefresh()
  }
}
