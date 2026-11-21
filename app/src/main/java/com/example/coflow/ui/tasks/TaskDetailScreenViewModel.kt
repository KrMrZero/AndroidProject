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
import kotlinx.coroutines.Job
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
    val bottomActionTextResId: Int? = null,
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

    private var observeJob: Job? = null

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
        observeJob?.cancel()
        observeJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null, errorMessageResId = null) }
                val currentUid = authRepository.getCurrentUserUid().orEmpty()

                // СИНХРОНИЗАЦИЯ: Подписываемся на изменения задачи (из refactor)
                taskRepository.observeById(taskId).collect { result ->
                    result.fold(
                        onSuccess = { task ->
                            // ВАЖНО: Сохраняем задачу для NotificationSender (из develop)
                            loadedTask = task

                            val project =
                                if (task.projectId.isNotBlank()) {
                                    projectRepository.getById(task.projectId).getOrNull()
                                } else {
                                    null
                                }

                            val authorName =
                                if (task.authorId.isNotBlank()) {
                                    userRepository.getById(task.authorId).getOrNull()?.name.orEmpty()
                                } else {
                                    ""
                                }

                            // ИСПРАВЛЕНИЕ: Разбиваем assigneeId по запятой (поддержка нескольких исполнителей)
                            val assigneesList = mutableListOf<String>()
                            if (task.assigneeId.isNotBlank()) {
                                val assigneeIds = task.assigneeId.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                for (id in assigneeIds) {
                                    val assigneeUser = userRepository.getById(id).getOrNull()
                                    if (assigneeUser != null) {
                                        assigneesList.add(if (assigneeUser.uid == currentUid) "Ты" else assigneeUser.name)
                                    }
                                }
                            }

                            val isOverdue = task.deadline.isBefore(LocalDateTime.now(ZoneOffset.UTC)) && task.status != TaskStatus.DONE
                            val formattedDate = task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yy"))

                            val isAuthor = task.authorId == currentUid
                            val isAssignee = task.assigneeId.split(",").map { it.trim() }.contains(currentUid)
                            val isProjectOwner = project?.ownerId == currentUid && currentUid.isNotBlank()
                            val canModifyTask = isAuthor || isAssignee || isProjectOwner

                            val (showAction, actionTextResId, canEdit) =
                                when (task.status) {
                                    TaskStatus.TODO -> Triple(canModifyTask, R.string.task_action_button_take, canModifyTask)
                                    TaskStatus.IN_PROGRESS -> Triple(canModifyTask, R.string.task_action_button_complete, canModifyTask)
                                    TaskStatus.DONE -> Triple(false, null, false)
                                }

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
                                    projectName = project?.title.orEmpty(),
                                    description = task.description,
                                    author = authorName,
                                    assignees = assigneesList,
                                    dueDateFormatted = formattedDate,
                                    isOverdue = isOverdue,
                                    canEdit = canEdit,
                                    showBottomAction = showAction,
                                    bottomActionTextResId = actionTextResId,
                                    isLoading = false,
                                )
                            }
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message,
                                    errorMessageResId = if (error.message == null) R.string.error_load_task else null,
                                )
                            }
                        },
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
                TaskStatusUi.IN_PROGRESS -> TaskStatusUi.DONE
                TaskStatusUi.DONE -> return
            }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val updateDto =
                TaskUpdateDto(
                    status = newStatus,
                    assigneeId = if (newStatus == TaskStatus.IN_PROGRESS) currentUid else null,
                )

            taskRepository.updateTask(taskId, updateDto).fold(
                onSuccess = {
                    notifyStatusChanged(currentUid, newStatus)
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                            errorMessageResId = if (error.message == null) R.string.error_update_task else null,
                        )
                    }
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
        _state.update { it.copy(errorMessage = null, errorMessageResId = null) }
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
