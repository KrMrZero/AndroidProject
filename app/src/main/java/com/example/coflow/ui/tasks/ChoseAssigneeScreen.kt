package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun ChoseAssigneeScreen(
    onBackClick: () -> Unit,
    viewModel: CreateTaskViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = {
        viewModel.cancelAssigneeSelection()
        onBackClick()
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coFlowTasksBackgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = CoFlowSpacing.ScreenHorizontal)
        ) {
            CoFlowScreenHeader(
                title = "Добавить исполнителя",
                onBackClick = {
                    focusManager.clearFocus()
                    viewModel.cancelAssigneeSelection()
                    onBackClick()
                }
            )

            CoFlowGlassTextField(
                value = state.assigneeSearchQuery,
                onValueChange = viewModel::onAssigneeSearchQueryChange,
                placeholder = "Имя или почта",
                leadingIcon = "search",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                )
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Исполнители",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextPrimary
                )

                Text(
                    text = "${state.allAssignees.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoFlowColors.TextSecondary
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = state.filteredAssignees,
                    key = { it.id }
                ) { assignee ->
                    val isSelected = assignee.id in state.pendingAssigneeIds

                    CoFlowGlassCard(
                        isSelected = isSelected,
                        modifier = Modifier.clickable {
                            viewModel.togglePendingAssignee(assignee.id)
                        }
                    ) {
                        AssigneeItem(
                            assignee = assignee,
                            isSelected = isSelected
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Text(
                    text = getSelectedText(state.pendingSelectedCount),
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CoFlowColors.TextPrimary
                )

                CoFlowPrimaryButton(
                    text = "Добавить исполнителей",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.confirmAssigneeSelection()
                        onBackClick()
                    },
                    enabled = state.canConfirmAssignees
                )
            }
        }
    }
}

@Composable
private fun AssigneeItem(
    assignee: Assignee,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = assignee.avatarColor.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = assignee.initials,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = assignee.avatarColor
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = assignee.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = assignee.email,
                fontSize = 12.sp,
                color = CoFlowColors.TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isSelected) CoFlowColors.Blue else Color.White
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) CoFlowColors.Blue
                    else CoFlowColors.TextSecondary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                CoFlowIcon(
                    name = "check",
                    modifier = Modifier.size(14.dp),
                    color = Color.White
                )
            }
        }
    }
}

private fun getSelectedText(count: Int): String {
    if (count == 0) return "Никто не выбран"

    val word = when {
        count % 100 in 11..19 -> "исполнителей"
        count % 10 == 1 -> "исполнитель"
        count % 10 in 2..4 -> "исполнителя"
        else -> "исполнителей"
    }

    return "Выбран $count $word"
}

@Preview(
    name = "Выбор исполнителя",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun ChoseAssigneeScreenPreview() {
    CoFlowTheme {
        ChoseAssigneeScreen(onBackClick = {})
    }
}