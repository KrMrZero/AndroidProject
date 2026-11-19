package com.example.coflow.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import com.example.coflow.notifications.NotificationSender
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseTaskRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.TaskRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.TaskUpdateDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class TaskStatusUi {
    TODO,
    IN_PROGRESS,
    DONE,
}

data class TaskDetailUiState(
    val id: String = "",
    val title: String = "",
    val status: TaskStatusUi = TaskStatusUi.TODO,
    val projectName: String = "",
    val description: String = "",
    val author: String = "",
    val assignees: List<String> = emptyList(),
    val dueDateFormatted: String = "",
    val isOverdue: Boolean = false,
    val canEdit: Boolean = false,
    val showBottomAction: Boolean = false,
    val bottomActionText: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
)

class TaskDetailScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository = FirebaseTaskRepositoryImpl(),
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val notificationSender: NotificationSender = NotificationSender(),
) : ViewModel() {
    // События для UI (например, навигация или показ Toast)
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    // Читаем taskId из маршрута "task_detail/{taskId}"
    private val taskId: String = savedStateHandle.get<String>("taskId").orEmpty()

    /** Последняя загруженная задача: нужна для уведомлений при смене статуса. */
    private var loadedTask: TaskModel? = null

    init {
        if (taskId.isNotBlank()) {
            loadTask()
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

            // 1. Загружаем саму задачу из Firestore
            val taskResult = taskRepository.getById(taskId)
            val task =
                taskResult.getOrElse { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                    return@launch
                }

            loadedTask = task

            // 2. Подгружаем связанные данные (проект, автора, исполнителя)
            val project =
                if (task.projectId.isNotBlank()) {
                    projectRepository.getById(task.projectId).getOrNull()
                } else {
                    null
                }
            val projectName = project?.title.orEmpty()

            val authorName =
                if (task.authorId.isNotBlank()) {
                    userRepository
                        .getById(task.authorId)
                        .getOrNull()
                        ?.name
                        .orEmpty()
                } else {
                    ""
                }

            val assigneesList = mutableListOf<String>()
            if (task.assigneeId.isNotBlank()) {
                // У UserModel ID хранится в поле uid
                val assigneeUser = userRepository.getById(task.assigneeId).getOrNull()
                if (assigneeUser != null) {
                    assigneesList.add(if (assigneeUser.uid == currentUid) "Ты" else assigneeUser.name)
                }
            }

            // 3. Вычисляем UI-поля (просрочена ли, формат даты)
            val isOverdue =
                task.deadline.isBefore(
                    LocalDateTime.now(ZoneOffset.UTC),
                ) && task.status != TaskStatus.DONE
            val formattedDate = task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy"))

            val isAuthor = task.authorId == currentUid
            val isAssignee =
                task.assigneeId
                    .split(",")
                    .map { it.trim() }
                    .contains(currentUid)
            val isProjectOwner = project?.ownerId == currentUid && currentUid.isNotBlank()

            val canModifyTask = isAuthor || isAssignee || isProjectOwner

            val (showAction, actionText, canEdit) =
                when (task.status) {
                    TaskStatus.TODO -> Triple(canModifyTask, "Взять задачу", canModifyTask)
                    TaskStatus.IN_PROGRESS -> Triple(canModifyTask, "Выполнить", canModifyTask)
                    TaskStatus.DONE -> Triple(false, "", false)
                }

            // 4. Обновляем стейт
            _state.update {
                it.copy(
                    id = task.id,
                    title = task.title,
                    status =
                        when (task.status) {
                            TaskStatus.TODO -> TaskStatusUi.TODO
                            TaskStatus.IN_PROGRESS -> TaskStatusUi.IN_PROGRESS
                            TaskStatus.DONE -> TaskStatusUi.DONE
                        },
                    projectName = projectName,
                    description = task.description,
                    author = authorName,
                    assignees = assigneesList,
                    dueDateFormatted = formattedDate,
                    isOverdue = isOverdue,
                    canEdit = canEdit,
                    showBottomAction = showAction,
                    bottomActionText = actionText,
                    isLoading = false,
                )
            }
        }
    }

    // --- Обработчики действий ---
    fun onBottomActionClick() {
        val current = _state.value
        if (current.isLoading || !current.showBottomAction) return

        val currentUid = authRepository.getCurrentUserUid().orEmpty()
        val newStatus =
            when (current.status) {
                TaskStatusUi.TODO -> TaskStatus.IN_PROGRESS
                TaskStatusUi.IN_PROGRESS -> TaskStatus.DONE
                TaskStatusUi.DONE -> return
            }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Создаем DTO для обновления задачи
            val updateDto =
                TaskUpdateDto(
                    status = newStatus,
                    // Если берем задачу (переводим в IN_PROGRESS), назначаем себя исполнителем
                    assigneeId = if (newStatus == TaskStatus.IN_PROGRESS) currentUid else null,
                )

            val result = taskRepository.updateTask(taskId, updateDto)

            result.fold(
                onSuccess = {
                    notifyStatusChanged(currentUid, newStatus)
                    loadTask() // Успех: перезагружаем данные
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                },
            )
        }
    }

    fun onEditClick() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.NavigateToEdit(taskId))
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.NavigateBack)
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Уведомляет автора задачи о том, что её взяли в работу или выполнили.
     *
     * Пишем из клиента после успешного обновления статуса. Себя не уведомляем,
     * ошибку записи не показываем — она не должна мешать основному действию.
     */
    private suspend fun notifyStatusChanged(
        actorId: String,
        newStatus: TaskStatus,
    ) {
        val task = loadedTask ?: return
        if (task.authorId.isBlank()) return

        notificationSender.taskStatusChanged(
            taskId = task.id,
            taskTitle = task.title,
            projectId = task.projectId,
            projectName = _state.value.projectName,
            actorId = actorId,
            authorId = task.authorId,
            isDone = newStatus == TaskStatus.DONE,
        )
    }

    // События, которые ViewModel отправляет в Route
    sealed class UiEvent {
        object NavigateBack : UiEvent()

        data class NavigateToEdit(
            val taskId: String,
        ) : UiEvent()
    }
}
