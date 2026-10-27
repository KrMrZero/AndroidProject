package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.MembershipModel
import com.example.coflow.models.ProjectModel
import com.example.coflow.models.Role
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Интеграционные тесты проектов на Firestore Emulator.
 *
 * Проверяют создание, чтение по ID/владельцу/участнику и мягкое удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseProjectRepositoryImplTest {
    private lateinit var repository: ProjectRepository
    private lateinit var membershipRepository: MembershipRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseProjectRepositoryImpl()
        membershipRepository = FirebaseMembershipRepositoryImpl()
    }

    @Test
    fun addReadMembershipAndDeleteWorkflow() {
        runBlocking {
            val suffix = System.currentTimeMillis()
            val ownerId = "test_owner_$suffix"
            val userId = "test_user_$suffix"
            val updatedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
            val project =
                ProjectModel(
                    ownerId = ownerId,
                    title = "Тестовый проект $suffix",
                    description = "Описание проекта для интеграционного теста",
                    updatedAt = updatedAt,
                )

            val saved = repository.addProject(project).getOrThrow()
            var membership: MembershipModel? = null
            try {
                assertTrue("ID проекта должен быть заполнен", saved.id.isNotBlank())
                assertEquals(ownerId, saved.ownerId)
                assertEquals(project.title, saved.title)
                assertEquals(updatedAt, saved.updatedAt)

                val fetchedById = repository.getById(saved.id).getOrThrow()
                assertEquals(project.title, fetchedById.title)

                val byOwner = repository.getByOwnerId(ownerId).getOrThrow()
                assertTrue(
                    "Проект должен находиться по владельцу",
                    byOwner.any { it.id == saved.id },
                )

                membership =
                    membershipRepository
                        .saveMember(
                            MembershipModel(
                                projectId = saved.id,
                                userId = userId,
                                role = Role.MEMBER,
                            ),
                        ).getOrThrow()

                val byUser = repository.getByUserId(userId).getOrThrow()
                assertTrue(
                    "Проект должен находиться по участнику",
                    byUser.any { it.id == saved.id },
                )
            } finally {
                membership?.let { membershipRepository.deleteMember(it.id).getOrThrow() }
                repository.deleteProject(saved.id).getOrThrow()
            }

            val deleted = repository.getById(saved.id).getOrThrow()
            assertTrue("Проект должен быть помечен удалённым", deleted.isDeleted)

            val byOwnerAfterDelete = repository.getByOwnerId(ownerId).getOrThrow()
            assertTrue(
                "Удалённый проект не должен попадать в список владельца",
                byOwnerAfterDelete.none { it.id == saved.id },
            )
        }
    }
}
