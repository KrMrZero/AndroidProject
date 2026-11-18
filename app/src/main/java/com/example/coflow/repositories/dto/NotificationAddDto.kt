package com.example.coflow.repositories.dto

import com.example.coflow.models.NotificationType

/**
 * DTO создания уведомления.
 *
 * Не содержит `id`, `isRead` и `createdAt` — их выставляет репозиторий.
 *
 * @property userId получатель. При создании через
 *   [com.example.coflow.repositories.NotificationRepository.createForUsers]
 *   поле переопределяется для каждого получателя.
 */
data class NotificationAddDto(
    val userId: String = "",
    val type: NotificationType = NotificationType.TASK_ASSIGNED,
    val title: String = "",
    val message: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val entityId: String = "",
    val actorId: String = "",
    val actorName: String = "",
)
