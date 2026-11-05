/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// 内置 easymotion: 并入的 IdeaVim-EasyMotion 源码 (来源与改动见 UPSTREAM.md) 加上 CJK 词移动规则.
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
  compileOnly(project(":"))
  compileOnly(project(":ideavim-common"))
  compileOnly(project(":ideavim-acejump-bundled"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }
  }

  // 只测试不依赖 IDE 的纯逻辑 (cjk 包), 写法与 vim-engine 的测试配置一致
  testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
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

tasks {
  test {
    useJUnitPlatform()
  }
}
