package com.example.coflow.repositories

import com.example.coflow.models.NotificationModel
import com.example.coflow.models.NotificationType
import com.example.coflow.repositories.dto.NotificationAddDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Firestore-реализация [NotificationRepository].
 *
 * Коллекция `notifications`, поля:
 * - `userId` — кому адресовано (строка, а не ссылка: так проще строить запросы);
 * - `type` — строковый код [NotificationType.code];
 * - `isRead` — Boolean, оставлен для старых документов и как запасной
 *   вариант, пока у пользователя нет отметки `notificationsReadAt`;
 * - `createdAt` — Timestamp (UTC, server timestamp).
 *
 * Прочитанность хранится в `users/{uid}.notificationsReadAt`: уведомление
 * прочитано, если `createdAt <= notificationsReadAt`. Поэтому «прочитать всё» —
 * одна запись в документ пользователя, а не обновление каждого уведомления.
 *
 * Список и счётчик используют `orderBy("createdAt", DESC)`, поэтому нужен
 * составной индекс `notifications: userId ASC, createdAt DESC`. Firestore
 * предложит ссылку на его создание при первом запросе (`FAILED_PRECONDITION`).
 *
 * Повторное открытие экрана не перечитывает коллекцию: [getCachedPage] отдаёт
 * сохранённые документы из локального кэша (0 чтений), а [getNewerThan]
 * запрашивает только то, что появилось после последнего известного.
 */
class FirebaseNotificationRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : NotificationRepository {
    private val notificationCollection: CollectionReference = db.collection("notifications")
    private val userCollection: CollectionReference = db.collection("users")

    override suspend fun create(notification: NotificationAddDto): Result<NotificationModel> =
        try {
            if (notification.userId.isBlank()) {
                throw Exception("Не указан получатель уведомления")
            }
            val document = notificationCollection.document()
            document.set(notification.toCreateMap()).await()
            Result.success(notification.toModel(document.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось создать уведомление"))
        }

    override suspend fun createForUsers(
        userIds: List<String>,
        notification: NotificationAddDto,
    ): Result<Unit> {
        return try {
            val recipients =
                userIds
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != notification.actorId }
                    .distinct()

            if (recipients.isEmpty()) return Result.success(Unit)

            // Одна пачка: либо уведомления создадутся всем, либо никому.
            val batch = db.batch()
            recipients.forEach { uid ->
                val document = notificationCollection.document()
                batch.set(document, notification.copy(userId = uid).toCreateMap())
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось создать уведомления"))
        }
    }

    override suspend fun getPage(
        userId: String,
        pageSize: Int,
        before: Instant?,
    ): Result<NotificationPage> {
        if (userId.isBlank() || pageSize <= 0) {
            return Result.success(NotificationPage())
        }

        return try {
            var query =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    // На один больше страницы: так узнаём, есть ли продолжение.
                    .limit((pageSize + 1).toLong())

            if (before != null) {
                query = query.startAfter(before.toTimestamp())
            }

            val documents = query.get().await().documents
            val page = documents.take(pageSize).mapNotNull { it.toNotificationModel() }

            Result.success(
                NotificationPage(
                    notifications = page,
                    hasMore = documents.size > pageSize,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось загрузить уведомления"))
        }
    }

    override suspend fun getCachedPage(
        userId: String,
        pageSize: Int,
        before: Instant?,
    ): Result<NotificationPage> {
        if (userId.isBlank() || pageSize <= 0) {
            return Result.success(NotificationPage())
        }

        return try {
            var query =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit((pageSize + 1).toLong())

            if (before != null) {
                query = query.startAfter(before.toTimestamp())
            }

            // Только локальный кэш: такие чтения не тарифицируются.
            val documents = query.get(Source.CACHE).await().documents

            Result.success(
                NotificationPage(
                    notifications = documents.take(pageSize).mapNotNull { it.toNotificationModel() },
                    // В кэше можем видеть не всё: если страница заполнена,
                    // считаем, что продолжение может быть на сервере.
                    hasMore = documents.size >= pageSize,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Пустой или недоступный кэш — не ошибка: экран загрузит с сервера.
            Result.success(NotificationPage())
        }
    }

    override suspend fun getNewerThan(
        userId: String,
        after: Instant,
        pageSize: Int,
    ): Result<List<NotificationModel>> {
        if (userId.isBlank() || pageSize <= 0) {
            return Result.success(emptyList())
        }

        return try {
            val documents =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .whereGreaterThan("createdAt", after.toTimestamp())
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())
                    .get()
                    .await()
                    .documents

            Result.success(documents.mapNotNull { it.toNotificationModel() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось загрузить новые уведомления"))
        }
    }

    override suspend fun getReadAt(userId: String): Result<Instant?> {
        if (userId.isBlank()) {
            return Result.success(null)
        }

        return try {
            val snapshot = userCollection.document(userId).get().await()
            Result.success(snapshot.getTimestamp(READ_AT_FIELD)?.toInstant())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось прочитать отметку уведомлений"))
        }
    }

    override suspend fun countUnread(
        userId: String,
        readAt: Instant?,
    ): Result<Int> {
        if (userId.isBlank()) {
            return Result.success(0)
        }

        return try {
            val query =
                if (readAt == null) {
                    // Пока отметки нет, считаем по старому полю isRead.
                    notificationCollection
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("isRead", false)
                } else {
                    // orderBy нужен, чтобы счётчик использовал тот же индекс,
                    // что и список: userId + createdAt (desc).
                    notificationCollection
                        .whereEqualTo("userId", userId)
                        .whereGreaterThan("createdAt", readAt.toTimestamp())
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                }

            val count = query.count().get(AggregateSource.SERVER).await().count
            Result.success(count.toInt())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось посчитать непрочитанные"))
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> =
        try {
            if (userId.isBlank()) {
                throw Exception("Не указан пользователь")
            }

            // Одна запись: всё, что создано до этого момента, становится прочитанным.
            userCollection
                .document(userId)
                .update(READ_AT_FIELD, FieldValue.serverTimestamp())
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось отметить уведомления прочитанными"))
        }

    override suspend fun deleteByUserId(userId: String): Result<Unit> =
        try {
            val documents =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .documents

            documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось удалить уведомления"))
        }

    private companion object {
        /** Поле в `users/{uid}` с отметкой прочитанности. */
        const val READ_AT_FIELD = "notificationsReadAt"
    }
}

private fun NotificationAddDto.toCreateMap(): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "type" to type.code,
        "title" to title,
        "message" to message,
        "projectId" to projectId,
        "projectName" to projectName,
        "entityId" to entityId,
        "actorId" to actorId,
        "actorName" to actorName,
        "isRead" to false,
        // Серверное время: прочитанность сравнивается с ним, часы устройства
        // не должны влиять на порядок и на «прочитано».
        "createdAt" to FieldValue.serverTimestamp(),
    )

private fun NotificationAddDto.toModel(id: String): NotificationModel =
    NotificationModel(
        id = id,
        userId = userId,
        type = type,
        title = title,
        message = message,
        projectId = projectId,
        projectName = projectName,
        entityId = entityId,
        actorId = actorId,
        actorName = actorName,
        isRead = false,
        createdAt = LocalDateTime.now(ZoneOffset.UTC),
    )

private fun DocumentSnapshot.toNotificationModel(): NotificationModel? {
    if (!exists()) return null

    val createdAt =
        getTimestamp("createdAt")?.toLocalDateTimeUtc() ?: LocalDateTime.now(ZoneOffset.UTC)

    return NotificationModel(
        id = id,
        userId = getString("userId").orEmpty(),
        type = NotificationType.fromCode(getString("type")),
        title = getString("title").orEmpty(),
        message = getString("message").orEmpty(),
        projectId = getString("projectId").orEmpty(),
        projectName = getString("projectName").orEmpty(),
        entityId = getString("entityId").orEmpty(),
        actorId = getString("actorId").orEmpty(),
        actorName = getString("actorName").orEmpty(),
        isRead = getBoolean("isRead") ?: false,
        createdAt = createdAt,
    )
}

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

private fun Timestamp.toLocalDateTimeUtc(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), ZoneOffset.UTC)

private fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)
