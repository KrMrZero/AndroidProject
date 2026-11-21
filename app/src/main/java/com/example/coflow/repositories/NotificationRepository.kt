package com.example.coflow.repositories

import com.example.coflow.models.NotificationModel
import com.example.coflow.repositories.dto.NotificationAddDto
import java.time.Instant

/**
 * Уведомления пользователей (коллекция `notifications`).
 *
 * Уведомление — сохранённый документ на одного получателя, а не событие в UI.
 * Поэтому оно доступно после перезапуска приложения.
 *
 * Прочитанность хранится не в каждом документе, а одной отметкой времени
 * `users/{uid}.notificationsReadAt`: уведомление прочитано, если его `createdAt`
 * не позже этой отметки. Так «прочитать всё» стоит одну запись, а не
 * обновление каждого документа.
 *
 * Порядок работы:
 * 1. После успешного действия (создание задачи, смена статуса) ViewModel
 *    вызывает [create] или [createForUsers].
 * 2. Экран показывает сохранённый список ([getCachedPage]) и запрашивает
 *    только новые документы ([getNewerThan]). Полная страница [getPage]
 *    нужна при первом запуске и для «Показать ещё».
 * 3. Колокольчик получает число непрочитанных через [countUnread],
 *    не читая сами документы.
 * 4. Открытие экрана сдвигает отметку через [markAllAsRead].
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
     * Загружает страницу уведомлений: новые сверху.
     *
     * @param pageSize сколько документов вернуть.
     * @param before время последнего уже загруженного уведомления;
     *   `null` — первая страница. Документы ровно с этим временем
     *   не повторяются.
     */
    suspend fun getPage(
        userId: String,
        pageSize: Int,
        before: Instant? = null,
    ): Result<NotificationPage>

    /**
     * Страница из локального кэша Firestore: без обращения к серверу
     * и без чтений. Если кэш пуст, вернёт пустую страницу.
     *
     * @param before время последнего уведомления предыдущей страницы;
     *   `null` — первая страница.
     */
    suspend fun getCachedPage(
        userId: String,
        pageSize: Int,
        before: Instant? = null,
    ): Result<NotificationPage>

    /**
     * Уведомления новее [after] — только новые документы.
     *
     * Если новых нет, запрос не возвращает документов и не тратит чтения.
     */
    suspend fun getNewerThan(
        userId: String,
        after: Instant,
        pageSize: Int,
    ): Result<List<NotificationModel>>

    /**
     * Возвращает отметку `notificationsReadAt` или `null`, если её ещё нет.
     * Это одно чтение документа пользователя, а не всей коллекции.
     */
    suspend fun getReadAt(userId: String): Result<Instant?>

    /**
     * Считает непрочитанные без чтения документов (~1 чтение на 1000 записей).
     *
     * @param readAt отметка прочитанности; `null` — считается по старому
     *   полю `isRead`, пока отметка не создана.
     */
    suspend fun countUnread(
        userId: String,
        readAt: Instant?,
    ): Result<Int>

    /**
     * Помечает прочитанным всё, что уже создано.
     *
     * Записывает одну отметку `notificationsReadAt = serverTimestamp`
     * в документ пользователя, сами уведомления не меняются.
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>

    /** Удаляет все уведомления пользователя (при удалении аккаунта). */
    suspend fun deleteByUserId(userId: String): Result<Unit>
}

/**
 * Страница уведомлений для списка.
 *
 * @property notifications документы страницы, новые сверху.
 * @property hasMore есть ли ещё документы старше последнего в [notifications].
 */
data class NotificationPage(
    val notifications: List<NotificationModel> = emptyList(),
    val hasMore: Boolean = false,
)
