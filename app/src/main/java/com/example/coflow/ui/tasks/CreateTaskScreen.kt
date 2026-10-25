package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun CreateTaskScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNavigateToAssignees: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToDate: () -> Unit,
    onCreateTask: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(coFlowTasksBackgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = CoFlowSpacing.ScreenHorizontal)
        ) {
            CoFlowScreenHeader(
                title = "Создание задачи",
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoFlowGlassTextField(
                    label = "Название",
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = "Введите название задачи"
                )

                CoFlowGlassTextField(
                    label = "Описание",
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    placeholder = "Введите описание задачи",
                    singleLine = false,
                    maxLength = 250
                )

                CoFlowGlassTextField(
                    label = "Исполнители",
                    value = state.assignees.joinToString(separator = ", ") { it.name },
                    onValueChange = {},
                    placeholder = "Выберите исполнителей",
                    leadingIcon = "person",
                    trailingIcon = "chevron",
                    onClick = onNavigateToAssignees
                )

                CoFlowGlassTextField(
                    label = "Проект",
                    value = state.project,
                    onValueChange = {},
                    placeholder = "Выберите проект",
                    leadingIcon = "folder",
                    trailingIcon = "chevron",
                    onClick = onNavigateToProject
                )

                CoFlowGlassTextField(
                    label = "Дата окончания",
                    value = state.dueDate,
                    onValueChange = {},
                    placeholder = "Выберите дату",
                    leadingIcon = "calendar",
                    trailingIcon = "chevron",
                    onClick = onNavigateToDate,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }

            CoFlowPrimaryButton(
                text = "Создать задачу",
                onClick = {
                    focusManager.clearFocus()
                    onCreateTask()
                }
            )
        }
    }
}

@Preview(
    name = "Создание задачи",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CreateTaskScreenPreview() {
    CoFlowTheme {
        CreateTaskScreen(
            state = CreateTaskUiState(
                title = "",
                description = "",
                assignees = listOf(
                    Assignee(
                        id = "1",
                        name = "Анна Смирнова",
                        email = "anna.s@example.com",
                        initials = "АС",
                        avatarColor = Color.Blue
                    )
                ),
                project = "",
                dueDate = ""
            ),
            onBackClick = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onNavigateToAssignees = {},
            onNavigateToProject = {},
            onNavigateToDate = {},
            onCreateTask = {}
        )
    }
}