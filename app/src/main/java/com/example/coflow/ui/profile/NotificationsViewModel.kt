package com.example.coflow.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * ЗОНА «УВЕДОМЛЕНИЯ» (экран 2 из 3).
 *
 * Не входит в каркас для команды: файл ведётся отдельно.
 * Данные сейчас демонстрационные (loadSampleNotifications), репозитория нет.
 *
 * TODO(уведомления): NotificationRepository.observeByUserId(uid),
 *  запись уведомлений при событиях, сохранение isRead в Firestore.
 */

/** Одно уведомление в списке. */
data class NotificationUi(
    val id: String,
    val projectName: String,
    val message: String,
    val isRead: Boolean,
)

/** Состояние экрана уведомлений. */
data class NotificationsUiState(
    val notifications: List<NotificationUi> = emptyList(),
    val unreadCount: Int = 0,
) {
    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * Логика экрана уведомлений.
 *
 * В проекте пока нет репозитория уведомлений, поэтому список — демонстрационный
 * и хранится только в памяти ViewModel. Когда появится коллекция `notifications`,
 * заменить [loadSampleNotifications] на подписку из репозитория.
 */
class NotificationsViewModel : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        loadSampleNotifications()
    }

    /** Помечает все уведомления прочитанными. */
    fun markAllAsRead() {
        _state.update { current ->
            current.copy(
                notifications = current.notifications.map { it.copy(isRead = true) },
                unreadCount = 0,
            )
        }
    }

    /** Помечает одно уведомление прочитанным. */
    fun markAsRead(id: String) {
        _state.update { current ->
            val updated = current.notifications.map { if (it.id == id) it.copy(isRead = true) else it }
            updated.toState()
        }
    }

    private fun loadSampleNotifications() {
        val sample =
            listOf(
                NotificationUi(
                    id = "1",
                    projectName = "Проект 1",
                    message = "Иван И. назначил вам задачу",
                    isRead = true,
                ),
                NotificationUi(
                    id = "2",
                    projectName = "Проект 2",
                    message = "Мария С. оставила комментарий к задаче",
                    isRead = false,
                ),
                NotificationUi(
                    id = "3",
                    projectName = "Проект 2",
                    message = "Срок задачи истекает завтра",
                    isRead = false,
                ),
            )
        _state.value = sample.toState()
    }

    private fun List<NotificationUi>.toState(): NotificationsUiState =
        NotificationsUiState(
            notifications = this,
            unreadCount = count { !it.isRead },
        )
}
