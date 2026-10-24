package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.TaskModel
import com.example.coflow.models.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseTaskRepositoryImplTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        repository = FirebaseTaskRepositoryImpl()
    }

    @Test
    fun testTaskRepositoryWorkflow_Success() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val testProjectId = "test_project_$suffix"
        val uniqueTitle = "Тестовая задача Firestore $suffix"

        val newTask = TaskModel(
            title = uniqueTitle,
            description = "Описание задачи для интеграционного теста",
            projectId = testProjectId,
            authorId = "test_author_$suffix",
            deadline = LocalDateTime.now(ZoneOffset.UTC),
            status = TaskStatus.IN_PROGRESS
        )

        var createdTaskId: String? = null

        try {
            // 1. addTask
            val addResult = repository.addTask(newTask)
            assertTrue(
                "addTask должна пройти успешно: ${addResult.exceptionOrNull()?.message}",
                addResult.isSuccess
            )

            val addedTask = addResult.getOrNull()
            assertNotNull("addTask должна вернуть задачу", addedTask)
            requireNotNull(addedTask)

            createdTaskId = addedTask.id
            assertTrue("ID созданной задачи должен быть заполнен", createdTaskId.isNotBlank())
            assertEquals(uniqueTitle, addedTask.title)
            assertEquals(testProjectId, addedTask.projectId)
            assertEquals(TaskStatus.IN_PROGRESS, addedTask.status)

            // 2. getById
            val byIdResult = repository.getById(createdTaskId)
            assertTrue(
                "getById должен вернуть успех: ${byIdResult.exceptionOrNull()?.message}",
                byIdResult.isSuccess
            )
            val fetched = byIdResult.getOrNull()
            assertNotNull("Задача должна существовать в БД по ID", fetched)
            requireNotNull(fetched)
            assertEquals(uniqueTitle, fetched.title)
            assertEquals(TaskStatus.IN_PROGRESS, fetched.status)

            // 3. getByTitle
            val byTitleResult = repository.getByTitle(uniqueTitle)
            assertTrue(byTitleResult.isSuccess)
            val tasksByTitle = byTitleResult.getOrNull().orEmpty()
            assertTrue("Должна найтись хотя бы одна задача по заголовку", tasksByTitle.isNotEmpty())
            assertTrue(
                "Созданная задача должна присутствовать в результатах",
                tasksByTitle.any { it.id == createdTaskId }
            )

            // 4. getByProjectId
            val byProjectResult = repository.getByProjectId(testProjectId)
            assertTrue(byProjectResult.isSuccess)
            val tasksByProject = byProjectResult.getOrNull().orEmpty()
            assertTrue("Должна найтись задача по ID проекта", tasksByProject.isNotEmpty())
            assertTrue(
                "Задача с нужным ID должна быть найдена",
                tasksByProject.any { it.id == createdTaskId }
            )

        } finally {
            // 5. cleanup
            if (!createdTaskId.isNullOrBlank()) {
                repository.deleteTask(createdTaskId)
                val checkDeleted = repository.getById(createdTaskId)
                assertTrue(
                    "После удаления getById должен вернуть failure",
                    checkDeleted.isFailure
                )
            }
        }
    }
}