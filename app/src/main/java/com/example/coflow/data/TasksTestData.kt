package com.example.coflow.data

import com.example.coflow.models.TaskStatus
import java.time.LocalDate

data class Task(
    val id: Int,
    val title: String,
    val description: String = "",
    val projectName: String = "",
    val author: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: LocalDate = LocalDate.now(),
    val isMine: Boolean = true
)

object TasksTestData {
    val tasks: List<Task> = listOf(
        Task(
            id = 1,
            title = "Задача 1",
            description = "Описание задачи 1",
            projectName = "Проект 1",
            author = "Участник 1",
            status = TaskStatus.IN_PROGRESS,
            dueDate = LocalDate.now().plusDays(1),
            isMine = true
        ),
        Task(
            id = 2,
            title = "Задача 2",
            description = "Описание задачи 2",
            projectName = "Проект 1",
            author = "Участник 2",
            status = TaskStatus.TODO,
            dueDate = LocalDate.now().plusDays(3),
            isMine = false
        ),
        Task(
            id = 3,
            title = "Задача 3",
            description = "Описание задачи 3",
            projectName = "Проект 2",
            author = "Участник 3",
            status = TaskStatus.DONE,
            dueDate = LocalDate.now().minusDays(1),
            isMine = true
        )
    )
}
