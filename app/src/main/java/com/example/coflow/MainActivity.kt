package com.example.coflow

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coflow.repositories.FirebaseAuthRepositoryImpl
import com.example.coflow.ui.auth.AuthRoute
import com.example.coflow.ui.components.CoFlowBottomNavigationBar
import com.example.coflow.ui.components.NavItem
import com.example.coflow.ui.profile.EditProfileRoute
import com.example.coflow.ui.profile.NotificationsRoute
import com.example.coflow.ui.profile.NotificationsViewModel
import com.example.coflow.ui.profile.ProfileRoute
import com.example.coflow.ui.projects.AddParticipantRoute
import com.example.coflow.ui.projects.AddProjectParticipantViewModel
import com.example.coflow.ui.projects.CreateProjectRoute
import com.example.coflow.ui.projects.EditProjectRoute
import com.example.coflow.ui.projects.ProjectDetailRoute
import com.example.coflow.ui.projects.ProjectTasksRoute
import com.example.coflow.ui.projects.ProjectsRoute
import com.example.coflow.ui.tasks.ChooseAssigneeRoute
import com.example.coflow.ui.tasks.ChooseDateRoute
import com.example.coflow.ui.tasks.ChooseProjectRoute
import com.example.coflow.ui.tasks.CreateTaskRoute
import com.example.coflow.ui.tasks.CreateTaskViewModel
import com.example.coflow.ui.tasks.EditTaskRoute
import com.example.coflow.ui.tasks.EditTaskViewModel
import com.example.coflow.ui.tasks.TaskDetailRoute
import com.example.coflow.ui.tasks.TasksRoute
import com.example.coflow.ui.tasks.TasksScreenViewModel
import com.example.coflow.ui.theme.CoFlowIndication
import com.example.coflow.ui.theme.CoFlowTheme

object Screen {
    const val TASKS = "tasks"
    const val CREATE_TASK = "create_task"
    const val CHOOSE_ASSIGNEE = "choose_assignee"
    const val CHOOSE_PROJECT = "choose_project"
    const val CHOOSE_DATE = "choose_date"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val PROJECTS = "projects"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val CREATE_PROJECT = "create_project"
    const val EDIT_TASK = "edit_task/{taskId}"

    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val NOTIFICATIONS = "notifications"

    const val EDIT_PROJECT = "edit_project/{projectId}"
    const val PROJECT_TASKS = "project_tasks/{projectId}"

    const val EDIT_TASK_ASSIGNEES = "edit_task/{taskId}/assignees"
    const val EDIT_TASK_PROJECT = "edit_task/{taskId}/project"
    const val EDIT_TASK_DATE = "edit_task/{taskId}/date"

    const val ADD_PROJECT_PARTICIPANT = "add_project_participant?projectId={projectId}"

    fun addProjectParticipant(projectId: String = "") = "add_project_participant?projectId=$projectId"
}

class MainActivity : ComponentActivity() {
    @SuppressLint("ComposableDestinationInComposeScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CoFlowTheme {
                CompositionLocalProvider(
                    LocalIndication provides CoFlowIndication,
                ) {
                    val authRepository = remember { FirebaseAuthRepositoryImpl() }

                    var isAuthenticated by remember {
                        mutableStateOf(
                            runCatching { authRepository.getCurrentUserUid() != null }
                                .getOrDefault(false),
                        )
                    }

                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        val createTaskViewModel: CreateTaskViewModel = viewModel()
                        val tasksViewModel: TasksScreenViewModel = viewModel()

                        // Один экземпляр на всё приложение: экран уведомлений и все
                        // колокольчики показывают одно и то же состояние.
                        val notificationsViewModel: NotificationsViewModel = viewModel()
                        val notificationsState by notificationsViewModel.state.collectAsStateWithLifecycle()

                        // ViewModel-и живут в Activity и переживают выход из аккаунта,
                        // поэтому после каждого входа подписываемся заново: иначе
                        // останутся данные предыдущего пользователя.
                        LaunchedEffect(Unit) {
                            tasksViewModel.refresh()
                        }

                        // Счётчик непрочитанных обновляем при каждом возврате
                        // в приложение: это один документ пользователя плюс агрегат,
                        // а не чтение всего списка уведомлений.
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner) {
                            val observer =
                                LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        notificationsViewModel.refreshUnread()
                                    }
                                }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        val notificationsHandler: (String) -> Unit =
                            run {
                                val context = LocalContext.current
                                { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                            }

                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        // Колокольчик обновляем при переходах между экранами:
                        // один документ пользователя плюс агрегат, без списка.
                        // На экране уведомлений счётчик обновляет сам экран.
                        LaunchedEffect(currentRoute) {
                            if (currentRoute != null && currentRoute != Screen.NOTIFICATIONS) {
                                notificationsViewModel.refreshUnread()
                            }
                        }

                        val showBottomBar =
                            currentRoute == Screen.TASKS ||
                                currentRoute == Screen.PROJECTS ||
                                currentRoute == Screen.PROJECT_TASKS ||
                                currentRoute == Screen.PROFILE

                        Column(Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.TASKS,
                                modifier = Modifier.weight(1f),
                            ) {
                                // Мои задачи
                                composable(Screen.TASKS) {
                                    TasksRoute(
                                        viewModel = tasksViewModel,
                                        onNavigateToCreateTask = { navController.navigate(Screen.CREATE_TASK) },
                                        onNavigateToTaskDetail = { taskId ->
                                            navController.navigate("task_detail/$taskId")
                                        },
                                        onNavigateToProjects = { navController.navigateToTab(Screen.PROJECTS) },
                                        onNavigateToProfile = { navController.navigateToTab(Screen.PROFILE) },
                                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                                        onTaskSwipe = tasksViewModel::onTaskSwipe,
                                        hasUnreadNotifications = notificationsState.hasUnread,
                                    )
                                }

                                // Проекты
                                composable(Screen.PROJECTS) {
                                    ProjectsRoute(
                                        onNavigateToCreateProject = { navController.navigate(Screen.CREATE_PROJECT) },
                                        onNavigateToTasks = { navController.navigateToTab(Screen.TASKS) },
                                        onNavigateToProjectTasks = { projectId ->
                                            navController.navigate("project_tasks/$projectId")
                                        },
                                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                                        hasUnreadNotifications = notificationsState.hasUnread,
                                    )
                                }

                                // Просмотр деталей проекта
                                composable(
                                    route = Screen.PROJECT_DETAIL,
                                    arguments =
                                        listOf(
                                            navArgument("projectId") {
                                                type = NavType.StringType
                                            },
                                        ),
                                ) {
                                    ProjectDetailRoute(
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToEdit = { projectId -> navController.navigate("edit_project/$projectId") },
                                    )
                                }

                                // Редактирование проекта
                                composable(
                                    route = Screen.EDIT_PROJECT,
                                    arguments =
                                        listOf(
                                            navArgument("projectId") {
                                                type = NavType.StringType
                                            },
                                        ),
                                ) {
                                    EditProjectRoute(
                                        onNavigateBack = { navController.popBackStack() },
                                        onProjectSaved = {
                                            navController.popBackStack()
                                        },
                                        onProjectDeleted = {
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Экран задач проекта
                                composable(
                                    route = Screen.PROJECT_TASKS,
                                    arguments =
                                        listOf(
                                            navArgument("projectId") {
                                                type = NavType.StringType
                                            },
                                        ),
                                ) { backStackEntry ->
                                    ProjectTasksRoute(
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToCreateTask = { navController.navigate("create_task") },
                                        onNavigateToTaskDetail = { taskId ->
                                            navController.navigate("task_detail/$taskId")
                                        },
                                        onNavigateToTasks = { navController.navigateToTab(Screen.TASKS) },
                                        onNavigateToProjects = { navController.popBackStack() },
                                        onNavigateToProfile = { navController.navigateToTab(Screen.PROFILE) },
                                        onNavigateToProjectDetail = { projectId ->
                                            navController.navigate(
                                                "project_detail/$projectId",
                                            )
                                        },
                                    )
                                }

                                // Экран создания проекта
                                composable(Screen.CREATE_PROJECT) { backStackEntry ->
                                    val addParticipantViewModel: AddProjectParticipantViewModel =
                                        viewModel(viewModelStoreOwner = backStackEntry)

                                    CreateProjectRoute(
                                        onBackClick = { navController.popBackStack() },
                                        onProjectCreated = { navController.popBackStack() },
                                        onAddParticipantClick = {
                                            navController.navigate(Screen.addProjectParticipant())
                                        },
                                        addParticipantViewModel = addParticipantViewModel,
                                    )
                                }

                                // Экран добавления участника
                                composable(
                                    route = Screen.ADD_PROJECT_PARTICIPANT,
                                    arguments =
                                        listOf(
                                            navArgument("projectId") {
                                                type = NavType.StringType
                                                defaultValue = ""
                                                nullable = true
                                            },
                                        ),
                                ) { backStackEntry ->
                                    val projectId =
                                        backStackEntry.arguments?.getString("projectId").orEmpty()

                                    if (projectId.isBlank()) {
                                        val createProjectEntry =
                                            remember(backStackEntry) {
                                                runCatching { navController.getBackStackEntry(Screen.CREATE_PROJECT) }.getOrNull()
                                            }

                                        if (createProjectEntry != null) {
                                            val addParticipantViewModel: AddProjectParticipantViewModel =
                                                viewModel(viewModelStoreOwner = createProjectEntry)

                                            AddParticipantRoute(
                                                projectId = "",
                                                onBackClick = { navController.popBackStack() },
                                                onInvited = { navController.popBackStack() },
                                                viewModel = addParticipantViewModel,
                                            )
                                        } else {
                                            AddParticipantRoute(
                                                projectId = "",
                                                onBackClick = { navController.popBackStack() },
                                                onInvited = { navController.popBackStack() },
                                            )
                                        }
                                    } else {
                                        AddParticipantRoute(
                                            projectId = projectId,
                                            onBackClick = { navController.popBackStack() },
                                            onInvited = { navController.popBackStack() },
                                        )
                                    }
                                }

                                // Просмотр задачи
                                composable(
                                    route = Screen.TASK_DETAIL,
                                    arguments =
                                        listOf(
                                            navArgument("taskId") {
                                                type = NavType.StringType
                                            },
                                        ),
                                ) {
                                    TaskDetailRoute(
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToEdit = { taskId -> navController.navigate("edit_task/$taskId") },
                                    )
                                }

                                // Создание задачи
                                composable(Screen.CREATE_TASK) {
                                    CreateTaskRoute(
                                        onBackClick = { navController.popBackStack() },
                                        onTaskCreated = {
                                            tasksViewModel.refresh()
                                            navController.popBackStack()
                                        },
                                        onNavigateToAssignees = { navController.navigate(Screen.CHOOSE_ASSIGNEE) },
                                        onNavigateToProject = { navController.navigate(Screen.CHOOSE_PROJECT) },
                                        onNavigateToDate = { navController.navigate(Screen.CHOOSE_DATE) },
                                        viewModel = createTaskViewModel,
                                    )
                                }

                                // ИСПРАВЛЕНО: Редактирование задачи (добавлен backStackEntry и реальные навигации)
                                composable(
                                    route = Screen.EDIT_TASK,
                                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                                ) { backStackEntry ->
                                    val taskId = backStackEntry.arguments?.getString("taskId").orEmpty()
                                    EditTaskRoute(
                                        onNavigateBack = { navController.popBackStack() },
                                        onTaskSaved = {
                                            tasksViewModel.refresh()
                                            navController.popBackStack()
                                        },
                                        onTaskDeleted = {
                                            tasksViewModel.refresh()
                                            navController.popBackStack()
                                        },
                                        onNavigateToAssignees = { navController.navigate("edit_task/$taskId/assignees") },
                                        onNavigateToProject = { navController.navigate("edit_task/$taskId/project") },
                                        onNavigateToDate = { navController.navigate("edit_task/$taskId/date") },
                                    )
                                }

                                // Выбор исполнителей при редактировании задачи
                                composable(
                                    route = Screen.EDIT_TASK_ASSIGNEES,
                                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                                ) {
                                    val parentEntry =
                                        remember(it) {
                                            navController.getBackStackEntry("edit_task/${it.arguments?.getString("taskId")}")
                                        }
                                    val editViewModel: EditTaskViewModel = viewModel(parentEntry)
                                    val state by editViewModel.state.collectAsStateWithLifecycle()

                                    ChooseAssigneeRoute(
                                        state = state,
                                        onBackClick = { navController.popBackStack() },
                                        onSearchQueryChange = editViewModel::onAssigneeSearchQueryChange,
                                        onToggleAssignee = editViewModel::togglePendingAssignee,
                                        onConfirmSelection = { assignees ->
                                            editViewModel.confirmAssigneeSelection(assignees)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Выбор даты при редактировании задачи
                                composable(
                                    route = Screen.EDIT_TASK_DATE,
                                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                                ) {
                                    val parentEntry =
                                        remember(it) {
                                            navController.getBackStackEntry("edit_task/${it.arguments?.getString("taskId")}")
                                        }
                                    val editViewModel: EditTaskViewModel = viewModel(parentEntry)
                                    val state by editViewModel.state.collectAsStateWithLifecycle()

                                    ChooseDateRoute(
                                        state = state,
                                        onBackClick = { navController.popBackStack() },
                                        onMonthChange = editViewModel::onCalendarMonthChange,
                                        onDateSelected = editViewModel::onDateSelected,
                                        onConfirmSelection = { date ->
                                            editViewModel.confirmDateSelection(date)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Выбор проекта при редактировании задачи
                                composable(
                                    route = Screen.EDIT_TASK_PROJECT,
                                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                                ) {
                                    val parentEntry =
                                        remember(it) {
                                            navController.getBackStackEntry("edit_task/${it.arguments?.getString("taskId")}")
                                        }
                                    val editViewModel: EditTaskViewModel = viewModel(parentEntry)
                                    val state by editViewModel.state.collectAsStateWithLifecycle()

                                    ChooseProjectRoute(
                                        state = state,
                                        onBackClick = { navController.popBackStack() },
                                        onSearchQueryChange = editViewModel::onProjectSearchQueryChange,
                                        onSelectProject = editViewModel::onPendingProjectChange,
                                        onConfirmSelection = { project ->
                                            editViewModel.confirmProjectSelection(project)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Выбор исполнителей (Создание)
                                composable(Screen.CHOOSE_ASSIGNEE) {
                                    val createState by createTaskViewModel.state.collectAsStateWithLifecycle()
                                    ChooseAssigneeRoute(
                                        state = createState,
                                        onBackClick = { navController.popBackStack() },
                                        onSearchQueryChange = createTaskViewModel::onAssigneeSearchQueryChange,
                                        onToggleAssignee = createTaskViewModel::togglePendingAssignee,
                                        onConfirmSelection = { assignees ->
                                            createTaskViewModel.confirmAssigneeSelection(assignees)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Выбор проекта (Создание)
                                composable(Screen.CHOOSE_PROJECT) {
                                    val createState by createTaskViewModel.state.collectAsStateWithLifecycle()
                                    ChooseProjectRoute(
                                        state = createState,
                                        onBackClick = { navController.popBackStack() },
                                        onSearchQueryChange = createTaskViewModel::onProjectSearchQueryChange,
                                        onSelectProject = createTaskViewModel::onPendingProjectChange,
                                        onConfirmSelection = { project ->
                                            createTaskViewModel.confirmProjectSelection(project)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Выбор даты (Создание)
                                composable(Screen.CHOOSE_DATE) {
                                    val createState by createTaskViewModel.state.collectAsStateWithLifecycle()
                                    ChooseDateRoute(
                                        state = createState,
                                        onBackClick = { navController.popBackStack() },
                                        onMonthChange = createTaskViewModel::onCalendarMonthChange,
                                        onDateSelected = createTaskViewModel::onDateSelected,
                                        onConfirmSelection = { date ->
                                            createTaskViewModel.confirmDateSelection(date)
                                            navController.popBackStack()
                                        },
                                    )
                                }

                                // Профиль
                                composable(Screen.PROFILE) {
                                    ProfileRoute(
                                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                                        onNavigateToEditProfile = { navController.navigate(Screen.EDIT_PROFILE) },
                                        hasUnreadNotifications = notificationsState.hasUnread,
                                        onChangePasswordClick = {
                                            notificationsHandler("Смена пароля пока недоступна")
                                        },
                                        onAccountDeleted = {
                                            notificationsHandler("Аккаунт удалён")
                                            notificationsViewModel.reset()
                                            isAuthenticated = false
                                        },
                                        onLoggedOut = {
                                            notificationsViewModel.reset()
                                            isAuthenticated = false
                                        },
                                    )
                                }

                                // Изменение профиля
                                composable(Screen.EDIT_PROFILE) {
                                    EditProfileRoute(
                                        onNavigateBack = { navController.popBackStack() },
                                        onProfileSaved = { navController.popBackStack() },
                                        onNavigateToNotifications = { navController.navigate(Screen.NOTIFICATIONS) },
                                        hasUnreadNotifications = notificationsState.hasUnread,
                                    )
                                }

                                // Уведомления
                                composable(Screen.NOTIFICATIONS) {
                                    NotificationsRoute(
                                        viewModel = notificationsViewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                    )
                                }
                            }

                            // Нижняя панель навигации
                            if (showBottomBar) {
                                CoFlowBottomNavigationBar(
                                    items =
                                        listOf(
                                            NavItem(
                                                "tasks",
                                                stringResource(R.string.navigation_tasks),
                                                selected = currentRoute == Screen.TASKS,
                                                onClick = { navController.navigateToTab(Screen.TASKS) },
                                            ),
                                            NavItem(
                                                "folder",
                                                stringResource(R.string.navigation_projects),
                                                selected =
                                                    currentRoute == Screen.PROJECTS ||
                                                        currentRoute == Screen.PROJECT_TASKS,
                                                onClick = { navController.navigateToTab(Screen.PROJECTS) },
                                            ),
                                            NavItem(
                                                "person",
                                                stringResource(R.string.navigation_profile),
                                                selected = currentRoute == Screen.PROFILE,
                                                onClick = { navController.navigateToTab(Screen.PROFILE) },
                                            ),
                                        ),
                                    modifier = Modifier.navigationBarsPadding(),
                                )
                            }
                        }
                    } else {
                        AuthRoute(onAuthSuccess = { isAuthenticated = true })
                    }
                }
            }
        }
    }

    private fun NavHostController.navigateToTab(route: String) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
