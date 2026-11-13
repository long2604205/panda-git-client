// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import java.nio.file.Path

class PandaGitProperties(private val communityHome: Path) : IdeaCommunityProperties(communityHome) {
  init {
    productLayout.bundledPluginModules = productLayout.bundledPluginModules
      .removing("intellij.featuresTrainer")
      .removing("intellij.mcpserver.plugin")
    productLayout.buildAllCompatiblePlugins = false
    buildSourcesArchive = false
  }

  override fun getSystemSelector(appInfo: ApplicationInfoProperties, buildNumber: String): String {
    return "PandaGit${appInfo.majorVersion}.${appInfo.minorVersionMainPart}"
  }

  override fun getBaseArtifactName(appInfo: ApplicationInfoProperties, buildNumber: String): String = "panda-git-$buildNumber"

  override fun getOutputDirectoryName(appInfo: ApplicationInfoProperties): String = "panda-git"

  override fun createWindowsCustomizer(projectHome: Path): WindowsDistributionCustomizer =
    ideaCommunityWindowsCustomizer(communityHome) {
      fileAssociations = emptyList()
      associateIpr = false
      fullName { "PandaGit" }
      installDirNameHandler { "PandaGit" }
      uninstallFeedbackUrl { null }
    }

  override fun createLinuxCustomizer(projectHome: Path): LinuxDistributionCustomizer =
    ideaCommunityLinuxCustomizer(communityHome) {
      rootDirectoryName { _, _ -> "panda-git" }
    }

  override fun createMacCustomizer(projectHome: Path): MacDistributionCustomizer =
    ideaCommunityMacCustomizer(communityHome) {
      bundleIdentifier = "com.pandagit.app"
      fileAssociations = emptyList()
      urlSchemes = emptyList()
      associateIpr = false
      rootDirectoryName { _, _ -> "PandaGit.app" }
    }
}
