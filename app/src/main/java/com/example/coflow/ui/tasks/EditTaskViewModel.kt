package com.example.coflow.ui.tasks

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.notifications.NotificationSender
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.TaskUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val assignees: List<String>,
    val projectName: String,
    val dueDate: String,
    val status: TaskStatusUi = TaskStatusUi.TODO,
)

data class EditTaskUiState(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    override val allAssignees: List<Assignee> = emptyList(),
    override val assigneeSearchQuery: String = "",
    override val pendingAssigneeIds: Set<String> = emptySet(),
    override val isLoadingAssignees: Boolean = false,
    val assignees: String = "",
    val project: String = "",
    val selectedProjectId: String = "",
    override val allProjectItems: List<ProjectItem> = emptyList(),
    override val allProjects: List<String> = emptyList(),
    override val projectSearchQuery: String = "",
    override val pendingProjectId: String = "",
    override val pendingProject: String = "",
    override val selectedDueDate: LocalDate = LocalDate.now(),
    override val calendarMonth: YearMonth = YearMonth.now(),
    override val dueDate: String = "",
    val canSave: Boolean = false,
    val showDeleteDialog: Boolean = false, // <-- Добавлено для управления диалогом из ViewModel
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
) : AssigneeSelectionState,
    DateSelectionState,
    ProjectSelectionState {
    override val filteredAssignees: List<Assignee>
        get() =
            if (assigneeSearchQuery.isBlank()) {
                allAssignees
            } else {
                allAssignees.filter {
                    it.name.contains(assigneeSearchQuery, ignoreCase = true) ||
                        it.email.contains(assigneeSearchQuery, ignoreCase = true)
                }
            }

    override val pendingSelectedCount: Int
        get() = pendingAssigneeIds.size

    override val canConfirmAssignees: Boolean
        get() = pendingAssigneeIds.isNotEmpty()

    override val filteredProjects: List<String>
        get() =
            if (projectSearchQuery.isBlank()) {
                allProjects
            } else {
                allProjects.filter { it.contains(projectSearchQuery, ignoreCase = true) }
            }

    override val canConfirmProject: Boolean
        get() = pendingProject.isNotBlank() || pendingProjectId.isNotBlank()
}

class EditTaskViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val notificationSender: NotificationSender = NotificationSender(),
) : ViewModel() {
    private val _state = MutableStateFlow(EditTaskUiState())
    val state: StateFlow<EditTaskUiState> = _state.asStateFlow()

    // Читаем taskId напрямую из аргументов навигации "edit_task/{taskId}"
    val taskId = savedStateHandle.get<String>("taskId").orEmpty()

    /** Исполнители на момент открытия экрана: уведомляем только новых. */
    private var initialAssigneeIds: Set<String> = emptySet()

    init {
        if (taskId.isNotBlank()) {
            loadTask()
            loadAssignees()
            loadProjects()
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessageResId = R.string.error_task_id_missing,
                )
            }
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUid = authRepository.getCurrentUserUid().orEmpty()

            // 1. Сама задача
            val taskResult = taskRepository.getById(taskId)
            val task =
                taskResult.getOrElse { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Ошибка загрузки задачи",
                        )
                    }
                    return@launch
                }

            // 2. Название проекта
            val projectName =
                if (task.projectId.isNotBlank()) {
                    projectRepository
                        .getById(task.projectId)
                        .getOrNull()
                        ?.title
                        .orEmpty()
                } else {
                    ""
                }

            // 4. Имена исполнителей
            val assigneeNames = mutableListOf<String>()
            if (task.assigneeId.isNotBlank()) {
                val assigneeUser = userRepository.getById(task.assigneeId).getOrNull()
                if (assigneeUser != null) {
                    assigneeNames.add(
                        if (assigneeUser.uid == currentUid) "Ты" else assigneeUser.name,
                    )
                }
            }

            // 5. Формат даты
            val formattedDate =
                task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy"))

            val parsedDeadline = task.deadline.toLocalDate()

            // 6. Публикуем в state
            _state.update {
                it.copy(
                    taskId = task.id,
                    title = task.title,
                    description = task.description,
                    assignees = assigneeNames.joinToString(", "),
                    project = projectName,
                    selectedProjectId = task.projectId,
                    pendingProject = projectName,
                    pendingProjectId = task.projectId,
                    dueDate = formattedDate,
                    selectedDueDate = parsedDeadline,
                    calendarMonth = YearMonth.from(parsedDeadline),
                    canSave = task.title.isNotBlank() && formattedDate.isNotBlank(),
                    isLoading = false,
                    errorMessage = null,
                    pendingAssigneeIds =
                        task.assigneeId
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet(),
                )
            }
            initialAssigneeIds = _state.value.pendingAssigneeIds
            loadAssignees(task.projectId)
        }
    }

    private var loadedProjectId: String = "" // запомним проект задачи для фильтрации исполнителей

    private fun loadAssignees(projectId: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAssignees = true) }
            try {
                val currentUid = authRepository.getCurrentUserUid().orEmpty()
                val userIds = mutableSetOf<String>()
                if (currentUid.isNotBlank()) {
                    userIds.add(currentUid)
                    val targetProjectId = projectId ?: loadedProjectId
                    val projectIds =
                        if (targetProjectId.isNotBlank()) {
                            listOf(targetProjectId)
                        } else {
                            val memberships = membershipRepository.getByUserId(currentUid).getOrDefault(emptyList())
                            val ownerProjects = projectRepository.getByOwnerId(currentUid).getOrDefault(emptyList())
                            val userProjects = projectRepository.getByUserId(currentUid).getOrDefault(emptyList())
                            (memberships.map { it.projectId } + ownerProjects.map { it.id } + userProjects.map { it.id })
                                .filter { it.isNotBlank() }
                                .distinct()
                        }
                    for (pId in projectIds) {
                        projectRepository.getById(pId).getOrNull()?.let { proj ->
                            if (proj.ownerId.isNotBlank()) userIds.add(proj.ownerId)
                        }
                        membershipRepository.getByProjectId(pId).getOrNull()?.forEach { m ->
                            if (m.userId.isNotBlank()) userIds.add(m.userId)
                        }
                    }
                }
                val assignees = mutableListOf<Assignee>()
                for (uid in userIds) {
                    val user = userRepository.getById(uid).getOrNull()
                    val isDeleted = user?.isDeleted ?: false
                    if (!isDeleted) {
                        val displayName =
                            user?.name?.ifBlank { user.email }
                                ?: if (uid == currentUid) "Я" else "Пользователь (${uid.take(6)})"
                        assignees.add(
                            Assignee(
                                id = uid,
                                name = displayName,
                                email = user?.email.orEmpty(),
                                initials = getInitials(displayName),
                                avatarColor = getAvatarColor(uid),
                            ),
                        )
                    }
                }
                _state.update { it.copy(allAssignees = assignees, isLoadingAssignees = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingAssignees = false, errorMessage = "Ошибка загрузки исполнителей: ${e.message}") }
            }
        }
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "??"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    private fun getAvatarColor(id: String): Color {
        val colors =
            listOf(
                Color(0xFF6B9AF3),
                Color(0xFFA3B8CC),
                Color(0xFF86D3B5),
                Color(0xFF8BB8F8),
                Color(0xFFAFA3F8),
                Color(0xFFF39C12),
                Color(0xFFE74C3C),
                Color(0xFF9B59B6),
            )
        val hash = abs(id.hashCode())
        return colors[hash % colors.size]
    }

    fun onAssigneeSearchQueryChange(query: String) {
        _state.update { it.copy(assigneeSearchQuery = query) }
    }

    fun togglePendingAssignee(assigneeId: String) {
        _state.update { current ->
            val pending =
                if (assigneeId in current.pendingAssigneeIds) {
                    current.pendingAssigneeIds - assigneeId
                } else {
                    current.pendingAssigneeIds + assigneeId
                }
            current.copy(pendingAssigneeIds = pending)
        }
    }

    fun confirmAssigneeSelection(assignees: List<Assignee>) {
        _state.update {
            it.copy(
                assignees = assignees.joinToString(", ") { a -> a.name },
                pendingAssigneeIds = assignees.map { it.id }.toSet(),
            )
        }
    }

    fun cancelAssigneeSelection() {
        _state.update { it.copy(pendingAssigneeIds = it.pendingAssigneeIds, assigneeSearchQuery = "") }
    }

    // обработчики для даты

    fun onCalendarMonthChange(month: YearMonth) {
        _state.update { it.copy(calendarMonth = month) }
    }

    fun onDateSelected(date: LocalDate) {
        _state.update { it.copy(selectedDueDate = date) }
    }

    fun confirmDateSelection(dateString: String) {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        _state.update {
            it.copy(
                selectedDueDate = date,
                dueDate = dateString,
                canSave = it.title.isNotBlank() && dateString.isNotBlank(),
            )
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUserUid().orEmpty()
                if (uid.isBlank()) return@launch
                val userProjects = projectRepository.getByUserId(uid).getOrDefault(emptyList())
                val ownerProjects = projectRepository.getByOwnerId(uid).getOrDefault(emptyList())
                val items =
                    (userProjects + ownerProjects)
                        .distinctBy { it.id }
                        .filterNot { it.isDeleted }
                        .map { ProjectItem(id = it.id, title = it.title) }
                _state.update {
                    it.copy(
                        allProjectItems = items,
                        allProjects = items.map { p -> p.title }.distinct(),
                    )
                }
            } catch (_: Exception) {
                // можно логировать
            }
        }
    }

    // обработчики для проектов

    fun onProjectSearchQueryChange(query: String) {
        _state.update { it.copy(projectSearchQuery = query) }
    }

    fun onPendingProjectChange(project: String) {
        val matched = _state.value.allProjectItems.firstOrNull { it.id == project || it.title == project }
        _state.update {
            it.copy(
                pendingProject = matched?.title ?: project,
                pendingProjectId = matched?.id ?: "",
            )
        }
    }

    fun confirmProjectSelection(project: String) {
        val matched = _state.value.allProjectItems.firstOrNull { it.id == project || it.title == project }
        val title = matched?.title ?: project
        val id = matched?.id ?: _state.value.pendingProjectId
        if (title.isNotBlank() || id.isNotBlank()) {
            _state.update {
                it.copy(
                    project = title,
                    selectedProjectId = id,
                )
            }
        }
        _state.update { it.copy(pendingProject = "", pendingProjectId = "") }
    }

    fun onTitleChange(newTitle: String) {
        _state.update { it.copy(title = newTitle, canSave = newTitle.isNotBlank() && it.dueDate.isNotBlank()) }
    }

    fun onDescriptionChange(newDescription: String) {
        _state.update { it.copy(description = newDescription) }
    }

    fun onAssigneesChange(newAssignees: String) {
        _state.update { it.copy(assignees = newAssignees) }
    }

    fun onProjectChange(newProject: String) {
        _state.update { it.copy(project = newProject) }
    }

    fun onDueDateChange(newDate: String) {
        _state.update { it.copy(dueDate = newDate, canSave = it.title.isNotBlank() && newDate.isNotBlank()) }
    }

    fun onSaveTask(onTaskSaved: (Task) -> Unit) {
        val current = _state.value
        if (current.isSaving) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val actorId = authRepository.getCurrentUserUid().orEmpty()
            val updateDto =
                TaskUpdateDto(
                    title = current.title.trim(),
                    description = current.description.trim(),
                    // нужно дополнить для исполнителей, проекта и даты
                    assigneeId = current.pendingAssigneeIds.joinToString(","),
                    deadline = LocalDateTime.of(current.selectedDueDate, LocalTime.MIDNIGHT),
                    projectId = current.selectedProjectId.ifBlank { null },
                )

            val result = taskRepository.updateTask(current.taskId, updateDto)

            result.fold(
                onSuccess = { updated ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            title = updated.title,
                            description = updated.description,
                            canSave = updated.title.isNotBlank(),
                        )
                    }
                    // Сначала уведомления: после onTaskSaved экран закроется
                    // и viewModelScope будет отменён.
                    notifyAssigneeChanges(
                        task = updated,
                        projectName = current.project,
                        actorId = actorId,
                        previousAssigneeIds = initialAssigneeIds,
                        currentAssigneeIds = current.pendingAssigneeIds,
                    )
                    initialAssigneeIds = current.pendingAssigneeIds
                    onTaskSaved(
                        Task(
                            id = updated.id,
                            title = updated.title,
                            description = updated.description,
                            assignees =
                                current.assignees
                                    .split(", ")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() },
                            projectName = current.project,
                            dueDate = current.dueDate,
                        ),
                    )
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message,
                        )
                    }
                },
            )
        }
    }

    /** Уведомляет о добавлении в исполнители и о снятии с задачи. */
    private suspend fun notifyAssigneeChanges(
        task: TaskModel,
        projectName: String,
        actorId: String,
        previousAssigneeIds: Set<String>,
        currentAssigneeIds: Set<String>,
    ) {
        val addedAssigneeIds = currentAssigneeIds - previousAssigneeIds
        if (addedAssigneeIds.isNotEmpty()) {
            notificationSender.taskAssigned(
                taskId = task.id,
                taskTitle = task.title,
                projectId = task.projectId,
                projectName = projectName,
                actorId = actorId,
                recipientIds = addedAssigneeIds.toList(),
            )
        }

        val removedAssigneeIds = previousAssigneeIds - currentAssigneeIds
        if (removedAssigneeIds.isNotEmpty()) {
            notificationSender.taskUnassigned(
                taskId = task.id,
                taskTitle = task.title,
                projectId = task.projectId,
                projectName = projectName,
                actorId = actorId,
                recipientIds = removedAssigneeIds.toList(),
            )
        }
    }

    fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

//    fun confirmDelete(onTaskDeleted: (String) -> Unit) {
//        onTaskDeleted(_state.value.taskId)
//        hideDeleteDialog()
//    }

    fun confirmDelete(onTaskDeleted: (String) -> Unit) {
        val current = _state.value
        if (current.isDeleting) return
        if (current.taskId.isBlank()) {
            _state.update { it.copy(showDeleteDialog = false, errorMessage = "ID задачи не задан") }
            return
        }

        // Закрываем диалог и блокируем повторное нажатие
        _state.update { it.copy(showDeleteDialog = false, isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            val actorId = authRepository.getCurrentUserUid().orEmpty()
            val result = taskRepository.deleteTask(current.taskId)

            result.fold(
                onSuccess = {
                    notificationSender.taskDeleted(
                        taskId = current.taskId,
                        taskTitle = current.title,
                        projectId = current.selectedProjectId,
                        projectName = current.project,
                        actorId = actorId,
                        recipientIds = current.pendingAssigneeIds.toList(),
                    )
                    _state.update { it.copy(isDeleting = false) }
                    onTaskDeleted(current.taskId)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.message ?: "Ошибка удаления задачи",
                        )
                    }
                },
            )
        }
    }
}
