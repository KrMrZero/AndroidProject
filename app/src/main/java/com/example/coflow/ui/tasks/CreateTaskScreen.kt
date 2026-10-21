package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
@Composable
fun CreateTaskScreen(
    state: CreateTaskUiState,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNavigateToAssignees: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToDate: () -> Unit,
    onCreateTask: (CreateTaskFormData) -> Unit
) {

    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBackClick)

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = CoFlowSpacing.ScreenHorizontal)) {

            CoFlowScreenHeader(
                title = "Создание задачи",
                onBackClick = { focusManager.clearFocus(); onBackClick() }
            )

            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoFlowGlassTextField(
                    label = "Название", value = state.title, onValueChange = onTitleChange,
                    placeholder = "Введите название задачи"
                )
                CoFlowGlassTextField(
                    label = "Описание", value = state.description, onValueChange = onDescriptionChange,
                    placeholder = "Введите описание задачи", singleLine = false, maxLength = 250
                )

                // Клик сработает по ВСЕМУ полю. Клавиатура не вылезет благодаря readOnly
                CoFlowGlassTextField(
                    label = "Исполнители",
                    value = state.assignees.joinToString(", ") { it.name },
                    onValueChange = {}, // Пустая лямбда, так как ввод заблокирован
                    placeholder = "Выберите исполнителей",
                    leadingIcon = "person",
                    trailingIcon = "chevron",
                    onClick = { onNavigateToAssignees() }
                )

                CoFlowGlassTextField(
                    label = "Проект",
                    value = state.project,
                    onValueChange = {}, // <--- Пустая лямбда
                    placeholder = "Выберите проект", leadingIcon = "folder", trailingIcon = "chevron",
                    onClick = { onNavigateToProject() }
                )

                CoFlowGlassTextField(
                    label = "Дата окончания",
                    value = state.dueDate,
                    onValueChange = {}, // <--- Пустая лямбда
                    placeholder = "Выберите дату", leadingIcon = "calendar", trailingIcon = "chevron",
                    onClick = { onNavigateToDate() },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }


            CoFlowPrimaryButton(
                text = "Создать задачу",
                onClick = {
                    focusManager.clearFocus()
                    onCreateTask(CreateTaskFormData(state.title.trim(), state.description.trim(), assignees = state.assignees, state.project.trim(), state.dueDate.trim()))
                },

            )
        }
    }
}

@Preview(name = "Создание задачи", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateTaskScreenPreview() {
    CoFlowTheme {
        CreateTaskScreen(
            state = CreateTaskUiState(
                title = "Купить пиво",
                description = "В бристоле возле университета",
                assignees = listOf(Assignee("1", "Степан Чуб", "chub@example.com", "СЧ", Color.Blue)),
                project = "Балтика-9",
                dueDate = "22.10.2026"
            ),
            onBackClick = { println("Preview: Назад") },
            onCreateTask = { println("Preview: Создание задачи: $it") },
            onNavigateToAssignees = { println("Preview: Переход к выбору исполнителей") },
            onNavigateToProject = { println("Preview: Переход к выбору проекта") },
            onNavigateToDate = { println("Preview: Переход к выбору даты") },
            onTitleChange = {},
            onDescriptionChange = {}
        )
    }
}