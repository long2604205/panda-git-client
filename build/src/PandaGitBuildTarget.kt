// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.intellij.build.BuildLifetime
import org.jetbrains.intellij.build.BuildOptions
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.OsFamily
import org.jetbrains.intellij.build.PandaGitProperties
import org.jetbrains.intellij.build.impl.buildDistributions
import org.jetbrains.intellij.build.impl.createBuildContext

internal object PandaGitBuildTarget {
  @JvmStatic
  fun main(args: Array<String>) {
    val options = BuildOptions().apply {
      incrementalCompilation = true
      useCompiledClassesFromProjectOutput = false
      buildStepsToSkip += BuildOptions.MAC_SIGN_STEP
      buildStepsToSkip += BuildOptions.WIN_SIGN_STEP
      if (OsFamily.currentOs == OsFamily.MACOS) {
        buildStepsToSkip += BuildOptions.WINDOWS_EXE_INSTALLER_STEP
      }
    }
    BuildLifetime().use { lifetime ->
      val communityRoot = COMMUNITY_ROOT.communityRoot
      val context = createBuildContext(
        projectHome = communityRoot,
        productProperties = PandaGitProperties(communityRoot),
        options = options,
        lifetime = lifetime,
      )
      context.compileModules(moduleNames = null, includingTestsInModules = emptyList())
      buildDistributions(context)
    }
  }
}
