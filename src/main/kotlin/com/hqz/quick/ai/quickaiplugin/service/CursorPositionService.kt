package com.hqz.quick.ai.quickaiplugin.service

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 光标位置信息数据类
 *
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
data class CursorPosition(
    val filePath: String,
    val lineNumber: Int,
    val columnNumber: Int
)

/**
 * 光标位置获取服务
 * 用于获取当前编辑器中光标所在的文件路径、行号和列号
 *
 * @see com.intellij.openapi.editor.Editor
 * @see com.intellij.openapi.fileEditor.FileEditorManager
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class CursorPositionService {

    /**
     * 获取当前光标位置信息
     *
     * @param project 当前项目
     * @return 光标位置信息，如果无法获取则返回 null
     *
     * @see com.intellij.openapi.application.readAction
     * @see com.intellij.openapi.editor.CaretModel
     *
     * Thread Safety Note: 此方法可在任意线程调用，内部使用 ReadAction 确保线程安全
     */
    suspend fun getCurrentCursorPosition(project: Project): CursorPosition? = withContext(Dispatchers.IO) {
        readAction {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val editor: Editor? = fileEditorManager.selectedTextEditor
            val virtualFile: VirtualFile? = fileEditorManager.selectedFiles.firstOrNull()

            if (editor != null && virtualFile != null) {
                val caretModel = editor.caretModel
                val caretOffset = caretModel.offset
                val lineNumber = editor.document.getLineNumber(caretOffset)
                val lineStartOffset = editor.document.getLineStartOffset(lineNumber)
                val columnNumber = caretOffset - lineStartOffset

                CursorPosition(
                    filePath = virtualFile.path,
                    lineNumber = lineNumber + 1,
                    columnNumber = columnNumber + 1
                )
            } else {
                null
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): CursorPositionService {
            return CursorPositionService()
        }
    }
}
