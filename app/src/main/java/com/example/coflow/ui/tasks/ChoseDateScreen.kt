package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChoseDateScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onConfirm: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val currentMonth = state.calendarMonth
    val selectedDate = state.selectedDueDate

    BackHandler(onBack = onBackClick)

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding()
                .padding(horizontal = CoFlowSpacing.ScreenHorizontal)
        ) {
            CoFlowScreenHeader(
                title = "Дата окончания",
                onBackClick = { focusManager.clearFocus(); onBackClick() }
            )

            Box(
                modifier = Modifier.fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp),
                        ambientColor = CoFlowGlassStyles.Shadow, spotColor = CoFlowGlassStyles.Shadow)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMonth.format(
                                DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
                            ).replaceFirstChar { it.titlecase(Locale("ru")) },
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = CoFlowColors.TextPrimary
                        )
                        Row {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .clickable { onMonthChange(currentMonth.minusMonths(1)) },
                                contentAlignment = Alignment.Center
                            ) {
                                CoFlowIcon(name = "chevron_left", modifier = Modifier.size(16.dp),
                                    color = CoFlowColors.Blue, contentDescription = "Предыдущий месяц")
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .clickable { onMonthChange(currentMonth.plusMonths(1)) },
                                contentAlignment = Alignment.Center
                            ) {
                                CoFlowIcon(name = "chevron", modifier = Modifier.size(16.dp),
                                    color = CoFlowColors.Blue, contentDescription = "Следующий месяц")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { day ->
                            Text(text = day, modifier = Modifier.width(36.dp), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium, color = CoFlowColors.TextSecondary,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    CalendarGrid(currentMonth = currentMonth, selectedDate = selectedDate) { date ->
                        if (date.month == currentMonth.month) onDateSelected(date)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            CoFlowPrimaryButton(
                text = "Готово",
                onClick = { focusManager.clearFocus(); onConfirm() }
            )
        }
    }
}

@Composable
private fun CalendarGrid(currentMonth: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val offset = firstDayOfMonth.dayOfWeek.value - 1
    val daysInMonth = currentMonth.lengthOfMonth()
    val prevMonth = currentMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val (dayNumber, date, isCurrentMonth) = when {
                        cellIndex < offset -> Triple(daysInPrevMonth - offset + cellIndex + 1, prevMonth.atDay(daysInPrevMonth - offset + cellIndex + 1), false)
                        cellIndex >= offset + daysInMonth -> Triple(cellIndex - (offset + daysInMonth) + 1, currentMonth.plusMonths(1).atDay(cellIndex - (offset + daysInMonth) + 1), false)
                        else -> Triple(cellIndex - offset + 1, currentMonth.atDay(cellIndex - offset + 1), true)
                    }
                    val isSelected = isCurrentMonth && date == selectedDate

                    Box(
                        modifier = Modifier.width(36.dp).height(40.dp).clip(CircleShape)
                            .background(if (isSelected) CoFlowColors.Blue else Color.Transparent)
                            .clickable(enabled = isCurrentMonth) { if (isCurrentMonth) onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber.toString(), fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                !isCurrentMonth -> CoFlowColors.TextSecondary.copy(alpha = 0.4f)
                                else -> CoFlowColors.TextPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Дата окончания", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChoseDateScreenPreview() {
    CoFlowTheme {
        ChoseDateScreen(
            state = CreateTaskUiState(),
            onBackClick = {},
            onMonthChange = {},
            onDateSelected = {},
            onConfirm = {}
        )
    }
}