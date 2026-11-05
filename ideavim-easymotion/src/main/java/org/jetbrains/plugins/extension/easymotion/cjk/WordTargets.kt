/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.extension.easymotion.cjk

import kotlin.math.abs

/** 跳转目标落在一次正则匹配的哪一端. */
enum class WordAnchor {
  /** 匹配的第一个字符, 用于 w / b 一类动作. */
  START,

  /** 匹配的最后一个字符, 用于 e / ge 一类动作. */
  END,
}

/**
 * 一个跳转目标.
 *
 * @property offset 目标在文档中的偏移.
 * @property lineStart 目标所在行的起始偏移, 用来判断两个目标是否同一行.
 */
data class WordTarget(val offset: Int, val lineStart: Int) {
  /** 目标在行内的列号, 以字符计. */
  val column: Int get() = offset - lineStart
}

/**
 * 词移动的跳转目标计算, 规则与 VSCodeVim fork 中的 easymotion 词移动一致. 不依赖 IDE, 便于单独测试.
 */
object WordTargets {
  /**
   * 扫描与 [from, to] 相交的每一整行, 每次正则匹配产生一个目标, 按文档顺序返回.
   *
   * 逐行匹配, 所以一次匹配不会跨行; 空匹配会被忽略, 避免 `\w*` 这类正则在每个位置都产生目标.
   * 返回的目标可能略超出 [from, to], 由调用方按实际边界再过滤.
   */
  fun find(text: CharSequence, from: Int, to: Int, regex: Regex, anchor: WordAnchor): List<WordTarget> {
    val start = from.coerceIn(0, text.length)
    val end = to.coerceIn(0, text.length)
    if (start > end) return emptyList()

    val targets = mutableListOf<WordTarget>()
    var lineStart = start
    while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--

    while (lineStart <= end) {
      var lineEnd = lineStart
      while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++

      for (match in regex.findAll(text.subSequence(lineStart, lineEnd))) {
        if (match.value.isEmpty()) continue
        val column = if (anchor == WordAnchor.START) match.range.first else match.range.last
        targets += WordTarget(lineStart + column, lineStart)
      }

      if (lineEnd >= text.length) break
      lineStart = lineEnd + 1
    }
    return targets
  }

  /**
   * 按给定顺序保留目标: 若与上一个已保留的目标同行, 且列距小于 [minSpacing], 就丢弃.
   *
   * [targets] 应按离光标由近到远排列. [minSpacing] 不大于 1 时原样返回.
   */
  fun applyMinSpacing(targets: List<WordTarget>, minSpacing: Int): List<WordTarget> {
    if (minSpacing <= 1) return targets

    val kept = mutableListOf<WordTarget>()
    var last: WordTarget? = null
    for (target in targets) {
      val previous = last
      if (previous == null ||
        previous.lineStart != target.lineStart ||
        abs(target.column - previous.column) >= minSpacing
      ) {
        kept += target
        last = target
      }
    }
    return kept
  }
}
