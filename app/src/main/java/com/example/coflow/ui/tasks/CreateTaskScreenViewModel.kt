package com.example.coflow.ui.tasks

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class Assignee(val id: String, val name: String, val email: String, val initials: String, val avatarColor: Color)
data class CreateTaskFormData(
    val title: String,
    val description: String,
    val assignees: List<Assignee>,
    val project: String,
    val dueDate: String
)

data class CreateTaskUiState(
    val title: String = "",
    val description: String = "",

    // Уже подтверждённые исполнители задачи
    val assignees: List<Assignee> = emptyList(),

    // Все доступные исполнители
    val allAssignees: List<Assignee> = emptyList(),

    // Временное состояние экрана выбора
    val assigneeSearchQuery: String = "",
    val pendingAssigneeIds: Set<String> = emptySet(),

    val selectedDueDate: LocalDate = LocalDate.now(),
    val calendarMonth: YearMonth = YearMonth.now(),
    val dueDate: String = "",

    val project: String = "",
    val allProjects: List<String> = emptyList(),
    val projectSearchQuery: String = "",
    val pendingProject: String = "",

    val titleError: String? = null,
    val descriptionError: String? = null,
    val isSubmitting: Boolean = false
) {
    val filteredAssignees: List<Assignee>
        get() = if (assigneeSearchQuery.isBlank()) {
            allAssignees
        } else {
            allAssignees.filter {
                it.name.contains(assigneeSearchQuery, ignoreCase = true) ||
                        it.email.contains(assigneeSearchQuery, ignoreCase = true)
            }
        }
    val filteredProjects: List<String>
        get() = if (projectSearchQuery.isBlank()) allProjects
        else allProjects.filter { it.contains(projectSearchQuery, ignoreCase = true) }
    val pendingSelectedCount: Int
        get() = pendingAssigneeIds.size

    val canConfirmAssignees: Boolean
        get() = pendingAssigneeIds.isNotEmpty()

    val canConfirmProject: Boolean get() = pendingProject.isNotBlank()
}

class CreateTaskViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreateTaskUiState())
    val state: StateFlow<CreateTaskUiState> = _state.asStateFlow()

    init {
        loadAssignees()
        loadProjects()
    }

    private fun loadAssignees() {
        // TODO: заменить на вызов репозитория
        val demo = listOf(
            Assignee("1", "Анна Смирнова", "anna.s@example.com", "АС", Color(0xFF6B9AF3)),
            Assignee("2", "Михаил Орлов", "m.orlov@example.com", "МО", Color(0xFFA3B8CC)),
            Assignee("3", "Елена Волкова", "elena.v@example.com", "ЕВ", Color(0xFF86D3B5)),
            Assignee("4", "Дмитрий Соколов", "d.sokolov@example.com", "ДС", Color(0xFF8BB8F8)),
            Assignee("5", "Мария Козлова", "maria.k@example.com", "МК", Color(0xFFAFA3F8))
        )

        _state.value = _state.value.copy(allAssignees = demo)
    }

    private fun loadProjects() {
        // TODO: заменить на вызов репозитория
        val demo = listOf("Проект 1", "Проект 2", "Проект 3", "Проект 4", "Проект 5")
        _state.value = _state.value.copy(allProjects = demo)
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, titleError = null)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(
            description = value.take(DESCRIPTION_MAX_LENGTH),
            descriptionError = null
        )
    }

    fun onOpenProjectPicker() {
        _state.value = _state.value.copy(
            projectSearchQuery = "",
            pendingProject = _state.value.project
        )
    }

    fun onProjectSearchQueryChange(value: String) {
        _state.value = _state.value.copy(projectSearchQuery = value)
    }

    fun onPendingProjectChange(value: String) {
        _state.value = _state.value.copy(pendingProject = value)
    }

    fun confirmProjectSelection() {
        _state.value = _state.value.copy(project = _state.value.pendingProject)
    }

    fun cancelProjectSelection() {
        _state.value = _state.value.copy(
            pendingProject = "",
            projectSearchQuery = ""
        )
    }

    fun onOpenAssigneePicker() {
        _state.value = _state.value.copy(
            assigneeSearchQuery = "",
            pendingAssigneeIds = _state.value.assignees.map { it.id }.toSet()
        )
    }

    fun onOpenDatePicker() {
        val current = _state.value
        val parsed = parseDueDate(current.dueDate)
        val date = parsed ?: current.selectedDueDate
        _state.value = current.copy(
            selectedDueDate = date,
            calendarMonth = YearMonth.from(date)
        )
    }

    fun onCalendarMonthChange(month: YearMonth) {
        _state.value = _state.value.copy(calendarMonth = month)
    }

    fun onDateSelected(date: LocalDate) {
        _state.value = _state.value.copy(selectedDueDate = date)
    }

    fun confirmDateSelection() {
        _state.value = _state.value.copy(
            dueDate = _state.value.selectedDueDate.format(DUE_DATE_FORMATTER)
        )
    }

    private fun parseDueDate(value: String): LocalDate? = try {
        if (value.isBlank()) null else LocalDate.parse(value, DUE_DATE_FORMATTER)
    } catch (e: DateTimeParseException) {
        null
    }

    fun onAssigneeSearchQueryChange(value: String) {
        _state.value = _state.value.copy(assigneeSearchQuery = value)
    }

    fun togglePendingAssignee(id: String) {
        val currentIds = _state.value.pendingAssigneeIds

        _state.value = _state.value.copy(
            pendingAssigneeIds = if (id in currentIds) {
                currentIds - id
            } else {
                currentIds + id
            }
        )
    }

    fun confirmAssigneeSelection() {
        val selected = _state.value.allAssignees.filter {
            it.id in _state.value.pendingAssigneeIds
        }

        _state.value = _state.value.copy(assignees = selected)
    }

    fun cancelAssigneeSelection() {
        _state.value = _state.value.copy(
            pendingAssigneeIds = emptySet(),
            assigneeSearchQuery = ""
        )
    }

    fun onCreateTask(onCreated: (CreateTaskFormData) -> Unit) {
        val current = _state.value
        val titleError = if (current.title.isBlank()) {
            "Введите название задачи"
        } else {
            null
        }

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
        val DUE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}