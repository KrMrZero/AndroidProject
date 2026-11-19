package com.example.coflow.notifications

import android.util.Log
import com.example.coflow.models.NotificationType
import com.example.coflow.models.Role
import com.example.coflow.repositories.FirebaseNotificationRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.NotificationRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.NotificationAddDto
import kotlinx.coroutines.CancellationException

/*
 * Единая точка создания уведомлений.
 *
 * Раньше запись жила в двух ViewModel-ях, и поэтому половина действий
 * (свайп по задаче, редактирование задачи, добавление в проект) проходила
 * мимо уведомлений. Теперь все события идут через [NotificationSender],
 * а экраны только вызывают нужный метод.
 *
 * Ошибка отправки не пробрасывается в основной сценарий: сохранение задачи
 * или проекта должно завершиться, даже если уведомление не записалось.
 * Причина пишется в Logcat с тегом [TAG].
 */

/**
 * Создаёт уведомления от имени текущего пользователя.
 *
 * Себя не уведомляем: получатели, совпадающие с [actorId], отфильтровываются.
 */
class NotificationSender(
    private val notificationRepository: NotificationRepository = FirebaseNotificationRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
) {
    /** Исполнителям назначили задачу. */
    suspend fun taskAssigned(
        taskId: String,
        taskTitle: String,
        projectId: String,
        projectName: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.TASK_ASSIGNED,
            title = "Вам назначили задачу",
            message = "$actorName назначил вам задачу «$taskTitle»",
            projectId = projectId,
            projectName = projectName,
            entityId = taskId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Пользователя сняли с задачи. */
    suspend fun taskUnassigned(
        taskId: String,
        taskTitle: String,
        projectId: String,
        projectName: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.TASK_UNASSIGNED,
            title = "Вас сняли с задачи",
            message = "$actorName снял вас с задачи «$taskTitle»",
            projectId = projectId,
            projectName = projectName,
            entityId = taskId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Задачу автора взяли в работу или выполнили. */
    suspend fun taskStatusChanged(
        taskId: String,
        taskTitle: String,
        projectId: String,
        projectName: String,
        actorId: String,
        authorId: String,
        isDone: Boolean,
    ) = send(listOf(authorId), actorId) { actorName ->
        if (isDone) {
            NotificationAddDto(
                type = NotificationType.TASK_DONE,
                title = "Задачу выполнили",
                message = "$actorName выполнил вашу задачу «$taskTitle»",
                projectId = projectId,
                projectName = projectName,
                entityId = taskId,
                actorId = actorId,
                actorName = actorName,
            )
        } else {
            NotificationAddDto(
                type = NotificationType.TASK_TAKEN,
                title = "Задачу взяли в работу",
                message = "$actorName взял вашу задачу «$taskTitle» в работу",
                projectId = projectId,
                projectName = projectName,
                entityId = taskId,
                actorId = actorId,
                actorName = actorName,
            )
        }
    }

    /** Задачу удалили. */
    suspend fun taskDeleted(
        taskId: String,
        taskTitle: String,
        projectId: String,
        projectName: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.TASK_DELETED,
            title = "Задача удалена",
            message = "$actorName удалил задачу «$taskTitle»",
            projectId = projectId,
            projectName = projectName,
            entityId = taskId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Пользователя добавили в проект. */
    suspend fun addedToProject(
        projectId: String,
        projectTitle: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.ADDED_TO_PROJECT,
            title = "Вас добавили в проект",
            message = "$actorName добавил вас в проект «$projectTitle»",
            projectId = projectId,
            projectName = projectTitle,
            entityId = projectId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Пользователя убрали из проекта. */
    suspend fun removedFromProject(
        projectId: String,
        projectTitle: String,
        actorId: String,
        recipientId: String,
    ) = send(listOf(recipientId), actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.REMOVED_FROM_PROJECT,
            title = "Вас убрали из проекта",
            message = "$actorName убрал вас из проекта «$projectTitle»",
            projectId = projectId,
            projectName = projectTitle,
            entityId = projectId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** В проекте изменили название или описание. */
    suspend fun projectUpdated(
        projectId: String,
        projectTitle: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.PROJECT_UPDATED,
            title = "Проект изменён",
            message = "$actorName обновил проект «$projectTitle»",
            projectId = projectId,
            projectName = projectTitle,
            entityId = projectId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Проект удалили. */
    suspend fun projectDeleted(
        projectId: String,
        projectTitle: String,
        actorId: String,
        recipientIds: List<String>,
    ) = send(recipientIds, actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.PROJECT_DELETED,
            title = "Проект удалён",
            message = "$actorName удалил проект «$projectTitle»",
            projectId = projectId,
            projectName = projectTitle,
            entityId = projectId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /** Пользователю изменили роль в проекте. */
    suspend fun roleChanged(
        projectId: String,
        projectTitle: String,
        actorId: String,
        recipientId: String,
        role: Role,
    ) = send(listOf(recipientId), actorId) { actorName ->
        NotificationAddDto(
            type = NotificationType.ROLE_CHANGED,
            title = "Роль в проекте изменена",
            message = "$actorName изменил вашу роль в проекте «$projectTitle»: ${role.toLabel()}",
            projectId = projectId,
            projectName = projectTitle,
            entityId = projectId,
            actorId = actorId,
            actorName = actorName,
        )
    }

    /**
     * Пишет по одному документу на получателя.
     *
     * @param createNotification вызывается только если есть кому отправлять,
     *   поэтому лишних запросов к базе не будет.
     */
    private suspend fun send(
        recipientIds: List<String>,
        actorId: String,
        createNotification: (actorName: String) -> NotificationAddDto,
    ) {
        val recipients =
            recipientIds
                .map { it.trim() }
                .filter { it.isNotBlank() && it != actorId }
                .distinct()
        if (recipients.isEmpty()) return

        try {
            val actorName = resolveActorName(actorId)
            notificationRepository
                .createForUsers(recipients, createNotification(actorName))
                .onFailure { Log.w(TAG, "Не удалось отправить уведомление", it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось отправить уведомление", e)
        }
    }

    private suspend fun resolveActorName(actorId: String): String {
        if (actorId.isBlank()) return DEFAULT_ACTOR_NAME

        return userRepository
            .getById(actorId)
            .getOrNull()
            ?.name
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ACTOR_NAME
    }

    private fun Role.toLabel(): String =
        when (this) {
            Role.OWNER -> "владелец"
            Role.MEMBER -> "участник"
        }

    private companion object {
        const val TAG = "NotificationSender"
        const val DEFAULT_ACTOR_NAME = "Пользователь"
    }
}
