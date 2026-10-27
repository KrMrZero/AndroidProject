package com.example.coflow.repositories

import com.example.coflow.models.UserModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUserRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {
    private val userCollection: CollectionReference = db.collection("users")

    override suspend fun getById(id: String): Result<UserModel> =
        try {
            val user =
                userCollection
                    .document(id)
                    .get()
                    .await()
                    .toUserModel()
                    ?: throw Exception("Пользователь не найден")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки пользователя"))
        }

    override suspend fun getByEmail(email: String): Result<UserModel> =
        try {
            val document =
                userCollection
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?: throw Exception("Пользователь не найден")

            val user =
                document.toUserModel()
                    ?: throw Exception("Ошибка чтения пользователя")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка поиска пользователя"))
        }

    override suspend fun addUser(user: UserModel): Result<UserModel> =
        try {
            val userDocument =
                if (user.uid.isNotBlank()) {
                    userCollection.document(user.uid)
                } else {
                    userCollection.document()
                }

            val saved = user.copy(uid = userDocument.id)
            userDocument.set(saved.toFirestoreMap()).await()
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка сохранения пользователя"))
        }

    override suspend fun deleteUser(id: String): Result<Unit> =
        try {
            userCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка удаления пользователя"))
        }
}

private fun UserModel.toFirestoreMap(): Map<String, Any> =
    buildMap {
        put("name", name)
        put("email", email)
        put("is_deleted", isDeleted)
        createdAt?.let { put("createdAt", it) }
    }

private fun DocumentSnapshot.toUserModel(): UserModel? {
    if (!exists()) return null

    return UserModel(
        uid = id,
        name = getString("name") ?: "",
        email = getString("email") ?: "",
        createdAt = getDate("createdAt"),
        isDeleted = getBoolean("is_deleted") ?: getBoolean("isDeleted") ?: false,
    )
}
