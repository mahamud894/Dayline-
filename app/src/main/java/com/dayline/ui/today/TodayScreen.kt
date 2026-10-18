package com.dayline.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.platform.calendar.CalendarCommitment
import com.dayline.domain.habits.isScheduledFor
import com.dayline.domain.model.Habit
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.domain.summary.SummaryCategory
import com.dayline.domain.summary.SummaryItem
import com.dayline.domain.summary.rankDailySummary
import com.dayline.ui.habits.HabitCard
import com.dayline.ui.common.ReminderScheduleNotice
import com.dayline.ui.tasks.formatTaskDueAt
import com.dayline.R
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    tasks: List<Task>,
    habits: List<Habit>,
    today: LocalDate,
    completedHabitIds: Set<String>,
    calendarCommitments: List<CalendarCommitment>,
    calendarImportEnabled: Boolean,
    calendarUnavailableMessage: String?,
    reminderScheduleStatus: ReminderScheduleStatus?,
    onCompleteHabit: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val now = remember { ZonedDateTime.now(zoneId) }
    val summary = remember(tasks, habits, now, zoneId) {
        rankDailySummary(tasks, habits, now, zoneId)
    }
    val todayDeadlines = remember(tasks, today, zoneId) {
        tasks.filter { task ->
            task.status != TaskStatus.COMPLETED &&
                task.dueAtEpochMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() == today
                } == true
        }.sortedBy(Task::dueAtEpochMillis)
    }
    val todayHabits = remember(habits, today) {
        habits.filter { isScheduledFor(it, today) }
    }
    val progressTasks = remember(tasks, today, zoneId) {
        tasks.filter { task ->
            task.dueAtEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() == today
            } == true
        }
    }
    val completedToday = progressTasks.count { it.status == TaskStatus.COMPLETED }
    val nextCommitment = tasks
        .filter {
            it.status == TaskStatus.ACTIVE &&
                it.dueAtEpochMillis?.let { due -> due > now.toInstant().toEpochMilli() } == true
        }
        .minByOrNull { it.dueAtEpochMillis ?: Long.MAX_VALUE }
    val nextCalendarCommitment = calendarCommitments
        .filter { it.startAtEpochMillis > now.toInstant().toEpochMilli() }
        .minByOrNull(CalendarCommitment::startAtEpochMillis)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (
            reminderScheduleStatus == ReminderScheduleStatus.WORK_MANAGER_FALLBACK ||
            reminderScheduleStatus == ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED
        ) {
            item { ReminderScheduleNotice(reminderScheduleStatus) }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.today_eyebrow),
                    color = Color(0xFFB7FF4A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.today_title),
                    color = Color(0xFFF4F5FF),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.format(DateTimeFormatter.ofPattern(stringResource(R.string.pattern_long_date))),
                    color = Color(0xFFC2C7D3),
                    fontSize = 14.sp
                )
            }
        }
        item {
            ProgressCard(
                completed = completedToday,
                total = progressTasks.size
            )
        }
        item {
            NextCommitmentCard(nextCommitment, nextCalendarCommitment)
        }
        if (calendarImportEnabled) {
            item {
                SectionHeader(
                    title = stringResource(R.string.today_calendar_section),
                    detail = stringResource(R.string.today_count_today, calendarCommitments.size)
                )
            }
            if (calendarUnavailableMessage != null) {
                item {
                    EmptyCard(calendarUnavailableMessage, testTag = "today-calendar-unavailable")
                }
            } else if (calendarCommitments.isEmpty()) {
                item { EmptyCard(stringResource(R.string.today_calendar_empty)) }
            } else {
                items(calendarCommitments, key = CalendarCommitment::id) { commitment ->
                    CalendarCommitmentCard(commitment)
                }
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.today_focus_section), detail = stringResource(R.string.today_focus_detail))
        }
        if (summary.isEmpty()) {
            item {
                EmptyCard(stringResource(R.string.today_focus_empty))
            }
        } else {
            items(summary) { item ->
                SummaryCard(item)
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.today_deadlines_section), detail = stringResource(R.string.today_count_today, todayDeadlines.size))
        }
        if (todayDeadlines.isEmpty()) {
            item { EmptyCard(stringResource(R.string.today_deadlines_empty)) }
        } else {
            items(todayDeadlines, key = Task::id) { task ->
                DeadlineCard(task)
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.today_habits_section), detail = stringResource(R.string.today_count_scheduled, todayHabits.size))
        }
        if (todayHabits.isEmpty()) {
            item { EmptyCard(stringResource(R.string.today_habits_empty)) }
        } else {
            items(todayHabits, key = Habit::id) { habit ->
                HabitCard(
                    habit = habit,
                    completedToday = habit.id in completedHabitIds,
                    onComplete = { onCompleteHabit(habit) }
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ProgressRing(completed, total)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.today_in_focus), color = Color(0xFFC2C7D3), fontSize = 11.sp)
                Text(
                    text = if (total == 0) stringResource(R.string.today_progress_none) else stringResource(R.string.today_progress, completed, total),
                    color = Color(0xFFF4F5FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.today_progress_hint),
                    color = Color(0xFFC2C7D3),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(completed: Int, total: Int) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val progressDescription = stringResource(R.string.today_progress_description, completed, total)
    Box(
        modifier = Modifier
            .size(76.dp)
            .testTag("today-progress")
            .semantics {
                contentDescription = progressDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )
            drawArc(
                color = Color(0xFF343B4B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFB7FF4A),
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text("$completed/$total", color = Color(0xFFF4F5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NextCommitmentCard(
    task: Task?,
    calendarCommitment: CalendarCommitment?
) {
    val taskDueAt = task?.dueAtEpochMillis
    val showCalendarEvent = calendarCommitment != null &&
        (taskDueAt == null || calendarCommitment.startAtEpochMillis < taskDueAt)
    val title = if (showCalendarEvent) {
        calendarCommitment?.title.orEmpty()
    } else {
        task?.title ?: stringResource(R.string.today_next_none_title)
    }
    val detail = if (showCalendarEvent) {
        formatCalendarCommitmentTime(requireNotNull(calendarCommitment))
    } else {
        task?.let { formatTaskDueAt(it.dueAtEpochMillis) }
            ?: stringResource(R.string.today_next_none_detail)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211C31))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (showCalendarEvent) stringResource(R.string.today_next_calendar) else stringResource(R.string.today_next_commitment),
                color = Color(0xFFC7A2FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                color = Color(0xFFF4F5FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = Color(0xFFD0C7E0),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun CalendarCommitmentCard(commitment: CalendarCommitment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar-commitment-${commitment.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.today_calendar_event),
                color = Color(0xFF59F5D8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                commitment.title,
                color = Color(0xFFF4F5FF),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                formatCalendarCommitmentTime(commitment),
                color = Color(0xFFC2C7D3),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun formatCalendarCommitmentTime(commitment: CalendarCommitment): String {
    val formatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_time))
    val zoneId = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(commitment.startAtEpochMillis)
        .atZone(zoneId)
        .format(formatter)
    val end = commitment.endAtEpochMillis?.let {
        Instant.ofEpochMilli(it).atZone(zoneId).format(formatter)
    }
    return if (end == null) start else "$start - $end"
}

@Composable
private fun SectionHeader(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color(0xFFF4F5FF),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = detail, color = Color(0xFFC2C7D3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryCard(item: SummaryItem) {
    val accent = when (item.category) {
        SummaryCategory.OVERDUE -> Color(0xFFFF8296)
        SummaryCategory.DEADLINE -> Color(0xFFC7A2FF)
        SummaryCategory.HIGH_PRIORITY -> Color(0xFFB7FF4A)
        SummaryCategory.HABIT -> Color(0xFF59F5D8)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151923))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = summaryCategoryLabel(item.category),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.task?.title ?: item.habit?.title.orEmpty(),
                color = Color(0xFFF4F5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.task?.let { formatTaskDueAt(it.dueAtEpochMillis) }
                    ?: item.habit?.let { stringResource(R.string.today_streak, it.currentStreak) }.orEmpty(),
                color = Color(0xFFC2C7D3),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DeadlineCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, color = Color(0xFFF4F5FF), fontWeight = FontWeight.SemiBold)
                Text(formatTaskDueAt(task.dueAtEpochMillis), color = Color(0xFFC2C7D3), fontSize = 12.sp)
            }
            Text(
                text = if (task.status == TaskStatus.OVERDUE) stringResource(R.string.label_overdue) else stringResource(R.string.label_due_today),
                color = if (task.status == TaskStatus.OVERDUE) Color(0xFFFF8296) else Color(0xFFC7A2FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyCard(message: String, testTag: String? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (testTag == null) Modifier else Modifier.testTag(testTag)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151923))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = Color(0xFFC2C7D3),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun summaryCategoryLabel(category: SummaryCategory): String = stringResource(
    when (category) {
        SummaryCategory.OVERDUE -> R.string.label_overdue
        SummaryCategory.DEADLINE -> R.string.label_deadline
        SummaryCategory.HIGH_PRIORITY -> R.string.label_high_priority
        SummaryCategory.HABIT -> R.string.label_habit
    }
)
