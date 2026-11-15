// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.pandagit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ex.isIndexingActivitiesSuppressedSync

/**
 * Points the guarded extensions at the TextMate file type, so that a bundle can highlight them.
 *
 * IntelliJ IDEA Community ships `defaultFileTypes.xml`. It declares a `JavaScript` file type for `.js` that holds only
 * a keyword list, and the bundler describes it as "JavaScript (syntax highlighting only)". `TextMateFileType` replaces
 * a file type only when that type is unknown, plain text, a TextMate type, or a `TextMateBackedFileType`. A guarded
 * extension therefore never reaches the richer TextMate grammar.
 *
 * Removing the association does not help: the platform registers the default association again when a project opens.
 * An explicit association does hold, because the default is applied only to an extension that nothing claims yet.
 *
 * A project activity runs after the event queue exists, so `invokeAndWait` reaches the EDT safely.
 */
internal class PandaGitTextMateFileTypeActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!isIndexingActivitiesSuppressedSync(project)) {
      return
    }
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        val manager = FileTypeManager.getInstance()
        val textMateType = manager.registeredFileTypes.firstOrNull { it.name == TEXT_MATE_FILE_TYPE_NAME } ?: return@runWriteAction
        for (extension in GUARDED_EXTENSIONS) {
          manager.associateExtension(textMateType, extension)
        }
      }
    }
  }

  private companion object {
    /**
     * The name that the TextMate plugin gives its file type in `intellij.textmate.xml`.
     */
    const val TEXT_MATE_FILE_TYPE_NAME = "textmate"

    /**
     * The extensions that `defaultFileTypes.xml` guards with a keyword-only file type, and that a TextMate bundle
     * covers with a full grammar.
     *
     * `.aj` is absent on purpose: no bundle covers AspectJ.
     */
    val GUARDED_EXTENSIONS = listOf("js", "jsx", "css", "sql", "php", "phtml", "vue")
  }
}
