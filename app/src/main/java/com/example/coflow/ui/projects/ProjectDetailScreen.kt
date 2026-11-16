package com.example.coflow.ui.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowIcon
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

@Composable
fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
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
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            CoFlowScreenHeader(
                title = stringResource(R.string.project_description_header),
                onBackClick = onBackClick,
                trailingContent = {
                    if (state.canEdit) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CoFlowColors.Blue)
                                    .clickable { onEditClick() },
                            contentAlignment = Alignment.Center,
                        ) {
                            CoFlowIcon(
                                name = "edit",
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                            )
                        }
                    }
                },
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Блок с основной информацией о проекте
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = state.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoFlowColors.TextPrimary,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagChip(text = getProjectStatusText(state.status))
                        }

                        Column {
                            Text(
                                stringResource(R.string.project_description_label),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoFlowColors.TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.description.ifBlank { stringResource(R.string.description_placeholder) },
                                fontSize = 13.sp,
                                color = CoFlowColors.TextSecondary,
                            )
                        }

                        Column {
                            Text(
                                stringResource(R.string.project_owner_label),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoFlowColors.TextPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.ownerName.ifBlank { stringResource(R.string.author_placeholder) },
                                fontSize = 13.sp,
                                color = CoFlowColors.TextSecondary,
                            )
                        }
                    }
                }

                // Блок с участниками
                CoFlowGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.participants_header),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoFlowColors.TextPrimary,
                        )

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.search_participants_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )

                        if (state.filteredParticipants.isEmpty()) {
                            Text(
                                text = stringResource(R.string.participants_placeholder),
                                fontSize = 13.sp,
                                color = CoFlowColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.filteredParticipants.forEach { participant ->
                                    ParticipantRow(participant = participant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: ProjectParticipantUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoFlowColors.Blue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = participant.initials,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CoFlowColors.Blue,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CoFlowColors.TextPrimary,
            )
            Text(
                text = participant.email,
                fontSize = 12.sp,
                color = CoFlowColors.TextSecondary,
            )
        }
        TagChip(text = getRoleText(participant.role))
    }
}

@Composable
private fun TagChip(text: String) {
    val bgColor = CoFlowColors.Blue.copy(alpha = 0.15f)
    val textColor = CoFlowColors.Blue
    Box(
        modifier =
            Modifier
                .background(bgColor, shape = RoundedCornerShape(8.dp))
                .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun getProjectStatusText(status: ProjectStatusUi): String =
    when (status) {
        ProjectStatusUi.IN_PROGRESS -> stringResource(R.string.project_status_in_progress)
        ProjectStatusUi.ARCHIVED -> stringResource(R.string.project_status_archived)
    }

@Composable
private fun getRoleText(role: Role): String =
    when (role) {
        Role.OWNER -> stringResource(R.string.role_owner)
        Role.MEMBER -> stringResource(R.string.role_member)
        else -> role.name
    }

@Composable
fun ProjectDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: ProjectDetailScreenViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        ProjectDetailScreenViewModel(createSavedStateHandle())
                    }
                },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Отслеживаем запрос на редактирование из ViewModel
    LaunchedEffect(state.isEditRequested) {
        if (state.isEditRequested) {
            onNavigateToEdit(state.projectId)
            viewModel.consumeEditRequest()
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush()),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = CoFlowColors.Blue)
            }
        }

        state.errorMessage != null || state.errorMessageResId != null -> {
            val errorResId = state.errorMessageResId
            val errorMsg = state.errorMessage

            val message =
                when {
                    errorResId != null && errorMsg != null -> stringResource(errorResId, errorMsg)
                    errorResId != null -> stringResource(errorResId)
                    else -> stringResource(R.string.error_prefix, errorMsg.orEmpty())
                }

            Box(
                modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush()),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = message, color = CoFlowColors.Error)
            }
        }

        else -> {
            ProjectDetailScreen(
                state = state,
                onBackClick = onNavigateBack,
                onEditClick = viewModel::onEditClick,
                onSearchQueryChange = viewModel::onSearchQueryChange,
            )
        }
    }
}

@Preview(name = "Проект - В работе", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ProjectDetailScreen_InProgress_Preview() {
    CoFlowTheme {
        ProjectDetailScreen(
            state =
                ProjectDetailUiState(
                    projectId = "1",
                    title = "Мобильное приложение CoFlow",
                    description = "Разработка кроссплатформенного мобильного приложения для управления задачами и проектами.",
                    status = ProjectStatusUi.IN_PROGRESS,
                    ownerName = "Алексей Смирнов",
                    participants =
                        listOf(
                            ProjectParticipantUi("1", "Алексей Смирнов", "alexey@coflow.com", "АС", Role.OWNER),
                            ProjectParticipantUi("2", "Мария Иванова", "maria@coflow.com", "МИ", Role.MEMBER),
                        ),
                    canEdit = true,
                    isLoading = false,
                ),
            onBackClick = {},
            onEditClick = {},
            onSearchQueryChange = {},
        )
    }
}

@Preview(name = "Проект - Архив", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ProjectDetailScreen_Archived_Preview() {
    CoFlowTheme {
        ProjectDetailScreen(
            state =
                ProjectDetailUiState(
                    projectId = "2",
                    title = "Редизайн лендинга",
                    description = "",
                    status = ProjectStatusUi.ARCHIVED,
                    ownerName = "Елена Козлова",
                    participants = emptyList(),
                    canEdit = false,
                    isLoading = false,
                ),
            onBackClick = {},
            onEditClick = {},
            onSearchQueryChange = {},
        )
    }
}
