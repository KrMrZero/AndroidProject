package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
data class CreateTaskFormData(
    val title: String, val description: String, val assignees: List<Assignee>,
    val project: String, val dueDate: String
)
data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",
    val assignees: List<Assignee> = emptyList(),
    val project: String = "",
    val dueDate: String = "",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val isSubmitting: Boolean = false,
)

class CreateTaskViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = null)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(
            description = value.take(DESCRIPTION_MAX_LENGTH),
            descriptionError = null
        )
    }

    fun onAssigneesChange(value: List<Assignee>) {
        _state.value = _state.value.copy(assignees = value)
    }

    fun onProjectChange(value: String) {
        _state.value = _state.value.copy(project = value)
    }

    fun onDueDateChange(value: String) {
        _state.value = _state.value.copy(dueDate = value)
    }

    fun onCreateTask(onCreated: (CreateTaskFormData) -> Unit) {
        val current = _state.value

        // Валидация: название обязательно (как и в исходном экране, canCreate = title.isNotBlank())
        val titleError = if (current.title.isBlank()) "Введите название задачи" else null

        if (titleError != null) {
            _state.value = current.copy(titleError = titleError)
            return
        }

        _state.value = current.copy(isSubmitting = true)

        onCreated(
            CreateTaskFormData(
                title = current.title.trim(),
                description = current.description.trim(),
                assignees = current.assignees,
                project = current.project.trim(),
                dueDate = current.dueDate.trim()
            )
        )

        _state.value = _state.value.copy(isSubmitting = false)
    }

    companion object {
        const val DESCRIPTION_MAX_LENGTH = 250
    }
}