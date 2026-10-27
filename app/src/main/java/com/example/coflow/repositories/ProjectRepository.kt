package com.example.coflow.repositories

import com.example.coflow.models.ProjectModel

interface ProjectRepository {
    suspend fun getById(id: String): Result<ProjectModel>

    suspend fun getByOwnerId(ownerId: String): Result<List<ProjectModel>>

    suspend fun getByUserId(userId: String): Result<List<ProjectModel>>

    suspend fun addProject(project: ProjectModel): Result<ProjectModel>

    /**
     * Помечает проект удалённым.
     * Документ остаётся в базе, но пропадает из списков.
     */
    suspend fun deleteProject(id: String): Result<Unit>
}
