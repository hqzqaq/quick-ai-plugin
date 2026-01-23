package com.hqz.quick.ai.quickaiplugin.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 日志和通知服务
 * 提供日志记录和通知功能
 *
 * @see com.intellij.notification.NotificationGroupManager
 * @author huquanzhi
 * @since 2026-01-14 12:00:00
 * @version 1.0
 */
@Service(Service.Level.APP)
class LogService {

    private val logger = Logger.getInstance("QuickAiPlugin")
    private val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup("QuickAiPlugin.Notifications")

    companion object {
        @JvmStatic
        fun getInstance(): LogService {
            return com.intellij.openapi.application.ApplicationManager.getApplication().getService(LogService::class.java)
        }
    }

    /**
     * 记录信息日志
     *
     * @param message 日志消息
     * @param params 参数
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun info(message: String, vararg params: Any) {
        if (params.isEmpty()) {
            logger.info(message)
        } else {
            logger.info(message.format(*params))
        }
    }

    /**
     * 记录警告日志
     *
     * @param message 日志消息
     * @param params 参数
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun warn(message: String, vararg params: Any) {
        if (params.isEmpty()) {
            logger.warn(message)
        } else {
            logger.warn(message.format(*params))
        }
    }

    /**
     * 记录错误日志
     *
     * @param message 日志消息
     * @param throwable 异常
     * @param params 参数
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun error(message: String, throwable: Throwable? = null, vararg params: Any) {
        val formattedMessage = if (params.isEmpty()) message else message.format(*params)
        if (throwable != null) {
            logger.error(formattedMessage, throwable)
        } else {
            logger.error(formattedMessage)
        }
    }

    /**
     * 记录调试日志
     *
     * @param message 日志消息
     * @param params 参数
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    fun debug(message: String, vararg params: Any) {
        if (params.isEmpty()) {
            logger.debug(message)
        } else {
            logger.debug(message.format(*params))
        }
    }

    /**
     * 显示信息通知
     *
     * @param project 项目
     * @param title 标题
     * @param message 消息
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun notifyInfo(project: Project?, title: String, message: String) {
        val notification = notificationGroup.createNotification(title, message, NotificationType.INFORMATION)
        notification.notify(project)
    }

    /**
     * 显示警告通知
     *
     * @param project 项目
     * @param title 标题
     * @param message 消息
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun notifyWarning(project: Project?, title: String, message: String) {
        val notification = notificationGroup.createNotification(title, message, NotificationType.WARNING)
        notification.notify(project)
    }

    /**
     * 显示错误通知
     *
     * @param project 项目
     * @param title 标题
     * @param message 消息
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun notifyError(project: Project?, title: String, message: String) {
        val notification = notificationGroup.createNotification(title, message, NotificationType.ERROR)
        notification.notify(project)
    }

    /**
     * 显示可查看详情的通知
     *
     * @param project 项目
     * @param title 标题
     * @param message 消息
     * @param details 详情
     *
     * Thread Safety Note: 此方法应在 EDT 调用
     */
    fun notifyWithDetails(project: Project?, title: String, message: String, details: String) {
        val notification = notificationGroup.createNotification(title, message, NotificationType.INFORMATION)
        notification.addAction(object : NotificationAction("View Details") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                Messages.showInfoMessage(details, title)
                notification.expire()
            }
        })
        notification.notify(project)
    }

    /**
     * 格式化字符串
     *
     * @param format 格式化字符串
     * @param args 参数
     * @return 格式化后的字符串
     *
     * Thread Safety Note: 此方法是线程安全的
     */
    private fun String.format(vararg args: Any): String {
        return try {
            String.format(this, *args)
        } catch (e: Exception) {
            this
        }
    }
}
