package com.example.coflow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.components.NotificationBell
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

/*
 * ЗОНА «ПРОФИЛЬ» (экран 1 из 3).
 *
 * Владелец зоны ведёт только этот файл и ProfileViewModel.kt.
 * Уже работает: загрузка имени/почты, выход, удаление аккаунта с подтверждением,
 * переходы в редактирование и уведомления.
 *
 * Доделать (подробнее — docs/PROFILE.md):
 * 1) тёмная тема: сейчас только флаг в состоянии, интерфейс не меняется;
 * 2) «Сменить пароль»: сейчас внешний колбэк, нужен диалог + метод в AuthRepository;
 * 3) аватар: в карточке иконка, при необходимости — фото из Storage;
 * 4) строки: вынести подписи в strings.xml.
 *
 * Не трогать: NotificationsScreen.kt / NotificationsViewModel.kt — зона уведомлений.
 * Общие файлы (ProfileComponents.kt, ProfileBottomNavigation.kt) — только по согласованию.
 */

/**
 * Точка входа экрана «Мой профиль».
 *
 * Навигацию держим снаружи: экран получает колбэки и ничего не знает о NavController.
 * Выход обрабатывается через [ProfileUiState.isLoggedOut] → [onLoggedOut].
 */
@Composable
fun ProfileRoute(
    onNavigateToTasks: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onAccountDeleted: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            viewModel.consumeLogout()
            onLoggedOut()
        }
    }

    LaunchedEffect(state.isAccountDeleted) {
        if (state.isAccountDeleted) {
            viewModel.consumeAccountDeleted()
            onAccountDeleted()
        }
    }

    ProfileScreen(
        state = state,
        onEditProfileClick = onNavigateToEditProfile,
        onNotificationsClick = onNavigateToNotifications,
        onDarkThemeToggle = viewModel::onDarkThemeToggle,
        onNotificationsToggle = viewModel::onNotificationsToggle,
        onLogoutClick = viewModel::logout,
        onChangePasswordClick = onChangePasswordClick,
        onDeleteAccountClick = { showDeleteDialog = true },
        onTabSelected = { tab ->
            when (tab) {
                ProfileTab.TASKS -> onNavigateToTasks()
                ProfileTab.PROJECTS -> onNavigateToProjects()
                ProfileTab.PROFILE -> Unit
            }
        },
    )

    // Подтверждение удаления аккаунта
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeletingAccount) {
                    showDeleteDialog = false
                    deletePassword = ""
                    viewModel.consumeDeleteAccountError()
                }
            },
            title = { Text(text = "Удалить аккаунт?", color = CoFlowColors.TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Профиль и участие в проектах будут удалены безвозвратно. Для подтверждения введите пароль.",
                        fontSize = 13.sp,
                        color = CoFlowColors.TextSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileTextField(
                        value = deletePassword,
                        onValueChange = {
                            deletePassword = it
                            viewModel.consumeDeleteAccountError()
                        },
                        label = "Пароль",
                        placeholder = "Введите пароль",
                        leadingIcon = Icons.Outlined.Lock,
                        isError = state.deleteAccountError != null,
                        supportingText = state.deleteAccountError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount(deletePassword) },
                    enabled = !state.isDeletingAccount && deletePassword.isNotBlank(),
                ) {
                    Text(
                        text = if (state.isDeletingAccount) "Удаление..." else "Удалить",
                        color = CoFlowColors.Error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deletePassword = ""
                        viewModel.consumeDeleteAccountError()
                    },
                    enabled = !state.isDeletingAccount,
                ) {
                    Text(text = "Отмена", color = CoFlowColors.Blue)
                }
            },
            containerColor = Color(0xFFF4F7FF),
        )
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onDarkThemeToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onTabSelected: (ProfileTab) -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(coFlowTasksBackgroundBrush()),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = "Мой профиль",
                subtitle = "Личные данные и настройки",
                trailingContent = {
                    NotificationBell(
                        hasUnread = state.hasUnreadNotifications,
                        onClick = onNotificationsClick,
                    )
                },
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Карточка пользователя с переходом в редактирование
                ProfileUserCard(
                    name = state.name,
                    email = state.email,
                    isLoading = state.isLoading,
                    onClick = onEditProfileClick,
                )

                // Настройки внешнего вида
                // TODO(профиль): переключатель меняет только локальный флаг.
                //  Нужно сохранять выбор и прокидывать его в CoFlowTheme (MainActivity).
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuRow(
                        title = "Тёмная тема",
                        leadingIcon = Icons.Outlined.DarkMode,
                        trailing = {
                            ProfileSwitch(
                                checked = state.isDarkTheme,
                                onCheckedChange = onDarkThemeToggle,
                            )
                        },
                    )
                }

                // Настройки уведомлений
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuRow(
                        title = "Уведомления",
                        leadingIcon = Icons.Outlined.Notifications,
                        trailing = {
                            ProfileSwitch(
                                checked = state.isNotificationsEnabled,
                                onCheckedChange = onNotificationsToggle,
                            )
                        },
                    )
                }

                // Действия с аккаунтом
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuRow(
                        title = "Выйти из аккаунта",
                        onClick = onLogoutClick,
                        trailing = { ProfileIconBadge(icon = Icons.AutoMirrored.Outlined.Logout) },
                    )
                    ProfileMenuDivider()
                    // TODO(профиль): сейчас только колбэк в MainActivity (Toast).
                    //  Нужен диалог ввода пароля + AuthRepository.changePassword().
                    ProfileMenuRow(
                        title = "Сменить пароль",
                        onClick = onChangePasswordClick,
                        trailing = { ProfileIconBadge(icon = Icons.Outlined.Lock) },
                    )
                }

                // Удаление аккаунта
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuRow(
                        title = "Удалить аккаунт",
                        leadingIcon = Icons.Outlined.DeleteOutline,
                        iconTint = CoFlowColors.Error,
                        titleColor = CoFlowColors.Error,
                        onClick = onDeleteAccountClick,
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = CoFlowColors.Error,
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // Нижняя навигация
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom =
                                WindowInsets.navigationBars
                                    .asPaddingValues()
                                    .calculateBottomPadding() + 2.dp,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileBottomBar(
                    selectedTab = ProfileTab.PROFILE,
                    onTabSelected = onTabSelected,
                )
            }
        }
    }
}

/** Карточка пользователя: аватар, имя, почта и кнопка редактирования. */
@Composable
private fun ProfileUserCard(
    name: String,
    email: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    CoFlowGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(CoFlowColors.Blue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                // TODO(профиль): сейчас заглушка-иконка.
                //  Если нужны фото — поле avatarUrl в UserModel + загрузка в Firebase Storage.
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CoFlowColors.Blue,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CoFlowColors.Blue,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = name.ifBlank { "Пользователь" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoFlowColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = email.ifBlank { "Почта не указана" },
                        fontSize = 12.sp,
                        color = CoFlowColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            ProfileIconBadge(icon = Icons.Outlined.Edit)
        }
    }
}

@Preview(name = "Мой профиль", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileScreenPreview() {
    CoFlowTheme {
        ProfileScreen(
            state =
                ProfileUiState(
                    name = "Елизавета",
                    email = "liza.simendeeva@mail.ru",
                    isLoading = false,
                    isNotificationsEnabled = true,
                ),
            onEditProfileClick = {},
            onNotificationsClick = {},
            onDarkThemeToggle = {},
            onNotificationsToggle = {},
            onLogoutClick = {},
            onChangePasswordClick = {},
            onDeleteAccountClick = {},
        )
    }
}
