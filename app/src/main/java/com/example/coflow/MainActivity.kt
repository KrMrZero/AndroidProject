package com.example.coflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.coflow.ui.auth.AuthRoute
import com.example.coflow.ui.tasks.ChoseAssigneeScreen
import com.example.coflow.ui.tasks.ChoseDateScreen
import com.example.coflow.ui.tasks.ChoseProjectScreen
import com.example.coflow.ui.tasks.CreateTaskScreen
import com.example.coflow.ui.tasks.TasksScreen
import com.example.coflow.ui.theme.CoFlowTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.ui.tasks.CreateTaskViewModel

object Screen {
    const val TASKS = "tasks"
    const val CREATE_TASK = "create_task"
    const val CHOOSE_ASSIGNEE = "choose_assignee"
    const val CHOOSE_PROJECT = "choose_project"
    const val CHOOSE_DATE = "choose_date"
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

                    NavHost(navController = navController, startDestination = Screen.TASKS) {
                        composable(Screen.TASKS) {
                            TasksScreen(
                                onNavigateToCreateTask = { navController.navigate(Screen.CREATE_TASK) }
                            )
                        }

                        composable(Screen.CREATE_TASK) {
                            // 1. Инициализируем ViewModel для этого экрана
                            val viewModel: CreateTaskViewModel = viewModel()

                            // 2. Подписываемся на состояние (state)
                            val state by viewModel.state.collectAsStateWithLifecycle()

                            // 3. Передаем состояние и методы ViewModel в ваш UI-компонент
                            CreateTaskScreen(
                                state = state, // <--- Передаем само состояние
                                onBackClick = { navController.popBackStack() },

                                // Передаем обработчики изменения полей напрямую из ViewModel
                                onTitleChange = viewModel::onTitleChange,
                                onDescriptionChange = viewModel::onDescriptionChange,

                                onNavigateToAssignees = { navController.navigate(Screen.CHOOSE_ASSIGNEE) },
                                onNavigateToProject = { navController.navigate(Screen.CHOOSE_PROJECT) },
                                onNavigateToDate = { navController.navigate(Screen.CHOOSE_DATE) },

                                // 4. Вызываем метод ViewModel для создания задачи
                                onCreateTask = {
                                    viewModel.onCreateTask { formData ->
                                        println("Задача создана: $formData")
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        /// Экран выбора исполнителей
                        composable(Screen.CHOOSE_ASSIGNEE) {
                            val createTaskEntry = remember(it) {
                                navController.getBackStackEntry(Screen.CREATE_TASK)
                            }
                            val createTaskViewModel: CreateTaskViewModel = viewModel(createTaskEntry)

                            // Инициализируем pending-состояние ОДИН раз
                            LaunchedEffect(Unit) {
                                createTaskViewModel.onOpenAssigneePicker()
                            }

                            ChoseAssigneeScreen(
                                onBackClick = { navController.popBackStack() },
                                viewModel = createTaskViewModel   // <-- передаём общий VM
                            )
                        }

                        // Экран выбора проекта
                        composable(Screen.CHOOSE_PROJECT) {
                            val createTaskEntry = remember(it) {
                                navController.getBackStackEntry(Screen.CREATE_TASK)
                            }
                            val createTaskViewModel: CreateTaskViewModel = viewModel(createTaskEntry)
                            val state by createTaskViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(Unit) {
                                createTaskViewModel.onOpenProjectPicker()
                            }

                            ChoseProjectScreen(
                                state = state,
                                onBackClick = {
                                    createTaskViewModel.cancelProjectSelection()
                                    navController.popBackStack()
                                },
                                onSearchQueryChange = createTaskViewModel::onProjectSearchQueryChange,
                                onSelectProject = createTaskViewModel::onPendingProjectChange,
                                onConfirm = {
                                    createTaskViewModel.confirmProjectSelection()
                                    navController.popBackStack()
                                }
                            )
                        }


                        // Экран выбора даты
                        composable(Screen.CHOOSE_DATE) {
                            val createTaskEntry = remember(it) {
                                navController.getBackStackEntry(Screen.CREATE_TASK)
                            }
                            val createTaskViewModel: CreateTaskViewModel = viewModel(createTaskEntry)
                            val state by createTaskViewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(Unit) {
                                createTaskViewModel.onOpenDatePicker()
                            }

                            ChoseDateScreen(
                                state = state,
                                onBackClick = { navController.popBackStack() },
                                onMonthChange = createTaskViewModel::onCalendarMonthChange,
                                onDateSelected = createTaskViewModel::onDateSelected,
                                onConfirm = {
                                    createTaskViewModel.confirmDateSelection()
                                    navController.popBackStack()
                                }
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