package com.example.coflow.ui.projects

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isInitialLoading: Boolean = true,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isInvited: Boolean = false,
    val errorMessage: String? = null,
    @param:StringRes val errorMessageResId: Int? = null,
) {
    /** Кнопка «Пригласить» активна, если список участников не пустой и нет процесса загрузки/отправки. */
    val canInvite: Boolean
        get() = participants.isNotEmpty() && !isSubmitting && !isLoading && !isInitialLoading
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

    private val _state = MutableStateFlow(AddProjectParticipantUiState())
    val state: StateFlow<AddProjectParticipantUiState> = _state.asStateFlow()

    // Набор ID пользователей, которые уже являются участниками данного проекта
    private var existingMemberUserIds: Set<String> = emptySet()

    init {
        if (projectId.isBlank()) {
            _state.update {
                it.copy(
                    isInitialLoading = false,
                    errorMessageResId = R.string.error_project_id_missing,
                )
            }
        } else {
            loadExistingMembers()
        }
    }

    /**
     * Загрузка списка уже существующих участников проекта для исключения повторного добавления.
     */
    private fun loadExistingMembers() {
        viewModelScope.launch {
            _state.update { it.copy(isInitialLoading = true) }
            try {
                val result = membershipRepository.getByProjectId(projectId)
                result.fold(
                    onSuccess = { memberships ->
                        existingMemberUserIds = memberships.map { it.userId }.toSet()
                        _state.update { it.copy(isInitialLoading = false) }
                    },
                    onFailure = {
                        _state.update {
                            it.copy(
                                isInitialLoading = false,
                                errorMessage = null,
                                errorMessageResId = R.string.error_system,
                            )
                        }
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isInitialLoading = false,
                        errorMessage = null,
                        errorMessageResId = R.string.error_system,
                    )
                }
            }
        }
    }

    /**
     * Изменение текста в поле ввода почты нового участника.
     */
    fun onEmailQueryChange(query: String) {
        _state.update { it.copy(emailQuery = query, errorMessage = null, errorMessageResId = null) }
    }

    /**
     * Добавление участника по кнопке «+».
     */
    fun onAddParticipantClick() {
        val currentState = _state.value
        if (currentState.isInitialLoading || currentState.isLoading) return

        val currentQuery = currentState.emailQuery.trim()
        if (currentQuery.isBlank()) return

        if (!EMAIL_REGEX.matches(currentQuery)) {
            _state.update {
                it.copy(
                    errorMessage = null,
                    errorMessageResId = R.string.error_invalid_email_format,
                )
            }
            return
        }

        if (currentState.participants.any { it.email.equals(currentQuery, ignoreCase = true) }) {
            _state.update {
                it.copy(
                    errorMessage = null,
                    errorMessageResId = R.string.error_participant_already_in_list,
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, errorMessageResId = null) }
            try {
                val currentUid = authRepository.getCurrentUserUid().orEmpty()

                val userResult = userRepository.getByEmail(currentQuery)
                if (userResult.isFailure) {
                    _state.update {
                        it.copy(
                            errorMessage = null,
                            errorMessageResId = R.string.error_system,
                        )
                    }
                    return@launch
                }

                val user = userResult.getOrNull()
                if (user == null || user.isDeleted || user.uid.isBlank()) {
                    _state.update {
                        it.copy(
                            errorMessage = null,
                            errorMessageResId = R.string.error_user_not_found,
                        )
                    }
                    return@launch
                }

                if (user.uid == currentUid) {
                    _state.update {
                        it.copy(
                            errorMessage = null,
                            errorMessageResId = R.string.error_already_project_member_self,
                        )
                    }
                    return@launch
                }

                if (existingMemberUserIds.contains(user.uid)) {
                    _state.update {
                        it.copy(
                            errorMessage = null,
                            errorMessageResId = R.string.error_already_project_member,
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
                        errorMessage = null,
                        errorMessageResId = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = null,
                        errorMessageResId = R.string.error_system,
                    )
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Удаление участника из подготавливаемого списка.
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
     * Кнопка «Сохранить» (заглушка по текущим требованиям).
     * Выставляет флаг `isInvited = true` в состоянии UI.
     */
    fun onInviteClick() {
        val currentParticipants = _state.value.participants
        if (currentParticipants.isEmpty() || _state.value.isSubmitting) return

        if (projectId.isBlank()) {
            _state.update { it.copy(errorMessageResId = R.string.error_project_id_missing) }
            return
        }

        _state.update { it.copy(isInvited = true) }
    }

    /**
     * Очистка ошибок.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null, errorMessageResId = null) }
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
