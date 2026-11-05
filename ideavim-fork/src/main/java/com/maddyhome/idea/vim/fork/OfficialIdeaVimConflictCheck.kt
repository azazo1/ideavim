/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.fork

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 启动时检查官方 IdeaVim 是否也处于启用状态.
 *
 * 本 fork 换了插件 id, IDE 会把它和官方 IdeaVim 当作两个插件, 允许同时启用. 两者的类名, 扩展点和按键处理相同,
 * 同时启用会冲突, 所以发现后提示 user 禁用其中一个. 做法与 VSCodeVim fork 检测上游扩展一致.
 */
internal class OfficialIdeaVimConflictCheck : ProjectActivity {
  override suspend fun execute(project: Project) {
    // 每次 IDE 运行只提示一次, 不在每个打开的项目里重复弹出
    if (!checked.compareAndSet(false, true)) return

    val official = PluginManagerCore.getPlugin(PluginId.getId(OFFICIAL_PLUGIN_ID))
    if (official == null || !official.isEnabled) return

    LOG.warn("Official IdeaVim ${official.version} is enabled together with this fork")
    NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(
        "官方 IdeaVim 与本 fork 同时启用",
        "两者不兼容, 同时启用会导致按键处理冲突. 请在插件设置中禁用或卸载官方 IdeaVim (${official.version}), 然后重启 IDE.",
        NotificationType.WARNING,
      )
      .addAction(NotificationAction.createSimpleExpiring("打开插件设置") {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
      })
      .notify(project)
  }

  private companion object {
    /** 插件市场上官方 IdeaVim 的插件 id */
    private const val OFFICIAL_PLUGIN_ID = "IdeaVIM"
    private const val NOTIFICATION_GROUP = "ideavim-fork"

    private val LOG = logger<OfficialIdeaVimConflictCheck>()
    private val checked = AtomicBoolean(false)
  }
}
