package com.example.coflow.data

import java.time.LocalDate
import com.example.coflow.models.TaskStatus

/**
 * Временные данные для проверки экрана задач.
 * Позже этот объект заменят данными из репозитория/бэкенда.
 */
object TasksTestData {
    private val today = LocalDate.now()

    val tasks = listOf(
        Task(
            id = 1,
            title = "Согласовать план проекта",
            projectName = "Проект 1",
            dueDate = today,
            status = TaskStatus.TODO,
            isMine = true,
            description = "Обсудить с командой этапы работы, разбить на подзадачи и распределить роли.",
            author = "Вы",
            assignees = listOf("Вы", "Анна Смирнова")
        ),
        Task(
            id = 2,
            title = "Подготовить макеты",
            projectName = "Проект 1",
            dueDate = today.plusDays(1),
            status = TaskStatus.TODO,
            isMine = true,
            description = "Отрисовать экраны в Figma согласно ТЗ и дизайн-системе.",
            author = "Вы",
            assignees = listOf("Вы")
        ),
        Task(
            id = 3,
            title = "Написать отчет по практике",
            projectName = "Мои",
            dueDate = today.minusDays(2),
            status = TaskStatus.TODO,
            isMine = true,
            description = "Собрать все разделы, проверить оформление по ГОСТу.",
            author = "Вы",
            assignees = emptyList()
        ),
        Task(
            id = 4,
            title = "Купить кофе для команды",
            projectName = "Мои",
            dueDate = today,
            status = TaskStatus.TODO,
            isMine = false,
            description = "Заказать зерна на Озоне.",
            author = "Петр",
            assignees = listOf("Петр")
        ),

        Task(
            id = 5,
            title = "Разработать экран авторизации",
            projectName = "Проект 2",
            dueDate = today.plusDays(2),
            status = TaskStatus.IN_PROGRESS,
            isMine = true,
            description = "Сверстать по макету, подключить Firebase Auth, обработать ошибки ввода.",
            author = "Вы",
            assignees = listOf("Вы", "Иван Петров", "Анна Смирнова")
        ),
        Task(
            id = 6,
            title = "Исправить баг с навигацией",
            projectName = "Проект 2",
            dueDate = today.plusDays(3),
            status = TaskStatus.IN_PROGRESS,
            isMine = false,
            description = "При переходе назад с экрана создания задачи приложение вылетает.",
            author = "Иван Петров",
            assignees = listOf("Анна Смирнова")
        ),
        Task(
            id = 7,
            title = "Подготовить презентацию",
            projectName = "Мои",
            dueDate = today.plusDays(5),
            status = TaskStatus.IN_PROGRESS,
            isMine = true,
            description = "Сделать слайды для защиты проекта, добавить графики и скриншоты.",
            author = "Вы",
            assignees = listOf("Вы")
        ),
        Task(
            id = 8,
            title = "Проверить пулл-реквест",
            projectName = "Проект 1",
            dueDate = today.plusDays(4),
            status = TaskStatus.IN_PROGRESS,
            isMine = false,
            description = "Посмотреть изменения в ветке feature/auth, оставить комментарии.",
            author = "Мария Иванова",
            assignees = listOf("Вы", "Иван Петров")
        ),

        Task(
            id = 9,
            title = "Настроить базу данных",
            projectName = "Проект 1",
            dueDate = today.minusDays(5),
            status = TaskStatus.DONE,
            isMine = false,
            description = "Подключить Firestore, создать коллекции пользователей и задач.",
            author = "Анна Смирнова",
            assignees = listOf("Иван Петров")
        ),
        Task(
            id = 10,
            title = "Утвердить дизайн-систему",
            projectName = "Проект 2",
            dueDate = today.minusDays(3),
            status = TaskStatus.DONE,
            isMine = true,
            description = "Согласовать цвета, шрифты и базовые компоненты с командой.",
            author = "Вы",
            assignees = listOf("Вы", "Мария Иванова")
        ),
        Task(
            id = 11,
            title = "Сдать лабораторную работу",
            projectName = "Мои",
            dueDate = today.minusDays(1),
            status = TaskStatus.DONE,
            isMine = true,
            description = "Загрузить отчет в систему и защитить перед преподавателем.",
            author = "Вы",
            assignees = emptyList()
        ),
        Task(
            id = 12,
            title = "Написать документацию",
            projectName = "Проект 2",
            dueDate = today.plusDays(10),
            status = TaskStatus.DONE,
            isMine = false,
            description = "Описать API, структуру БД и инструкцию по запуску проекта.",
            author = "Мария Иванова",
            assignees = listOf("Анна Смирнова", "Иван Петров")
        )
    )
}
