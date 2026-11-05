/*
 * IdeaVim-EasyMotion. Easymotion emulator plugin for IdeaVim.
 * Copyright (C) 2019-2022  Alex Plate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("PrivatePropertyName")

package org.jetbrains.plugins.extension.easymotion

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.isLineEmpty
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.helper.*
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.StandardBoundaries
import org.acejump.input.JumpMode
import org.acejump.search.Pattern
import org.acejump.session.Session
import org.intellij.lang.annotations.Language
import org.jetbrains.plugins.extension.easymotion.MotionType.*
import org.jetbrains.plugins.extension.easymotion.cjk.WordAnchor
import org.jetbrains.plugins.extension.easymotion.cjk.WordPattern

class EasyMotionExtension : VimExtension {
    override fun getName(): String = pluginName

    override fun getOwner() = mappingOwner

    companion object {
        const val pluginPrefix = "<Plug>(easymotion-prefix)"
        const val defaultPrefix = "<leader><leader>"

        @Language("RegExp")
        private const val WORD = "(?<=\\s|\\A)[^\\s]"

        @Language("RegExp")
        private const val WORD_END = "[^\\s](?=\\s|\\Z)"

        @Language("RegExp")
        private const val LINE_END_NO_NEWLINE = "(.(?=\\n|\\Z))|(^$)"

        // IdeaVim 保存 `let g:xxx` 时去掉 `g:` 前缀, 带前缀的键读不到 ideavimrc 里的设置, 所以这里统一不带前缀
        private const val jumpAnywhere = "EasyMotion_re_anywhere"
        private const val lineJumpAnywhere = "EasyMotion_re_line_anywhere"
        const val doMapping = "EasyMotion_do_mapping"
        const val doMappingNoG = "EasyMotion_do_mapping"
        const val startOfLine = "EasyMotion_startofline"
        const val overrideAcejump = "EasyMotion_override_acejump"

        private const val defaultRe = """\v(<.|^${'$'})|(.>|^${'$'})|(\l)\zs(\u)|(_\zs.)|(#\zs.)"""

        private const val pluginName = "easymotion"
        val mappingOwner = MappingOwner.Plugin.get(pluginName)
    }

    override fun init() {
        val vimScriptVariables = injector.variableService.getGlobalVariables() as MutableMap

        vimScriptVariables.let { vars ->
            // 只在用户没有设置时写入默认值, 上游会无条件覆盖
            if (jumpAnywhere !in vars) vars[jumpAnywhere] = VimString(defaultRe)
            if (lineJumpAnywhere !in vars) vars[lineJumpAnywhere] = VimString(defaultRe)
            if (doMapping !in vars && doMappingNoG !in vars) vars[doMapping] = VimInt.ONE
            if (startOfLine !in vars) vars[startOfLine] = VimInt.ONE
            if (overrideAcejump !in vars) vars[overrideAcejump] = VimInt.ONE
        }

        // -----------  Default mapping table ---------------------//

        // @formatter:off

        mapToFunctionAndProvideKeys("f", MultiInput(INCLUSIVE, StandardBoundaries.AFTER_CARET))         // Works as `fn`
        mapToFunctionAndProvideKeys("F", MultiInput(EXCLUSIVE, StandardBoundaries.BEFORE_CARET))        // Works as `Fn`
        mapToFunctionAndProvideKeys("t", MultiInputPreStop(INCLUSIVE, StandardBoundaries.AFTER_CARET))  // Works as `tn`
        mapToFunctionAndProvideKeys("T", MultiInputPreStop(EXCLUSIVE, StandardBoundaries.BEFORE_CARET)) // Works as `Tn`
        // w/b/e/ge 及其 bd-, 行内变体改用 cjk.WordPattern, 词规则由 g:EasyMotion_word_regex 决定
        mapToFunctionAndProvideKeys("w", WordPattern(WordAnchor.START, StandardBoundaries.AFTER_CARET, EXCLUSIVE, useMinSpacing = true))
        mapToFunctionAndProvideKeys("W", CustomPattern(WORD, StandardBoundaries.AFTER_CARET, EXCLUSIVE))
        mapToFunctionAndProvideKeys("b", WordPattern(WordAnchor.START, StandardBoundaries.BEFORE_CARET, EXCLUSIVE, useMinSpacing = true))
        mapToFunctionAndProvideKeys("B", CustomPattern(WORD, StandardBoundaries.BEFORE_CARET, EXCLUSIVE))
        mapToFunctionAndProvideKeys("e", WordPattern(WordAnchor.END, StandardBoundaries.AFTER_CARET, INCLUSIVE))
        mapToFunctionAndProvideKeys("E", CustomPattern(WORD_END, StandardBoundaries.AFTER_CARET, EXCLUSIVE))
        mapToFunctionAndProvideKeys("ge", WordPattern(WordAnchor.END, StandardBoundaries.BEFORE_CARET, INCLUSIVE))
        mapToFunctionAndProvideKeys("gE", CustomPattern(WORD_END, StandardBoundaries.BEFORE_CARET, EXCLUSIVE))
        mapToFunctionAndProvideKeys("j", JkMotion(StandardBoundaries.AFTER_CARET))
        mapToFunctionAndProvideKeys("k", JkMotion(StandardBoundaries.BEFORE_CARET))

        mapToFunctionAndProvideKeys("s", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))  // Works as `sn`
        mapToFunctionAndProvideKeys("n", RepeatSearch(forward = true, respectVimDirection = false))
        mapToFunctionAndProvideKeys("N", RepeatSearch(forward = false, respectVimDirection = false))

        // ------------ Extended mapping table -------------------//

        mapToFunction("bd-f", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))
        mapToFunction("bd-t", BiDirectionalPreStop(false))
        mapToFunction("bd-w", WordPattern(WordAnchor.START, StandardBoundaries.VISIBLE_ON_SCREEN, EXCLUSIVE))
        mapToFunction("bd-W", CustomPattern(WORD, StandardBoundaries.VISIBLE_ON_SCREEN, EXCLUSIVE))
        mapToFunction("bd-e", WordPattern(WordAnchor.END, StandardBoundaries.VISIBLE_ON_SCREEN, BIDIRECTIONAL_INCLUSIVE))
        mapToFunction("bd-E", CustomPattern(WORD_END, StandardBoundaries.VISIBLE_ON_SCREEN, EXCLUSIVE))
        mapToFunction("bd-jk", PredefinedPattern(Pattern.LINE_INDENTS, StandardBoundaries.WHOLE_FILE, LINE))
        mapToFunction("bd-n", RepeatSearch(forward = false, respectVimDirection = false, bidirect = true))
        mapToFunction("jumptoanywhere", Jumptoanywhere())
        mapToFunction("sol-j", PredefinedPattern(Pattern.LINE_STARTS, StandardBoundaries.AFTER_CARET, LINE))
        mapToFunction("sol-k", PredefinedPattern(Pattern.LINE_STARTS, StandardBoundaries.BEFORE_CARET, LINE))
        mapToFunction("eol-j", EndOfLinePattern(StandardBoundaries.AFTER_CARET))
        mapToFunction("eol-k", EndOfLinePattern(StandardBoundaries.BEFORE_CARET))
        mapToFunction("iskeyword-w", KeyWordStart(StandardBoundaries.AFTER_CARET))
        mapToFunction("iskeyword-b", KeyWordStart(StandardBoundaries.BEFORE_CARET))
        mapToFunction("iskeyword-bd-w", KeyWordStart(StandardBoundaries.VISIBLE_ON_SCREEN))
        mapToFunction("iskeyword-e", KeyWordEnd(StandardBoundaries.AFTER_CARET, INCLUSIVE))
        mapToFunction("iskeyword-ge", KeyWordEnd(StandardBoundaries.BEFORE_CARET, INCLUSIVE))
        mapToFunction("iskeyword-bd-e", KeyWordEnd(StandardBoundaries.VISIBLE_ON_SCREEN, BIDIRECTIONAL_INCLUSIVE))
        mapToFunction("vim-n", RepeatSearch(forward = true, respectVimDirection = true))
        mapToFunction("vim-N", RepeatSearch(forward = false, respectVimDirection = true))

        // ------------ Within Line Motion -----------------------//

        mapToFunction("sl", MultiInput(BIDIRECTIONAL_INCLUSIVE, FullLineBoundary))  // Works as `sln`
        mapToFunction("fl", MultiInput(INCLUSIVE, AfterCaretLineBoundary))          // Works as `fln`
        mapToFunction("Fl", MultiInput(EXCLUSIVE, BeforeCaretLineBoundary))         // Works as `Fln`
        mapToFunction("bd-fl", MultiInput(BIDIRECTIONAL_INCLUSIVE, FullLineBoundary)) // Works as `sln`
        mapToFunction("tl", MultiInputPreStop(INCLUSIVE, AfterCaretLineBoundary))   // Works as `tln`
        mapToFunction("Tl", MultiInputPreStop(EXCLUSIVE, BeforeCaretLineBoundary))  // Works as `Tln`
        mapToFunction("bd-tl", BiDirectionalPreStop(true))                   // Works as `bd-tln`
        mapToFunction("wl", WordPattern(WordAnchor.START, AfterCaretLineBoundary, EXCLUSIVE))
        mapToFunction("bl", WordPattern(WordAnchor.START, BeforeCaretLineBoundary, EXCLUSIVE))
        mapToFunction("bd-wl", WordPattern(WordAnchor.START, FullLineBoundary, EXCLUSIVE))
        mapToFunction("el", WordPattern(WordAnchor.END, AfterCaretLineBoundary, INCLUSIVE))
        mapToFunction("gel", WordPattern(WordAnchor.END, BeforeCaretLineBoundary, INCLUSIVE))
        mapToFunction("bd-el", WordPattern(WordAnchor.END, FullLineBoundary, BIDIRECTIONAL_INCLUSIVE))
        mapToFunction("lineforward", JumptoanywhereInLine(0))
        mapToFunction("linebackward", JumptoanywhereInLine(1))
        mapToFunction("lineanywhere", JumptoanywhereInLine(2))

        // ------------ Multi input mapping table ----------------//
        mapToFunction("s2", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))     // Works as `sn`
        mapToFunction("f2", MultiInput(INCLUSIVE, StandardBoundaries.AFTER_CARET))                         // Works as `fn`
        mapToFunction("F2", MultiInput(EXCLUSIVE, StandardBoundaries.BEFORE_CARET))                        // Works as `Fn`
        mapToFunction("bd-f2", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))  // Works as `sn`
        mapToFunction("t2", MultiInputPreStop(INCLUSIVE, StandardBoundaries.AFTER_CARET))                  // Works as `tn`
        mapToFunction("T2", MultiInputPreStop(EXCLUSIVE, StandardBoundaries.BEFORE_CARET))                 // Works as `Tn`
        mapToFunction("bd-t2", BiDirectionalPreStop(false))

        mapToFunction("sl2", MultiInput(BIDIRECTIONAL_INCLUSIVE, FullLineBoundary))        // Works as `sln`
        mapToFunction("fl2", MultiInput(INCLUSIVE, AfterCaretLineBoundary))          // Works as `fln`
        mapToFunction("Fl2", MultiInput(EXCLUSIVE, BeforeCaretLineBoundary))         // Works as `Fln`
        mapToFunction("tl2", MultiInputPreStop(INCLUSIVE, AfterCaretLineBoundary))   // Works as `tln`
        mapToFunction("Tl2", MultiInputPreStop(EXCLUSIVE, BeforeCaretLineBoundary))  // Works as `Tln`

        mapToFunction("sn", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))
        mapToFunction("fn", MultiInput(INCLUSIVE, StandardBoundaries.AFTER_CARET))
        mapToFunction("Fn", MultiInput(EXCLUSIVE, StandardBoundaries.BEFORE_CARET))
        mapToFunction("bd-fn", MultiInput(BIDIRECTIONAL_INCLUSIVE, StandardBoundaries.VISIBLE_ON_SCREEN))
        mapToFunction("tn", MultiInputPreStop(INCLUSIVE, StandardBoundaries.AFTER_CARET))
        mapToFunction("Tn", MultiInputPreStop(EXCLUSIVE, StandardBoundaries.BEFORE_CARET))
        mapToFunction("bd-tn", BiDirectionalPreStop(false))

        mapToFunction("sln", MultiInput(BIDIRECTIONAL_INCLUSIVE, FullLineBoundary))
        mapToFunction("fln", MultiInput(INCLUSIVE, AfterCaretLineBoundary))
        mapToFunction("Fln", MultiInput(EXCLUSIVE, BeforeCaretLineBoundary))
        mapToFunction("bd-fln", MultiInput(BIDIRECTIONAL_INCLUSIVE, FullLineBoundary))
        mapToFunction("tln", MultiInputPreStop(INCLUSIVE, AfterCaretLineBoundary))
        mapToFunction("Tln", MultiInputPreStop(EXCLUSIVE, BeforeCaretLineBoundary))
        mapToFunction("bd-tln", BiDirectionalPreStop(true))

        injector.keyGroup.putKeyMapping(
            MappingMode.NVO,
            injector.parser.parseKeys("<Plug>(acejump-linemarks)"),
            owner,
            getHandler(LineMarks),
            false
        )

        // @formatter:on
        if (vimScriptVariables[doMapping] == VimInt.ONE || vimScriptVariables[doMappingNoG] == VimInt.ONE) {
            putKeyMappingIfMissing(MappingMode.NVO,
                injector.parser.parseKeys(defaultPrefix), owner, injector.parser.parseKeys(pluginPrefix), true)
        }

        if (vimScriptVariables[overrideAcejump] == VimInt.ONE) {
            MappingConfigurator.configureMappings()
        }
    }

    private object LineMarks : HandlerProcessor(EXCLUSIVE) {
        override fun customization(editor: Editor, session: Session) {
            if (editor.vim.isEndAllowed) {
                session.startRegexSearch(Pattern.LINE_ALL_MARKS, StandardBoundaries.VISIBLE_ON_SCREEN)
            } else {
                session.startRegexSearch(
                    "$LINE_END_NO_NEWLINE|${Pattern.LINE_STARTS.regex}",
                    StandardBoundaries.VISIBLE_ON_SCREEN
                )
            }
        }
    }

    private class RepeatSearch(
        val forward: Boolean,
        val respectVimDirection: Boolean,
        val bidirect: Boolean = false
    ) : HandlerProcessor(EXCLUSIVE) {
        override fun customization(editor: Editor, session: Session) {
            val lastSearch = injector.searchGroup.lastSearchPattern ?: run {
                session.end()
                return
            }
            val lastDirection = injector.searchGroup.getLastSearchDirection().toInt()

            val currentOffset = editor.caretModel.offset
            val currentLine = editor.caretModel.logicalPosition.line

            val lineRange = if (bidirect) {
                0 to -1
            } else {
                if (respectVimDirection) {
                    if (forward) {
                        if (lastDirection > 0) currentLine to -1 else 0 to currentLine
                    } else {
                        if (lastDirection > 0) 0 to currentLine else currentLine to -1
                    }
                } else {
                    if (forward) currentLine to -1 else 0 to currentLine
                }
            }

            val startOffsets = injector.searchHelper.findAll(editor.vim, lastSearch, lineRange.first, lineRange.second, false)
                .map { it.startOffset }
                .filter { if (bidirect) true else if (lineRange.second == -1) it > currentOffset else it < currentOffset }
                .toSortedSet()

            session.markResults(startOffsets)
        }
    }

    private class Jumptoanywhere : HandlerProcessor(EXCLUSIVE) {
        override fun customization(editor: Editor, session: Session) {
            val pattern = injector.variableService.getGlobalVariableValue(jumpAnywhere)?.asString() ?: return

            val fileSize = editor.vim.fileSize()
            val startOffsets = injector.searchHelper.findAll(editor.vim, pattern, 0, -1, false)
                .map { it.startOffset }

                // TODO: 09.04.2021 Some issues on the IdeaVim side. Adds a boundary outsize of the file size
                .filter { it < fileSize }
                .toSortedSet()
            session.markResults(startOffsets)
        }
    }

    /** Directions as in vim */
    private class JumptoanywhereInLine(private val direction: Int) : HandlerProcessor(EXCLUSIVE) {
        override fun customization(editor: Editor, session: Session) {
            val pattern = injector.variableService.getGlobalVariableValue(lineJumpAnywhere)?.asString() ?: return
            val boundary = when (direction) {
                0 -> AfterCaretLineBoundary
                1 -> BeforeCaretLineBoundary
                else -> FullLineBoundary
            }

            val currentLine = editor.caretModel.logicalPosition.line
            val startOffsets = injector.searchHelper.findAll(editor.vim, pattern, currentLine, currentLine, false)
                .map { it.startOffset }
                .filter { boundary.isOffsetInside(editor, it) }
                .toSortedSet()
            session.markResults(startOffsets)
        }
    }

    private class KeyWordStart(val boundary: Boundaries) : HandlerProcessor(EXCLUSIVE) {
        override fun customization(editor: Editor, session: Session) {
            val kw = keywordRegex()?.let {
                "((?<=\\s|\\A|[^$it])[$it])|" +  // Take a char from keyword that is preceded by a not-keyword char, or
                        "((?<=[$it])[^$it\\s])|" // non-keyword char that is preceded by a keyword char.
            } ?: ""
            val regex = "$kw$WORD"
            session.startRegexSearch(regex, boundary)
        }
    }

    private class KeyWordEnd(val boundary: Boundaries, motionType: MotionType) : HandlerProcessor(motionType) {
        override fun customization(editor: Editor, session: Session) {
            val kw = keywordRegex()?.let {
                "([$it](?=\\s|\\Z|[^$it]))|" +  // Take a char from keyword that is preceded by a not-keyword char, or
                        "([^$it\\s](?=[$it]))|" // non-keyword char that is preceded by a keyword char.
            } ?: ""
            val regex = "$kw$WORD_END"
            session.startRegexSearch(regex, boundary)
        }
    }

    private class CustomPattern(
        val pattern: String,
        val boundary: Boundaries,
        motionType: MotionType
    ) : HandlerProcessor(motionType) {
        override fun customization(editor: Editor, session: Session) {
            session.startRegexSearch(pattern, boundary)
        }
    }

    private class JkMotion(val boundary: Boundaries) : HandlerProcessor(LINE) {
        override fun customization(editor: Editor, session: Session) {
            if (injector.variableService.getGlobalVariableValue(startOfLine) != VimInt.ZERO) {
                session.startRegexSearch(Pattern.LINE_INDENTS, boundary)
            } else {
                val vp = editor.caretModel.visualPosition
                val res = when (boundary) {
                    StandardBoundaries.AFTER_CARET -> generateLineOffsets(editor, vp, true)
                    StandardBoundaries.BEFORE_CARET -> generateLineOffsets(editor, vp, false)
                    else -> throw UnsupportedOperationException("This boundary is not supported: $boundary")
                }
                val fileSize = editor.vim.fileSize()
                val resultsToMark = res.filter { it < fileSize }.toSortedSet()
                session.markResults(resultsToMark)
            }
        }

        private fun generateLineOffsets(
            editor: Editor,
            vp: VisualPosition,
            direction: Boolean
        ): MutableSet<Int> {
            val dir = if (direction) 1 else -1
            var counter = 0
            val res = mutableSetOf<Int>()
            val lastLine = editor.vim.getVisualLineCount()
            while (counter >= 0) {
                counter++
                val nextLine = vp.line + dir * counter
                if (nextLine > lastLine || nextLine < 0) break
                var offset = editor.visualPositionToOffset(VisualPosition(nextLine, vp.column))
                if (editor.offsetToVisualPosition(offset).column < vp.column) {
                    if (!editor.vim.isLineEmpty(editor.offsetToVisualPosition(offset).line, false)) {
                        if (!editor.vim.isEndAllowed) offset--
                    }
                }
                if (boundary.isOffsetInside(editor, offset)) {
                    res += offset
                } else {
                    counter = -1
                }
            }
            return res
        }
    }

    private class EndOfLinePattern(val boundary: Boundaries) : HandlerProcessor(LINE) {
        override fun customization(editor: Editor, session: Session) {
            if (editor.vim.isEndAllowed) {
                session.startRegexSearch(Pattern.LINE_ENDS, boundary)
            } else {
                session.startRegexSearch(LINE_END_NO_NEWLINE, boundary)
            }
        }
    }

    private class PredefinedPattern(
        val pattern: Pattern,
        val boundary: Boundaries,
        motionType: MotionType
    ) : HandlerProcessor(motionType) {
        override fun customization(editor: Editor, session: Session) {
            session.startRegexSearch(pattern, boundary)
        }
    }

    private class MultiInput(
        motionType: MotionType,
        private val boundary: Boundaries,
    ) : HandlerProcessor(motionType) {
        override fun customization(editor: Editor, session: Session) {
            session.toggleJumpMode(JumpMode.JUMP, boundary)
        }
    }

    private class MultiInputPreStop(
        motionType: MotionType,
        private val boundaries: Boundaries,
    ) : HandlerProcessor(motionType) {
        override fun customization(editor: Editor, session: Session) {
            session.toggleJumpMode(JumpMode.JUMP, boundaries)
        }

        override fun onFinish(editor: Editor, query: String?) {
            if (boundaries === StandardBoundaries.AFTER_CARET || boundaries === AfterCaretLineBoundary) {
                editor.caretModel.moveToOffset((editor.caretModel.offset - 1).coerceAtLeast(0))
            } else if (boundaries === StandardBoundaries.BEFORE_CARET || boundaries === BeforeCaretLineBoundary) {
                val fileSize = editor.document.textLength

                val newOffset = (editor.caretModel.offset + (query?.length ?: 0)).coerceAtMost(fileSize)
                editor.caretModel.moveToOffset(newOffset)
            }
        }
    }

    private class BiDirectionalPreStop(val inLine: Boolean) : HandlerProcessor(BIDIRECTIONAL_INCLUSIVE) {
        var caretPosition: Int? = null

        override fun customization(editor: Editor, session: Session) {
            val boundaries = if (inLine) FullLineBoundary else StandardBoundaries.VISIBLE_ON_SCREEN
            session.toggleJumpMode(JumpMode.JUMP, boundaries)
            caretPosition = editor.caretModel.offset
        }

        override fun onFinish(editor: Editor, query: String?) {
            val oldCaretOffset = caretPosition.also { caretPosition = null } ?: return
            val newCaretOffset = editor.caretModel.offset
            if (newCaretOffset > oldCaretOffset) {
                editor.caretModel.moveToOffset((editor.caretModel.offset - 1).coerceAtLeast(0))
            } else if (newCaretOffset < oldCaretOffset) {
                val fileSize = editor.document.textLength

                val newOffset = (editor.caretModel.offset + (query?.length ?: 0)).coerceAtMost(fileSize)
                editor.caretModel.moveToOffset(newOffset)
            }
        }
    }
}
