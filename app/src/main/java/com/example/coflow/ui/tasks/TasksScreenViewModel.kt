package com.example.coflow.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.TaskStatus
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TaskGrouping { STATUS, DEADLINE }

data class TaskRowUi(
    val id: String,
    val title: String,
    val projectId: String,
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
    private val _uiState =
        MutableStateFlow(
            TasksUiState(
                activeCount = 0,
                completedCount = 0,
                groups = emptyList(),
                currentGrouping = TaskGrouping.STATUS,
                selectedFilters = emptySet(),
                filterButtonText = "Без фильтра",
                availableFilters = listOf("Без фильтра", "Мои задачи"), // Базовые фильтры есть всегда
                isBottomBarVisible = true,
                isLoading = true, // Сразу показываем крутилку
            ),
        )
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null
    private var observeJob: Job? = null

    init {
        loadTasks()
    }

    private fun loadTasks() {
        // Отменяем предыдущую подписку, если она была
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Убираем !!, используем локальную val
            val uid = authRepository.getCurrentUserUid()
            if (uid == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Пользователь не авторизован") }
                return@launch
            }
            currentUid = uid

            // 2. ВОЗВРАЩАЕМ getByOwnerId и загружаем оба списка
            val userProjectsResult = projectRepository.getByUserId(uid)
            val ownerProjectsResult = projectRepository.getByOwnerId(uid)

            // 3. Объединяем обработку ошибок
            if (userProjectsResult.isFailure && ownerProjectsResult.isFailure) {
                val error = userProjectsResult.exceptionOrNull() ?: ownerProjectsResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error?.message ?: "Ошибка загрузки проектов",
                    )
                }
                return@launch
            }

            val projects =
                (userProjectsResult.getOrDefault(emptyList()) + ownerProjectsResult.getOrDefault(emptyList()))
                    .distinctBy { it.id }
                    .filterNot { it.isDeleted }

            val projectMap = projects.associateBy { it.id }

            // Создаем список Flow для каждого проекта
            val taskFlows = projects.map { project ->
                taskRepository.observeTasksByProjectId(project.id)
            }

            // 4. Обновляем availableFilters при пустом списке проектов
            if (taskFlows.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allTaskRows = emptyList(),
                        availableFilters = listOf("Без фильтра", "Мои задачи"),
                    )
                }
                applyFiltersAndGrouping()
                return@launch
            }

            // Используем combine, чтобы собирать данные со всех проектов одновременно
            combine(taskFlows) { resultsArray ->
                resultsArray.flatMap { it.getOrNull() ?: emptyList() }
            }.collect { allFetchedTasks ->
                val taskRows = allFetchedTasks.map { task ->
                    TaskRowUi(
                        id = task.id,
                        projectId = task.projectId,
                        title = task.title,
                        projectName = projectMap[task.projectId]?.title ?: "Без проекта",
                        authorId = task.authorId,
                        assigneeId = task.assigneeId,
                        dueDateFormatted = task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                        status = task.status,
                        isDone = task.status == TaskStatus.DONE,
                        isOverdue = task.deadline.isBefore(LocalDateTime.now(ZoneOffset.UTC)) && task.status != TaskStatus.DONE,
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allTaskRows = taskRows,
                        availableFilters = listOf("Без фильтра", "Мои задачи") +
                                projects.map { "Проект: ${it.title}" },
                    )
                }
                applyFiltersAndGrouping()
            }
        }
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
            val uid = currentUid

            // «Мои задачи»
            if (state.selectedFilters.contains("Мои задачи") && uid != null) {
                filtered = filtered.filter { it.authorId == uid || it.assigneeId == uid }
            }

            // Проекты
            val selectedProjectNames =
                state.selectedFilters
                    .filter { it.startsWith("Проект: ") }
                    .map { it.removePrefix("Проект: ") }

            if (selectedProjectNames.isNotEmpty()) {
                val selectedProjectIds =
                    state.allTaskRows
                        .filter { it.projectName in selectedProjectNames }
                        .map { it.projectId }
                        .toSet()
                filtered = filtered.filter { it.projectId in selectedProjectIds }
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

    // 5. Мёртвый onNotificationClick УДАЛЕН

    fun refresh() {
        loadTasks()
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