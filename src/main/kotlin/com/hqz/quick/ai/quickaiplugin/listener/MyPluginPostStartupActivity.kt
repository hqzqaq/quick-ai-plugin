package com.hqz.quick.ai.quickaiplugin.listener

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * 插件启动活动
 * 在项目打开时注册编辑器鼠标监听器
 *
 * @see com.intellij.openapi.startup.ProjectActivity
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class MyPluginPostStartupActivity : ProjectActivity {

    /**
     * 在项目打开时执行
     *
     * @param project 当前项目
     *
     * @see com.intellij.openapi.startup.ProjectActivity
     *
     * Thread Safety Note: 此方法在协程中执行，不在 EDT
     */
    override suspend fun execute(project: Project) {
        service<IdeEditorMouseListenerManager>().registerEditorMouseListener(project)
    }
}
