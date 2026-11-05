/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.extension.easymotion.cjk

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.session.Session
import org.jetbrains.plugins.extension.easymotion.HandlerProcessor
import org.jetbrains.plugins.extension.easymotion.MotionType
import kotlin.math.abs

/**
 * 按 [WordMotionSettings] 的词正则计算目标, 取代上游只认 ASCII 的词规则.
 *
 * @param anchor 目标取匹配的开头还是结尾.
 * @param boundary 动作本身的范围, 例如光标之后或当前行.
 * @param useMinSpacing 是否应用最小列距, VSCodeVim fork 只对 `w` / `b` 应用.
 */
class WordPattern(
  private val anchor: WordAnchor,
  private val boundary: Boundaries,
  motionType: MotionType,
  private val useMinSpacing: Boolean = false,
) : HandlerProcessor(motionType) {
  override fun customization(editor: Editor, session: Session) {
    val caret = editor.caretModel.offset
    val cache = EditorOffsetCache.new()
    // 与 AceJump 会话默认的搜索范围取交集, 和上游 startRegexSearch 的行为一致
    val searchArea = boundary.intersection(
      if (AceConfig.searchWholeFile) StandardBoundaries.WHOLE_FILE else StandardBoundaries.VISIBLE_ON_SCREEN,
    )
    val range = searchArea.getOffsetRange(editor, cache)

    var targets = WordTargets.find(editor.document.immutableCharSequence, range.first, range.last, WordMotionSettings.wordRegex(), anchor)
      .filter { it.offset != caret && searchArea.isOffsetInside(editor, it.offset, cache) }
      .sortedBy { abs(it.offset - caret) }
    if (useMinSpacing) {
      targets = WordTargets.applyMinSpacing(targets, WordMotionSettings.minSpacing())
    }

    LOG.debug { "easymotion word targets: anchor=$anchor, range=$range, count=${targets.size}" }
    session.markResults(targets.mapTo(sortedSetOf()) { it.offset })
  }

  private companion object {
    private val LOG = vimLogger<WordPattern>()
  }
}
