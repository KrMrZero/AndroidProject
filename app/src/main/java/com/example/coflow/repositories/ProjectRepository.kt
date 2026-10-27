package com.example.coflow.repositories

import com.example.coflow.models.ProjectModel
import com.example.coflow.repositories.dto.ProjectUpdateDto

/**
 * Проекты (коллекция `projects`).
 *
 * Единый стандарт изменения данных:
 * - [addProject] — создание нового документа;
 * - [updateProject] — частичное обновление по `id`;
 * - [deleteProject] — мягкое удаление.
 *
 * В `updateProject` поля [ProjectUpdateDto], равные `null`, не меняются.
 * `updatedAt` репозиторий обновляет автоматически.
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
     * Частично обновляет проект [id].
     *
     * Меняются только поля [ProjectUpdateDto], отличные от `null`.
     * Если не передано ни одного поля, вернёт [Result.failure].
     */
    suspend fun updateProject(
        id: String,
        update: ProjectUpdateDto,
    ): Result<ProjectModel>

    /**
     * Помечает проект удалённым.
     * Документ остаётся в базе, но пропадает из списков.
     */
    suspend fun deleteProject(id: String): Result<Unit>
}
