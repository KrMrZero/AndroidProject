package com.example.coflow.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coflow.repositories.AuthRepository
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.repositories.FirebaseUserRepositoryImpl
import com.example.coflow.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Логика главного экрана профиля.
 *
 * Данные берутся из `users/{uid}`: UID — из AuthRepository, профиль — из UserRepository.
 * Выход выполняет AuthRepository.logout() и выставляет isLoggedOut,
 * чтобы экран сам решил, куда навигировать.
 *
 * Точка непрочитанных на колокольчике приходит параметром из MainActivity
 * (общий NotificationsViewModel), в этом состоянии её нет.
 *
 * TODO(профиль): «Сменить пароль» — метод changePassword в AuthRepository + диалог.
 */

/**
 * Состояние главного экрана профиля.
 *
 * Настройки внешнего вида и уведомлений сюда не входят: переключатели убраны
 * до появления сохранения настроек и применения темы в CoFlowTheme.
 */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null,
    val isAccountDeleted: Boolean = false,
)

/**
 * Логика главного экрана профиля.
 *
 * Данные берутся из `users/{uid}`: UID — из [AuthRepository], профиль — из [UserRepository].
 * Выход выполняет [AuthRepository.logout] и выставляет [ProfileUiState.isLoggedOut],
 * чтобы экран сам решил, куда навигировать.
 */
class ProfileViewModel(
    private val userRepository: UserRepository = FirebaseUserRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Перечитывает профиль текущего пользователя. */
    fun refresh() {
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

    /** Выход из аккаунта. Профиль в Firestore при этом остаётся. */
    fun logout() {
        runCatching { authRepository.logout() }
            .onSuccess { _state.update { it.copy(isLoggedOut = true) } }
            .onFailure { error ->
                _state.update {
                    it.copy(errorMessage = error.message ?: "Не удалось выйти из аккаунта")
                }
            }
    }

    /** Сбрасывает флаг выхода после навигации. */
    fun consumeLogout() {
        _state.update { it.copy(isLoggedOut = false) }
    }

    /**
     * Удаляет аккаунт текущего пользователя.
     *
     * Пароль нужен Firebase для повторной аутентификации перед удалением.
     * После успешного удаления выставляет [ProfileUiState.isAccountDeleted].
     */
    fun deleteAccount(password: String) {
        if (_state.value.isDeletingAccount) return

        _state.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }

        viewModelScope.launch {
            authRepository
                .deleteAccount(password)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteAccountError = null,
                            isAccountDeleted = true,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteAccountError = error.message ?: "Не удалось удалить аккаунт",
                        )
                    }
                }
        }
    }

    /** Сбрасывает ошибку удаления аккаунта. */
    fun consumeDeleteAccountError() {
        _state.update { it.copy(deleteAccountError = null) }
    }

    /** Сбрасывает флаг удаления аккаунта после навигации. */
    fun consumeAccountDeleted() {
        _state.update { it.copy(isAccountDeleted = false) }
    }
}
