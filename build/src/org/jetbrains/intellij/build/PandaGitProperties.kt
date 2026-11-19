// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import org.jetbrains.intellij.build.kotlin.KotlinPluginBuilder
import org.jetbrains.intellij.build.productLayout.CommunityModuleSets
import org.jetbrains.intellij.build.productLayout.CommunityProductFragments
import org.jetbrains.intellij.build.productLayout.ProductModulesContentSpec
import org.jetbrains.intellij.build.productLayout.productModules
import java.nio.file.Path

/**
 * The plugins a Git client does not need.
 *
 * The set names the language support and the build systems whose analyzers, indexers and project models a Git client never
 * runs. `PyCharm Community` drops the same Java support and stays a full IDE, so this list follows its precedent:
 * see `python/build/plugin-list.txt`.
 */
private val PANDA_GIT_DROPPED_PLUGINS: List<String> = listOf(
  // Java support and the analyzers it brings
  JavaPluginLayout.MAIN_MODULE_NAME,
  "intellij.java.ide.customization",
  "intellij.java.i18n",
  "intellij.java.byteCodeViewer",
  "intellij.java.coverage",
  "intellij.java.decompiler",
  // Kotlin support and the UI toolkit that belongs to it
  KotlinPluginBuilder.MAIN_KOTLIN_PLUGIN_MODULE,
  "intellij.compose.ide.plugin",
  // JavaFX support depends on the Java backend, which this product does not ship
  "intellij.javaFX.community",
  // Build systems and the importers they run on project open
  "intellij.maven.plugin",
  "intellij.gradle.plugin",
  "intellij.gradle.java.plugin",
  "intellij.android.gradle.declarative.lang.ide",
  "intellij.android.gradle.dsl",
  "intellij.eclipse",
  // Languages a Git client does not edit.
  // `intellij.json` and `intellij.yaml` stay: the GitHub and GitLab plugins parse their API payloads with them.
  // `intellij.textmate.plugin` stays: it is the syntax highlighter for files no bundled language parser claims.
  "intellij.groovy",
  "intellij.groovy.scripting",
  "intellij.groovy.live.templates",
  "intellij.properties",
  "intellij.toml",
  "intellij.html.tools",
  "intellij.configurationScript",
  "intellij.markdown",
  "intellij.mermaid",
  "intellij.webp",
  "intellij.grazie",
  // Test frameworks
  "intellij.junit",
  "intellij.testng",
  // Anything else a Git client has no use for
  "intellij.copyright",
  "intellij.featuresTrainer",
  "intellij.mcpserver.plugin",
  "intellij.performanceTesting",
  "intellij.keymap.eclipse",
  "intellij.keymap.visualStudio",
  "intellij.keymap.netbeans",
  // Editor navigation aids and analysis views. A Git client reads a diff; it does not browse a symbol tree, search
  // structurally, collect problems, or track a TODO.
  "intellij.platform.bookmarks.plugin",
  "intellij.platform.structuralSearch.plugin",
  "intellij.platform.structureView.plugin",
  "intellij.platform.problemView.plugin",
  "intellij.platform.todo.plugin",
  "intellij.platform.tasks.plugin",
  "intellij.platform.execution.serviceView.plugin",
  // The plugin development toolchain. It ships a settings page and the module that reads plugin descriptors, which a
  // Git client never opens.
  "intellij.devkit",
  "intellij.dev",
  // The table viewer for CSV and database rows. The commit list draws its own tree.
  "intellij.grid.plugin",
  // Compose UI. The plugin carries Compose, Jewel, and Skiko, and nothing in this product draws with them.
  "intellij.platform.compose.plugin",
  // The embedded Chromium browser. No shipped feature opens a web view, and the native bundle is the largest file in
  // the distribution.
  "intellij.jcef.plugin",
  // `intellij.jna.plugin` stays on purpose: `intellij.libraries.pty4j` links `intellij.libraries.jna`, and the Terminal
  // uses pty4j for every local shell. Removing JNA breaks the Terminal that this product keeps.
)

class PandaGitProperties(private val communityHome: Path) : IdeaCommunityProperties(communityHome) {
  init {
    productLayout.bundledPluginModules = PANDA_GIT_DROPPED_PLUGINS.fold(productLayout.bundledPluginModules) { modules, dropped ->
      modules.removing(dropped)
    }
    productLayout.buildAllCompatiblePlugins = false
    buildSourcesArchive = false
    // The IDEA descriptor names `com.intellij.java` and `com.intellij.java.ide` as essential, and the bootstrap
    // refuses to start without them. This module states the Git plugins instead.
    applicationInfoModule = "intellij.pandagit.resources"
    // `intellij.idea.community.customization` carries the IDEA `IdeaApplicationInfo.xml` and `IdeaPlugin.xml`, and
    // the bootstrap reads the first copy on the classpath. PyCharm replaces that module with its own, and this
    // product does the same. The branding images that module also holds are copied into `intellij.pandagit.resources`,
    // because the splash screen reads `/idea_community_logo.png` from the product module.
    productLayout.productImplementationModules = listOf(
      "intellij.platform.starter",
      "intellij.pandagit.resources",
    )
    // The icon files the distribution copies; this product keeps the community ones.
    brandingResourcePaths = listOf(communityHome.resolve("pandagit/resources/src"))
    // `CE_CLASS_VERSIONS` states the bytecode level of jars in plugins this product drops. The check fails on a rule
    // that matches no file, so keep only the rules for the platform jars in `lib/`.
    versionCheckerConfig = CE_CLASS_VERSIONS.filterKeys { path -> !path.startsWith("plugins/") }
    // This product ships an installer, not a Maven repository. The community build validates that every Jewel module
    // produces a Maven artifact, and this product drops those modules, so the check would fail on a module that is not
    // built. Nothing here publishes to Maven Central, so the validation is switched off.
    mavenArtifacts.validate = { _, _ -> }
    // A Git client holds a file tree, a diff and a commit list. The default 2 GB heap reserves address space this
    // product never fills: a measured session commits 200 MB and uses 130 MB. 768 MB keeps a five-fold margin over the
    // measurement, so a large diff or a long history still fits. 512 MB saves no further resident memory, because the
    // working set is metaspace and native buffers, not heap.
    customJvmMemoryOptions = mapOf(
      "-Xms" to "128m",
      "-Xmx" to "768m",
    )
  }

  /**
   * The IDEA descriptor also declares the `java-capable` alias and the Java IDE base fragment. Both pull the Java
   * support back in, so this product declares its own content instead.
   */
  override fun getProductContentDescriptor(): ProductModulesContentSpec = productModules {
    alias("com.intellij.modules.idea")
    alias("com.intellij.modules.idea.community")
    alias("com.intellij.platform.ide.provisioner")

    include(CommunityProductFragments.platformCoreFragment())

    module("intellij.platform.coverage")
    module("intellij.platform.coverage.agent")
    module("intellij.xml.xmlbeans")
    module("intellij.libraries.log4j.to.slf4j")
    module("intellij.libraries.xmlbeans")
    module("intellij.platform.ide.newUiOnboarding")
    module("intellij.platform.ide.newUsersOnboarding")
    module("intellij.ide.startup.importSettings")
    privateModule("intellij.libraries.sqlite")
    module("intellij.platform.customization.min")
    module("intellij.idea.customization.base")
    // `intellij.idea.community.customization` reaches this module through a runtime dependency. Naming it here keeps
    // the plugin model the only truth for packaging, so the implicit-inclusion check passes.
    module("intellij.java.ide.resources")

    moduleSet(CommunityModuleSets.ideCommon())
    moduleSet(CommunityModuleSets.composeRuntime())
    moduleSet(CommunityModuleSets.platformResourceDefaults())
    moduleSet(CommunityModuleSets.rdCommon())

    embeddedModule("intellij.idea.community.ide.customization")
    // The capability that keeps scanning and indexing away from a PandaGit project.
    module("intellij.pandagit.features")

    module("intellij.platform.ide.nonModalWelcomeScreen")
    module("intellij.platform.ide.nonModalWelcomeScreen.frontend")
    module("intellij.platform.ide.nonModalWelcomeScreen.backend")
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
