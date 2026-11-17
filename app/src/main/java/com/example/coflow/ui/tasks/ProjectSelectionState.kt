package com.example.coflow.ui.tasks

interface ProjectSelectionState {
    val allProjectItems: List<ProjectItem>
    val allProjects: List<String>
    val projectSearchQuery: String
    val pendingProjectId: String
    val pendingProject: String
    val filteredProjects: List<String>
    val canConfirmProject: Boolean
}
