package com.example.coflow.repositories

import com.example.coflow.models.MembershipModel
import com.example.coflow.repositories.dto.MembershipUpdateDto

/**
 * Членство пользователя в проекте (коллекция `memberships`).
 *
 * Единый стандарт изменения данных:
 * - [addMember] — создание членства;
 * - [updateMember] — изменение роли по ID;
 * - [deleteMember] — удаление.
 *
 * ID документа вычисляется как `projectId_userId`. Поэтому проект и
 * пользователь у существующего членства не меняются — только роль.
 *
 * ```kotlin
 * val saved = repository.addMember(
 *     MembershipModel(projectId = projectId, userId = userId, role = Role.MEMBER),
 * ).getOrThrow()
 *
 * repository.updateMember(
 *     id = saved.id,
 *     update = MembershipUpdateDto(role = Role.OWNER),
 * ).getOrThrow()
 * ```
 */
interface MembershipRepository {
    /** Возвращает все членства пользователя (записи `projectId` + `role`). */
    suspend fun getByUserId(userId: String): Result<List<MembershipModel>>

    /** Возвращает все членства проекта. */
    suspend fun getByProjectId(projectId: String): Result<List<MembershipModel>>

    /**
     * Создаёт членство. ID вычисляется из `projectId` + `userId`.
     * Если такой участник уже есть, вернёт [Result.failure].
     */
    suspend fun addMember(member: MembershipModel): Result<MembershipModel>

    /**
     * Частично обновляет членство по ID.
     *
     * Меняются только поля [MembershipUpdateDto], отличные от `null`.
     * Если не передано ни одного поля, вернёт [Result.failure].
     */
    suspend fun updateMember(
        id: String,
        update: MembershipUpdateDto,
    ): Result<MembershipModel>

    /** Удаляет членство по ID документа. */
    suspend fun deleteMember(id: String): Result<Unit>
}
