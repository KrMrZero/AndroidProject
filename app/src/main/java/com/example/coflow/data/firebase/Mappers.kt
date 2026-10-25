package com.example.coflow.data.firebase

import com.example.coflow.models.TaskModel
import com.example.coflow.models.UserModel
import com.google.firebase.firestore.FirebaseFirestore

fun UserSchema.toDomain() : UserModel{
    return UserModel(
        id = this.uid,
        name = this.name,
        email = this.email
    )
}

fun UserModel.toSchema() : UserSchema{
    return UserSchema(
        uid = this.id,
        name = this.name,
        email = this.email
    )
}

fun TaskSchema.toDomain(): TaskModel{
    return TaskModel(
        id = this.id,
        title = this.title,
        description = this.description,
        projectId = this.projectRef?.id ?: "",
        authorId = this.authorRef?.id ?: "",
        assigneeIds = this.executorRef?.id ?: "",
        deadline = this.deadline,
        //status = this.status
    )
}

fun TaskModel.toSchema(db: FirebaseFirestore): TaskSchema {
    return TaskSchema(
        id = id,
        projectRef = projectId.takeIf { it.isNotBlank() }
            ?.let { db.collection("Project").document(it) },
        authorRef = authorId.takeIf { it.isNotBlank() }
            ?.let { db.collection("users").document(it) },
        executorRef = assigneeIds.takeIf { it.isNotBlank() }
            ?.let { db.collection("users").document(it) },
        title = title,
        description = description,
        deadline = deadline,
        //status = status
    )
}