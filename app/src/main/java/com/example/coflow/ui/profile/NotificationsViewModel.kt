package com.example.coflow.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.NotificationModel
import com.example.coflow.models.NotificationType
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseNotificationRepositoryImpl
import com.example.coflow.repositories.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Логика экрана уведомлений.
 *
 * Данные приходят из коллекции `notifications` через NotificationRepository.
 * Один экземпляр ViewModel создаётся в MainActivity и используется и экраном,
 * и колокольчиками — поэтому точка непрочитанных везде одинаковая.
 *
 * Что осталось:
 * - переход по клику на задачу/проект по type + entityId;
 * - пагинация и чистка старых уведомлений (сейчас грузим все);
 * - дедлайны и push.
 */

/** Одно уведомление в списке. */
data class NotificationUi(
    val id: String,
    val projectName: String,
    val message: String,
    val isRead: Boolean,
    val type: NotificationType = NotificationType.TASK_ASSIGNED,
    val entityId: String = "",
)

/** Состояние экрана уведомлений. */
data class NotificationsUiState(
    val notifications: List<NotificationUi> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * Логика экрана уведомлений.
 *
 * Подписывается на `notifications` текущего пользователя. Так как Firestore
 * присылает изменения сам, после [markAsRead] список и счётчик обновляются
 * без ручной перезагрузки.
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = FirebaseNotificationRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        subscribe()
    }

    /**
     * Подписка на уведомления текущего пользователя.
     *
     * Вызывается из init, а также после смены пользователя: ViewModel живёт
     * в Activity и переживает выход из аккаунта.
     */
    fun subscribe() {
        observeJob?.cancel()

        val userId = authRepository.getCurrentUserUid().orEmpty()
        if (userId.isBlank()) {
            _state.value = NotificationsUiState(isLoading = false)
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }

        observeJob =
            viewModelScope.launch {
                notificationRepository.observeByUserId(userId).collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            _state.value =
                                NotificationsUiState(
                                    notifications = notifications.map { it.toUi() },
                                    unreadCount = notifications.count { !it.isRead },
                                    isLoading = false,
                                )
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage =
                                        error.message ?: "Не удалось загрузить уведомления",
                                )
                            }
                        },
                    )
                }
            }
    }

    /** Сбрасывает состояние при выходе из аккаунта. */
    fun reset() {
        observeJob?.cancel()
        _state.value = NotificationsUiState(isLoading = false)
    }

    /** Помечает все уведомления прочитанными. */
    fun markAllAsRead() {
        val userId = authRepository.getCurrentUserUid().orEmpty()
        if (userId.isBlank()) return

        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }

    /** Помечает одно уведомление прочитанным. */
    fun markAsRead(id: String) {
        if (id.isBlank()) return

        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }
}

private fun NotificationModel.toUi(): NotificationUi =
    NotificationUi(
        id = id,
        projectName = projectName,
        message = message.ifBlank { title },
        isRead = isRead,
        type = type,
        entityId = entityId,
    )
