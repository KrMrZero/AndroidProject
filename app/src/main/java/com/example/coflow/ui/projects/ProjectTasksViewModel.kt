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
    val allTasks: List<ProjectTaskRowUi> = emptyList(),
    val groups: List<ProjectTaskGroupUi> = emptyList(),
    val currentGrouping: ProjectTaskGrouping = ProjectTaskGrouping.STATUS,
    val currentFilter: ProjectTaskFilterType = ProjectTaskFilterType.ALL,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val projectTitle: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
)

// --- ViewModel ---

class ProjectTasksViewModel : ViewModel() {
    // По условию задачи: ViewModel пустая, данные будут появляться позже.
    private val _uiState = MutableStateFlow(ProjectTasksUiState())
    val uiState: StateFlow<ProjectTasksUiState> = _uiState.asStateFlow()

    fun onGroupingChange(grouping: ProjectTaskGrouping) {
        _uiState.update { recalculate(it.copy(currentGrouping = grouping)) }
    }

    fun onFilterChange(filter: ProjectTaskFilterType) {
        _uiState.update { recalculate(it.copy(currentFilter = filter)) }
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
    private fun recalculate(state: ProjectTasksUiState): ProjectTasksUiState {
        val filtered =
            when (state.currentFilter) {
                ProjectTaskFilterType.ALL -> state.allTasks
                ProjectTaskFilterType.MINE -> state.allTasks.filter { it.isMine }
            }
        val newGroups =
            when (state.currentGrouping) {
                ProjectTaskGrouping.STATUS -> groupTasksByStatus(filtered, state.groups)
                ProjectTaskGrouping.DEADLINE -> groupTasksByDeadline(filtered, state.groups)
            }
        return state.copy(
            groups = newGroups,
            activeCount = state.allTasks.count { !it.isDone }, // ← от allTasks, не от filtered
            completedCount = state.allTasks.count { it.isDone }, // ← от allTasks
        )
    }

    private fun groupTasksByStatus(
        tasks: List<ProjectTaskRowUi>,
        previousGroups: List<ProjectTaskGroupUi>,
    ): List<ProjectTaskGroupUi> {
        val expandedById = previousGroups.associate { it.id to it.isExpanded }
        val todo = tasks.filter { it.status == TaskStatus.TODO }
        val inProgress = tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.filter { it.status == TaskStatus.DONE }

        return listOf(
            ProjectTaskGroupUi(
                id = "TODO",
                titleResId = R.string.tasks_group_planned,
                count = todo.size,
                isExpanded = expandedById["TODO"] ?: true,
                tasks = todo,
            ),
            ProjectTaskGroupUi(
                id = "IN_PROGRESS",
                titleResId = R.string.tasks_group_active,
                count = inProgress.size,
                isExpanded = expandedById["IN_PROGRESS"] ?: false,
                tasks = inProgress,
            ),
            ProjectTaskGroupUi(
                id = "DONE",
                titleResId = R.string.tasks_group_completed,
                count = done.size,
                isExpanded = expandedById["DONE"] ?: false,
                tasks = done,
            ),
        )
    }

    private fun groupTasksByDeadline(
        tasks: List<ProjectTaskRowUi>,
        previousGroups: List<ProjectTaskGroupUi>,
    ): List<ProjectTaskGroupUi> {
        val expandedById = previousGroups.associate { it.id to it.isExpanded }
        val overdue = tasks.filter { it.isOverdue && !it.isDone }
        val upcoming = tasks.filter { !it.isOverdue && !it.isDone }
        val done = tasks.filter { it.isDone }

        return listOf(
            ProjectTaskGroupUi(
                id = "OVERDUE",
                titleResId = R.string.tasks_group_overdue,
                count = overdue.size,
                isExpanded = expandedById["OVERDUE"] ?: true,
                tasks = overdue,
            ),
            ProjectTaskGroupUi(
                id = "UPCOMING",
                titleResId = R.string.tasks_group_upcoming,
                count = upcoming.size,
                isExpanded = expandedById["UPCOMING"] ?: false,
                tasks = upcoming,
            ),
            ProjectTaskGroupUi(
                id = "COMPLETED",
                titleResId = R.string.tasks_group_completed_deadline,
                count = done.size,
                isExpanded = expandedById["COMPLETED"] ?: false,
                tasks = done,
            ),
        )
    }
}
