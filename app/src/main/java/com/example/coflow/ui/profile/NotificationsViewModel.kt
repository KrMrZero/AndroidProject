package com.example.coflow.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.NotificationModel
import com.example.coflow.models.NotificationType
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseNotificationRepositoryImpl
import com.example.coflow.repositories.NotificationPage
import com.example.coflow.repositories.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/*
 * Логика экрана уведомлений.
 *
 * Список грузится один раз, дальше живёт в памяти и локальном кэше Firestore.
 * При повторном открытии экрана показываем сохранённое (0 чтений) и
 * запрашиваем с сервера только документы новее последнего известного.
 * Поэтому повторное открытие без новых уведомлений не тратит чтения.
 *
 * Прочитанность — одна отметка `users/{uid}.notificationsReadAt`:
 * уведомление прочитано, если его `createdAt` не позже отметки. Открытие
 * экрана сдвигает отметку, поэтому «прочитать всё» — одна запись.
 */

/** Одно уведомление в списке. */
data class NotificationUi(
    val id: String,
    val projectName: String,
    val message: String,
    val isRead: Boolean,
    val type: NotificationType = NotificationType.TASK_ASSIGNED,
    val entityId: String = "",
    /** Время создания в UTC: по нему строится курсор следующей страницы. */
    val createdAt: Instant = Instant.EPOCH,
)

/** Состояние экрана уведомлений. */
data class NotificationsUiState(
    val notifications: List<NotificationUi> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasUnread: Boolean
        get() = unreadCount > 0
}

/**
 * Логика экрана уведомлений.
 *
 * Экран показывает первую страницу и догружает остальные по кнопке
 * «Показать ещё». Новые уведомления забираются отдельным запросом
 * «новее последнего известного», поэтому уже прочитанные документы
 * повторно не читаются.
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository = FirebaseNotificationRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    /** Текущая отметка «всё до этого момента прочитано». */
    private var readAt: Instant? = null

    /** Отметка уже прочитана из документа пользователя — повторно не читаем. */
    private var readAtKnown: Boolean = false

    private var screenJob: Job? = null
    private var loadMoreJob: Job? = null
    private var markJob: Job? = null

    /**
     * Обновляет только счётчик непрочитанных.
     *
     * Вызывается при старте, возврате в приложение и уходе с экрана:
     * это одно чтение документа пользователя плюс агрегат, а не список.
     */
    fun refreshUnread() {
        val userId = currentUserId() ?: return

        // Пока отметка записывается, читать её с сервера бессмысленно:
        // значение ещё старое, и точка загорится снова.
        if (markJob?.isActive == true) return

        viewModelScope.launch {
            val readAtResult = notificationRepository.getReadAt(userId)
            if (readAtResult.isFailure) {
                Log.w(TAG, "Не удалось прочитать отметку уведомлений", readAtResult.exceptionOrNull())
                return@launch
            }

            val value = readAtResult.getOrNull()
            val unread =
                notificationRepository.countUnread(userId, value).getOrElse { error ->
                    Log.w(TAG, "Не удалось посчитать непрочитанные", error)
                    return@launch
                }

            readAt = value
            readAtKnown = true
            _state.update { it.copy(unreadCount = unread) }
        }
    }

    /**
     * Открытие экрана.
     *
     * 1. Показывает то, что уже есть в памяти или в кэше Firestore — без сети.
     * 2. Запрашивает с сервера только уведомления новее последнего известного.
     * 3. Сдвигает отметку прочитанности, если появилось что-то новое.
     */
    fun openScreen() {
        val userId = currentUserId()
        if (userId == null) {
            _state.value = NotificationsUiState(isLoading = false)
            return
        }

        screenJob?.cancel()
        loadMoreJob?.cancel()

        screenJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, isLoadingMore = false, errorMessage = null) }

                val knownReadAt = loadReadAt(userId)
                val markKnown = readAtKnown

                // 1. Сразу показываем сохранённое: память, затем кэш Firestore.
                var items = _state.value.notifications
                var hasMore = _state.value.hasMore

                if (items.isEmpty()) {
                    val cached = notificationRepository.getCachedPage(userId, PAGE_SIZE).getOrNull()
                    if (cached != null && cached.notifications.isNotEmpty()) {
                        items = cached.notifications.map { it.toUi(knownReadAt) }
                        hasMore = cached.hasMore

                        _state.update {
                            it.copy(
                                notifications = items,
                                isLoading = false,
                                hasMore = hasMore,
                                errorMessage = null,
                            )
                        }
                    }
                }

                // 2. С сервера — только новое. Если новых нет, чтений не будет.
                val newestKnown = items.firstOrNull()?.createdAt
                val freshResult =
                    if (newestKnown != null) {
                        notificationRepository
                            .getNewerThan(userId, newestKnown, PAGE_SIZE)
                            .map { fresh -> FreshNotifications(fresh, hasMore) }
                    } else {
                        notificationRepository
                            .getPage(userId, PAGE_SIZE)
                            .map { page -> FreshNotifications(page.notifications, page.hasMore) }
                    }

                val fresh =
                    freshResult.getOrElse { error ->
                        Log.w(TAG, "Не удалось загрузить уведомления", error)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage =
                                    if (items.isEmpty()) {
                                        error.message ?: "Не удалось загрузить уведомления"
                                    } else {
                                        null
                                    },
                            )
                        }
                        return@launch
                    }

                // 3. Открыл экран — значит прочитал. Одна запись на всё.
                val newest =
                    maxOf(
                        items.firstOrNull()?.createdAt ?: Instant.MIN,
                        fresh.notifications.firstOrNull()?.createdAt?.toInstant(ZoneOffset.UTC) ?: Instant.MIN,
                    )
                val hasNew =
                    markKnown &&
                        newest != Instant.MIN &&
                        (knownReadAt == null || newest.isAfter(knownReadAt))

                var effectiveReadAt = knownReadAt
                if (hasNew) {
                    // Запись не ждём: офлайн она уйдёт в очередь, а Task
                    // завершится только после ответа сервера. Интерфейс
                    // обновляем сразу, сервер догонит.
                    effectiveReadAt = maxOf(Instant.now(), newest)
                    markReadInBackground(userId)
                }
                readAt = effectiveReadAt

                val merged =
                    (fresh.notifications.map { it.toUi(effectiveReadAt) } + items)
                        .distinctBy(NotificationUi::id)
                        .sortedByDescending { it.createdAt }
                        .let { list -> if (hasNew) list.map { it.copy(isRead = true) } else list }

                _state.update {
                    it.copy(
                        notifications = merged,
                        unreadCount = if (hasNew || markKnown) 0 else it.unreadCount,
                        isLoading = false,
                        hasMore = fresh.hasMore,
                        errorMessage = null,
                    )
                }
            }
    }

    /** Догружает следующую страницу по времени последнего уведомления. */
    fun loadMore() {
        val userId = currentUserId() ?: return
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return

        val before = current.notifications.lastOrNull()?.createdAt ?: return

        loadMoreJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoadingMore = true) }

                // Старые страницы не меняются: если они уже в кэше, сервер не нужен.
                val cached = notificationRepository.getCachedPage(userId, PAGE_SIZE, before).getOrNull()
                if (cached != null && cached.notifications.isNotEmpty()) {
                    appendPage(cached)
                    return@launch
                }

                notificationRepository.getPage(userId, PAGE_SIZE, before).fold(
                    onSuccess = { page -> appendPage(page) },
                    onFailure = { error ->
                        Log.w(TAG, "Не удалось загрузить следующую страницу", error)
                        _state.update {
                            it.copy(
                                isLoadingMore = false,
                                errorMessage = error.message ?: "Не удалось загрузить ещё",
                            )
                        }
                    },
                )
            }
    }

    private fun appendPage(page: NotificationPage) {
        _state.update { state ->
            state.copy(
                notifications =
                    (state.notifications + page.notifications.map { it.toUi(readAt) })
                        .distinctBy(NotificationUi::id),
                isLoadingMore = false,
                hasMore = page.hasMore,
            )
        }
    }

    /** Сбрасывает состояние при выходе из аккаунта. */
    fun reset() {
        screenJob?.cancel()
        loadMoreJob?.cancel()
        markJob?.cancel()
        readAt = null
        readAtKnown = false
        _state.value = NotificationsUiState(isLoading = false)
    }

    /** Помечает прочитанным всё, что уже создано (одна запись). */
    fun markAllAsRead() {
        val userId = currentUserId() ?: return

        val newest = _state.value.notifications.firstOrNull()?.createdAt
        readAt = maxOf(Instant.now(), newest ?: Instant.now())
        readAtKnown = true

        _state.update {
            it.copy(
                notifications = it.notifications.map { notification -> notification.copy(isRead = true) },
                unreadCount = 0,
            )
        }

        markReadInBackground(userId)
    }

    /** Читает отметку один раз за жизнь ViewModel, дальше берёт из памяти. */
    private suspend fun loadReadAt(userId: String): Instant? {
        if (readAtKnown) return readAt

        val result = notificationRepository.getReadAt(userId)
        if (result.isSuccess) {
            readAt = result.getOrNull()
            readAtKnown = true
        }
        return readAt
    }

    /**
     * Отправляет отметку в фоне: офлайн запись не должна вешать экран,
     * а её Task завершится только после подтверждения сервера.
     */
    private fun markReadInBackground(userId: String) {
        markJob?.cancel()
        markJob =
            viewModelScope.launch {
                notificationRepository.markAllAsRead(userId).onFailure {
                    Log.w(TAG, "Не удалось сохранить отметку прочитанности", it)
                }
            }
    }

    private fun currentUserId(): String? =
        authRepository.getCurrentUserUid()?.takeIf { it.isNotBlank() }

    private companion object {
        const val TAG = "NotificationsViewModel"

        /** Сколько уведомлений грузим за один раз. */
        const val PAGE_SIZE = 30
    }
}

/** Результат запроса новых документов. */
private data class FreshNotifications(
    val notifications: List<NotificationModel>,
    val hasMore: Boolean,
)

/**
 * Прочитанность вычисляется, а не хранится в документе.
 *
 * Пока отметки нет (старые пользователи), используется прежнее поле `isRead`.
 */
private fun NotificationModel.toUi(readAt: Instant?): NotificationUi =
    NotificationUi(
        id = id,
        projectName = projectName,
        message = message.ifBlank { title },
        isRead = readAt?.let { !createdAt.toInstant(ZoneOffset.UTC).isAfter(it) } ?: isRead,
        type = type,
        entityId = entityId,
        createdAt = createdAt.toInstant(ZoneOffset.UTC),
    )
