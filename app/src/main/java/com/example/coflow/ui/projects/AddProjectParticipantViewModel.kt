package com.example.coflow.ui.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Участник, добавляемый в проект на экране «Добавить участника».
 */
data class ProjectParticipantItem(
    val uid: String,
    val email: String,
    val name: String,
    val initials: String,
)

/**
 * Состояние экрана «Добавить участника».
 */
data class AddProjectParticipantUiState(
    val emailQuery: String = "",
    val participants: List<ProjectParticipantItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Кнопка «Пригласить» активна, если список участников не пустой и нет процесса отправки. */
    val canInvite: Boolean
        get() = participants.isNotEmpty() && !isSubmitting && !isLoading
}

/**
 * Одноразовые события UI (например, успешная отправка приглашений).
 */
sealed class AddProjectParticipantUiEvent {
    object InviteSuccess : AddProjectParticipantUiEvent()

    data class ShowToast(
        val message: String,
    ) : AddProjectParticipantUiEvent()
}

/**
 * ViewModel для экрана добавления участников к существующему проекту.
 * Получает `projectId` через [SavedStateHandle] из аргументов навигации.
 */
class AddProjectParticipantViewModel(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val projectId: String = savedStateHandle.get<String>("projectId").orEmpty()

    private val _uiEvent = Channel<AddProjectParticipantUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _state = MutableStateFlow(AddProjectParticipantUiState())
    val state: StateFlow<AddProjectParticipantUiState> = _state.asStateFlow()

    // Набор ID пользователей, которые уже являются участниками данного проекта
    private var existingMemberUserIds: Set<String> = emptySet()

    init {
        if (projectId.isBlank()) {
            _state.update { it.copy(errorMessage = "Не указан ID проекта") }
        } else {
            loadExistingMembers()
        }
    }

    /**
     * Загрузка списка уже существующих участников проекта для исключения повторного добавления.
     */
    private fun loadExistingMembers() {
        viewModelScope.launch {
            membershipRepository.getByProjectId(projectId).onSuccess { memberships ->
                existingMemberUserIds = memberships.map { it.userId }.toSet()
            }
        }
    }

    /**
     * Изменение текста в поле ввода почты нового участника.
     */
    fun onEmailQueryChange(query: String) {
        _state.update { it.copy(emailQuery = query, errorMessage = null) }
    }

    /**
     * Добавление участника по кнопке «+».
     * Выполняет проверки:
     * - Валидность почты
     * - Отсутствие в текущем списке подготавливаемых участников
     * - Существование зарегистрированного пользователя в базе
     * - Проверка, не является ли пользователь текущим пользователем или уже членом проекта
     */
    fun onAddParticipantClick() {
        val currentQuery = _state.value.emailQuery.trim()

        if (currentQuery.isBlank()) return

        if (!EMAIL_REGEX.matches(currentQuery)) {
            _state.update { it.copy(errorMessage = "Некорректный формат электронной почты") }
            return
        }

        if (_state.value.participants.any { it.email.equals(currentQuery, ignoreCase = true) }) {
            _state.update { it.copy(errorMessage = "Этот участник уже добавлен в текущий список") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val userResult = userRepository.getByEmail(currentQuery)
            val user = userResult.getOrNull()

            if (user == null || user.isDeleted || user.uid.isBlank()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Пользователь не найден или не зарегистрирован в системе",
                    )
                }
                return@launch
            }

            val currentUid = authRepository.getCurrentUserUid().orEmpty()
            if (user.uid == currentUid) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Вы уже являетесь участником данного проекта",
                    )
                }
                return@launch
            }

            if (existingMemberUserIds.contains(user.uid)) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Этот пользователь уже состоит в данном проекте",
                    )
                }
                return@launch
            }

            val participantName = user.name.ifBlank { user.email.ifBlank { currentQuery } }
            val initials = getInitials(participantName)

            val newParticipant =
                ProjectParticipantItem(
                    uid = user.uid,
                    email = user.email.ifBlank { currentQuery },
                    name = participantName,
                    initials = initials,
                )

            _state.update { state ->
                state.copy(
                    participants = state.participants + newParticipant,
                    emailQuery = "",
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    /**
     * Удаление участника из текущего списка подготавливаемых участников.
     */
    fun onRemoveParticipant(participant: ProjectParticipantItem) {
        _state.update { state ->
            state.copy(
                participants =
                    state.participants.filterNot {
                        it.uid == participant.uid || it.email.equals(participant.email, ignoreCase = true)
                    },
            )
        }
    }

    /**
     * Функционал рассылки приглашений будет реализован позже.
     */
    fun onInviteClick(onSuccessfullyInvited: (() -> Unit)? = null) {
        onSuccessfullyInvited?.invoke()
    }

    /**
     * Очистка ошибки.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun getInitials(value: String): String {
        val parts = value.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    }
}
