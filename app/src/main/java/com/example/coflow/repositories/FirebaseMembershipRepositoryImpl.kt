package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel
import com.example.coflow.models.Role
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseMembershipRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MembershipRepository {
    private val membershipCollection: CollectionReference = db.collection("memberships")

    override suspend fun getByUserId(userId: String): Result<List<MembershipModel>> =
        try {
            val documents =
                membershipCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { it.toMembershipModel() })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки членства"))
        }

    override suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>> =
        try {
            val documents =
                membershipCollection
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()
                    .documents
            Result.success(documents.mapNotNull { it.toMembershipModel() })
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка загрузки участников проекта"))
        }

    override suspend fun saveMember(member: MembershipModel): Result<MembershipModel> =
        try {
            require(member.projectId.isNotBlank() && member.userId.isNotBlank()) {
                "projectId и userId обязательны"
            }

            // Один пользователь может быть в проекте только один раз.
            val id = "${member.projectId}_${member.userId}"
            val saved = member.copy(id = id)

            membershipCollection.document(id).set(saved.toFirestoreMap()).await()
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка сохранения участника"))
        }

    override suspend fun deleteMember(id: String): Result<Unit> =
        try {
            membershipCollection.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Ошибка удаления участника"))
        }
}

private fun MembershipModel.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "projectId" to projectId,
        "userId" to userId,
        "role" to role.code,
    )

private fun DocumentSnapshot.toMembershipModel(): MembershipModel? {
    if (!exists()) return null
    return MembershipModel(
        id = id,
        projectId = getString("projectId") ?: "",
        userId = getString("userId") ?: "",
        role = Role.fromInt((getLong("role") ?: Role.MEMBER.code.toLong()).toInt()),
    )
}
