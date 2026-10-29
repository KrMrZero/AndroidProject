package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TaskGrouping { STATUS, DEADLINE }

data class TaskRowUi(
    val id: Int,
    val title: String,
    val projectName: String,
    val dueDateFormatted: String,
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
)

class TasksScreenViewModel : ViewModel() {
    // Данные для заглушки
    private val stubTasks =
        listOf(
            TaskRowUi(1, "Обновить иконки навигации", "Дизайн-система", "30.10.26", isDone = false, isOverdue = false),
            TaskRowUi(2, "Согласовать ТЗ с заказчиком", "Сайт", "01.11.26", isDone = false, isOverdue = false),
            TaskRowUi(3, "Разработать онбординг", "Мобильное приложение", "24.10.26", isDone = false, isOverdue = true),
            TaskRowUi(4, "Настроить CI/CD", "Бэкенд", "28.10.26", isDone = false, isOverdue = false),
            TaskRowUi(5, "Провести ревью кода", "Бэкенд", "20.10.26", isDone = true, isOverdue = false),
            TaskRowUi(6, "Написать тесты", "Бэкенд", "22.10.26", isDone = true, isOverdue = false),
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
        )
    }

    // --- Обработчики событий от UI ---

    fun onGroupingChange(grouping: TaskGrouping) {
        _uiState.update { state ->
            val newGroups =
                when (grouping) {
                    TaskGrouping.STATUS -> groupTasksByStatus(stubTasks)
                    TaskGrouping.DEADLINE -> groupTasksByDeadline(stubTasks)
                }
            state.copy(
                currentGrouping = grouping,
                groups = newGroups,
            )
        }
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

    fun onNotificationClick() {
        // Заглушка: здесь можно отправить событие или ничего не делать
    }

    // --- Вспомогательные функции для группировки ---

    private fun groupTasksByStatus(tasks: List<TaskRowUi>): List<TaskGroupUi> {
        val todo = tasks.filter { !it.isDone && !it.isOverdue }
        // Упрощение для заглушки: считаем "В процессе" те задачи, которые не выполнены, но просрочены
        val inProgress = tasks.filter { !it.isDone && it.isOverdue }
        val done = tasks.filter { it.isDone }

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
}
