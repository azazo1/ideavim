# 上游来源

本模块 `src/main/java/org/jetbrains/plugins/extension/easymotion/` 下的 6 个文件复制自 IdeaVim-EasyMotion.

- 仓库: <https://github.com/AlexPl292/IdeaVim-EasyMotion>
- commit: `dcf69942de909ac131baeba5e4584c0c2011e65c` (版本 1.16)
- 原路径: `src/main/kotlin/org/jetbrains/plugins/extension/easymotion/`
- 许可证: GPL-3.0, 见本目录 `LICENSE`

依赖的 AceJump 换成本插件内的 `ideavim-acejump-bundled` 模块.

## 本地改动

上游文件只改了 `EasyMotionExtension.kt`, 其余 5 个与上游逐字一致:

- 变量名常量去掉 `g:` 前缀. IdeaVim 保存 `let g:xxx` 时去掉前缀, 上游带前缀的键读不到 ideavimrc 里的 `g:EasyMotion_startofline` 等设置.
- `g:EasyMotion_re_anywhere` 和 `g:EasyMotion_re_line_anywhere` 只在用户没有设置时写入默认值, 上游会无条件覆盖.
- `w`, `b`, `e`, `ge`, `bd-w`, `bd-e`, `wl`, `bl`, `bd-wl`, `el`, `gel`, `bd-el` 改用 `cjk.WordPattern`, 删除不再使用的 `wordEnd` 常量.

新增的代码与上游文件分开放:

- `cjk/`: 词移动规则, 移植自 VSCodeVim fork (`azazo1/VSCodeVim`) 的 `vim.easymotionWordRegex` 和 `vim.easymotionWordMoveMinSpacing`. 对应变量是 `g:EasyMotion_word_regex` 和 `g:EasyMotion_word_min_spacing`, 用法见 `doc/IdeaVim Plugins.md`.
- `integration/`: 向 IdeaVim 报告内置 AceJump 会话状态, 让 IdeaVim 在会话期间把 Esc, Enter 等按键让给 AceJump.

上游的测试依赖 IdeaVim 的测试基类, 没有复制. `src/test` 只测试 `cjk` 包里不依赖 IDE 的逻辑.

## 限制

与独立安装的 IdeaVim-EasyMotion 都注册名为 `easymotion` 的扩展, 两者不能同时启用.

## 同步上游

用新 commit 的源码覆盖上游的 6 个文件 (不要覆盖 `cjk/` 和 `integration/`), 再重新应用 `EasyMotionExtension.kt` 的改动.
