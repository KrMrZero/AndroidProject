package com.example.coflow.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.models.Role
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.MembershipAddDto
import com.example.coflow.repositories.dto.ProjectAddDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Участник, которого приглашают в проект.
 */
data class ProjectParticipant(
    val email: String,
    val initials: String? = null,
)

/**
 * Результат создания проекта.
 */
data class CreateProjectResult(
    val projectId: String,
    val unresolvedEmails: List<String> = emptyList(),
)

data class CreateProjectUiState(
    val title: String = "",
    val description: String = "",
    val participants: List<ProjectParticipant> = emptyList(),
    val participantPendingRemoval: ProjectParticipant? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val canCreate: Boolean
        get() = title.isNotBlank() && !isSubmitting
}

class CreateProjectViewModel(
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateProjectUiState())
    val state: StateFlow<CreateProjectUiState> = _state.asStateFlow()

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value.take(MAX_DESCRIPTION_LENGTH), errorMessage = null) }
    }

    // Синхронизация списка участников из AddProjectParticipantViewModel
    fun syncParticipants(items: List<ProjectParticipantItem>) {
        _state.update { current ->
            current.copy(
                participants =
                    items.map {
                        ProjectParticipant(email = it.email, initials = it.initials)
                    },
            )
        }
    }

    // --- Отмена приглашения ---
    fun onRemoveParticipantClick(participant: ProjectParticipant) {
        _state.update { it.copy(participantPendingRemoval = participant) }
    }

    fun onDismissRemoveParticipantDialog() {
        _state.update { it.copy(participantPendingRemoval = null) }
    }

    fun onConfirmRemoveParticipant() {
        _state.update { it.copy(participantPendingRemoval = null) }
    }

    // --- Создание проекта ---
    fun onCreateProject(onProjectCreated: (CreateProjectResult) -> Unit) {
        val current = _state.value
        val title = current.title.trim()

        if (title.isBlank()) {
            _state.update { it.copy(errorMessage = "Название проекта не может быть пустым") }
            return
        }

        if (current.isSubmitting) return

        _state.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val ownerId = authRepository.getCurrentUserUid()
                if (ownerId.isNullOrBlank()) {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Пользователь не авторизован",
                        )
                    }
                    return@launch
                }

                projectRepository
                    .addProject(
                        ProjectAddDto(
                            ownerId = ownerId,
                            title = title,
                            description = current.description.trim(),
                        ),
                    ).fold(
                        onSuccess = { project ->
                            val unresolvedEmails = addParticipants(project.id, current.participants)
                            _state.update { it.copy(isSubmitting = false) }
                            onProjectCreated(
                                CreateProjectResult(
                                    projectId = project.id,
                                    unresolvedEmails = unresolvedEmails,
                                ),
                            )
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    errorMessage = error.message ?: "Ошибка создания проекта",
                                )
                            }
                        },
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Системная ошибка при создании проекта",
                    )
                }
            }
        }
    }

    private suspend fun addParticipants(
        projectId: String,
        participants: List<ProjectParticipant>,
    ): List<String> {
        val unresolved = mutableListOf<String>()

        participants.forEach { participant ->
            try {
                val user = userRepository.getByEmail(participant.email).getOrNull()

                if (user == null || user.isDeleted || user.uid.isBlank()) {
                    unresolved += participant.email
                    return@forEach
                }

                membershipRepository
                    .addMember(
                        MembershipAddDto(
                            projectId = projectId,
                            userId = user.uid,
                            role = Role.MEMBER,
                        ),
                    ).onFailure {
                        unresolved += participant.email
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                unresolved += participant.email
            }
        }

        return unresolved
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 250
        // EMAIL_REGEX удален, так как он больше не используется в этом классе
    }
}
