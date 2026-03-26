package com.hqz.quick.ai.quickaiplugin.listener

import com.hqz.quick.ai.quickaiplugin.config.PluginSettings
import com.hqz.quick.ai.quickaiplugin.service.CursorPositionService
import com.hqz.quick.ai.quickaiplugin.service.IdeCommandService
import com.hqz.quick.ai.quickaiplugin.util.I18nService
import com.hqz.quick.ai.quickaiplugin.util.LogService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 编辑器鼠标监听器
 * 监听编辑器中的鼠标点击事件，检测快捷键组合并触发打开 IDE 操作
 *
 * @see com.intellij.openapi.editor.event.EditorMouseListener
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class IdeEditorMouseListener(
    private val project: Project
) : EditorMouseListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val settings = PluginSettings.getInstance(project)
    private val cursorPositionService = CursorPositionService.getInstance()
    private val ideCommandService = IdeCommandService.getInstance()
    private val logService = LogService.getInstance()
    private val i18nService = I18nService.getInstance()

    override fun mouseReleased(event: EditorMouseEvent) {
        if (!settings.state.shortcutEnabled) return

        val mouseEvent = event.mouseEvent
        if (!isShortcutTriggered(mouseEvent)) return

        if (!isFocusedWindow(project)) {
            return
        }

        logService.info("[IdeEditorMouseListener] Shortcut triggered, project: ${project.name}")
        logService.info("[IdeEditorMouseListener] All IDE configs: ${settings.state.ideConfigs}")

        val defaultIdeConfig = settings.state.getDefaultIdeConfig()
        logService.info("[IdeEditorMouseListener] DefaultIdeConfig: $defaultIdeConfig")
        
        if (defaultIdeConfig == null || defaultIdeConfig.path.isBlank()) {
            logService.warn("[IdeEditorMouseListener] No default IDE config or path is blank. defaultIdeConfig: $defaultIdeConfig")
            logService.notifyWarning(
                project,
                i18nService.message("file.chooser.invalid.path"),
                i18nService.message("config.ide.path") + " " + i18nService.message("file.chooser.invalid.path")
            )
            return
        }

        logService.info("[IdeEditorMouseListener] Validating IDE path: ${defaultIdeConfig.path}")

        val (isValid, errorDetail) = ideCommandService.validateIdePathWithDetail(defaultIdeConfig.path)
        if (!isValid) {
            logService.warn("[IdeEditorMouseListener] IDE path validation failed: ${defaultIdeConfig.path}, reason: $errorDetail")
            val osType = when {
                System.getProperty("os.name").lowercase().contains("win") -> "Windows"
                System.getProperty("os.name").lowercase().contains("mac") -> "macOS"
                else -> "Linux"
            }
            val expectedCommand = "<IDE路径> <项目路径> -g <文件路径>:<行号>:<列号>"
            val errorMessage = buildString {
                appendLine(i18nService.message("file.chooser.invalid.path") + ": ${defaultIdeConfig.path}")
                appendLine()
                appendLine("验证失败原因: $errorDetail")
                appendLine()
                appendLine("操作系统: $osType")
                appendLine("预期命令格式: $expectedCommand")
                appendLine()
                appendLine("示例:")
                when {
                    System.getProperty("os.name").lowercase().contains("win") -> {
                        appendLine("  Windows: C:\\Users\\xxx\\AppData\\Local\\Programs\\Cursor\\Cursor.exe")
                    }
                    System.getProperty("os.name").lowercase().contains("mac") -> {
                        appendLine("  macOS: /Applications/Cursor.app")
                        appendLine("  macOS: /Applications/Cursor.app/Contents/MacOS/Cursor")
                    }
                    else -> {
                        appendLine("  Linux: /usr/bin/code")
                        appendLine("  Linux: /usr/share/cursor/cursor")
                    }
                }
            }
            logService.notifyError(
                project,
                i18nService.message("file.chooser.invalid.path"),
                errorMessage
            )
            return
        }

        logService.info("[IdeEditorMouseListener] IDE path validated successfully, executing open command")
        event.consume()
        executeOpenInIde(defaultIdeConfig.path)
    }

    /**
     * 检测是否触发了快捷键组合
     *
     * @param mouseEvent 鼠标事件
     * @return 是否触发了快捷键
     *
     * Thread Safety Note: 此方法在 EDT 调用
     */
    private fun isShortcutTriggered(mouseEvent: java.awt.event.MouseEvent): Boolean {
        val modifiers = mouseEvent.modifiersEx

        val isCtrlDown = modifiers and java.awt.event.InputEvent.CTRL_DOWN_MASK != 0
        val isShiftDown = modifiers and java.awt.event.InputEvent.SHIFT_DOWN_MASK != 0
        val isAltDown = modifiers and java.awt.event.InputEvent.ALT_DOWN_MASK != 0
        val isMetaDown = modifiers and java.awt.event.InputEvent.META_DOWN_MASK != 0

        val isLeftClick = mouseEvent.button == java.awt.event.MouseEvent.BUTTON1

        val isWindowsOrLinux = System.getProperty("os.name").lowercase().contains("win") ||
            System.getProperty("os.name").lowercase().contains("nix") ||
            System.getProperty("os.name").lowercase().contains("nux")

        return when {
            isWindowsOrLinux -> isCtrlDown && isShiftDown && isAltDown && isLeftClick
            else -> isMetaDown && isShiftDown && isAltDown && isLeftClick
        }
    }

    /**
     * 检查当前项目窗口是否是活跃窗口
     *
     * @param project 当前项目
     * @return 当前窗口是否是活跃窗口
     *
     * @see com.intellij.openapi.wm.WindowManager
     *
     * Thread Safety Note: 此方法在 EDT 调用
     */
    private fun isFocusedWindow(project: Project): Boolean {
        val windowManager = WindowManager.getInstance()
        val projectFrame = windowManager.getFrame(project) ?: return false
        return projectFrame.isActive
    }

    /**
     * 执行打开文件到 IDE 的操作
     *
     * @param idePath IDE 可执行文件路径
     *
     * @see com.hqz.quick.ai.quickaiplugin.service.CursorPositionService
     * @see com.hqz.quick.ai.quickaiplugin.service.IdeCommandService
     *
     * Thread Safety Note: 此方法在 EDT 调用，内部使用协程在后台执行
     */
    private fun executeOpenInIde(idePath: String) {
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
}
