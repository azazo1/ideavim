/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// 并入的 AceJump 源码 (来源与改动见 UPSTREAM.md), 供内置 easymotion 使用, 不依赖 IdeaVim 的任何代码.
plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  // AceJump 用它把非 ASCII 字符转写成 ASCII 再匹配 (AceUtil.mapToASCII), 版本与上游一致
  implementation("com.anyascii:anyascii:0.3.2")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
