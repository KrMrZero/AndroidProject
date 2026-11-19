package com.example.coflow.models

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Уведомление пользователя (коллекция `notifications`).
 *
 * Это не событие в UI, а сохранённый документ: он переживает перезапуск
 * приложения и хранит, прочитал его адресат или нет.
 *
 * Одно уведомление адресовано ровно одному пользователю ([userId]).
 * Если задачу назначили трём исполнителям, в базе будет три документа.
 *
 * @property projectName сохранён на момент события, чтобы список не делал
 *   дополнительных запросов в `projects`.
 * @property entityId ID задачи или проекта — цель перехода по клику.
 * @property actorId кто совершил действие; пусто у системных уведомлений.
 */
data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.TASK_ASSIGNED,
    val title: String = "",
    val message: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val entityId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val isRead: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
