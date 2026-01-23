package com.hqz.quick.ai.quickaiplugin.action

import com.hqz.quick.ai.quickaiplugin.config.PluginSettings
import com.hqz.quick.ai.quickaiplugin.service.CursorPositionService
import com.hqz.quick.ai.quickaiplugin.service.IdeCommandService
import com.hqz.quick.ai.quickaiplugin.util.I18nService
import com.hqz.quick.ai.quickaiplugin.util.LogService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 打开文件到外部 IDE 的 Action
 * 响应快捷键和鼠标事件，在配置的 IDE 中打开当前文件并跳转到光标位置
 *
 * @see com.intellij.openapi.actionSystem.AnAction
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class OpenInIdeAction : AnAction(), DumbAware {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = PluginSettings.getInstance(project)
        val logService = LogService.getInstance()
        val i18nService = I18nService.getInstance()

        if (!settings.state.shortcutEnabled) {
            logService.notifyWarning(
                project,
                i18nService.message("notification.opening.ide.failed"),
                i18nService.message("notification.opening.ide.failed")
            )
            return
        }

        val defaultIdeConfig = settings.state.getDefaultIdeConfig()
        if (defaultIdeConfig == null || defaultIdeConfig.path.isBlank()) {
            logService.notifyWarning(
                project,
                i18nService.message("file.chooser.invalid.path"),
                i18nService.message("config.ide.path") + " " + i18nService.message("file.chooser.invalid.path")
            )
            return
        }

        val ideCommandService = IdeCommandService.getInstance()

        if (!ideCommandService.validateIdePath(defaultIdeConfig.path)) {
            logService.notifyError(
                project,
                i18nService.message("file.chooser.invalid.path"),
                i18nService.message("file.chooser.invalid.path") + ": ${defaultIdeConfig.path}"
            )
            return
        }

        executeOpenInIde(project, defaultIdeConfig.path)
    }

    /**
     * 执行打开文件到 IDE 的操作
     *
     * @param project 当前项目
     * @param idePath IDE 可执行文件路径
     *
     * @see com.intellij.openapi.progress.Task.Backgroundable
     *
     * Thread Safety Note: 此方法在 EDT 调用，内部使用协程和 Task.Backgroundable 在后台执行
     */
    private fun executeOpenInIde(project: Project, idePath: String) {
        val cursorPositionService = CursorPositionService.getInstance()
        val ideCommandService = IdeCommandService.getInstance()
        val logService = LogService.getInstance()
        val i18nService = I18nService.getInstance()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Opening in IDE", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Getting cursor position..."
                indicator.isIndeterminate = true

                coroutineScope.launch {
                    try {
                        val cursorPosition = cursorPositionService.getCurrentCursorPosition(project)

                        if (cursorPosition == null) {
                            logService.notifyWarning(
                                project,
                                i18nService.message("notification.opening.ide.failed"),
                                i18nService.message("notification.opening.ide.failed")
                            )
                            return@launch
                        }

                        indicator.text = "Opening ${cursorPosition.filePath} in IDE..."

                        ideCommandService.openFileInIde(
                            project = project,
                            idePath = idePath,
                            projectPath = project.basePath ?: "",
                            cursorPosition = cursorPosition,
                            onSuccess = {
                            },
                            onError = { error ->
                                logService.notifyError(
                                    project,
                                    i18nService.message("notification.opening.ide.failed"),
                                    error
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logService.error(i18nService.message("log.command.error", -1, e.message ?: "Unknown error"), e)
                        logService.notifyError(
                            project,
                            i18nService.message("notification.opening.ide.failed"),
                            e.message ?: "Unknown error"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val settings = project?.let { PluginSettings.getInstance(it) }

        val defaultIdeConfig = settings?.state?.getDefaultIdeConfig()
        e.presentation.isEnabledAndVisible = project != null &&
            settings != null &&
            settings.state.shortcutEnabled &&
            defaultIdeConfig != null &&
            defaultIdeConfig.path.isNotBlank()
    }
}
