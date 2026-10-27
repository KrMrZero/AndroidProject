package com.example.coflow.repositories

import com.example.coflow.models.TaskModel

/**
 * Репозиторий задач (коллекция `tasks`).
 *
 * Все методы suspend и возвращают `Result`.
 * - Создание: [addTask] — новый документ, ID генерируется.
 * - Изменение: [updateTask] — `id` обязателен, меняем через `copy`.
 * - Удаление: [deleteTask] — документ удаляется физически.
 *
 * ```kotlin
 * viewModelScope.launch {
 *     val task = repository.getById(taskId).getOrThrow()
 *     repository.updateTask(task.copy(status = TaskStatus.DONE)).getOrThrow()
 * }
 * ```
 */
interface TaskRepository {
    /** Возвращает задачу или [Result.failure], если документа нет. */
    suspend fun getById(id: String): Result<TaskModel>

    /** Возвращает список задач с указанным заголовком; пустой список — норма. */
    suspend fun getByTitle(title: String): Result<List<TaskModel>>

    /** Возвращает задачи проекта по строковому `projectId`; пустой список — норма. */
    suspend fun getByProjectId(id: String): Result<List<TaskModel>>

    /**
     * Создаёт задачу. `id` входной модели игнорируется — новый ID генерирует Firestore.
     * Возвращает уже сохранённую модель.
     */
    suspend fun addTask(task: TaskModel): Result<TaskModel>

    /**
     * Обновляет существующую задачу по [TaskModel.id].
     *
     * Передавайте изменённую копию:
     * ```kotlin
     * repository.updateTask(task.copy(title = "Новое название"))
     * ```
     * Если документа с таким ID нет, вернёт [Result.failure].
     */
    suspend fun updateTask(task: TaskModel): Result<TaskModel>

    /** Удаляет задачу по ID. Возвращает [Result.failure], если удалить не удалось. */
    suspend fun deleteTask(id: String): Result<Unit>
}
