package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel

interface MembershipRepository {
    suspend fun getByUserId(userId: String): Result<List<MembershipModel>>

    suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>>

    /**
     * Создаёт или обновляет членство.
     *
     * ID документа вычисляется из projectId + userId, поэтому повторное
     * сохранение того же участника обновляет роль, а не создаёт дубль.
     */
    suspend fun saveMember(member: MembershipModel): Result<MembershipModel>

    suspend fun deleteMember(id: String): Result<Unit>
}
