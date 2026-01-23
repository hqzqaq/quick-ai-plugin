package com.hqz.quick.ai.quickaiplugin.service

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * 文件选择器服务
 * 提供跨平台的文件选择功能，特别支持 macOS .app 包选择
 *
 * @see com.intellij.openapi.fileChooser.FileChooser
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class FileChooserService {

    private val logger = Logger.getInstance("QuickAiPlugin.FileChooser")
    private val logService = com.hqz.quick.ai.quickaiplugin.util.LogService.getInstance()
    private val i18nService = com.hqz.quick.ai.quickaiplugin.util.I18nService.getInstance()

    /**
     * 选择 IDE 可执行文件
     * 支持 macOS .app 包选择，自动定位到可执行文件
     *
     * @param project 当前项目
     * @return 选择的文件路径，如果取消则返回 null
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun chooseIdeExecutable(project: Project?): String? {
        val descriptor = FileChooserDescriptor(
            true,
            false,
            false,
            false,
            false,
            true
        ).withTitle(i18nService.message("file.chooser.title"))
            .withDescription("Select an IDE application or executable")

        val selectedFile = FileChooser.chooseFile(descriptor, project, null)
        
        if (selectedFile == null) {
            logService.info("File selection cancelled")
            return null
        }

        val filePath = selectedFile.path
        logService.info(i18nService.message("log.ide.path.validating"), filePath)

        val executablePath = resolveExecutablePath(filePath)
        
        if (executablePath != null) {
            logService.info(i18nService.message("log.ide.path.valid"), executablePath)
        } else {
            logService.warn(i18nService.message("log.ide.path.invalid"), filePath)
            Messages.showErrorDialog(
                i18nService.message("file.chooser.invalid.path"),
                "Error"
            )
        }

        return executablePath
    }

    /**
     * 解析可执行文件路径
     * 对于 macOS .app 包，自动定位到可执行文件
     *
     * @param path 文件路径
     * @return 可执行文件路径，如果无效则返回 null
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    private fun resolveExecutablePath(path: String): String? {
        val file = File(path)
        
        if (!file.exists()) {
            return null
        }

        if (file.isDirectory) {
            if (path.endsWith(".app")) {
                val macExecutablePath = findMacOSExecutable(path)
                if (macExecutablePath != null && File(macExecutablePath).canExecute()) {
                    return macExecutablePath
                }
            }
            return null
        }

        if (file.canExecute()) {
            return path
        }

        return null
    }

    /**
     * 查找 macOS .app 包中的可执行文件
     *
     * @param appPath .app 包路径
     * @return 可执行文件路径，如果找不到则返回 null
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    private fun findMacOSExecutable(appPath: String): String? {
        val possiblePaths = listOf(
            "$appPath/Contents/Resources/app/bin/code",
            "$appPath/Contents/MacOS/Electron",
            "$appPath/Contents/MacOS/${File(appPath).nameWithoutExtension}"
        )

        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists() && file.canExecute()) {
                return path
            }
        }

        return null
    }

    companion object {
        @JvmStatic
        fun getInstance(): FileChooserService {
            return FileChooserService()
        }
    }
}
