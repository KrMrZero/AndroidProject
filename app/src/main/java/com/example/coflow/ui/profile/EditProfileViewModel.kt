package com.example.coflow.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.UserRepository
import com.example.coflow.repositories.dto.UserUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Логика экрана «Изменение профиля».
 *
 * Уже работает: чтение профиля, валидация полей, сохранение в users/{uid}.
 *
 * TODO(редактирование профиля):
 * - смена почты сейчас меняет только Firestore. Для входа нужен
 *   AuthRepository.updateEmail(...) с повторной аутентификацией;
 * - имя можно дублировать в FirebaseUser.displayName;
 * - ограничение длины имени и проверка занятости почты.
 */

/** Состояние экрана «Изменение профиля». */
data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving && name.isNotBlank() && email.isNotBlank()
}

/**
 * Логика редактирования профиля.
 *
 * Чтение и запись идут через `users/{uid}` ([UserRepository]).
 * Firebase Auth (логин/пароль/email для входа) здесь не меняется — это ограничение
 * текущего [AuthRepository], поэтому меняется только документ профиля.
 */
class EditProfileViewModel(
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    /** Подставляет текущие имя и почту пользователя в форму. */
    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val uid = authRepository.getCurrentUserUid()
            if (uid.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Пользователь не авторизован",
                    )
                }
                return@launch
            }

            userRepository
                .getById(uid)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            name = user.name,
                            email = user.email,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить профиль",
                        )
                    }
                }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    /** Проверяет поля и сохраняет профиль в Firestore. */
    fun onSaveClick() {
        val current = _state.value
        val name = current.name.trim()
        val email = current.email.trim().lowercase()

        val nameError = if (name.isBlank()) "Введите имя" else null
        val emailError =
            when {
                email.isBlank() -> "Введите почту"
                !EMAIL_PATTERN.matches(email) -> "Некорректная почта"
                else -> null
            }

        if (nameError != null || emailError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }

        val uid = authRepository.getCurrentUserUid()
        if (uid.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Пользователь не авторизован") }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            // TODO(редактирование профиля): после успешного сохранения обновить
            //  почту и в Firebase Auth (AuthRepository.updateEmail), иначе вход
            //  останется по старой почте.
            userRepository
                .updateUser(
                    id = uid,
                    update = UserUpdateDto(name = name, email = email),
                ).onSuccess { user ->
                    _state.update {
                        it.copy(
                            name = user.name,
                            email = user.email,
                            isSaving = false,
                            isSaved = true,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Не удалось сохранить профиль",
                        )
                    }
                }
        }
    }

    /** Сбрасывает флаг сохранения после навигации назад. */
    fun consumeSaved() {
        _state.update { it.copy(isSaved = false) }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
