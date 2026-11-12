// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.intellij.build.BuildOptions
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.GitUiProperties
import org.jetbrains.intellij.build.JvmArchitecture
import org.jetbrains.intellij.build.OsFamily
import org.jetbrains.intellij.build.impl.buildDistributions
import org.jetbrains.intellij.build.impl.createBuildContext
import org.jetbrains.intellij.build.impl.createPlatformLayout

@ApiStatus.Internal
object GitUiInstallersBuildTarget {
  private val GIT_PLUGIN_CONTENT_MODULES = listOf(
    "intellij.vcs.git",
    "intellij.vcs.git.backend",
    "intellij.vcs.git.localHistory",
    "intellij.vcs.git.terminal",
    "intellij.vcs.git.coverage",
    "intellij.vcs.git.shared",
    "intellij.vcs.git.frontend",
    "intellij.vcs.git.frontend.split",
    "intellij.vcs.git.backend.split",
    "intellij.vcs.git.model.generated",
    "intellij.vcs.git.performanceTesting",
    "intellij.terminal",
    "intellij.terminal.completion",
  )

  @JvmStatic
  fun main(args: Array<String>) {
    runBlocking(Dispatchers.Default) {
      val options = BuildOptions().apply {
        incrementalCompilation = true
        useCompiledClassesFromProjectOutput = false
        targetOs = persistentListOf(OsFamily.currentOs)
        targetArch = JvmArchitecture.currentJvmArch
        buildStepsToSkip += BuildOptions.MAC_SIGN_STEP
        buildStepsToSkip += BuildOptions.WIN_SIGN_STEP
      }
      val context = createBuildContext(
        projectHome = COMMUNITY_ROOT.communityRoot,
        productProperties = GitUiProperties(COMMUNITY_ROOT.communityRoot),
        setupTracer = true,
        options = options,
      )
      val platformLayout = createPlatformLayout(context)
      options.modulesToCompile = (
        platformLayout.includedModules.map { it.moduleName } +
        context.getBundledPluginModules() +
        GIT_PLUGIN_CONTENT_MODULES
      ).distinct()
      buildDistributions(context)
    }
  }
}
