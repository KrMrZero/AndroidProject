package com.example.coflow.ui.tasks

import java.time.LocalDate
import java.time.YearMonth

interface DateSelectionState {
    val selectedDueDate: LocalDate
    val calendarMonth: YearMonth
    val dueDate: String
}
