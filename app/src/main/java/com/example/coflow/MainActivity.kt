package com.example.coflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                                    // ViewModel.onCreateTask принимает лямбду, которая вызовется после успешной валидации
                                    viewModel.onCreateTask { formData ->
                                        // TODO: Здесь вызов репозитория для сохранения formData (например, в Firebase)
                                        // Пока просто выводим в лог и возвращаемся назад
                                        println("Задача создана: $formData")
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        // Экран выбора исполнителей
                        composable(Screen.CHOOSE_ASSIGNEE) {
                            // 1. Получаем доступ к тому же самому CreateTaskViewModel, который работает на экране создания
                            val createTaskEntry = remember(it) {
                                navController.getBackStackEntry(Screen.CREATE_TASK)
                            }
                            val createTaskViewModel: CreateTaskViewModel = viewModel(createTaskEntry)

                            ChoseAssigneeScreen(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { selectedAssignee ->
                                    // 2. Сохраняем выбранное значение прямо в ViewModel
                                    createTaskViewModel.onAssigneesChange(selectedAssignee)
                                    // 3. Возвращаемся назад. Экран создания задачи сам обновится, так как он слушает StateFlow!
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Экран выбора проекта
                        composable(Screen.CHOOSE_PROJECT) {
                            val createTaskEntry = remember(it) {
                                navController.getBackStackEntry(Screen.CREATE_TASK)
                            }
                            val createTaskViewModel: CreateTaskViewModel = viewModel(createTaskEntry)

                            ChoseProjectScreen(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { selectedProject ->
                                    createTaskViewModel.onProjectChange(selectedProject)
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

                            ChoseDateScreen(
                                onBackClick = { navController.popBackStack() },
                                onConfirmSelection = { selectedDate ->
                                    createTaskViewModel.onDueDateChange(selectedDate)
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