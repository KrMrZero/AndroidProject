package com.example.coflow.repositories

import com.example.coflow.models.NotificationModel
import com.example.coflow.repositories.dto.NotificationAddDto
import kotlinx.coroutines.flow.Flow

/**
 * Уведомления пользователей (коллекция `notifications`).
 *
 * Уведомление — сохранённый документ на одного получателя, а не событие в UI.
 * Поэтому оно доступно после перезапуска приложения, а прочитанность
 * хранится в самом документе.
 *
 * Порядок работы:
 * 1. После успешного действия (создание задачи, смена статуса) ViewModel
 *    вызывает [create] или [createForUsers].
 * 2. Экран подписывается на [observeByUserId] и показывает список.
 * 3. По этому же списку экран понимает, есть ли непрочитанные, и передаёт
 *    флаг в колокольчики.
 * 4. Пользователь читает — [markAsRead] или [markAllAsRead].
 */
interface NotificationRepository {
    /**
     * Создаёт одно уведомление.
     *
     * @return сохранённая модель с заполненными `id`, `isRead = false`
     *   и `createdAt`.
     */
    suspend fun create(notification: NotificationAddDto): Result<NotificationModel>

    /**
     * Создаёт одинаковое уведомление нескольким получателям — по документу
     * на каждого. Пустые ID, дубликаты и автора действия
     * ([NotificationAddDto.actorId]) репозиторий отбрасывает сам.
     *
     * Пустой список получателей — не ошибка: вернёт [Result.success].
     */
    suspend fun createForUsers(
        userIds: List<String>,
        notification: NotificationAddDto,
    ): Result<Unit>

    /**
     * Наблюдает за уведомлениями пользователя: новые сверху.
     *
     * Холодный [Flow]: при подписке сразу приходит текущий список,
     * затем изменения из Firestore. Пустой список — нормальное состояние.
     */
    fun observeByUserId(userId: String): Flow<Result<List<NotificationModel>>>

    /**
     * Наблюдает за количеством непрочитанных уведомлений.
     *
     * Отдельный запрос нужен, чтобы точка на колокольчике не зависела
     * от лимита загруженного списка.
     */
    fun observeUnreadCount(userId: String): Flow<Int>

    /** Помечает одно уведомление прочитанным. */
    suspend fun markAsRead(id: String): Result<Unit>

    /** Помечает все уведомления пользователя прочитанными. */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /** Удаляет все уведомления пользователя (при удалении аккаунта). */
    suspend fun deleteByUserId(userId: String): Result<Unit>
}
