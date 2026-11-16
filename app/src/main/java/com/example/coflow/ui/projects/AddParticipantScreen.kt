package com.example.coflow.ui.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowGlassTextField
import com.example.coflow.ui.components.CoFlowPrimaryButton
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

@Composable
fun AddProjectParticipantScreen(
    state: AddProjectParticipantUiState,
    onBackClick: () -> Unit,
    onEmailQueryChange: (String) -> Unit,
    onAddParticipantClick: () -> Unit,
    onRemoveParticipantClick: (ProjectParticipantItem) -> Unit,
    onInviteClick: () -> Unit,
    onClearError: () -> Unit,
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
                title = stringResource(R.string.add_participant_header),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            Spacer(Modifier.height(10.dp))

            // Список уже добавленных (но ещё не приглашённых) участников
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = state.participants,
                    key = { it.uid.ifBlank { it.email } },
                ) { participant ->
                    ProjectParticipantRow(
                        participant = participant,
                        onRemove = { onRemoveParticipantClick(participant) },
                    )
                }
            }

            // Ошибка (из строкового ресурса или сырая)
            val errorText =
                state.errorMessageResId?.let { stringResource(it) }
                    ?: state.errorMessage
            if (errorText != null) {
                Text(
                    text = errorText,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = CoFlowColors.Error,
                )
                LaunchedEffect(errorText) { onClearError() }
            }

            // Нижняя панель: поле ввода + кнопка "+", кнопка "Сохранить"
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoFlowGlassTextField(
                        value = state.emailQuery,
                        onValueChange = onEmailQueryChange,
                        placeholder = stringResource(R.string.enter_email_placeholder),
                        leadingIcon = "search",
                        modifier = Modifier.weight(1f),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                onAddParticipantClick()
                            }),
                    )

                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CoFlowColors.Blue)
                                .clickable(enabled = !state.isLoading) {
                                    onAddParticipantClick()
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_participant_header),
                                tint = Color.White,
                            )
                        }
                    }
                }

                CoFlowPrimaryButton(
                    text = stringResource(R.string.save_participant_button),
                    onClick = {
                        focusManager.clearFocus()
                        onInviteClick()
                    },
                    enabled = state.canInvite,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProjectParticipantRow(
    participant: ProjectParticipantItem,
    onRemove: () -> Unit,
) {
    CoFlowGlassCard(modifier = Modifier.height(64.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CoFlowColors.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = participant.initials,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = participant.email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoFlowColors.TextPrimary,
                    )
                    Text(
                        text = participant.name,
                        fontSize = 12.sp,
                        color = CoFlowColors.TextSecondary,
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircle,
                    contentDescription = "Remove",
                    tint = Color.Red.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
fun AddParticipantRoute(
    projectId: String,
    onBackClick: () -> Unit,
    onInvited: (List<ProjectParticipantItem>) -> Unit,
    viewModel: AddProjectParticipantViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isInvited) {
        if (state.isInvited) {
            val invited = state.participants
            viewModel.onInvitedHandled()
            onInvited(invited)
        }
    }

    AddProjectParticipantScreen(
        state = state,
        onBackClick = onBackClick,
        onEmailQueryChange = viewModel::onEmailQueryChange,
        onAddParticipantClick = viewModel::onAddParticipantClick,
        onRemoveParticipantClick = viewModel::onRemoveParticipant,
        onInviteClick = viewModel::onInviteClick,
        onClearError = viewModel::clearError,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddProjectParticipantScreenPreview() {
    CoFlowTheme {
        AddProjectParticipantScreen(
            state =
                AddProjectParticipantUiState(
                    emailQuery = "",
                    participants =
                        listOf(
                            ProjectParticipantItem("uid1", "anna.s@example.com", "Анна Смирнова", "АС"),
                            ProjectParticipantItem("uid2", "sixseven@example.com", "Six Seven", "SS"),
                        ),
                    isInitialLoading = false,
                ),
            onBackClick = {},
            onEmailQueryChange = {},
            onAddParticipantClick = {},
            onRemoveParticipantClick = {},
            onInviteClick = {},
            onClearError = {},
        )
    }
}
