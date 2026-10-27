package com.example.coflow.repositories

import com.example.coflow.models.UserModel

interface UserRepository {
    suspend fun getById(id: String): Result<UserModel>

    suspend fun getByEmail(email: String): Result<UserModel>

    suspend fun addUser(user: UserModel): Result<UserModel>

    suspend fun deleteUser(id: String): Result<Unit>
}
