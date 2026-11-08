package com.mariam.live.data

import com.mariam.live.model.Notification

class NotificationRepository {

    private val notifications = mutableListOf<Notification>()

    fun getNotifications(userId: String): List<Notification> {
        return notifications.filter { it.userId == userId }
    }

    fun addNotification(notification: Notification) {
        notifications.add(notification)
    }

    fun markAsRead(id: String) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            notifications[index] =
                notifications[index].copy(isRead = true)
        }
    }

    fun deleteNotification(id: String) {
        notifications.removeAll { it.id == id }
    }
}
