package com.example.coflow.data.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseUserRepositoryImplTest {

    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        repository = FirebaseUserRepositoryImpl()
    }

    @Test
    fun testUserRepositoryWorkflow_Success() = runBlocking {
        val testEmail = "test_user_${System.currentTimeMillis()}@example.com"
        val newUser = UserSchema(
            name = "Тестовый Пользователь",
            email = testEmail,
            isDeleted = false,
        )

        var createdUserId: String? = null

        try {
            // 1. Тестируем добавление пользователя
            val addedUser = repository.addUser(newUser)
            createdUserId = addedUser.id
            assertTrue("ID созданного пользователя должен быть заполнен", createdUserId.isNotBlank())
            assertEquals("Тестовый Пользователь", addedUser.name)
            assertEquals(testEmail, addedUser.email)

            // 2. Тестируем получение по ID
            val fetchedById = repository.getById(createdUserId)
            assertNotNull("Пользователь должен существовать в БД по ID", fetchedById)
            assertEquals("Тестовый Пользователь", fetchedById?.name)
            assertEquals(testEmail, fetchedById?.email)
            assertFalse(fetchedById?.isDeleted ?: true)

            // 3. Тестируем получение по Email
            val fetchedByEmail = repository.getByEmail(testEmail)
            assertNotNull("Пользователь должен быть найден по email", fetchedByEmail)
            assertEquals(createdUserId, fetchedByEmail?.id)
            assertEquals("Тестовый Пользователь", fetchedByEmail?.name)

        } finally {
            // 4. Очистка созданного пользователя в БД после окончания теста
            if (!createdUserId.isNullOrBlank()) {
                repository.deleteUser(createdUserId)
                val checkDeleted = repository.getById(createdUserId)
                assertNull("Пользователь должен быть удален из Firestore", checkDeleted)
            }
        }
    }
}
