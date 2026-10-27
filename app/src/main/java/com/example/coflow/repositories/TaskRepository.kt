package com.example.coflow.repositories

import com.example.coflow.models.TaskModel

interface TaskRepository {
    suspend fun getById(id: String): Result<TaskModel>

    suspend fun getByTitle(title: String): Result<List<TaskModel>>

    suspend fun getByProjectId(id: String): Result<List<TaskModel>>

    suspend fun addTask(task: TaskModel): Result<TaskModel>

    /**
     * Обновляет существующую задачу по [TaskModel.id].
     * Если документа с таким ID нет, вернёт [Result.failure].
     */
    suspend fun updateTask(task: TaskModel): Result<TaskModel>

    suspend fun deleteTask(id: String): Result<Unit>
}
