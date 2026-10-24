package com.example.coflow.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Реализация репозитория пользователей для Firestore
class FirebaseUserRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {

    private val userCollection: CollectionReference
        get() = db.collection("users")

    override suspend fun getById(id: String): UserSchema? {
        val snapshot = userCollection.document(id).get().await()
        return UserSchema.fromDocument(snapshot)
    }

    override suspend fun getByEmail(email: String): UserSchema? {
        val querySnapshot = userCollection
            .whereEqualTo("email", email)
            .get()
            .await()

        val doc = querySnapshot.documents.firstOrNull() ?: return null
        return UserSchema.fromDocument(doc)
    }

    override suspend fun addUser(user: UserSchema): UserSchema {
        return if (user.id.isNotBlank()) {
            userCollection.document(user.id).set(user.toMap()).await()
            user
        } else {
            val docRef = userCollection.add(user.toMap()).await()
            user.copy(id = docRef.id)
        }
    }

    override suspend fun deleteUser(id: String) {
        userCollection.document(id).delete().await()
    }
}
