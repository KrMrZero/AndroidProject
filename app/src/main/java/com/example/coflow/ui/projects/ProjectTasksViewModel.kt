package com.example.coflow.ui.projects

import androidx.lifecycle.ViewModel
import com.example.coflow.R
import com.example.coflow.models.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// --- Типы для группировки и фильтров ---

enum class ProjectTaskGrouping { STATUS, DEADLINE }

enum class ProjectTaskFilterType { ALL, MINE }

// --- UI-модели ---

data class ProjectTaskRowUi(
    val id: String,
    val title: String,
    val projectName: String,
    val authorId: String,
    val assigneeId: String,
    val dueDateFormatted: String,
    val status: TaskStatus,
    val isDone: Boolean,
    val isOverdue: Boolean,
    val isMine: Boolean = false,
)

data class ProjectTaskGroupUi(
    val id: String,
    val titleResId: Int, // Используем ID ресурса для локализации
    val count: Int,
    val isExpanded: Boolean,
    val tasks: List<ProjectTaskRowUi>,
)

data class ProjectTasksUiState(
    // Данные
    val allTasks: List<ProjectTaskRowUi> = emptyList(),
    val groups: List<ProjectTaskGroupUi> = emptyList(),
    // Настройки
    val currentGrouping: ProjectTaskGrouping = ProjectTaskGrouping.STATUS,
    val currentFilter: ProjectTaskFilterType = ProjectTaskFilterType.ALL,
    // Счетчики
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    // Заглушка для названия проекта (позже будет браться из БД)
    val projectTitle: String = "Название проекта",
)

// --- ViewModel ---

class ProjectTasksViewModel : ViewModel() {
    // По условию задачи: ViewModel пустая, данные будут появляться позже.
    private val _uiState = MutableStateFlow(ProjectTasksUiState())
    val uiState: StateFlow<ProjectTasksUiState> = _uiState.asStateFlow()

    // Смена группировки (По статусу / По сроку)
    fun onGroupingChange(grouping: ProjectTaskGrouping) {
        _uiState.update { it.copy(currentGrouping = grouping) }
        applyFiltersAndGrouping()
    }

    // Смена фильтра (Все / Мои)
    fun onFilterChange(filter: ProjectTaskFilterType) {
        _uiState.update { it.copy(currentFilter = filter) }
        applyFiltersAndGrouping()
    }

    // Свернуть/развернуть группу
    fun onToggleGroup(groupId: String) {
        _uiState.update { state ->
            val updatedGroups =
                state.groups.map { group ->
                    if (group.id == groupId) group.copy(isExpanded = !group.isExpanded) else group
                }
            state.copy(groups = updatedGroups)
        }
    }

    // Внутренняя логика фильтрации и группировки
    private fun applyFiltersAndGrouping() {
        _uiState.update { state ->
            var filtered = state.allTasks

            // Применяем фильтр "Мои"
            if (state.currentFilter == ProjectTaskFilterType.MINE) {
                filtered = filtered.filter { it.isMine }
            }

            // Применяем группировку
            val newGroups =
                when (state.currentGrouping) {
                    ProjectTaskGrouping.STATUS -> groupTasksByStatus(filtered)
                    ProjectTaskGrouping.DEADLINE -> groupTasksByDeadline(filtered)
                }

            state.copy(
                groups = newGroups,
                activeCount = filtered.count { !it.isDone },
                completedCount = filtered.count { it.isDone },
            )
        }
    }

    private fun groupTasksByStatus(tasks: List<ProjectTaskRowUi>): List<ProjectTaskGroupUi> {
        val todo = tasks.filter { it.status == TaskStatus.TODO }
        val inProgress = tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.filter { it.status == TaskStatus.DONE }

        return listOf(
            ProjectTaskGroupUi("TODO", R.string.tasks_group_planned, todo.size, isExpanded = true, tasks = todo),
            ProjectTaskGroupUi("IN_PROGRESS", R.string.tasks_group_active, inProgress.size, isExpanded = false, tasks = inProgress),
            ProjectTaskGroupUi("DONE", R.string.tasks_group_completed, done.size, isExpanded = false, tasks = done),
        )
    }

    private fun groupTasksByDeadline(tasks: List<ProjectTaskRowUi>): List<ProjectTaskGroupUi> {
        val overdue = tasks.filter { it.isOverdue && !it.isDone }
        val upcoming = tasks.filter { !it.isOverdue && !it.isDone }
        val done = tasks.filter { it.isDone }

        return listOf(
            ProjectTaskGroupUi("OVERDUE", R.string.tasks_group_overdue, overdue.size, isExpanded = true, tasks = overdue),
            ProjectTaskGroupUi("UPCOMING", R.string.tasks_group_upcoming, upcoming.size, isExpanded = false, tasks = upcoming),
            ProjectTaskGroupUi("COMPLETED", R.string.tasks_group_completed_deadline, done.size, isExpanded = false, tasks = done),
        )
    }
}
