// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.pandagit

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * Removes the IDE menus that a Git client does not need from the main menu bar.
 *
 * PandaGit shows a diff, a commit list, and a file tree. It does not edit code, so the edit, navigation, refactoring,
 * run, and tools menus only push the Git entries away from the first screen.
 *
 * The groups stay registered. Only their place in `MainMenu` is dropped, so a shortcut or a search entry that names one
 * of their actions still works.
 *
 * The listener runs when the frame exists, which is the first moment the main menu bar can be changed.
 */
internal class PandaGitMenuListener : AppLifecycleListener {
  override fun appFrameCreated(commandLineArgs: List<String>) {
    val actionManager = ActionManager.getInstance()
    val mainMenu = actionManager.getAction(MAIN_MENU_ID) as? DefaultActionGroup ?: return
    for (groupId in REMOVED_MENU_IDS) {
      val group = actionManager.getActionOrStub(groupId) ?: continue
      mainMenu.remove(group, actionManager)
    }
  }

  private companion object {
    const val MAIN_MENU_ID = "MainMenu"

    /**
     * The menu groups that leave the main menu bar.
     *
     * `File`, `View`, `Git`, `Window`, and `Help` stay: a Git client still opens a file, toggles a panel, runs a Git
     * command, switches a window, and reads the version.
     */
    val REMOVED_MENU_IDS = listOf(
      "EditMenu",
      "GoToMenu",
      "RefactoringMenu",
      "RunMenu",
      "ToolsMenu",
      "CodeMenu",
    )
  }
}
