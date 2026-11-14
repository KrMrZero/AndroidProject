package com.example.coflow.ui.projects

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coflow.R
import com.example.coflow.models.Role
import com.example.coflow.ui.components.CoFlowEditDeleteButton
import com.example.coflow.ui.components.CoFlowEditSaveButton
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowGlassTextField
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

@Composable
fun EditProjectScreen(
    state: EditProjectUiState,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMemberRoleChange: (String, Role) -> Unit,
    onMemberRemoveClick: (String) -> Unit,
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
                title = stringResource(R.string.edit_project_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
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
                // Название проекта
                CoFlowGlassTextField(
                    label = stringResource(R.string.creation_task_name_label),
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.creation_task_name_placeholder),
                )

                // Описание проекта
                CoFlowGlassTextField(
                    label = stringResource(R.string.creation_task_description_label),
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    placeholder = stringResource(R.string.creation_task_description_placeholder),
                    singleLine = false,
                    maxLength = EditProjectViewModel.MAX_DESCRIPTION_LENGTH,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Секция участников
                Text(
                    text = stringResource(R.string.assignees_header), // Переиспользуем "Исполнители" / "Участники"
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoFlowColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (state.members.isEmpty()) {
                            Text(
                                text = stringResource(R.string.assignees_placeholder),
                                color = CoFlowColors.TextSecondary,
                                modifier =
                                    Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally),
                            )
                        } else {
                            state.members.forEachIndexed { index, member ->
                                MemberRow(
                                    member = member,
                                    onRoleChange = { newRole -> onMemberRoleChange(member.membershipId, newRole) },
                                    onRemoveClick = { onMemberRemoveClick(member.membershipId) },
                                )
                                if (index < state.members.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Разделитель
                                    Spacer(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(CoFlowColors.TextSecondary.copy(alpha = 0.1f)),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))
            }

            // Кнопки сохранения и удаления
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CoFlowEditDeleteButton(
                    text = stringResource(R.string.edit_project_delete_button),
                    onClick = {
                        focusManager.clearFocus()
                        onDeleteClick()
                    },
                    modifier = Modifier.weight(1f),
                )

                CoFlowEditSaveButton(
                    text = stringResource(R.string.edit_task_save_button),
                    onClick = {
                        focusManager.clearFocus()
                        onSaveClick()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.canSave,
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: ProjectMemberUi,
    onRoleChange: (Role) -> Unit,
    onRemoveClick: () -> Unit,
) {
    val isOwner = member.role == Role.OWNER
    var menuExpanded by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Аватар (инициалы)
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CoFlowColors.Blue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.initials,
                color = CoFlowColors.Blue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(12.dp))

        // Имя и email
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                fontWeight = FontWeight.Medium,
                color = CoFlowColors.TextPrimary,
                fontSize = 14.sp,
            )
            Text(
                text = member.email,
                fontSize = 12.sp,
                color = CoFlowColors.TextSecondary,
            )
        }

        // Роль (теперь только информационная подпись)
        Text(
            text = getRoleText(member.role),
            color = if (isOwner) CoFlowColors.TextSecondary else CoFlowColors.Blue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))

        if (!isOwner) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Действия с участником",
                        tint = CoFlowColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Основное мини-меню: Сменить роль / Удалить
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.change_role),
                                color = CoFlowColors.TextPrimary,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            roleMenuExpanded = true
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete_member),
                                color = CoFlowColors.Error,
                                fontSize = 14.sp,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemoveClick()
                        },
                    )
                }

                // Каскадное меню выбора новой роли
                DropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false },
                ) {
                    Role
                        .values()
                        .filter { it != Role.OWNER }
                        .forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = getRoleText(role),
                                        color =
                                            if (role == member.role) {
                                                CoFlowColors.Blue
                                            } else {
                                                CoFlowColors.TextPrimary
                                            },
                                        fontWeight =
                                            if (role == member.role) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                        fontSize = 14.sp,
                                    )
                                },
                                onClick = {
                                    roleMenuExpanded = false
                                    onRoleChange(role)
                                },
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun getRoleText(role: Role): String =
    when (role) {
        Role.OWNER -> stringResource(R.string.role_owner)

        // Role.ADMIN -> "Администратор" // Раскомментируйте, если в вашем enum есть ADMIN
        else -> stringResource(R.string.role_member)
    }

@Composable
fun EditProjectRoute(
    onNavigateBack: () -> Unit,
    onProjectSaved: () -> Unit,
    onProjectDeleted: () -> Unit,
    viewModel: EditProjectViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        EditProjectViewModel(createSavedStateHandle())
                    }
                },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Обработка успешного сохранения
    LaunchedEffect(state.isProjectSaved) {
        if (state.isProjectSaved) {
            Toast.makeText(context, "Проект сохранен", Toast.LENGTH_SHORT).show()
            viewModel.consumeProjectSaved()
            onProjectSaved()
        }
    }

    // Обработка успешного удаления
    LaunchedEffect(state.isProjectDeleted) {
        if (state.isProjectDeleted) {
            Toast.makeText(context, "Проект удален", Toast.LENGTH_SHORT).show()
            viewModel.consumeProjectDeleted()
            onProjectDeleted()
        }
    }

    EditProjectScreen(
        state = state,
        onBackClick = onNavigateBack,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::showDeleteDialog,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onMemberRoleChange = viewModel::changeMemberRole,
        onMemberRemoveClick = viewModel::removeMember,
    )

    // Диалог подтверждения удаления
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text(stringResource(R.string.edit_task_delete_dialog_title)) },
            text = { Text(stringResource(R.string.edit_task_delete_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text(stringResource(R.string.edit_task_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text(stringResource(R.string.edit_task_delete_cancel))
                }
            },
        )
    }
}

@Preview(name = "Редактирование проекта", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun EditProjectScreen_Preview() {
    CoFlowTheme {
        EditProjectScreen(
            state =
                EditProjectUiState(
                    projectId = "1",
                    title = "Мобильное приложение CoFlow",
                    description = "Разработка кроссплатформенного мобильного приложения для управления задачами и проектами.",
                    members =
                        listOf(
                            ProjectMemberUi("m1", "u1", "Алексей Смирнов", "alexey@coflow.com", "АС", Role.OWNER, isCurrentUser = true),
                            ProjectMemberUi("m2", "u2", "Мария Иванова", "maria@coflow.com", "МИ", Role.MEMBER, isCurrentUser = false),
                        ),
                    isLoading = false,
                    isSaving = false,
                ),
            onBackClick = {},
            onSaveClick = {},
            onDeleteClick = {},
            onTitleChange = {},
            onDescriptionChange = {},
            onMemberRoleChange = { _, _ -> },
            onMemberRemoveClick = {},
        )
    }
}
