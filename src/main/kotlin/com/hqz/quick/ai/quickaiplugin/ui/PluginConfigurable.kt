package com.hqz.quick.ai.quickaiplugin.ui

import com.hqz.quick.ai.quickaiplugin.config.PluginSettings
import com.hqz.quick.ai.quickaiplugin.service.FileChooserService
import com.hqz.quick.ai.quickaiplugin.service.IdeCommandService
import com.hqz.quick.ai.quickaiplugin.util.I18nService
import com.hqz.quick.ai.quickaiplugin.util.LogService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import java.util.*

/**
 * 插件设置配置界面
 * 使用 Kotlin UI DSL v2 构建设置页面
 *
 * @see com.intellij.openapi.options.Configurable
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
class PluginConfigurable : Configurable {

    private val ideCommandService = IdeCommandService.getInstance()
    private val fileChooserService = FileChooserService.getInstance()
    private val i18nService = I18nService.getInstance()
    private val logService = LogService.getInstance()
    private lateinit var panel: DialogPanel

    override fun getDisplayName(): String = i18nService.message("plugin.configurable.name")

    override fun createComponent(): DialogPanel {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        val settings = project?.let { PluginSettings.getInstance(it) }

        panel = panel {
            group(i18nService.message("config.language")) {
                row {
                    comboBox(
                        listOf(
                            Pair(Locale.ENGLISH, i18nService.message("config.language.english")),
                            Pair(Locale.SIMPLIFIED_CHINESE, i18nService.message("config.language.chinese"))
                        )
                    ).bindItem(
                        { 
                            val savedLanguage = settings?.state?.language ?: "en"
                            val currentLocale = when (savedLanguage) {
                                "zh" -> Locale.SIMPLIFIED_CHINESE
                                else -> Locale.ENGLISH
                            }
                            when (currentLocale) {
                                Locale.ENGLISH -> Pair(Locale.ENGLISH, i18nService.message("config.language.english"))
                                Locale.SIMPLIFIED_CHINESE -> Pair(Locale.SIMPLIFIED_CHINESE, i18nService.message("config.language.chinese"))
                                else -> Pair(Locale.ENGLISH, i18nService.message("config.language.english"))
                            }
                        },
                        { 
                            it?.first?.let { locale -> i18nService.setLocale(locale) }
                        }
                    )
                }
            }

            group(i18nService.message("config.ide.configuration")) {
                row {
                    text(i18nService.message("config.ide.comment"))
                        .bold()
                }
                separator()
                
                val ideConfigs = settings?.state?.ideConfigs ?: mutableListOf()
                
                if (ideConfigs.isEmpty()) {
                    row {
                        text(i18nService.message("config.ide.empty.list"))
                    }
                } else {
                    ideConfigs.forEachIndexed { index, config ->
                        if (config.isDefault) {
                            row {
                                text(i18nService.message("config.ide.default.label"))
                                    .bold()
                                    .component.apply {
                                        foreground = java.awt.Color(0, 120, 215)
                                    }
                            }
                        }
                        
                        row {
                            textField()
                                .align(AlignX.FILL)
                                .bindText(config::name)
                                .comment(i18nService.message("config.ide.name.placeholder"))
                            
                            val pathField = textField()
                                .align(AlignX.FILL)
                                .component
                                .apply { text = config.path }
                                .also { it.isEditable = false }
                            
                            button(i18nService.message("config.ide.browse")) {
                                val selectedPath = fileChooserService.chooseIdeExecutable(project)
                                if (selectedPath != null) {
                                    if (ideCommandService.validateIdePath(selectedPath)) {
                                        pathField.text = selectedPath
                                        config.path = selectedPath
                                        logService.info(i18nService.message("log.ide.path.valid"), selectedPath)
                                    } else {
                                        logService.warn(i18nService.message("log.ide.path.invalid"), selectedPath)
                                        throw ConfigurationException(i18nService.message("file.chooser.invalid.path"))
                                    }
                                }
                            }
                            
                            if (!config.isDefault) {
                                button(i18nService.message("config.ide.set.default")) {
                                    settings?.state?.setDefaultIdeConfig(config.id)
                                    resetPanel()
                                }
                            }
                            
                            button(i18nService.message("config.ide.remove")) {
                                settings?.state?.removeIdeConfig(config.id)
                                resetPanel()
                            }
                        }
                        
                        if (index < ideConfigs.size - 1) {
                            separator()
                        }
                    }
                }
                
                separator()
                
                row {
                    button(i18nService.message("config.ide.add")) {
                        addNewIde(project, settings)
                    }
                }
            }

            group(i18nService.message("config.shortcut.configuration")) {
                row(i18nService.message("config.shortcut.enable")) {
                    checkBox("")
                        .bindSelected(
                            { settings?.state?.shortcutEnabled ?: true },
                            { settings?.state?.shortcutEnabled = it }
                        )
                }

                row(i18nService.message("config.shortcut.custom")) {
                    val shortcutField = textField()
                        .align(AlignX.FILL)
                        .component
                        .apply { text = settings?.state?.customShortcut ?: "ctrl shift alt button1" }
                        .also { it.isEditable = false }

                    button("Record Shortcut") {
                        val dialog = ShortcutRecorderDialog()
                        if (dialog.showAndGet()) {
                            val recordedShortcut = dialog.getRecordedShortcut()
                            if (recordedShortcut != null) {
                                shortcutField.text = recordedShortcut
                                settings?.state?.customShortcut = recordedShortcut
                                logService.info(i18nService.message("log.shortcut.recorded"), recordedShortcut)
                            }
                        }
                    }
                }

                row {
                    comment(i18nService.message("config.shortcut.default.comment"))
                }
            }

            group(i18nService.message("config.help")) {
                row {
                    text(
                        i18nService.message("config.help.text")
                    )
                }
            }
        }

        return panel
    }

    private fun resetPanel() {
        val parent = panel.parent
        parent.remove(panel)
        panel = createComponent()
        parent.add(panel)
        parent.revalidate()
        parent.repaint()
    }

    private fun addNewIde(project: com.intellij.openapi.project.Project?, settings: PluginSettings?) {
        val newConfig = PluginSettings.IdeConfig(
            id = settings?.state?.generateId() ?: java.util.UUID.randomUUID().toString(),
            name = "",
            path = "",
            isDefault = false
        )
        
        val existingNames = settings?.state?.ideConfigs?.map { it.name }?.toSet() ?: emptySet()
        val dialog = AddIdeDialog(newConfig, project, i18nService, fileChooserService, ideCommandService, existingNames)
        if (dialog.showAndGet()) {
            val config = dialog.getIdeConfig()
            if (config != null) {
                settings?.state?.addIdeConfig(config)
                resetPanel()
            }
        }
    }

    override fun isModified(): Boolean {
        return panel.isModified()
    }

    override fun apply() {
        panel.apply()

        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        val settings = project?.let { PluginSettings.getInstance(it) }

        settings?.state?.ideConfigs?.forEach { config ->
            if (config.path.isNotBlank()) {
                if (!ideCommandService.validateIdePath(config.path)) {
                    logService.error(i18nService.message("log.ide.path.invalid", config.path))
                    throw ConfigurationException(i18nService.message("file.chooser.invalid.path") + ": ${config.path}")
                }
            }
        }
    }

    override fun reset() {
        panel.reset()
    }
}