package com.example.coflow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowShapes

/*
 * ОБЩИЙ UI-НАБОР ПРОФИЛЯ (не менять без согласования).
 *
 * Здесь только переиспользуемые элементы двух экранов:
 * - строка меню, разделитель, переключатель, круглая иконка;
 * - стеклянное поле ввода профиля.
 *
 * Экраны и ViewModel лежат в отдельных файлах:
 * - профиль: ProfileScreen.kt / ProfileViewModel.kt;
 * - редактирование: EditProfileScreen.kt / EditProfileViewModel.kt.
 */

/**
 * Круглая «таблетка» с иконкой: используется справа в строках меню
 * («Выйти из аккаунта», «Сменить пароль») и в карточке пользователя («карандаш»).
 */
@Composable
fun ProfileIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = CoFlowColors.Blue,
    containerColor: Color = CoFlowColors.Blue.copy(alpha = 0.10f),
    containerSize: Dp = 36.dp,
    iconSize: Dp = 18.dp,
) {
    Box(
        modifier =
            modifier
                .size(containerSize)
                .clip(CircleShape)
                .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

/**
 * Строка меню внутри стеклянной карточки.
 *
 * @param leadingIcon иконка слева (у переключателей);
 * @param trailing содержимое справа (переключатель или [ProfileIconBadge]).
 */
@Composable
fun ProfileMenuRow(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = CoFlowColors.TextPrimary,
    titleColor: Color = CoFlowColors.TextPrimary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
            Spacer(Modifier.width(12.dp))
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
        )

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** Разделитель между строками меню. */
@Composable
fun ProfileMenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 4.dp),
        thickness = 1.dp,
        color = CoFlowColors.TextSecondary.copy(alpha = 0.12f),
    )
}

/** Переключатель в фирменных цветах CoFlow. */
@Composable
fun ProfileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CoFlowColors.Blue,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFDCE6F7),
                uncheckedBorderColor = Color.Transparent,
            ),
    )
}

/**
 * Стеклянное поле ввода для экрана редактирования профиля
 * (и для поля пароля в диалоге удаления аккаунта).
 *
 * Повторяет визуал [com.example.coflow.ui.components.CoFlowGlassTextField],
 * но принимает любую иконку слева (в том числе Material) — поэтому живёт здесь.
 */
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CoFlowColors.TextPrimary,
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = CoFlowShapes.Input,
                        ambientColor = CoFlowGlassStyles.Shadow,
                        spotColor = CoFlowGlassStyles.Shadow,
                    ).clip(CoFlowShapes.Input)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = 0.72f),
                                        Color(0xFFF5F8FF).copy(alpha = 0.58f),
                                    ),
                            ),
                    ).border(
                        width = 1.dp,
                        color = if (isError) CoFlowColors.Error else CoFlowGlassStyles.BlueBorder,
                        shape = CoFlowShapes.Input,
                    ),
            textStyle = TextStyle(color = CoFlowColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(CoFlowColors.Blue),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = CoFlowColors.TextSecondary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(text = placeholder, color = CoFlowColors.TextSecondary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
            },
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                fontSize = 11.sp,
                color = CoFlowColors.Error,
            )
        }
    }
}
