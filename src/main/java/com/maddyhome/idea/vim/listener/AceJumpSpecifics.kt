/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Key handling for IdeaVim should be updated to editorHandler usage. In this case this class can be safely removed.
 */

interface AceJumpService {
  fun isActive(editor: Editor): Boolean

  companion object {
    /** 本插件内并入的 AceJump 副本 (内置 easymotion 使用) 通过这个扩展点报告会话状态 */
    private val BUNDLED_EP = ExtensionPointName.create<AceJumpService>("IdeaVIM.bundledAceJumpService")

    fun getInstance(): AceJumpService? = ApplicationManager.getApplication().getService(AceJumpService::class.java)

    /** 独立安装的 AceJump 插件, 或本插件内的 AceJump 副本, 是否有会话在 [editor] 中进行 */
    fun isAnyActive(editor: Editor): Boolean =
      getInstance()?.isActive(editor) == true || BUNDLED_EP.extensionList.any { it.isActive(editor) }
  }
}
