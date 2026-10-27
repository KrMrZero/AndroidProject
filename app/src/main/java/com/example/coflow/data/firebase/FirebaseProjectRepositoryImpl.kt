package com.example.coflow.data.firebase

import com.example.coflow.models.ProjectModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseProjectRepositoryImpl (
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ProjectRepository {

    private val projectCollection: CollectionReference = db.collection("Project")

    override suspend fun getById(id: String): ProjectModel? {
        val snap = projectCollection.document(id).get().await()
        return ProjectModel.fromDocument(snap)
    }

    override suspend fun getOwnerId(id: String): List<ProjectModel> {
        val qs = projectCollection.whereEqualTo("ownerId", id).get().await()
        return qs.documents.mapNotNull { ProjectModel.fromDocument(it) }
    }

    override suspend fun getByUserId(id: String): List<ProjectModel> {
        TODO("Not yet implemented")
    }

    override suspend fun addProject(project: ProjectModel): ProjectModel {
        val docRef = projectCollection.document()
        docRef.set(project.toMap()).await()
        return project
    }

    override suspend fun deleteProject(id: String) {
        TODO("Not yet implemented")
    }
}