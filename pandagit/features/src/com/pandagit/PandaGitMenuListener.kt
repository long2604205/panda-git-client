// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.pandagit

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * Removes the Code menu from the main menu bar.
 *
 * A Git client shows a diff and a commit list. It never generates a constructor, never inspects code, and never
 * reformats a statement, so a whole menu of such commands only pushes the useful items away.
 *
 * The menu stays registered. Only its place in `MainMenu` is dropped, so a shortcut or a search entry that names one of
 * its actions still works.
 *
 * The listener runs when the frame exists, which is the first moment the main menu bar can be changed.
 */
internal class PandaGitMenuListener : AppLifecycleListener {
  override fun appFrameCreated(commandLineArgs: List<String>) {
    val actionManager = ActionManager.getInstance()
    val mainMenu = actionManager.getAction(MAIN_MENU_ID) as? DefaultActionGroup ?: return
    val codeMenu = actionManager.getActionOrStub(CODE_MENU_ID) ?: return
    mainMenu.remove(codeMenu, actionManager)
  }

  private companion object {
    const val MAIN_MENU_ID = "MainMenu"
    const val CODE_MENU_ID = "CodeMenu"
  }
}
