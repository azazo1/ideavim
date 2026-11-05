/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.extension.easymotion.integration

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.listener.AceJumpService
import org.acejump.session.SessionManager

/**
 * 向 IdeaVim 报告内置 AceJump 会话是否进行中. 作用与 ideavim-acejump 模块中针对独立 AceJump 插件的
 * AceJumpServiceImpl 相同, 但这里看的是本插件内并入的那份 AceJump.
 */
class BundledAceJumpSessionService : AceJumpService {
  override fun isActive(editor: Editor): Boolean {
    return try {
      SessionManager[editor] != null
    } catch (e: Throwable) {
      false
    }
  }
}
