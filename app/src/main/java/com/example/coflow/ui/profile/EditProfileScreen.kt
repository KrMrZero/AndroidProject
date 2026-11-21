package com.example.coflow.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.ui.components.CoFlowPrimaryButton
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.components.NotificationBell
import com.example.coflow.ui.components.ProfileTextField
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

/*
 * Экран «Изменение профиля».
 *
 * Уже работает: загрузка текущих имени/почты, валидация, сохранение в Firestore,
 * переход назад и на нижнюю навигацию.
 *
 * Доделать:
 * 1) смена почты для входа: Firestore обновляется, Firebase Auth — нет;
 * 2) ограничение длины имени и проверка занятости почты;
 * 3) аватар (если решите делать фото);
 * 4) строки: вынести подписи в strings.xml.
 *
 * Экраны уведомлений лежат в NotificationsScreen.kt / NotificationsViewModel.kt.
 * Общие элементы профиля — в ui/components (CoFlowLayouts.kt, CoFlowInputs.kt).
 */

/** Точка входа экрана «Изменение профиля». */
@Composable
fun EditProfileRoute(
    onNavigateBack: () -> Unit,
    onProfileSaved: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    viewModel: EditProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Профиль сохранён", Toast.LENGTH_SHORT).show()
            viewModel.consumeSaved()
            onProfileSaved()
        }
    }

    EditProfileScreen(
        state = state,
        onBackClick = onNavigateBack,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onSaveClick = viewModel::onSaveClick,
        onNotificationsClick = onNavigateToNotifications,
        hasUnreadNotifications = hasUnreadNotifications,
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    hasUnreadNotifications: Boolean = false,
) {
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

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
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = "Изменение профиля",
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
                trailingContent = {
                    NotificationBell(
                        hasUnread = hasUnreadNotifications,
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
                ProfileTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Имя пользователя",
                    placeholder = "Введите имя",
                    leadingIcon = Icons.Outlined.PersonOutline,
                    isError = state.nameError != null,
                    supportingText = state.nameError,
                )

                ProfileTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Почта",
                    placeholder = "name@example.com",
                    leadingIcon = Icons.Outlined.MailOutline,
                    isError = state.emailError != null,
                    supportingText = state.emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 12.sp,
                        color = CoFlowColors.Error,
                    )
                }

                Spacer(Modifier.height(4.dp))

                CoFlowPrimaryButton(
                    text = "Сохранить изменения",
                    onClick = {
                        focusManager.clearFocus()
                        onSaveClick()
                    },
                    enabled = state.canSave,
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Preview(name = "Изменение профиля", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EditProfileScreenPreview() {
    CoFlowTheme {
        EditProfileScreen(
            state =
                EditProfileUiState(
                    name = "Елизавета",
                    email = "liza.simendeeva@mail.ru",
                    isLoading = false,
                    isSaving = false,
                ),
            onBackClick = {},
            onNameChange = {},
            onEmailChange = {},
            onSaveClick = {},
            onNotificationsClick = {},
            hasUnreadNotifications = true,
        )
    }
}
