package com.example.coflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coflow.ui.auth.AuthRoute
import com.example.coflow.ui.projects.ProjectsRoute
import com.example.coflow.ui.tasks.ChooseAssigneeRoute
import com.example.coflow.ui.tasks.ChooseDateRoute
import com.example.coflow.ui.tasks.ChooseProjectRoute
import com.example.coflow.ui.tasks.CreateTaskRoute
import com.example.coflow.ui.tasks.CreateTaskViewModel
import com.example.coflow.ui.tasks.TaskDetailScreen
import com.example.coflow.ui.tasks.TaskDetailUiState
import com.example.coflow.ui.tasks.TaskGroupUi
import com.example.coflow.ui.tasks.TaskGrouping
import com.example.coflow.ui.tasks.TaskRowUi
import com.example.coflow.ui.tasks.TaskStatusUi
import com.example.coflow.ui.tasks.TasksScreen
import com.example.coflow.ui.tasks.TasksUiState
import com.example.coflow.ui.theme.CoFlowTheme

object Screen {
    const val TASKS = "tasks"
    const val CREATE_TASK = "create_task"
    const val CHOOSE_ASSIGNEE = "choose_assignee"
    const val CHOOSE_PROJECT = "choose_project"
    const val CHOOSE_DATE = "choose_date"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val PROJECTS = "projects"
}

class MainActivity : ComponentActivity() {
    @SuppressLint("ComposableDestinationInComposeScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CoFlowTheme {
                var isAuthenticated by remember { mutableStateOf(true) }

                if (isAuthenticated) {
                    val navController = rememberNavController()
                    val createTaskViewModel: CreateTaskViewModel = viewModel()

                    NavHost(navController = navController, startDestination = Screen.TASKS) {
                        // Экран Мои задачи
                        composable(Screen.TASKS) {
                            TasksScreen(
                                onNavigateToCreateTask = { navController.navigate(Screen.CREATE_TASK) },
                                onNavigateToTaskDetail = { taskId -> navController.navigate("task_detail/$taskId") },
                                onNavigateToProjects = { navController.navigate(Screen.PROJECTS) }
                            )
                        }

                        // Экран Проекты
                        composable(Screen.PROJECTS) {
                            ProjectsRoute(
                                onNavigateToCreateProject = {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Добавление проекта ляля",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onNavigateToTasks = { navController.popBackStack() }
                            )
                        }

                        // Экран просмотра задачи
                        composable(
                            route = Screen.TASK_DETAIL,
                        ) { backStackEntry ->
                            val taskId =
                                backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 1

                            val mockTaskDetailState =
                                when (taskId) {
                                    1 -> {
                                        TaskDetailUiState(
                                            title = "Обновить иконки в навигации",
                                            status = TaskStatusUi.TODO,
                                            projectName = "Дизайн-система",
                                            description = "",
                                            author = "Елена Козлова",
                                            assignees = emptyList(),
                                            dueDateFormatted = "30.10.26",
                                            isOverdue = false,
                                            canEdit = true,
                                            showBottomAction = true,
                                            bottomActionText = "Взять задачу",
                                        )
                                    }

                                    3 -> {
                                        TaskDetailUiState(
                                            title = "Разработать новый онбординг для пользователей",
                                            status = TaskStatusUi.IN_PROGRESS,
                                            projectName = "Мобильное приложение",
                                            description = "Нужно сделать 3 экрана с анимацией и кнопкой пропуска.",
                                            author = "Алексей Смирнов",
                                            assignees = listOf("Мария Иванова", "Ты"),
                                            dueDateFormatted = "24.10.26",
                                            isOverdue = true,
                                            canEdit = false,
                                            showBottomAction = true,
                                            bottomActionText = "Выполнить",
                                        )
                                    }

                                    else -> {
                                        TaskDetailUiState(
                                            title = "Провести ревью кода модуля авторизации",
                                            status = TaskStatusUi.DONE,
                                            projectName = "Бэкенд",
                                            description = "Все замечания исправлены, код утвержден.",
                                            author = "Дмитрий Петров",
                                            assignees = listOf("Ты"),
                                            dueDateFormatted = "20.10.26",
                                            isOverdue = false,
                                            canEdit = false,
                                            showBottomAction = false,
                                            bottomActionText = "",
                                        )
                                    }
                                }

                            TaskDetailScreen(
                                state = mockTaskDetailState,
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { /* TODO: Navigate to edit screen */ },
                                onBottomActionClick = { /* TODO: Handle bottom action */ },
                            )
                        }

                        composable(Screen.CREATE_TASK) {
                            CreateTaskRoute(
                                onBackClick = { navController.popBackStack() },
                                onTaskCreated = { formData ->
                                    // TODO: Сохранение задачи через TasksRepository
                                    navController.popBackStack()
                                },
                                onNavigateToAssignees = { navController.navigate(Screen.CHOOSE_ASSIGNEE) },
                                onNavigateToProject = { navController.navigate(Screen.CHOOSE_PROJECT) },
                                onNavigateToDate = { navController.navigate(Screen.CHOOSE_DATE) },
                            )
                        }

                        // Экран создания задачи
                        composable(Screen.CREATE_TASK) {
                            CreateTaskRoute(
                                onBackClick = { navController.popBackStack() },
                                onTaskCreated = { formData ->
                                    // TODO: Сохранение задачи через TasksRepository
                                    navController.popBackStack()
                                },
                                onNavigateToAssignees = { navController.navigate(Screen.CHOOSE_ASSIGNEE) },
                                onNavigateToProject = { navController.navigate(Screen.CHOOSE_PROJECT) },
                                onNavigateToDate = { navController.navigate(Screen.CHOOSE_DATE) },
                                viewModel = createTaskViewModel
                            )
                        }

                        // Экран выбора исполнителей
                        composable(Screen.CHOOSE_ASSIGNEE) {
                            ChooseAssigneeRoute(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { assignees ->
                                    createTaskViewModel.confirmAssigneeSelection(assignees)
                                    navController.popBackStack()
                                },
                                viewModel = createTaskViewModel
                            )
                        }

                        // Экран выбора проекта
                        composable(Screen.CHOOSE_PROJECT) {
                            ChooseProjectRoute(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { project ->
                                    createTaskViewModel.confirmProjectSelection(project)
                                    navController.popBackStack()
                                },
                                viewModel = createTaskViewModel
                            )
                        }

                        // Экран выбора даты
                        composable(Screen.CHOOSE_DATE) {
                            ChooseDateRoute(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { date ->
                                    createTaskViewModel.confirmDateSelection(date)
                                    navController.popBackStack()
                                },
                                viewModel = createTaskViewModel
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