package com.hqz.quick.ai.quickaiplugin.ui

import com.intellij.BundleBase
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*

/**
 * 快捷键录制对话框
 * 用于录制用户自定义快捷键
 *
 * @see com.intellij.openapi.ui.DialogWrapper
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class ShortcutRecorderDialog : DialogWrapper(true) {

    private val i18nService = com.hqz.quick.ai.quickaiplugin.util.I18nService.getInstance()
    private val logService = com.hqz.quick.ai.quickaiplugin.util.LogService.getInstance()

    private val shortcutField = JBTextField()
    private var recordedShortcut: String? = null

    init {
        title = i18nService.message("shortcut.recording.title")
        init()
    }

    /**
     * 创建中心面板
     *
     * @return 中心面板
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        val instructionLabel = JBLabel(i18nService.message("shortcut.recording.instruction"))
        instructionLabel.border = JBUI.Borders.empty(0, 0, 10, 0)

        shortcutField.isEditable = false
        shortcutField.text = "..."
        shortcutField.border = JBUI.Borders.empty(5)

        shortcutField.addKeyListener(object : KeyListener {
            private var modifiers = 0
            private var keyCode = 0

            override fun keyPressed(e: KeyEvent) {
                modifiers = e.modifiersEx
                keyCode = e.keyCode
                e.consume()
            }

            override fun keyReleased(e: KeyEvent) {
                if (keyCode != 0) {
                    val shortcut = buildShortcutString(modifiers, keyCode)
                    shortcutField.text = shortcut
                    recordedShortcut = shortcut
                    logService.info(i18nService.message("log.shortcut.recorded"), shortcut)
                }
                e.consume()
            }

            override fun keyTyped(e: KeyEvent) {
                e.consume()
            }
        })

        panel.add(instructionLabel, BorderLayout.NORTH)
        panel.add(shortcutField, BorderLayout.CENTER)

        return panel
    }

    /**
     * 构建快捷键字符串
     *
     * @param modifiers 修饰键
     * @param keyCode 键码
     * @return 快捷键字符串
     */
    private fun buildShortcutString(modifiers: Int, keyCode: Int): String {
        val parts = mutableListOf<String>()

        if (modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
            parts.add("ctrl")
        }
        if (modifiers and KeyEvent.META_DOWN_MASK != 0) {
            parts.add("meta")
        }
        if (modifiers and KeyEvent.ALT_DOWN_MASK != 0) {
            parts.add("alt")
        }
        if (modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) {
            parts.add("shift")
        }

        val keyText = KeyEvent.getKeyText(keyCode).lowercase()
        parts.add(keyText)

        return parts.joinToString(" ")
    }

    /**
     * 获取录制的快捷键
     *
     * @return 快捷键字符串
     */
    fun getRecordedShortcut(): String? = recordedShortcut

    /**
     * 获取首选焦点组件
     *
     * @return 焦点组件
     */
    override fun getPreferredFocusedComponent(): JComponent {
        return shortcutField
    }
}
