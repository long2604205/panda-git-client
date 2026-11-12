// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.kotlin.KotlinPluginBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitUiPropertiesTest {
  @Test
  fun `bundles Git Terminal and TextMate without full language and agent plugins`() {
    val properties = GitUiProperties(COMMUNITY_ROOT.communityRoot)
    val bundledPlugins = properties.productLayout.bundledPluginModules

    assertTrue("intellij.vcs.git" in bundledPlugins)
    assertTrue("intellij.terminal" in bundledPlugins)
    assertTrue("intellij.textmate.plugin" in bundledPlugins)
    assertTrue("intellij.platform.images" in bundledPlugins)
    assertTrue("intellij.platform.navbar.plugin" in bundledPlugins)
    assertTrue("intellij.platform.recentFiles.plugin" in bundledPlugins)
    for (plugin in listOf(
      "intellij.jcef.plugin",
      "intellij.platform.bookmarks.plugin",
      "intellij.grid.core.plugin",
      "intellij.platform.structuralSearch.plugin",
      "intellij.platform.structureView.plugin",
      "intellij.platform.execution.serviceView.plugin",
      "intellij.platform.todo.plugin",
    )) {
      assertFalse(plugin in bundledPlugins, plugin)
    }
    assertFalse(JavaPluginLayout.MAIN_MODULE_NAME in bundledPlugins)
    assertFalse(KotlinPluginBuilder.MAIN_KOTLIN_PLUGIN_MODULE in bundledPlugins)
    assertFalse("intellij.mcpserver" in bundledPlugins)
    assertFalse("intellij.platform.acp.plugin" in bundledPlugins)
    assertTrue("intellij.idea.community.customization" in properties.productLayout.productImplementationModules)
    assertFalse(properties.productLayout.pluginLayouts.any { it.mainModule == JavaPluginLayout.MAIN_MODULE_NAME })
    assertFalse(properties.productLayout.buildAllCompatiblePlugins)
    assertTrue(properties.mavenArtifacts.additionalModules.isEmpty())
    assertFalse(properties.buildSourcesArchive)
    assertFalse(properties.buildDocAuthoringAssets)
  }

  @Test
  fun `leaves IntelliJ Community defaults unchanged`() {
    val properties = IdeaCommunityProperties(COMMUNITY_ROOT.communityRoot)
    val bundledPlugins = properties.productLayout.bundledPluginModules

    assertTrue(JavaPluginLayout.MAIN_MODULE_NAME in bundledPlugins)
    assertTrue(KotlinPluginBuilder.MAIN_KOTLIN_PLUGIN_MODULE in bundledPlugins)
    assertTrue("intellij.vcs.git" in bundledPlugins)
    assertTrue("intellij.terminal" in bundledPlugins)
    assertTrue(properties.productLayout.buildAllCompatiblePlugins)
  }
}
