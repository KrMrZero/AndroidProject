package com.example.coflow.repositories

import com.example.coflow.models.NotificationModel
import com.example.coflow.models.NotificationType
import com.example.coflow.repositories.dto.NotificationAddDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * - `isRead` — Boolean;
 * - `createdAt` — Timestamp (UTC).
 *
 * Для запросов достаточно фильтра по `userId`: сортировка выполняется на клиенте,
 * поэтому составные индексы не нужны. Если позже появятся пагинация или
 * серверная сортировка, понадобится индекс `userId + createdAt (desc)`. */
class FirebaseNotificationRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : NotificationRepository {
    private val notificationCollection: CollectionReference = db.collection("notifications")

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

    override fun observeByUserId(userId: String): Flow<Result<List<NotificationModel>>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(Result.success(emptyList()))
                close()
                return@callbackFlow
            }

            val registration =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(
                                Result.failure(
                                    Exception(error.message ?: "Ошибка наблюдения за уведомлениями"),
                                ),
                            )
                            close()
                        } else {
                            trySend(
                                Result.success(
                                    snapshot
                                        ?.documents
                                        ?.mapNotNull { it.toNotificationModel() }
                                        // Сортируем на клиенте: так не нужен составной индекс.
                                        ?.sortedByDescending { it.createdAt }
                                        .orEmpty(),
                                ),
                            )
                        }
                    }

            awaitClose { registration.remove() }
        }

    override fun observeUnreadCount(userId: String): Flow<Int> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(0)
                close()
                return@callbackFlow
            }

            val registration =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isRead", false)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // Счётчик не критичен: не роняем экран, просто показываем 0.
                            trySend(0)
                        } else {
                            trySend(snapshot?.size() ?: 0)
                        }
                    }

            awaitClose { registration.remove() }
        }

    override suspend fun markAsRead(id: String): Result<Unit> =
        try {
            notificationCollection.document(id).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось отметить уведомление прочитанным"))
        }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val unread =
                notificationCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                    .documents

            if (unread.isEmpty()) return Result.success(Unit)

            val batch = db.batch()
            unread.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Не удалось отметить уведомления прочитанными"))
        }
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
        "createdAt" to Timestamp.now(),
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
        getTimestamp("createdAt")?.let {
            LocalDateTime.ofInstant(
                Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()),
                ZoneOffset.UTC,
            )
        } ?: LocalDateTime.now(ZoneOffset.UTC)

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
