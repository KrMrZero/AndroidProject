package com.example.coflow.ui.tasks

interface AssigneeSelectionState {
    val allAssignees: List<Assignee>
    val assigneeSearchQuery: String
    val pendingAssigneeIds: Set<String>
    val isLoadingAssignees: Boolean

    val filteredAssignees: List<Assignee>
    val pendingSelectedCount: Int
    val canConfirmAssignees: Boolean
}
