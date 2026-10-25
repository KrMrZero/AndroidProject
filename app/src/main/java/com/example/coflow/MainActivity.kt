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
import com.example.coflow.data.TasksTestData
import com.example.coflow.ui.auth.AuthRoute
import com.example.coflow.ui.projects.ProjectsRoute
import com.example.coflow.ui.tasks.ChooseAssigneeRoute
import com.example.coflow.ui.tasks.ChooseDateRoute
import com.example.coflow.ui.tasks.ChooseProjectRoute
import com.example.coflow.ui.tasks.CreateTaskRoute
import com.example.coflow.ui.tasks.CreateTaskViewModel
import com.example.coflow.ui.tasks.TaskDetailScreen
import com.example.coflow.ui.tasks.TasksScreen
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
                            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getInt("taskId")
                            // Ищем задачу в тестовых данных по ID
                            val task = TasksTestData.tasks.find { it.id == taskId }

                            if (task != null) {
                                TaskDetailScreen(
                                    task = task,
                                    onBackClick = { navController.popBackStack() },
                                    onEditClick = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Редактирование ляля",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
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
