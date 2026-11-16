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
 * Стеклянная кнопка-колокольчик с точкой непрочитанных уведомлений.
 *
 * Клик обрабатывает вызывающий экран (обычно — переход на экран уведомлений).
 */
@Composable
fun NotificationBell(
    onClick: () -> Unit,
    hasUnread: Boolean = true,
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
