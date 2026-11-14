// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.pandagit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ProjectFrameCapabilitiesProvider
import com.intellij.openapi.wm.ex.ProjectFrameCapability
import com.intellij.openapi.wm.ex.ProjectFrameUiPolicy

/**
 * Keeps scanning and indexing away from every project PandaGit opens.
 *
 * A Git client reads the repository, the working tree and the commit graph. It never resolves a source symbol, so no
 * code that runs on a PandaGit project reads an index. The capability makes the platform skip the scan that would
 * build one: `UnindexedFilesScannerExecutorImpl` returns before it walks a file, and `FileBasedIndex` answers an empty
 * result instead of waiting for one.
 *
 * The repository itself stays reachable. `ProjectRootEntity` still registers the project directory as
 * `CONTENT_NON_INDEXABLE`, so the Project View lists the tree, the editor opens a file, and a version control mapping
 * still points at a real directory.
 */
internal class PandaGitProjectFrameCapabilitiesProvider : ProjectFrameCapabilitiesProvider {
  override fun getCapabilities(project: Project): Set<ProjectFrameCapability> {
    return setOf(ProjectFrameCapability.SUPPRESS_INDEXING_ACTIVITIES)
  }

  override fun getUiPolicy(project: Project, capabilities: Set<ProjectFrameCapability>): ProjectFrameUiPolicy? = null
}
