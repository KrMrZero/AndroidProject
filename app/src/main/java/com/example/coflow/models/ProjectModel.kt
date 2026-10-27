package com.example.coflow.models

import com.example.coflow.data.firebase.TaskSchema
import com.example.coflow.data.firebase.TaskStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ProjectModel(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "ownerId" to ownerId,
            "title" to title,
            "description" to description,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): ProjectModel? {
            if (!doc.exists()) return null

            val updatedAtTimestamp = doc.getTimestamp("updatedAtTimestamp")
            val updatedAtLocal = if (updatedAtTimestamp != null) {
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(updatedAtTimestamp.seconds, updatedAtTimestamp.nanoseconds.toLong()),
                    ZoneOffset.UTC
                )
            } else {
                LocalDateTime.now(ZoneOffset.UTC)
            }

            return ProjectModel(
                id = doc.id,
                ownerId = doc.getString("ownerId") ?: "",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                updatedAt = updatedAtLocal,
                isDeleted = doc.getBoolean("isDeleted") ?: false
            )
        }
    }
}