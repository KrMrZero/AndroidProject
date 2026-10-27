package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel

/**
 * Членство пользователя в проекте (коллекция `memberships`).
 *
 * [saveMember] одновременно создаёт и изменяет запись:
 * ```kotlin
 * val saved = repository.saveMember(
 *     MembershipModel(projectId = projectId, userId = userId, role = Role.MEMBER),
 * ).getOrThrow()
 *
 * // Изменение роли: сохраняем ту же пару projectId + userId
 * repository.saveMember(saved.copy(role = Role.OWNER)).getOrThrow()
 * ```
 *
 * ID документа вычисляется как `projectId_userId`, поэтому повторное сохранение
 * обновляет роль, а не создаёт дубль. Физически удаляет запись [deleteMember].
 */
interface MembershipRepository {
    /** Возвращает все членства пользователя (записи `projectId` + `role`). */
    suspend fun getByUserId(userId: String): Result<List<MembershipModel>>

    /** Возвращает все членства проекта. */
    suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>>

    /**
     * Создаёт или обновляет членство.
     *
     * Если `projectId` и `userId` уже существуют, обновляется роль.
     * [MembershipModel.id] входной модели игнорируется — ID вычисляется заново.
     */
    suspend fun saveMember(member: MembershipModel): Result<MembershipModel>

    /** Удаляет членство по ID документа. */
    suspend fun deleteMember(id: String): Result<Unit>
}
