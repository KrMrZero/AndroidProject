package com.example.coflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles

@Composable
fun CoFlowScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null, // <-- Слот для правой части (например, колокольчик)
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 60.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Кнопка назад рисуется ТОЛЬКО если передан onBackClick
            if (onBackClick != null) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onBackClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    CoFlowIcon(
                        name = "back",
                        modifier = Modifier.size(22.dp),
                        color = CoFlowColors.TextSecondary,
                        contentDescription = "Назад",
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            Column {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoFlowColors.TextPrimary,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = CoFlowColors.TextSecondary,
                    )
                }
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

data class NavItem(
    val icon: String,
    val title: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun CoFlowBottomNavigationBar(
    items: List<NavItem>,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOfFirst { it.selected }.coerceAtLeast(0)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = 230.dp)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    CoFlowGlassStyles.ControlSurface.copy(alpha = 0.52f),
                                    CoFlowGlassStyles.ControlSurface.copy(alpha = 0.34f),
                                ),
                        ),
                    shape = CoFlowGlassStyles.NavigationShape,
                ).border(
                    1.dp,
                    CoFlowGlassStyles.Border.copy(alpha = 0.68f),
                    CoFlowGlassStyles.NavigationShape,
                ).padding(3.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(70.dp)) {
            val itemWidth = maxWidth / items.size

            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec =
                    tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing,
                    ),
                label = "indicatorOffset",
            )

            Box(
                modifier =
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(3.dp)
                        .background(CoFlowColors.Blue, CoFlowGlassStyles.SelectedItemShape),
            )

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else CoFlowColors.TextPrimary,
                        animationSpec = tween(300),
                        label = "tabColor_$index",
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { item.onClick() }
                                .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CoFlowIcon(
                            name = item.icon,
                            modifier = Modifier.size(18.dp),
                            color = contentColor,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
