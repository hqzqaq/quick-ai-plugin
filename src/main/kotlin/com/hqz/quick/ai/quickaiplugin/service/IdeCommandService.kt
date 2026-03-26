package com.hqz.quick.ai.quickaiplugin.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.hqz.quick.ai.quickaiplugin.service.CursorPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 操作系统类型枚举
 *
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
enum class OSType {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN
}

/**
 * 跨平台命令执行服务
 * 负责在不同操作系统上执行 VS Code 等编辑器的打开命令
 *
 * @see com.intellij.openapi.progress.Task.Backgroundable
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class IdeCommandService {

    /**
     * 在指定 IDE 中打开文件并跳转到指定位置
     *
     * @param project 当前项目
     * @param idePath IDE 可执行文件路径
     * @param projectPath 项目根路径
     * @param cursorPosition 光标位置信息
     * @param onSuccess 成功回调
     * @param onError 错误回调
     *
     * @see com.intellij.openapi.progress.Task.Backgroundable
     *
     * Thread Safety Note: 此方法可在任意线程调用，内部使用 Task.Backgroundable 在后台线程执行
     */
    fun openFileInIde(
        project: Project,
        idePath: String,
        projectPath: String,
        cursorPosition: CursorPosition,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val command = buildCommand(idePath, projectPath, cursorPosition)
        val logService = com.hqz.quick.ai.quickaiplugin.util.LogService.getInstance()
        val i18nService = com.hqz.quick.ai.quickaiplugin.util.I18nService.getInstance()

        logService.info(i18nService.message("log.command.executing", command.joinToString(" ")))

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Opening in IDE", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val process = executeCommand(command)
                    val exitCode = process.waitFor()

                    if (exitCode == 0) {
                        logService.info(i18nService.message("log.command.success", exitCode))
                        ApplicationManager.getApplication().invokeLater {
                            onSuccess()
                        }
                    } else {
                        val error = readErrorStream(process)
                        logService.error(i18nService.message("log.command.error", exitCode, error))
                        ApplicationManager.getApplication().invokeLater {
                            logService.notifyError(project, i18nService.message("notification.opening.ide.failed"), 
                                i18nService.message("notification.command.error", error))
                            onError("Command failed with exit code $exitCode: $error")
                        }
                    }
                } catch (e: Exception) {
                    logService.error(i18nService.message("log.command.error", -1, e.message ?: "Unknown error"), e)
                    ApplicationManager.getApplication().invokeLater {
                        logService.notifyError(project, i18nService.message("notification.opening.ide.failed"), 
                            i18nService.message("notification.command.error", e.message ?: "Unknown error"))
                        onError("Failed to execute command: ${e.message}")
                    }
                }
            }
        })
    }

    /**
     * 构建跨平台命令
     *
     * @param idePath IDE 可执行文件路径
     * @param projectPath 项目根路径
     * @param cursorPosition 光标位置信息
     * @return 构建好的命令列表
     *
     * Thread Safety Note: 此方法是纯函数，可在任意线程调用
     */
    private fun buildCommand(idePath: String, projectPath: String, cursorPosition: CursorPosition): List<String> {
        val osType = detectOSType()
        val filePath = cursorPosition.filePath
        val location = "${cursorPosition.lineNumber}:${cursorPosition.columnNumber}"

        return when (osType) {
            OSType.WINDOWS -> {
                listOf("cmd", "/c", "\"$idePath\"", projectPath, "-g", "$filePath:$location")
            }
            OSType.MACOS, OSType.LINUX -> {
                listOf(idePath, projectPath, "-g", "$filePath:$location")
            }
            OSType.UNKNOWN -> {
                listOf(idePath, projectPath, "-g", "$filePath:$location")
            }
        }
    }

    /**
     * 执行命令
     *
     * @param command 命令列表
     * @return 进程对象
     *
     * @see ProcessBuilder
     *
     * Thread Safety Note: 此方法应在后台线程调用
     */
    private fun executeCommand(command: List<String>): Process {
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
    }

    /**
     * 读取进程错误流
     *
     * @param process 进程对象
     * @return 错误信息
     *
     * Thread Safety Note: 此方法应在后台线程调用
     */
    private fun readErrorStream(process: Process): String {
        return BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            reader.lines().toList().joinToString("\n")
        }
    }

    /**
     * 检测操作系统类型
     *
     * @return 操作系统类型
     *
     * Thread Safety Note: 此方法是纯函数，可在任意线程调用
     */
    private fun detectOSType(): OSType {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> OSType.WINDOWS
            osName.contains("mac") -> OSType.MACOS
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OSType.LINUX
            else -> OSType.UNKNOWN
        }
    }

    /**
     * 验证 IDE 路径是否有效
     *
     * @param idePath IDE 可执行文件路径
     * @return 是否有效
     *
     * Thread Safety Note: 此方法是纯函数，可在任意线程调用
     */
    fun validateIdePath(idePath: String): Boolean {
        return validateIdePathWithDetail(idePath).first
    }

    /**
     * 验证 IDE 路径是否有效，并返回详细错误信息
     *
     * @param idePath IDE 可执行文件路径
     * @return Pair<是否有效, 详细错误信息>
     *
     * Thread Safety Note: 此方法是纯函数，可在任意线程调用
     */
    fun validateIdePathWithDetail(idePath: String): Pair<Boolean, String> {
        val logService = com.hqz.quick.ai.quickaiplugin.util.LogService.getInstance()

        logService.info("[validateIdePath] Starting validation for path: '$idePath'")

        val cleanPath = idePath.trim('"')
        logService.info("[validateIdePath] Cleaned path: '$cleanPath'")

        if (cleanPath.isBlank()) {
            logService.warn("[validateIdePath] Path is blank after trimming")
            return Pair(false, "路径为空")
        }

        val file = File(cleanPath)
        logService.info("[validateIdePath] File exists: ${file.exists()}, isDirectory: ${file.isDirectory}, canExecute: ${file.canExecute()}")
        logService.info("[validateIdePath] Absolute path: ${file.absolutePath}")
        logService.info("[validateIdePath] Canonical path: ${file.canonicalPath}")

        if (!file.exists()) {
            val errorMsg = "文件不存在: ${file.absolutePath}"
            logService.warn("[validateIdePath] $errorMsg")
            return Pair(false, errorMsg)
        }

        if (file.isDirectory) {
            logService.info("[validateIdePath] Path is a directory, checking if it's a .app bundle")
            if (cleanPath.endsWith(".app")) {
                logService.info("[validateIdePath] It's a .app bundle, searching for executable")
                val macExecutablePath = findMacOSExecutable(cleanPath)
                logService.info("[validateIdePath] Found macOS executable: $macExecutablePath")
                if (macExecutablePath != null) {
                    val execFile = File(macExecutablePath)
                    logService.info("[validateIdePath] Executable exists: ${execFile.exists()}, canExecute: ${execFile.canExecute()}")
                    if (execFile.canExecute()) {
                        logService.info("[validateIdePath] .app bundle validation passed")
                        return Pair(true, "")
                    } else {
                        val errorMsg = ".app 包中的可执行文件没有执行权限: $macExecutablePath"
                        logService.warn("[validateIdePath] $errorMsg")
                        return Pair(false, errorMsg)
                    }
                } else {
                    val errorMsg = ".app 包中未找到可执行文件，尝试路径: $cleanPath/Contents/Resources/app/bin/code, $cleanPath/Contents/MacOS/Electron"
                    logService.warn("[validateIdePath] $errorMsg")
                    return Pair(false, errorMsg)
                }
            }
            val errorMsg = "路径是目录但不是有效的 .app 包: $cleanPath"
            logService.warn("[validateIdePath] $errorMsg")
            return Pair(false, errorMsg)
        }

        if (!file.canExecute()) {
            val errorMsg = "文件没有执行权限: ${file.absolutePath}"
            logService.warn("[validateIdePath] $errorMsg")
            return Pair(false, errorMsg)
        }

        logService.info("[validateIdePath] Final validation result: true")
        return Pair(true, "")
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
        fun getInstance(): IdeCommandService {
            return IdeCommandService()
        }
    }
}
