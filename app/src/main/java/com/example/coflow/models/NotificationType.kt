package com.example.coflow.models

/**
 * Тип уведомления.
 *
 * В Firestore хранится строковый [code] — так проще читать базу глазами
 * и добавлять новые типы без миграции числовых кодов.
 *
 * От типа зависит, куда вести пользователя по клику:
 * - задачи (`TASK_*`) — на экран задачи;
 * - проекты (`*_PROJECT`, `ROLE_CHANGED`) — на экран проекта.
 */
enum class NotificationType(
    val code: String,
) {
    /** Пользователю назначили задачу. */
    TASK_ASSIGNED("task_assigned"),

    /** Пользователя сняли с задачи. */
    TASK_UNASSIGNED("task_unassigned"),

    /** Задачу автора взяли в работу. */
    TASK_TAKEN("task_taken"),

    /** Задачу автора выполнили. */
    TASK_DONE("task_done"),

    /** Задачу удалили. */
    TASK_DELETED("task_deleted"),

    /** Скоро срок задачи (пока не создаётся: для этого нужен планировщик). */
    TASK_DEADLINE("task_deadline"),

    /** Пользователя добавили в проект. */
    ADDED_TO_PROJECT("added_to_project"),

    /** Пользователя убрали из проекта. */
    REMOVED_FROM_PROJECT("removed_from_project"),

    /** Изменились данные проекта. */
    PROJECT_UPDATED("project_updated"),

    /** Изменилась роль пользователя в проекте. */
    ROLE_CHANGED("role_changed"),

    /** Проект удалили. */
    PROJECT_DELETED("project_deleted"),
    ;

    companion object {
        /** Неизвестный код не роняет список: считаем уведомление назначением задачи. */
        fun fromCode(value: String?): NotificationType = entries.firstOrNull { it.code == value } ?: TASK_ASSIGNED
    }
}
