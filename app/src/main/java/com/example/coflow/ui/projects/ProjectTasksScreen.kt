package com.example.coflow.ui.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coflow.R
import com.example.coflow.models.TaskStatus
import com.example.coflow.ui.components.CoFlowBottomNavigationBar
import com.example.coflow.ui.components.CoFlowFabButton
import com.example.coflow.ui.components.CoFlowGlassCard
import com.example.coflow.ui.components.CoFlowGlassIconButton
import com.example.coflow.ui.components.CoFlowIcon
import com.example.coflow.ui.components.CoFlowScreenHeader
import com.example.coflow.ui.components.NavItem
import com.example.coflow.ui.theme.CoFlowColors
import com.example.coflow.ui.theme.CoFlowGlassStyles
import com.example.coflow.ui.theme.CoFlowSpacing
import com.example.coflow.ui.theme.CoFlowTheme
import com.example.coflow.ui.theme.coFlowTasksBackgroundBrush

@Composable
fun ProjectTasksRoute(
    onBackClick: () -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProjectDetail: (String) -> Unit,
    viewModel: ProjectTasksViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { ProjectTasksViewModel(createSavedStateHandle()) }
                },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectTasksScreen(
        state = state,
        onBackClick = onBackClick,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onNavigateToTasks = onNavigateToTasks,
        onNavigateToProjects = onNavigateToProjects,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToProjectDetail = onNavigateToProjectDetail,
        onGroupingChange = viewModel::onGroupingChange,
        onFilterChange = viewModel::onFilterChange,
        onToggleGroup = viewModel::onToggleGroup,
    )
}

@Composable
fun ProjectTasksScreen(
    state: ProjectTasksUiState,
    onBackClick: () -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProjectDetail: (String) -> Unit,
    onGroupingChange: (ProjectTaskGrouping) -> Unit,
    onFilterChange: (ProjectTaskFilterType) -> Unit,
    onToggleGroup: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    BackHandler(onBack = onBackClick)

    Box(modifier = Modifier.fillMaxSize().background(coFlowTasksBackgroundBrush())) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = CoFlowSpacing.ScreenHorizontal)) {
            // Заголовок
            CoFlowScreenHeader(
                title = state.projectTitle.ifBlank { stringResource(R.string.project_tasks_title) },
                subtitle = "${state.activeCount} ${stringResource(
                    R.string.project_tasks_active,
                )} · ${state.completedCount} ${stringResource(R.string.project_tasks_completed)}",
                onBackClick = onBackClick,
                trailingContent = {
                    CoFlowGlassIconButton(
                        iconName = "info",
                        onClick = { onNavigateToProjectDetail(state.projectId) },
                    )
                },
            )

            Spacer(Modifier.height(16.dp))

            // Список задач
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp)
                        .clipToBounds()
                        .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.groups.forEach { group ->
                    ProjectTaskGroupCard(
                        group = group,
                        onToggle = { onToggleGroup(group.id) },
                        onTaskClick = { task -> onNavigateToTaskDetail(task.id) },
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // Нижняя панель (Фильтры, FAB и НИЖНЕЕ МЕНЮ)
            Column(
                modifier =
                    Modifier.fillMaxWidth().padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 2.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.widthIn(max = 330.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProjectSortFilterPanel(
                        modifier = Modifier.weight(1f),
                        grouping = state.currentGrouping,
                        onGroupingChange = onGroupingChange,
                        currentFilter = state.currentFilter,
                        onFilterChange = onFilterChange,
                    )
                    Spacer(Modifier.width(16.dp))
                    CoFlowFabButton(onClick = onNavigateToCreateTask, iconName = "task_plus")
                }

                Spacer(Modifier.height(7.dp))

                // Возвращаем нижнее меню!
                CoFlowBottomNavigationBar(
                    items =
                        listOf(
                            NavItem(
                                icon = "tasks",
                                title = stringResource(R.string.navigation_tasks),
                                selected = false,
                                onClick = onNavigateToTasks,
                            ),
                            NavItem(
                                icon = "folder",
                                title = stringResource(R.string.navigation_projects),
                                selected = true,
                                onClick = onNavigateToProjects,
                            ),
                            NavItem(
                                icon = "person",
                                title = stringResource(R.string.navigation_profile),
                                selected = false,
                                onClick = onNavigateToProfile,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun ProjectTaskGroupCard(
    group: ProjectTaskGroupUi,
    onToggle: () -> Unit,
    onTaskClick: (ProjectTaskRowUi) -> Unit,
) {
    CoFlowGlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(group.titleResId),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoFlowColors.TextPrimary,
            )
            Spacer(Modifier.width(7.dp))
            Box(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(5.dp),
                        ).background(CoFlowColors.BlueLight.copy(alpha = 0.38f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(text = group.tasks.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = CoFlowColors.Blue)
            }
            Spacer(Modifier.weight(1f))
            CoFlowIcon(
                name = if (group.isExpanded) "chevron_down" else "chevron",
                modifier = Modifier.size(18.dp),
                color = CoFlowColors.Blue,
            )
        }

        if (group.isExpanded) {
            if (group.tasks.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(text = stringResource(R.string.no_tasks_placeholder), fontSize = 11.sp, color = CoFlowColors.TextSecondary)
            } else {
                Spacer(Modifier.height(3.dp))
                group.tasks.forEachIndexed { index, task ->
                    ProjectTaskRow(task = task, onClick = { onTaskClick(task) })
                    if (index < group.tasks.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CoFlowGlassStyles.BlueBorder.copy(alpha = 0.45f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectTaskRow(
    task: ProjectTaskRowUi,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.isDone) CoFlowColors.TextSecondary else CoFlowColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (task.isMine) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier =
                        Modifier
                            .clip(
                                RoundedCornerShape(6.dp),
                            ).background(CoFlowColors.Blue.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.project_tasks_mine_badge),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoFlowColors.Blue,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "", fontSize = 10.sp)
            Text(
                text = task.dueDateFormatted,
                fontSize = 10.sp,
                color = if (task.isOverdue) CoFlowColors.Error else CoFlowColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ProjectSortFilterPanel(
    modifier: Modifier = Modifier,
    grouping: ProjectTaskGrouping,
    onGroupingChange: (ProjectTaskGrouping) -> Unit,
    currentFilter: ProjectTaskFilterType,
    onFilterChange: (ProjectTaskFilterType) -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        // Группировка
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Column {
                    Text(text = stringResource(R.string.grouping_header), fontSize = 9.sp, color = CoFlowColors.TextSecondary)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text =
                                if (grouping ==
                                    ProjectTaskGrouping.STATUS
                                ) {
                                    stringResource(R.string.grouping_status)
                                } else {
                                    stringResource(R.string.grouping_deadline)
                                },
                            fontSize = 11.sp,
                            color = CoFlowColors.TextPrimary,
                            maxLines = 1,
                        )
                        CoFlowIcon(
                            name = if (expanded) "chevron_up" else "chevron",
                            modifier = Modifier.size(14.dp),
                            color = CoFlowColors.Blue,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                shape =
                    RoundedCornerShape(
                        16.dp,
                    ),
                containerColor =
                    Color(
                        0xFFF4F8FF,
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                GlassGroupingOption(title = stringResource(R.string.grouping_status), selected = grouping == ProjectTaskGrouping.STATUS) {
                    onGroupingChange(ProjectTaskGrouping.STATUS)
                    expanded =
                        false
                }
                GlassGroupingOption(
                    title = stringResource(R.string.grouping_deadline),
                    selected = grouping == ProjectTaskGrouping.DEADLINE,
                ) {
                    onGroupingChange(ProjectTaskGrouping.DEADLINE)
                    expanded =
                        false
                }
            }
        }

        // Фильтр
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            GlassControlSurface(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Column {
                    Text(text = stringResource(R.string.filter_header), fontSize = 9.sp, color = CoFlowColors.TextSecondary)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text =
                                if (currentFilter ==
                                    ProjectTaskFilterType.ALL
                                ) {
                                    stringResource(R.string.project_tasks_filter_all)
                                } else {
                                    stringResource(R.string.filter_mine)
                                },
                            fontSize = 11.sp,
                            color = CoFlowColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(3.dp))
                        CoFlowIcon(
                            name = if (expanded) "chevron_up" else "chevron",
                            modifier = Modifier.size(14.dp),
                            color = CoFlowColors.Blue,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                shape =
                    RoundedCornerShape(
                        16.dp,
                    ),
                containerColor =
                    Color(
                        0xFFF4F8FF,
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
            ) {
                // ВОТ ТУТ ИСПОЛЬЗУЕМ КРУЖОЧКИ (GlassGroupingOption) ВМЕСТО КВАДРАТИКОВ
                GlassGroupingOption(
                    title = stringResource(R.string.project_tasks_filter_all),
                    selected =
                        currentFilter == ProjectTaskFilterType.ALL,
                ) {
                    onFilterChange(ProjectTaskFilterType.ALL)
                    expanded = false
                }
                GlassGroupingOption(title = stringResource(R.string.filter_mine), selected = currentFilter == ProjectTaskFilterType.MINE) {
                    onFilterChange(ProjectTaskFilterType.MINE)
                    expanded =
                        false
                }
            }
        }
    }
}

@Composable
private fun GlassControlSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(13.dp))
                .background(
                    brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.30f), Color(0xFFDDEAFF).copy(alpha = 0.16f))),
                ).border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.92f), CoFlowColors.Blue.copy(alpha = 0.14f))),
                    shape = RoundedCornerShape(13.dp),
                ).padding(horizontal = 9.dp, vertical = 7.dp),
    ) { content() }
}

@Composable
private fun GlassGroupingOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) CoFlowColors.BlueLight.copy(alpha = 0.14f) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            if (selected) {
                drawCircle(
                    brush =
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6EA5FF), CoFlowColors.Blue),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                )
            } else {
                drawCircle(color = CoFlowGlassStyles.BlueBorder, style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        Spacer(Modifier.width(9.dp))
        Text(text = title, fontSize = 13.sp, color = CoFlowColors.TextPrimary, maxLines = 1)
    }
}

@Preview(name = "Экран задач проекта", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProjectTasksScreenPreview() {
    CoFlowTheme {
        ProjectTasksScreen(
            state =
                ProjectTasksUiState(
                    projectTitle = "Название проекта 1",
                    activeCount = 28,
                    completedCount = 2,
                    groups =
                        listOf(
                            ProjectTaskGroupUi(
                                id = "TODO",
                                titleResId = R.string.tasks_group_planned,
                                count = 3,
                                isExpanded = true,
                                tasks =
                                    listOf(
                                        ProjectTaskRowUi(
                                            "1",
                                            "Согласовать план проекта",
                                            "",
                                            "",
                                            "",
                                            "01.01.27",
                                            TaskStatus.TODO,
                                            isDone = false,
                                            isOverdue = false,
                                            isMine = true,
                                        ),
                                        ProjectTaskRowUi(
                                            "2",
                                            "Подготовить макеты",
                                            "",
                                            "",
                                            "",
                                            "01.01.27",
                                            TaskStatus.TODO,
                                            isDone = false,
                                            isOverdue = false,
                                            isMine = true,
                                        ),
                                        ProjectTaskRowUi(
                                            "3",
                                            "Собрать обратную связь",
                                            "",
                                            "",
                                            "",
                                            "01.01.27",
                                            TaskStatus.TODO,
                                            isDone = false,
                                            isOverdue = false,
                                            isMine = false,
                                        ),
                                    ),
                            ),
                            ProjectTaskGroupUi(
                                id = "IN_PROGRESS",
                                titleResId = R.string.tasks_group_active,
                                count = 25,
                                isExpanded = false,
                                tasks = emptyList(),
                            ),
                            ProjectTaskGroupUi(
                                id = "DONE",
                                titleResId = R.string.tasks_group_completed,
                                count = 2,
                                isExpanded = true,
                                tasks =
                                    listOf(
                                        ProjectTaskRowUi(
                                            "4",
                                            "Отправить материалы",
                                            "",
                                            "",
                                            "",
                                            "01.01.27",
                                            TaskStatus.DONE,
                                            isDone = true,
                                            isOverdue = false,
                                            isMine = false,
                                        ),
                                        ProjectTaskRowUi(
                                            "5",
                                            "Разобрать заметки",
                                            "",
                                            "",
                                            "",
                                            "01.01.27",
                                            TaskStatus.DONE,
                                            true,
                                            isOverdue = false,
                                            isMine = false,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onBackClick = {},
            onNavigateToCreateTask = {},
            onNavigateToTaskDetail = {},
            onNavigateToTasks = {},
            onNavigateToProjects = {},
            onNavigateToProfile = {},
            onGroupingChange = {},
            onFilterChange = {},
            onToggleGroup = {},
            onNavigateToProjectDetail = {},
        )
    }
}
