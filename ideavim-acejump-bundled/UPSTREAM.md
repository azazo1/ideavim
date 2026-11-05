# 上游来源

本模块的 `src/main/java/org/acejump/` 复制自 AceJump, 供内置 easymotion 使用.

- 仓库: <https://github.com/acejump/AceJump>
- commit: `21f55b8830cf9ff8d9a5bb10f1fccfb355d849d3` (版本 3.8.22)
- 原路径: `src/main/kotlin/org/acejump/`
- 许可证: GPL-3.0, 见本目录 `LICENSE`

## 本地改动

源码改了:

- `config/AceConfig.kt`: 状态名改为 `IdeaVimBundledAceConfig`, 存储文件改为 `IdeaVimBundledAceJump.xml`, 避免和独立安装的 AceJump 共用设置文件.
- `config/AceConfigurable.kt`: 设置页显示名改为 `EasyMotion`.
- `session/Session.kt`: `markResults` 像 `startRegexSearch` 一样保存搜索器. 上游没有保存, 输入两字符标签的首字符后, 第二个字符不再显示.

模块描述文件 `IdeaVIM.ideavim-acejump-bundled.xml` 由上游 `plugin.xml` 改写:

- 不注册 `ctrl+;` 等独立 AceJump 动作, 会话只由 easymotion 启动.
- 设置页和按键处理器换用新的 id.

## 限制

包名保持 `org.acejump`, 与独立安装的 AceJump 插件的类名和服务相同, 两者不能同时启用.

## 同步上游

用新 commit 的 `src/main/kotlin/org/acejump/` 覆盖本模块的 `src/main/java/org/acejump/`, 再重新应用上面几处源码改动, 并对照上游 `plugin.xml` 检查描述文件.
