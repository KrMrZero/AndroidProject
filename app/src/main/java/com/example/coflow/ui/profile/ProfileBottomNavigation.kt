package com.example.coflow.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.coflow.R
import com.example.coflow.ui.components.CoFlowBottomNavigationBar
import com.example.coflow.ui.components.NavItem

/*
 * ОБЩИЙ ФАЙЛ (не менять без согласования).
 *
 * Нижняя навигация используется тремя экранами: профиль, редактирование профиля
 * и уведомления. Любое изменение здесь затрагивает все три зоны.
 */

/**
 * Вкладки нижней навигации, которые встречаются на экранах профиля.
 * Нужны, чтобы каждый экран мог подсветить свой раздел.
 */
enum class ProfileTab { TASKS, PROJECTS, PROFILE }

/**
 * Нижняя навигация в стиле CoFlow.
 * Повторяет [CoFlowBottomNavigationBar], но сразу знает про вкладки профиля.
 *
 * Экран не навигирует сам: он лишь сообщает выбранную вкладку через [onTabSelected].
 */
@Composable
fun ProfileBottomBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    CoFlowBottomNavigationBar(
        modifier = modifier,
        items =
            listOf(
                NavItem(
                    icon = "tasks",
                    title = stringResource(R.string.navigation_tasks),
                    selected = selectedTab == ProfileTab.TASKS,
                    onClick = { onTabSelected(ProfileTab.TASKS) },
                ),
                NavItem(
                    icon = "folder",
                    title = stringResource(R.string.navigation_projects),
                    selected = selectedTab == ProfileTab.PROJECTS,
                    onClick = { onTabSelected(ProfileTab.PROJECTS) },
                ),
                NavItem(
                    icon = "person",
                    title = stringResource(R.string.navigation_profile),
                    selected = selectedTab == ProfileTab.PROFILE,
                    onClick = { onTabSelected(ProfileTab.PROFILE) },
                ),
            ),
    )
}
