package com.example.coflow.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.coflow.ui.theme.coFlowBackgroundBrush

/*
 * ЗОНА «УВЕДОМЛЕНИЯ» (экран 2 из 3).
 *
 * Не входит в каркас для команды: этот экран и NotificationsViewModel.kt
 * дорабатываются отдельно. Сейчас данные демонстрационные.
 *
 * Следующие шаги: NotificationModel + NotificationRepository (Firestore),
 * реальные события (назначение задачи, комментарии), сохранение isRead,
 * реальный счётчик непрочитанных для колокольчика.
 */

/**
 * Точка входа экрана уведомлений.
 *
 * @param selectedTab какая вкладка подсвечена в нижней навигации
 * (в макете подсвечены «Мои задачи», по умолчанию — «Профиль»).
 */
@Composable
fun NotificationsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    selectedTab: ProfileTab = ProfileTab.PROFILE,
    viewModel: NotificationsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NotificationsScreen(
        state = state,
        onBackClick = onNavigateBack,
        onMarkAllAsRead = viewModel::markAllAsRead,
        onNotificationClick = viewModel::markAsRead,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            when (tab) {
                ProfileTab.TASKS -> onNavigateToTasks()
                ProfileTab.PROJECTS -> onNavigateToProjects()
                ProfileTab.PROFILE -> onNavigateToProfile()
            }
        },
    )
}

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBackClick: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (String) -> Unit,
    onTabSelected: (ProfileTab) -> Unit,
    selectedTab: ProfileTab = ProfileTab.PROFILE,
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(coFlowBackgroundBrush()),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = "Уведомления",
                onBackClick = onBackClick,
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { onNotificationClick(notification.id) },
                    )
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
                    color = Color.White.copy(alpha = if (state.hasUnread) 1f else 0.55f),
                )
            }

            Spacer(Modifier.height(4.dp))

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
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                )
            }
        }
    }
}

/** Карточка одного уведомления. */
@Composable
private fun NotificationCard(
    notification: NotificationUi,
    onClick: () -> Unit,
) {
    CoFlowGlassCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = !notification.isRead,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            Text(
                text = notification.projectName,
                fontSize = 12.sp,
                color = CoFlowColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = notification.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CoFlowColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (notification.isRead) "Прочитано" else "Не прочитано",
                fontSize = 12.sp,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                color = if (notification.isRead) CoFlowColors.TextSecondary else CoFlowColors.Blue,
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
                                message = "Иван И. назначил вам задачу",
                                isRead = true,
                            ),
                            NotificationUi(
                                id = "2",
                                projectName = "Проект 2",
                                message = "Мария С. оставила комментарий к задаче",
                                isRead = false,
                            ),
                        ),
                    unreadCount = 1,
                ),
            onBackClick = {},
            onMarkAllAsRead = {},
            onNotificationClick = {},
            onTabSelected = {},
        )
    }
}
