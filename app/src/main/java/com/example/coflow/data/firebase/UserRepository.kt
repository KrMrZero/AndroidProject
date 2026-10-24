package com.example.coflow.data.firebase

// Интерфейс репозитория для работы с пользователями в Firestore
interface UserRepository {
    suspend fun getById(id: String): UserSchema?
    suspend fun getByEmail(email: String): UserSchema?
    suspend fun addUser(user: UserSchema): UserSchema
    suspend fun deleteUser(id: String)
}
