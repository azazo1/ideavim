# IdeaVim fork: Agent Instructions

> 开发之前确保阅读 AGENTS.md 和 CLAUDE.md. CLAUDE.md 与 CONTRIBUTING.md 来自上游, 描述 IdeaVim 本身的约定, 仍然适用; 本文件只记录本 fork 自己的规则.

本仓库是上游 [JetBrains/ideavim](https://github.com/JetBrains/ideavim) 的 fork, 用于自用.

## README.md 相对上游的修改规则

- 本 fork 新增或改变的功能, 写进 README.md 开头的 "本分支新增功能" 一节, 每项一句话说清楚做了什么.
- 详细用法 (配置变量, 映射) 写在 `doc/` 下对应的文档里, README 只放链接.
- 与上游或其他插件不兼容的地方, 写进该节末尾的 `> [!IMPORTANT]` 提示.
- README 的其余部分保持上游原文, 不做修改, 减少同步上游时的冲突.

## 代码结构

- 新功能放在独立的模块或包里, 不和上游代码混在一起.
- 必须改上游文件时, 改动尽量小, 并在改动处加注释说明原因.
- 并入的第三方源码单独成模块, 保留原许可证, 并附 `UPSTREAM.md` 记录来源仓库, commit, 本地改动和同步上游的步骤.

本 fork 新增的模块:

- `ideavim-acejump-bundled/`: 并入的 AceJump 源码, 供内置 easymotion 使用.
- `ideavim-easymotion/`: 并入的 IdeaVim-EasyMotion 源码, 加上 `cjk/` (CJK 词移动规则) 和 `integration/` (接入 IdeaVim 的代码).

不依赖 IDE 的逻辑在模块自己的 `src/test` 下测试.

## 构建与测试

推荐在 Docker 容器里构建, 本机不需要安装 JDK 21. 依赖缓存放在 Docker 卷 `ideavim-gradle` 里, 第二次构建不会重新下载.

```shell
docker run --rm \
  -v "$PWD":/work -w /work \
  -v ideavim-gradle:/gradle -e GRADLE_USER_HOME=/gradle \
  -e ORG_GRADLE_PROJECT_instrumentPluginCode=false \
  -e ORG_GRADLE_PROJECT_downloadIdeaSources=false \
  eclipse-temurin:21-jdk \
  ./gradlew --no-daemon --console=plain :ideavim-easymotion:test buildPlugin
```

- 需要代理时, 先在 Docker Desktop 设置里配置代理, 再给容器加 `-e JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=host.docker.internal -Dhttps.proxyPort=<端口>"`.
- macOS 上 Docker Desktop 的命令在 `~/.docker/bin`, 不在 PATH 里时拉取镜像会找不到 `docker-credential-desktop`.
- 首次构建要下载 Gradle 和 IntelliJ IU, 共几个 GB.
- `buildSearchableOptions` 任务会打印大量堆栈, 这是上游原有的噪音, 以 `BUILD SUCCESSFUL` 为准.
- 产物在 `build/distributions/IdeaVIM-SNAPSHOT.zip`, 在 IDE 中用 Install Plugin from Disk 安装. 它与官方 IdeaVim 使用同一个插件 id, 安装后要关闭官方插件的自动更新.
- 不再需要缓存时执行 `docker volume rm ideavim-gradle`.

## 版本号

采用双重版本号: 上游版本 + fork 版本, 形如 `2.48.1-v0.1.0`.

- 前段跟随 fork 所基于的上游 tag (如 `2.48.1`).
- 后段 `-v0.1.0` 为本 fork 自身的版本, 每次发布递增.

打 tag 和创建 release 时使用此格式.
