/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.extension.easymotion.cjk

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.util.regex.PatternSyntaxException

/**
 * 从 ideavimrc 读取词移动的配置. IdeaVim 保存 `let g:xxx` 时去掉 `g:` 前缀, 所以这里的变量名不带前缀.
 */
object WordMotionSettings {
  /** 词移动的 Java 正则, 每次匹配是一个跳转目标. 对应 VSCodeVim fork 的 `vim.easymotionWordRegex`. */
  const val WORD_REGEX_VARIABLE = "EasyMotion_word_regex"

  /** `w` / `b` 相邻目标的最小列距, 设为 1 显示全部. 对应 VSCodeVim fork 的 `vim.easymotionWordMoveMinSpacing`. */
  const val MIN_SPACING_VARIABLE = "EasyMotion_word_min_spacing"

  /** 默认规则: ASCII 词, 连续汉字, 连续全角/中文标点, 连续 ASCII 符号各算一个词. 与 VSCodeVim fork 的默认值相同. */
  const val DEFAULT_WORD_REGEX = """\w+|[\u4e00-\u9fff]+|[\u3000-\u303f\uff00-\uffef]+|[!-/:-@\[-`{-~]+"""

  const val DEFAULT_MIN_SPACING = 2

  private val LOG = vimLogger<WordMotionSettings>()
  private val defaultRegex = Regex(DEFAULT_WORD_REGEX)

  // 只缓存最近一次的结果: 正则不变时不重复编译, 同一个错误正则也只告警一次
  @Volatile
  private var cached: Pair<String, Regex> = DEFAULT_WORD_REGEX to defaultRegex

  /** 当前生效的词正则. 用户正则无法编译时回退到默认规则. */
  fun wordRegex(): Regex {
    val pattern = injector.variableService.getGlobalVariableValue(WORD_REGEX_VARIABLE)?.toVimString()?.value
      ?: DEFAULT_WORD_REGEX
    cached.let { (cachedPattern, regex) -> if (cachedPattern == pattern) return regex }

    val regex = try {
      Regex(pattern)
    } catch (e: PatternSyntaxException) {
      LOG.warn("g:$WORD_REGEX_VARIABLE is not a valid regex, falling back to the default: ${e.description}")
      defaultRegex
    }
    cached = pattern to regex
    return regex
  }

  /** 当前生效的最小列距, 至少为 1. */
  fun minSpacing(): Int {
    val value = injector.variableService.getGlobalVariableValue(MIN_SPACING_VARIABLE)?.toVimNumber()?.value
      ?: DEFAULT_MIN_SPACING
    return value.coerceAtLeast(1)
  }
}
