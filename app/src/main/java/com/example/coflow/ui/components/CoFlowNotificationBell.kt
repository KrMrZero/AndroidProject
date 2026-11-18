package com.example.coflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coflow.ui.theme.CoFlowColors

/**
 * Кнопка-колокольчик с точкой непрочитанных уведомлений.
 *
 * Клик обрабатывает вызывающий экран — обычно это переход на экран уведомлений.
 *
 * @param hasUnread показывать ли точку; значение приходит из общего
 *   `NotificationsViewModel`, поэтому на всех экранах оно одинаковое.
 */
@Composable
fun NotificationBell(
    onClick: () -> Unit,
    hasUnread: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CoFlowGlassIconButton(iconName = "bell", onClick = onClick)
        if (hasUnread) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .size(6.dp)
                        .background(CoFlowColors.Blue, CircleShape),
            )
        }
    }
}
