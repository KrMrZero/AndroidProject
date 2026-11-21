package com.example.coflow.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

/*
 * Экран «Уведомления».
 *
 * Данные приходят из коллекции `notifications` через NotificationsViewModel.
 * Экран общий для всех колокольчиков приложения: он не создаёт свой ViewModel,
 * если его передали из MainActivity.
 *
 * Список берётся из памяти или локального кэша Firestore (0 чтений),
 * с сервера запрашиваются только новые уведомления; «Показать ещё»
 * догружает старые страницы. Открытие экрана сдвигает отметку
 * прочитанности в `users/{uid}` — прочитать всё стоит одну запись.
 *
 * Осталось: переход по клику на задачу/проект, напоминания о дедлайнах и push.
 */

/**
 * Точка входа экрана уведомлений.
 */
@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Пока экран открыт, загружаем первую страницу и помечаем прочитанным.
    LaunchedEffect(viewModel) {
        viewModel.openScreen()
    }

    // После ухода обновляем только счётчик: за время просмотра могли прийти новые.
    DisposableEffect(viewModel) {
        onDispose { viewModel.refreshUnread() }
    }

    NotificationsScreen(
        state = state,
        onBackClick = onNavigateBack,
        onMarkAllAsRead = viewModel::markAllAsRead,
        onLoadMore = viewModel::loadMore,
    )
}

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBackClick: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onLoadMore: () -> Unit,
) {
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
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = "Уведомления",
                onBackClick = onBackClick,
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = CoFlowColors.TextSecondary)
                    }
                }

                state.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.errorMessage ?: "Пока нет уведомлений",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = CoFlowColors.TextSecondary,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationCard(notification = notification)
                        }

                        if (state.hasMore) {
                            item(key = "load_more") {
                                if (state.isLoadingMore) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = CoFlowColors.TextSecondary,
                                        )
                                    }
                                } else {
                                    TextButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "Показать ещё",
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            color = CoFlowColors.TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // «Отметить прочитанным всё» — как в макете, подчёркнутый текст
            TextButton(
                onClick = onMarkAllAsRead,
                enabled = state.hasUnread,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Отметить прочитанным всё",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    color = CoFlowColors.TextPrimary.copy(alpha = if (state.hasUnread) 1f else 0.45f),
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Карточка одного уведомления. */
@Composable
private fun NotificationCard(notification: NotificationUi) {
    CoFlowGlassCard(
        modifier = Modifier.fillMaxWidth(),
        // Фронтенд просил не синий: карточки в тон светлому фону приложения.
        containerColor = Color(0xFFF2F6FF),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            if (notification.projectName.isNotBlank()) {
                Text(
                    text = notification.projectName,
                    fontSize = 12.sp,
                    color = CoFlowColors.TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = notification.message,
                fontSize = 14.sp,
                fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (notification.isRead) "Прочитано" else "Не прочитано",
                fontSize = 12.sp,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                color = if (notification.isRead) CoFlowColors.TextSecondary else CoFlowColors.TextPrimary,
            )
        }
    }
}

@Preview(name = "Уведомления", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsScreenPreview() {
    CoFlowTheme {
        NotificationsScreen(
            state =
                NotificationsUiState(
                    notifications =
                        listOf(
                            NotificationUi(
                                id = "1",
                                projectName = "Проект 1",
                                message = "Иван И. назначил вам задачу «Подготовить отчёт»",
                                isRead = true,
                            ),
                            NotificationUi(
                                id = "2",
                                projectName = "Проект 2",
                                message = "Мария С. взяла вашу задачу «Сверстать экран» в работу",
                                isRead = false,
                            ),
                        ),
                    unreadCount = 1,
                    isLoading = false,
                ),
            onBackClick = {},
            onMarkAllAsRead = {},
            onLoadMore = {},
        )
    }
}
