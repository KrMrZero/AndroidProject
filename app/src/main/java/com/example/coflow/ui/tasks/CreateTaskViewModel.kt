package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CreateTaskFormData(
    val title: String,
    val description: String,
    val assignees: String,
    val project: String,
    val dueDate: String,
)

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val assignees: String = "",
    val project: String = "",
    val dueDate: String = "",
) {
    // Кнопка создания доступна только при заполнении обязательного поля — название
    val canCreate: Boolean get() = title.isNotBlank()
}

class CreateTaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        // Описание ограничено 250 символами, как и в UI
        _state.update { it.copy(description = value.take(250)) }
    }

    fun onAssigneesChange(value: String) {
        _state.update { it.copy(assignees = value) }
    }

    fun onProjectChange(value: String) {
        _state.update { it.copy(project = value) }
    }

    fun onDueDateChange(value: String) {
        _state.update { it.copy(dueDate = value) }
    }

    fun onCreateTask(onTaskCreated: (CreateTaskFormData) -> Unit) {
        val current = _state.value

        // Валидация: название — обязательное поле
        if (current.title.isBlank()) return

        onTaskCreated(
            CreateTaskFormData(
                title = current.title.trim(),
                description = current.description.trim(),
                assignees = current.assignees.trim(),
                project = current.project.trim(),
                dueDate = current.dueDate.trim(),
            )
        )
    }
}
