package com.example.coflow.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coflow.models.MembershipModel
import com.example.coflow.models.Role
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты членства в проектах на Firestore Emulator.
 *
 * Проверяют создание, чтение по пользователю/проекту, изменение роли и удаление.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseMembershipRepositoryImplTest {
    private lateinit var repository: MembershipRepository

    @Before
    fun setup() {
        FirebaseEmulatorConfig.connect()
        repository = FirebaseMembershipRepositoryImpl()
    }

    @Test
    fun saveReadUpdateAndDeleteWorkflow() {
        runBlocking {
            val suffix = System.currentTimeMillis()
            val projectId = "test_project_$suffix"
            val userId = "test_user_$suffix"
            val member =
                MembershipModel(
                    projectId = projectId,
                    userId = userId,
                    role = Role.MEMBER,
                )

            val saved = repository.saveMember(member).getOrThrow()
            try {
                assertEquals("${projectId}_$userId", saved.id)
                assertEquals(Role.MEMBER, saved.role)

                val byUser = repository.getByUserId(userId).getOrThrow()
                assertTrue(
                    "Членство должно находиться по пользователю",
                    byUser.any { it.id == saved.id },
                )

                val byProject = repository.getByProjectId(projectId).getOrThrow()
                assertTrue(
                    "Членство должно находиться по проекту",
                    byProject.any { it.id == saved.id },
                )

                val updated = repository.saveMember(saved.copy(role = Role.OWNER)).getOrThrow()
                assertEquals(Role.OWNER, updated.role)

                val reloaded =
                    repository
                        .getByProjectId(projectId)
                        .getOrThrow()
                        .first { it.id == saved.id }
                assertEquals(Role.OWNER, reloaded.role)
            } finally {
                repository.deleteMember(saved.id).getOrThrow()
            }

            assertTrue(
                "Членство должно быть удалено",
                repository.getByUserId(userId).getOrThrow().isEmpty(),
            )
        }
    }
}
