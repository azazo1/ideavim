/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.extension.easymotion.cjk

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WordTargetsTest {
  private val defaultRegex = Regex(WordMotionSettings.DEFAULT_WORD_REGEX)

  private fun offsets(text: String, anchor: WordAnchor, regex: Regex = defaultRegex) =
    WordTargets.find(text, 0, text.length, regex, anchor).map { it.offset }

  @Test
  fun `default regex groups consecutive CJK, punctuation, words and symbols`() {
    // abc | 中文 | ， | 测试 | def | ();
    val text = "abc 中文，测试 def();"
    assertEquals(listOf(0, 4, 6, 7, 10, 13), offsets(text, WordAnchor.START))
    assertEquals(listOf(2, 5, 6, 8, 12, 15), offsets(text, WordAnchor.END))
  }

  @Test
  fun `matches never span lines and every intersecting line is scanned whole`() {
    val text = "ab cd\nef gh"
    // 范围只落在第一行中间, 仍然扫描整行; 范围伸到第二行行首 (偏移 6) 时, 第二行也被扫描
    assertEquals(listOf(0, 3), WordTargets.find(text, 1, 2, defaultRegex, WordAnchor.START).map { it.offset })
    assertEquals(listOf(0, 3, 6, 9), WordTargets.find(text, 4, 6, defaultRegex, WordAnchor.START).map { it.offset })
  }

  @Test
  fun `empty matches are ignored`() {
    assertEquals(listOf(1), offsets("a1b", WordAnchor.START, Regex("""\d*""")))
  }

  @Test
  fun `min spacing drops close targets on the same line only`() {
    val sameLine = listOf(0, 1, 3, 4).map { WordTarget(it, 0) }
    assertEquals(listOf(0, 3), WordTargets.applyMinSpacing(sameLine, 2).map { it.offset })

    // 两个目标列号相同, 但不在同一行, 都保留
    val twoLines = listOf(WordTarget(0, 0), WordTarget(3, 3))
    assertEquals(listOf(0, 3), WordTargets.applyMinSpacing(twoLines, 2).map { it.offset })

    assertEquals(sameLine, WordTargets.applyMinSpacing(sameLine, 1))
  }
}
