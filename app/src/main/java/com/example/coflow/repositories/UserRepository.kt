package com.example.coflow.repositories

import com.example.coflow.models.UserModel

/**
 * Пользователи (коллекция `users`).
 *
 * [addUser] работает как upsert:
 * - `uid` пустой — создаётся новый документ, UID генерирует Firestore;
 * - `uid` заполнен — обновляется существующий документ.
 *
 * ```kotlin
 * val user = repository.addUser(
 *     UserModel(name = "Иван", email = "ivan@example.com"),
 * ).getOrThrow()
 *
 * // Изменение профиля
 * repository.addUser(user.copy(name = "Иван Петров")).getOrThrow()
 * ```
 *
 * [deleteUser] физически удаляет документ. Поле `isDeleted` читается и
 * записывается как `is_deleted`, но сейчас не используется для мягкого удаления.
 */
interface UserRepository {
    /** Возвращает пользователя по UID или [Result.failure], если его нет. */
    suspend fun getById(id: String): Result<UserModel>

    /** Ищет пользователя по email; возвращает [Result.failure], если не найден. */
    suspend fun getByEmail(email: String): Result<UserModel>

    /** Создаёт пользователя или обновляет существующего по `uid`. */
    suspend fun addUser(user: UserModel): Result<UserModel>

    /** Удаляет документ пользователя по UID. */
    suspend fun deleteUser(id: String): Result<Unit>
}
