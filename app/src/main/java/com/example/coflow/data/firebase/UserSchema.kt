package com.example.coflow.data.firebase

import com.google.firebase.firestore.DocumentSnapshot

// Модель данных пользователя для Firestore
data class UserSchema(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val isDeleted: Boolean = false,
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "email" to email,
            "is_deleted" to isDeleted,
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): UserSchema? {
            if (!doc.exists()) return null

            return UserSchema(
                id = doc.id,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                isDeleted = doc.getBoolean("is_deleted") ?: doc.getBoolean("isDeleted") ?: false,
            )
        }
    }
}
