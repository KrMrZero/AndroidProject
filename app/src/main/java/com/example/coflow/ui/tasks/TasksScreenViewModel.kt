package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TaskGrouping { STATUS, DEADLINE }

data class TaskRowUi(
    val id: String,
    val title: String,
    val projectName: String,
    val authorId: String,
    val assigneeId: String,
    val dueDateFormatted: String,
    val status: TaskStatus,
    val isDone: Boolean,
    val isOverdue: Boolean,
)

data class TaskGroupUi(
    val id: String,
    val title: String,
    val count: Int,
    val isExpanded: Boolean,
    val tasks: List<TaskRowUi>,
)

data class TasksUiState(
    val activeCount: Int,
    val completedCount: Int,
    val groups: List<TaskGroupUi>,
    val currentGrouping: TaskGrouping,
    val selectedFilters: Set<String>,
    val filterButtonText: String,
    val availableFilters: List<String>,
    val isBottomBarVisible: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allTaskRows: List<TaskRowUi> = emptyList(),
)

class TasksScreenViewModel(
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    // Данные для заглушки
    private val stubTasks =
        listOf(
            TaskRowUi(
                "1",
                "Обновить иконки навигации",
                "Дизайн-система",
                "user_1",
                "user_1",
                "30.10.26",
                TaskStatus.TODO,
                isDone = false,
                isOverdue = false,
            ),
            TaskRowUi(
                "2",
                "Согласовать ТЗ с заказчиком",
                "Сайт",
                "user_2",
                "user_1",
                "01.11.26",
                TaskStatus.TODO,
                isDone = false,
                isOverdue = false,
            ),
            TaskRowUi(
                "3",
                "Разработать онбординг",
                "Мобильное приложение",
                "user_1",
                "user_3",
                "24.10.26",
                TaskStatus.IN_PROGRESS,
                isDone = false,
                isOverdue = true,
            ),
            TaskRowUi(
                "4",
                "Настроить CI/CD",
                "Бэкенд",
                "user_3",
                "user_2",
                "28.10.26",
                TaskStatus.IN_PROGRESS,
                isDone = false,
                isOverdue = false,
            ),
            TaskRowUi(
                "5",
                "Провести ревью кода",
                "Бэкенд",
                "user_2",
                "user_1",
                "20.10.26",
                TaskStatus.DONE,
                isDone = true,
                isOverdue = false,
            ),
            TaskRowUi("6", "Написать тесты", "Бэкенд", "user_1", "user_1", "22.10.26", TaskStatus.DONE, isDone = true, isOverdue = false),
        )

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private fun createInitialState(): TasksUiState {
        val activeTasks = stubTasks.count { !it.isDone }
        val completedTasks = stubTasks.count { it.isDone }

        return TasksUiState(
            activeCount = activeTasks,
            completedCount = completedTasks,
            groups = groupTasksByStatus(stubTasks),
            currentGrouping = TaskGrouping.STATUS,
            selectedFilters = emptySet(),
            filterButtonText = "Без фильтра",
            availableFilters = listOf("Без фильтра", "Мои задачи", "Проект: Дизайн-система", "Проект: Мобильное приложение"),
            isBottomBarVisible = true,
            allTaskRows = stubTasks,
        )
    }

    // --- Обработчики событий от UI ---

    fun onGroupingChange(grouping: TaskGrouping) {
        _uiState.update { it.copy(currentGrouping = grouping) }
        applyFiltersAndGrouping()
    }

    fun onFilterToggle(filter: String) {
        _uiState.update { state ->
            val newFilters = state.selectedFilters.toMutableSet()

            if (filter == "Без фильтра") {
                newFilters.clear()
            } else {
                if (newFilters.contains(filter)) {
                    newFilters.remove(filter)
                } else {
                    newFilters.add(filter)
                }
            }

            val buttonText = if (newFilters.isEmpty()) "Без фильтра" else newFilters.joinToString(", ")

            state.copy(
                selectedFilters = newFilters,
                filterButtonText = buttonText,
            )
        }
        applyFiltersAndGrouping()
    }

    fun onToggleGroup(groupId: String) {
        _uiState.update { state ->
            val updatedGroups =
                state.groups.map { group ->
                    if (group.id == groupId) {
                        group.copy(isExpanded = !group.isExpanded)
                    } else {
                        group
                    }
                }
            state.copy(groups = updatedGroups)
        }
    }

    private fun applyFiltersAndGrouping() {
        _uiState.update { state ->
            var filtered = state.allTaskRows

            if (state.selectedFilters.isNotEmpty()) {
                if (state.selectedFilters.contains("Мои задачи")) {
                    val currentUid = "user_1" // Временно для тестов!
                    filtered = filtered.filter { it.authorId == currentUid || it.assigneeId == currentUid }
                }

                val selectedProjects = state.selectedFilters.filter { it.startsWith("Проект: ") }.map { it.removePrefix("Проект: ") }
                if (selectedProjects.isNotEmpty()) {
                    filtered = filtered.filter { selectedProjects.contains(it.projectName) }
                }
            }

            val newGroups =
                when (state.currentGrouping) {
                    TaskGrouping.STATUS -> groupTasksByStatus(filtered)
                    TaskGrouping.DEADLINE -> groupTasksByDeadline(filtered)
                }

            state.copy(
                groups = newGroups,
                activeCount = filtered.count { !it.isDone },
                completedCount = filtered.count { it.isDone },
            )
        }
    }

    fun onNotificationClick() {
        // Заглушка: здесь можно отправить событие или ничего не делать
    }

    // --- Вспомогательные функции для группировки ---

    private fun groupTasksByStatus(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val todo = tasks.filter { it.status == TaskStatus.TODO }
        val inProgress = tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        val done = tasks.filter { it.status == TaskStatus.DONE }

        return listOf(
            TaskGroupUi("TODO", "К исполнению", todo.size, isExpanded = true, tasks = todo),
            TaskGroupUi("IN_PROGRESS", "В процессе", inProgress.size, isExpanded = false, tasks = inProgress),
            TaskGroupUi("DONE", "Сделано", done.size, isExpanded = false, tasks = done),
        )
    }

    private fun groupTasksByDeadline(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val overdue = tasks.filter { it.isOverdue && !it.isDone }
        val upcoming = tasks.filter { !it.isOverdue && !it.isDone }
        val done = tasks.filter { it.isDone }

        return listOf(
            TaskGroupUi("OVERDUE", "Просрочено", overdue.size, isExpanded = true, tasks = overdue),
            TaskGroupUi("UPCOMING", "Предстоящие", upcoming.size, isExpanded = false, tasks = upcoming),
            TaskGroupUi("COMPLETED", "Завершенные", done.size, isExpanded = false, tasks = done),
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
