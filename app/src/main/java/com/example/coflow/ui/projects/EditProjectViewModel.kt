package com.example.coflow.ui.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.R
import com.example.coflow.models.MembershipModel
import com.example.coflow.models.ProjectModel
import com.example.coflow.models.Role
import com.example.coflow.models.UserModel
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseMembershipRepositoryImpl
import com.example.coflow.repositories.FirebaseProjectRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.MembershipRepository
import com.example.coflow.repositories.ProjectRepository
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.MembershipUpdateDto
import com.example.coflow.repositories.dto.ProjectUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectMemberUi(
    val membershipId: String,
    val userId: String,
    val name: String,
    val email: String,
    val initials: String,
    val role: Role,
    val isCurrentUser: Boolean = false,
)

data class EditProjectUiState(
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val members: List<ProjectMemberUi> = emptyList(),
    val canSave: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null,
) {
    val descriptionLength: Int get() = description.length
}

class EditProjectViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository = FirebaseProjectRepositoryImpl(),
    private val membershipRepository: MembershipRepository = FirebaseMembershipRepositoryImpl(),
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {

    private val _state = MutableStateFlow(EditProjectUiState())
    val state: StateFlow<EditProjectUiState> = _state.asStateFlow()

    private val projectId: String = savedStateHandle.get<String>("projectId").orEmpty()

    // Кэш пользователей, чтобы не дёргать Firestore повторно
    private val userCache = mutableMapOf<String, UserModel>()

    init {
        if (projectId.isBlank()) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessageResId = R.string.error_project_id_missing,
                )
            }
        } else {
            loadProject()
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Загружаем сам проект
            val project = projectRepository.getById(projectId).getOrElse { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка загрузки проекта",
                    )
                }
                return@launch
            }

            // 2. Загружаем участников
            val memberships = membershipRepository.getByProjectId(projectId)
                .getOrElse { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Ошибка загрузки участников",
                        )
                    }
                    return@launch
                }

            // 3. Подтягиваем данные пользователей и собираем UI-модели
            val currentUid = authRepository.getCurrentUserUid()
            val membersUi = memberships.map { membership ->
                buildMemberUi(membership, currentUid)
            }

            _state.update {
                it.copy(
                    projectId = project.id,
                    title = project.title,
                    description = project.description,
                    members = membersUi,
                    canSave = project.title.isNotBlank(),
                    isLoading = false,
                )
            }
        }
    }

    private suspend fun buildMemberUi(
        membership: MembershipModel,
        currentUid: String?,
    ): ProjectMemberUi {
        val user = userCache[membership.userId]
            ?: userRepository.getById(membership.userId).getOrNull()?.also {
                userCache[membership.userId] = it
            }

        val name = user?.name?.takeIf { it.isNotBlank() } ?: "Пользователь"
        val email = user?.email.orEmpty()

        return ProjectMemberUi(
            membershipId = membership.id,
            userId = membership.userId,
            name = if (membership.userId == currentUid) "$name (Вы)" else name,
            email = email,
            initials = buildInitials(name),
            role = membership.role,
            isCurrentUser = membership.userId == currentUid,
        )
    }

    private fun buildInitials(name: String): String =
        name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifBlank { "?" }

    // Редактирование полей проекта

    fun onTitleChange(newTitle: String) {
        _state.update {
            it.copy(
                title = newTitle,
                canSave = newTitle.isNotBlank(),
            )
        }
    }

    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length > MAX_DESCRIPTION_LENGTH) return
        _state.update { it.copy(description = newDescription) }
    }

    fun removeMember(membershipId: String) {
        viewModelScope.launch {
            val result = membershipRepository.deleteMember(membershipId)
            result.fold(
                onSuccess = {
                    _state.update { state ->
                        state.copy(
                            members = state.members.filterNot { it.membershipId == membershipId },
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(errorMessage = error.message ?: "Ошибка удаления участника")
                    }
                },
            )
        }
    }

    fun changeMemberRole(membershipId: String, newRole: Role) {
        viewModelScope.launch {
            val result = membershipRepository.updateMember(
                id = membershipId,
                update = MembershipUpdateDto(role = newRole),
            )
            result.fold(
                onSuccess = { updated ->
                    _state.update { state ->
                        state.copy(
                            members = state.members.map { member ->
                                if (member.membershipId == updated.id) {
                                    member.copy(role = updated.role)
                                } else {
                                    member
                                }
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(errorMessage = error.message ?: "Ошибка смены роли")
                    }
                },
            )
        }
    }

    // Сохранение и удаление проекта

    fun onSaveClick(onProjectSaved: () -> Unit) {
        val current = _state.value
        if (current.isSaving) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val updateDto = ProjectUpdateDto(
                title = current.title.trim(),
                description = current.description.trim(),
            )

            val result = projectRepository.updateProject(current.projectId, updateDto)

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
                    onProjectSaved()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Ошибка сохранения проекта",
                        )
                    }
                },
            )
        }
    }

    fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDelete(onProjectDeleted: () -> Unit) {
        val current = _state.value
        if (current.isDeleting || current.projectId.isBlank()) return

        _state.update {
            it.copy(showDeleteDialog = false, isDeleting = true, errorMessage = null)
        }

        viewModelScope.launch {
            val result = projectRepository.deleteProject(current.projectId)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isDeleting = false) }
                    onProjectDeleted()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.message ?: "Ошибка архивации проекта",
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 250
    }
}