
package com.example.coflow.data

import com.example.coflow.models.TaskStatus
import java.time.LocalDate

// Модель одной задачи.
data class Task(
    val id: Int,
    val title: String,
    val projectName: String,
    val dueDate: LocalDate,
    val status: TaskStatus,
    val isMine: Boolean = true,

    val description: String = "",
    val author: String = "",
    val assignees: List<String> = emptyList()
)