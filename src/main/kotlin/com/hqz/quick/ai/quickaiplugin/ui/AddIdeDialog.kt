package com.hqz.quick.ai.quickaiplugin.ui

import com.hqz.quick.ai.quickaiplugin.config.PluginSettings
import com.hqz.quick.ai.quickaiplugin.service.FileChooserService
import com.hqz.quick.ai.quickaiplugin.service.IdeCommandService
import com.hqz.quick.ai.quickaiplugin.util.I18nService
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

/**
 * 添加 IDE 配置对话框
 * 用于添加新的 IDE 配置，包括 IDE 名称和路径
 *
 * @see com.intellij.openapi.ui.DialogWrapper
 * @author huquanzhi
 * @since 2026-01-23 12:00:00
 * @version 1.0
 */
class AddIdeDialog(
    private var config: PluginSettings.IdeConfig,
    private val project: Project?,
    private val i18nService: I18nService,
    private val fileChooserService: FileChooserService,
    private val ideCommandService: IdeCommandService,
    private val existingNames: Set<String> = emptySet()
) : DialogWrapper(true) {

    private val nameField = JBTextField()
    private val pathField = JBTextField()
    private var isValidConfig = false

    init {
        title = i18nService.message("config.ide.add")
        setOKButtonText(i18nService.message("config.ide.add"))
        setCancelButtonText(i18nService.message("shortcut.recording.cancel"))
        init()
        nameField.text = config.name
        pathField.text = config.path
        pathField.isEditable = false
        validateInput()
    }

    /**
     * 创建中心面板
     *
     * @return 中心面板
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(10)

        val nameLabel = JBLabel(i18nService.message("config.ide.name"))
        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(nameLabel)
        panel.add(Box.createVerticalStrut(5))

        nameField.preferredSize = Dimension(400, 30)
        nameField.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(nameField)
        panel.add(Box.createVerticalStrut(15))

        val pathLabel = JBLabel(i18nService.message("config.ide.path"))
        pathLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(pathLabel)
        panel.add(Box.createVerticalStrut(5))

        val pathPanel = JPanel(BorderLayout())
        pathPanel.alignmentX = Component.LEFT_ALIGNMENT
        pathPanel.maximumSize = Dimension(Int.MAX_VALUE, 30)

        pathField.preferredSize = Dimension(300, 30)
        pathPanel.add(pathField, BorderLayout.CENTER)

        val browseButton = JButton(i18nService.message("config.ide.browse"))
        browseButton.addActionListener {
            val selectedPath = fileChooserService.chooseIdeExecutable(project)
            if (selectedPath != null) {
                if (ideCommandService.validateIdePath(selectedPath)) {
                    pathField.text = selectedPath
                    config.path = selectedPath
                    validateInput()
                } else {
                    throw ConfigurationException(i18nService.message("file.chooser.invalid.path"))
                }
            }
        }
        pathPanel.add(browseButton, BorderLayout.EAST)
        panel.add(pathPanel)
        panel.add(Box.createVerticalStrut(10))

        val namePlaceholder = JBLabel(i18nService.message("config.ide.name.placeholder"))
        namePlaceholder.alignmentX = Component.LEFT_ALIGNMENT
        namePlaceholder.font = namePlaceholder.font.deriveFont(11f)
        panel.add(namePlaceholder)

        return panel
    }

    /**
     * 验证输入
     *
     * @return 是否有效
     */
    override fun doValidate(): ValidationInfo? {
        val name = nameField.text.trim()
        
        if (name.isBlank()) {
            return ValidationInfo(i18nService.message("config.ide.validate.name"), nameField)
        }

        if (existingNames.contains(name) && name != config.name) {
            return ValidationInfo(i18nService.message("config.ide.validate.duplicate"), nameField)
        }

        if (pathField.text.isBlank()) {
            return ValidationInfo(i18nService.message("config.ide.validate.path"), pathField)
        }

        return null
    }

    /**
     * 执行确定操作
     */
    override fun doOKAction() {
        config.name = nameField.text.trim()
        config.path = pathField.text.trim()
        super.doOKAction()
    }

    /**
     * 获取 IDE 配置
     *
     * @return IDE 配置
     */
    fun getIdeConfig(): PluginSettings.IdeConfig? {
        return if (isValidConfig) config else null
    }

    /**
     * 获取首选焦点组件
     *
     * @return 焦点组件
     */
    override fun getPreferredFocusedComponent(): JComponent {
        return nameField
    }

    /**
     * 验证输入并更新状态
     */
    private fun validateInput() {
        isValidConfig = nameField.text.isNotBlank() && pathField.text.isNotBlank()
        isOKActionEnabled = isValidConfig
    }
}
