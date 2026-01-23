package com.hqz.quick.ai.quickaiplugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.ProjectManager
import java.text.MessageFormat
import java.util.*

/**
 * 国际化工具类
 * 提供多语言支持
 *
 * @see ResourceBundle
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
@Service(Service.Level.APP)
class I18nService {

    private val bundleName = "messages.QuickAiPluginBundle"
    private var currentLocale = Locale.getDefault()

    init {
        loadSavedLanguage()
    }

    companion object {
        @JvmStatic
        fun getInstance(): I18nService {
            return ApplicationManager.getApplication().getService(I18nService::class.java)
        }

        private const val BUNDLE = "messages.QuickAiPluginBundle"
    }

    /**
     * 加载保存的语言设置
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    private fun loadSavedLanguage() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val settings = com.hqz.quick.ai.quickaiplugin.config.PluginSettings.getInstance(project)
            currentLocale = when (settings.state.language) {
                "zh" -> Locale.SIMPLIFIED_CHINESE
                else -> Locale.ENGLISH
            }
        }
    }

    /**
     * 获取本地化消息
     *
     * @param key 消息键
     * @param params 参数
     * @return 本地化消息
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun message(key: String, vararg params: Any): String {
        val bundle = ResourceBundle.getBundle(BUNDLE, currentLocale)
        val pattern = bundle.getString(key)
        
        return if (params.isEmpty()) {
            pattern
        } else {
            try {
                MessageFormat.format(pattern, *params)
            } catch (e: Exception) {
                pattern
            }
        }
    }

    /**
     * 设置语言
     *
     * @param locale 语言环境
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun setLocale(locale: Locale) {
        currentLocale = locale
        
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project != null) {
            val settings = com.hqz.quick.ai.quickaiplugin.config.PluginSettings.getInstance(project)
            settings.state.language = when (locale) {
                Locale.ENGLISH -> "en"
                Locale.SIMPLIFIED_CHINESE -> "zh"
                else -> "en"
            }
        }
    }

    /**
     * 获取当前语言
     *
     * @return 当前语言环境
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun getCurrentLocale(): Locale = currentLocale

    /**
     * 切换到英文
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun setEnglish() {
        setLocale(Locale.ENGLISH)
    }

    /**
     * 切换到中文
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun setChinese() {
        setLocale(Locale.SIMPLIFIED_CHINESE)
    }
}
