package com.dayline.ui.tasks

import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSearchTest {
    private val task = Task(
        id = "t",
        title = "Submit Client Report",
        notes = "Ask Rahim for the বাজেট numbers",
        dueAtEpochMillis = null,
        priority = TaskPriority.NORMAL,
        status = TaskStatus.ACTIVE,
        quietModeOverrideAllowed = false,
        completedAtEpochMillis = null
    )

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(task.matchesSearch(""))
        assertTrue(task.matchesSearch("   "))
    }

    @Test
    fun matchesTitleOrNotesIgnoringCaseAndSurroundingSpaces() {
        assertTrue(task.matchesSearch("client"))
        assertTrue(task.matchesSearch("  REPORT "))
        assertTrue(task.matchesSearch("rahim"))
        assertTrue(task.matchesSearch("বাজেট"))
    }

    @Test
    fun everyWordMustAppearSomewhere() {
        assertTrue(task.matchesSearch("report rahim"))
        assertFalse(task.matchesSearch("report invoice"))
    }

    @Test
    fun taskWithoutNotesStillMatchesOnTitle() {
        val noNotes = task.copy(notes = null)

        assertTrue(noNotes.matchesSearch("submit"))
        assertFalse(noNotes.matchesSearch("rahim"))
    }
}
