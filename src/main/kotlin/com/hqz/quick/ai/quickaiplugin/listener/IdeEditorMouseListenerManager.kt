package com.hqz.quick.ai.quickaiplugin.listener

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import java.lang.ref.WeakReference

/**
 * 编辑器鼠标监听器管理器
 * 负责注册和管理编辑器鼠标监听器
 *
 * @see com.intellij.openapi.editor.EditorFactory
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
@Service(Service.Level.APP)
class IdeEditorMouseListenerManager {

    private val listenerMap = mutableMapOf<Project, WeakReference<IdeEditorMouseListener>>()

    /**
     * 注册编辑器鼠标监听器
     *
     * @param project 当前项目
     *
     * @see com.intellij.openapi.editor.EditorFactory
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun registerEditorMouseListener(project: Project) {
        val settings = project.service<com.hqz.quick.ai.quickaiplugin.config.PluginSettings>()

        if (!settings.state.shortcutEnabled) return

        if (listenerMap.containsKey(project)) {
            return
        }

        val editorFactory = EditorFactory.getInstance()
        val mouseListener = IdeEditorMouseListener(project)

        editorFactory.eventMulticaster.addEditorMouseListener(mouseListener, project)
        listenerMap[project] = WeakReference(mouseListener)
    }
}
