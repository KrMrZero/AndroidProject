package com.example.coflow.data.firebase

import com.example.coflow.models.ProjectModel

interface ProjectRepository {

    suspend fun getById(id: String): ProjectModel?
    suspend fun getOwnerId(id: String): List<ProjectModel>
    suspend fun getByUserId(id: String): List<ProjectModel>
    suspend fun addProject(project: ProjectModel): ProjectModel
    suspend fun deleteProject(id: String)

}