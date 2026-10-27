package com.example.coflow.repositories

import com.example.coflow.models.ProjectModel

/**
 * Проекты (коллекция `projects`).
 *
 * Создание:
 * ```kotlin
 * val project = repository.addProject(
 *     ProjectModel(ownerId = uid, title = "Новый проект"),
 * ).getOrThrow()
 * ```
 *
 * [addProject] всегда создаёт новый документ, поэтому `id` входной модели
 * игнорируется. [deleteProject] выполняет мягкое удаление: ставит
 * `isDeleted = true`, документ остаётся в базе и пропадает из списков.
 *
 * Отдельного метода обновления проекта сейчас нет — для изменения названия или
 * описания потребуется добавить `updateProject()` с `update` по `project.id`.
 */
interface ProjectRepository {
    /**
     * Возвращает проект по ID.
     * Мягко удалённый проект тоже находится — проверяйте [ProjectModel.isDeleted].
     */
    suspend fun getById(id: String): Result<ProjectModel>

    /** Возвращает неудалённые проекты владельца. */
    suspend fun getByOwnerId(ownerId: String): Result<List<ProjectModel>>

    /**
     * Возвращает неудалённые проекты, в которых пользователь состоит.
     * Список участников берётся из коллекции `memberships`.
     */
    suspend fun getByUserId(userId: String): Result<List<ProjectModel>>

    /**
     * Создаёт проект. `ownerId` обязателен, остальные поля берутся из модели.
     * Возвращает сохранённый проект с новым ID.
     */
    suspend fun addProject(project: ProjectModel): Result<ProjectModel>

    /**
     * Помечает проект удалённым.
     * Документ остаётся в базе, но пропадает из списков.
     */
    suspend fun deleteProject(id: String): Result<Unit>
}
