package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseAuthRepositoryImplTest {

    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        repository = FirebaseAuthRepositoryImpl()
        repository.logout()   // сбрасываем состояние предыдущих тестов
    }

    @After
    fun tearDown() {
        repository.logout()   // не оставляем залогиненных пользователей
    }

    @Test
    fun testRegisterAndLogin_Success() = runBlocking {
        val randomEmail = "test_${System.currentTimeMillis()}@example.com"
        val password = "password123"
        val name = "Тестовый Пользователь"

        // 1. Регистрация
        val registerResult = repository.register(randomEmail, password, name)
        assertTrue(
            "Регистрация должна пройти успешно: ${registerResult.exceptionOrNull()?.message}",
            registerResult.isSuccess
        )

        val registeredUser = registerResult.getOrNull()
        assertNotNull("Пользователь не должен быть null", registeredUser)
        requireNotNull(registeredUser)

        assertTrue("UID не должен быть пустым", registeredUser.uid.isNotBlank())
        assertEquals("Email должен совпадать", randomEmail, registeredUser.email)
        assertEquals("Имя должно совпадать", name, registeredUser.name)

        val uid = registeredUser.uid

        // 2. Вход
        val loginResult = repository.login(randomEmail, password)
        assertTrue(
            "Вход должен пройти успешно: ${loginResult.exceptionOrNull()?.message}",
            loginResult.isSuccess
        )

        val loggedInUser = loginResult.getOrNull()
        assertNotNull("Пользователь после входа не должен быть null", loggedInUser)
        requireNotNull(loggedInUser)

        assertEquals("UID после входа должен совпадать", uid, loggedInUser.uid)
        assertEquals("Email после входа должен совпадать", randomEmail, loggedInUser.email)

        // 3. Текущий пользователь
        val currentUid = repository.getCurrentUserUid()
        assertEquals("Текущий UID должен совпадать", uid, currentUid)
    }

    @Test
    fun testLogin_WrongPassword_Failure() = runBlocking {
        val email = "test_wrong_${System.currentTimeMillis()}@example.com"
        val password = "password123"

        // Регистрируем и сразу разлогиниваемся, чтобы login проверял пароль с нуля
        val regResult = repository.register(email, password, "Test")
        assertTrue(
            "Регистрация должна пройти: ${regResult.exceptionOrNull()?.message}",
            regResult.isSuccess
        )
        repository.logout()

        // Неверный пароль
        val result = repository.login(email, "wrong_password")

        assertTrue(
            "Вход с неверным паролем должен вернуть ошибку",
            result.isFailure
        )
        val error = result.exceptionOrNull()?.message
        println("Текст ошибки от Firebase: $error")
        assertNotNull("Ошибка должна содержать сообщение", error)
        assertFalse("Пользователь не должен быть залогинен", repository.getCurrentUserUid() != null)
    }
}