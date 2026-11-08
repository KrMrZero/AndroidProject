package com.example.coflow.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

data class Participant(
    val email: String,
    val name: String = "Пользователь",
    val initials: String = email.take(2).uppercase()
)

@Composable
fun AddParticipantScreen(
    onBackClick: () -> Unit,
    onSaveClick: (List<Participant>) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    var emailInput by remember { mutableStateOf("") }
    var participants by remember {
        mutableStateOf(
            listOf(
                Participant("anna.s@example.com", "Анна Смирнова", "AC"),
                Participant("sixseven@example.com", "Six Seven", "SS"),
                Participant("bebra@example.com", "Bebra", "BB")
            )
        )
    }

    // Системная кнопка "Назад"
    BackHandler(onBack = { onBackClick() })

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = CoFlowSpacing.ScreenHorizontal),
        ) {
            // Заголовок экрана
            CoFlowScreenHeader(
                title = stringResource(R.string.add_participant_header), // "Добавить участника"
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )

            Spacer(Modifier.height(10.dp))

            // Список приглашенных пользователей
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = participants,
                    key = { it.email },
                ) { participant ->
                    ParticipantItem(
                        participant = participant,
                        onRemove = {
                            // Удаление из локального списка
                            participants = participants.filter { it.email != participant.email }
                        }
                    )
                }
            }

            // Нижняя панель: Поле ввода + кнопка "+" и Кнопка "Сохранить"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Поле ввода и кнопка +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoFlowGlassTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = stringResource(R.string.enter_email_placeholder), // "Введите почту..."
                        leadingIcon = "search",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            // Логика добавления по Enter
                            addParticipantLocally(emailInput, participants) { newEmail, newList ->
                                emailInput = newEmail
                                participants = newList
                            }
                        }),
                    )

                    // Кнопка "+"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CoFlowColors.Blue)
                            .clickable {
                                // Логика добавления по кнопке
                                addParticipantLocally(emailInput, participants) { newEmail, newList ->
                                    emailInput = newEmail
                                    participants = newList
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add participant",
                            tint = Color.White
                        )
                    }
                }

                // Кнопка подтверждения "Сохранить"
                CoFlowPrimaryButton(
                    text = stringResource(R.string.save_participant_button), // "Сохранить"
                    onClick = {
                        focusManager.clearFocus()
                        onSaveClick(participants)
                    },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Вспомогательная функция для локального добавления (чтобы не дублировать код)
private fun addParticipantLocally(
    currentEmail: String,
    currentList: List<Participant>,
    onUpdate: (String, List<Participant>) -> Unit
) {
    val email = currentEmail.trim()
    if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        if (currentList.none { it.email == email }) {
            val newParticipant = Participant(email = email, name = "Новый участник")
            onUpdate("", currentList + newParticipant) // Очищаем поле и добавляем участника
        }
    }
}

@Composable
fun ParticipantItem(
    participant: Participant,
    onRemove: () -> Unit
) {
    CoFlowGlassCard(
        modifier = Modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Левая часть: Аватар + Текст
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Кружок с инициалами
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CoFlowColors.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.initials,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = participant.email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoFlowColors.TextPrimary
                    )
                    Text(
                        text = participant.name,
                        fontSize = 12.sp,
                        color = CoFlowColors.TextSecondary
                    )
                }
            }

            // Правая часть: Кнопка удаления (красный минус)
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircle,
                    contentDescription = "Remove",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(
    name = "Добавление участника",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun AddParticipantScreenPreview() {
    CoFlowTheme {
        AddParticipantScreen(
            onBackClick = {},
            onSaveClick = {}
        )
    }
}