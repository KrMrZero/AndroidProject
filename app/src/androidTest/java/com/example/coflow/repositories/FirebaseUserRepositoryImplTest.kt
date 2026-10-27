package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.UserModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты репозитория пользователей на Firestore Emulator.
 *
 * Проверяют создание, чтение по ID/email и удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseUserRepositoryImplTest {
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseUserRepositoryImpl()
    }

    @Test
    fun addReadAndDeleteWorkflow() {
        runBlocking {
            val email = "user_repo_${System.currentTimeMillis()}@example.com"
            val user =
                UserModel(
                    name = "Тестовый Пользователь",
                    email = email,
                )

            val added = repository.addUser(user).getOrThrow()
            try {
                assertTrue("UID созданного пользователя должен быть заполнен", added.uid.isNotBlank())
                assertEquals(user.name, added.name)
                assertEquals(email, added.email)
                assertFalse("Новый пользователь не должен быть удалён", added.isDeleted)

                val fetchedById = repository.getById(added.uid).getOrThrow()
                assertEquals(user.name, fetchedById.name)
                assertEquals(email, fetchedById.email)

                val fetchedByEmail = repository.getByEmail(email).getOrThrow()
                assertEquals(added.uid, fetchedByEmail.uid)
                assertEquals(user.name, fetchedByEmail.name)
            } finally {
                repository.deleteUser(added.uid).getOrThrow()
            }

            assertTrue(
                "Пользователь должен быть удалён",
                repository.getById(added.uid).isFailure,
            )
        }
    }
}
