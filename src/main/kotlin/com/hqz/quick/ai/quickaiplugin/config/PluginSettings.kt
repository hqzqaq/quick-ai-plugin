package com.hqz.quick.ai.quickaiplugin.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * 插件配置状态类，用于持久化用户配置
 *
 * @see com.intellij.openapi.components.PersistentStateComponent
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
@State(name = "QuickAiPluginSettings", storages = [Storage("QuickAiPluginSettings.xml")])
@Service(Service.Level.PROJECT)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class IdeConfig(
        var id: String = "",
        var name: String = "",
        var path: String = "",
        var isDefault: Boolean = false
    )

    data class State(
        var idePath: String = "",
        var defaultIde: String = "VS Code",
        var shortcutEnabled: Boolean = true,
        var customShortcut: String = "ctrl shift alt button1",
        var language: String = "en",
        var ideConfigs: MutableList<IdeConfig> = mutableListOf()
    ) {
        init {
            migrateLegacyData()
        }

        private fun migrateLegacyData() {
            if (ideConfigs.isEmpty() && idePath.isNotEmpty()) {
                val defaultIdeName = when (defaultIde) {
                    "VS Code" -> "VS Code"
                    "Cursor" -> "Cursor"
                    "Trae" -> "Trae"
                    else -> defaultIde
                }
                ideConfigs.add(IdeConfig(id = generateId(), name = defaultIdeName, path = idePath, isDefault = true))
                idePath = ""
            }
        }

        fun generateId(): String = java.util.UUID.randomUUID().toString()

        fun getDefaultIdeConfig(): IdeConfig? = ideConfigs.firstOrNull { it.isDefault }

        fun setDefaultIdeConfig(id: String) {
            ideConfigs.forEach { it.isDefault = (it.id == id) }
        }

        fun addIdeConfig(config: IdeConfig) {
            if (ideConfigs.isEmpty()) {
                config.isDefault = true
            }
            ideConfigs.add(config)
        }

        fun removeIdeConfig(id: String) {
            val removed = ideConfigs.removeIf { it.id == id }
            if (removed && ideConfigs.isNotEmpty() && ideConfigs.none { it.isDefault }) {
                ideConfigs[0].isDefault = true
            }
        }
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): PluginSettings {
            return project.getService(PluginSettings::class.java)
        }
    }
}
