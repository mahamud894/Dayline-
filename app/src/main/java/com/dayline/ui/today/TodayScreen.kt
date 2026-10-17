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
import com.dayline.domain.habits.isScheduledFor
import com.dayline.domain.model.Habit
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.summary.SummaryCategory
import com.dayline.domain.summary.SummaryItem
import com.dayline.domain.summary.rankDailySummary
import com.dayline.ui.tasks.formatTaskDueAt
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    tasks: List<Task>,
    habits: List<Habit>,
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val now = remember { ZonedDateTime.now(zoneId) }
    val today = now.toLocalDate()
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "DAYLINE / TODAY",
                    color = Color(0xFFB7FF4A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Make today count",
                    color = Color(0xFFF4F5FF),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
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
            NextCommitmentCard(nextCommitment)
        }
        item {
            SectionHeader(title = "Focus queue", detail = "TOP PRIORITIES")
        }
        if (summary.isEmpty()) {
            item {
                EmptyCard("A little breathing room. Add something when you're ready.")
            }
        } else {
            items(summary) { item ->
                SummaryCard(item)
            }
        }
        item {
            SectionHeader(title = "Hard Deadlines", detail = "${todayDeadlines.size} TODAY")
        }
        if (todayDeadlines.isEmpty()) {
            item { EmptyCard("No fixed-time deadlines on your calendar today.") }
        } else {
            items(todayDeadlines, key = Task::id) { task ->
                DeadlineCard(task)
            }
        }
        item {
            SectionHeader(title = "Daily Habits", detail = "${todayHabits.size} SCHEDULED")
        }
        if (todayHabits.isEmpty()) {
            item { EmptyCard("Your routines will appear here on their scheduled days.") }
        } else {
            items(todayHabits, key = Habit::id) { habit ->
                HabitCard(habit)
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
                Text("TODAY IN FOCUS", color = Color(0xFFC2C7D3), fontSize = 11.sp)
                Text(
                    text = if (total == 0) "Start with one thing" else "$completed of $total done",
                    color = Color(0xFFF4F5FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Small steps keep the day moving.",
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
    Box(
        modifier = Modifier
            .size(76.dp)
            .testTag("today-progress")
            .semantics {
                contentDescription = "Progress: $completed of $total deadlines complete"
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
private fun NextCommitmentCard(task: Task?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF211C31))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Next commitment", color = Color(0xFFC7A2FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = task?.title ?: "Nothing on the radar",
                color = Color(0xFFF4F5FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = task?.let { formatTaskDueAt(it.dueAtEpochMillis) }
                    ?: "Your day is open. Keep it that way or add a task.",
                color = Color(0xFFD0C7E0),
                fontSize = 13.sp
            )
        }
    }
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
                text = item.category.name.replace('_', ' '),
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
                    ?: item.habit?.let { "${it.currentStreak} day streak" }.orEmpty(),
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
                text = if (task.status == TaskStatus.OVERDUE) "OVERDUE" else "DUE TODAY",
                color = if (task.status == TaskStatus.OVERDUE) Color(0xFFFF8296) else Color(0xFFC7A2FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HabitCard(habit: Habit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142220))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(habit.title, color = Color(0xFFF4F5FF), fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (habit.currentStreak == 1) "1 day streak" else "${habit.currentStreak} day streak",
                    color = Color(0xFFA7DCD2),
                    fontSize = 12.sp
                )
            }
            Text("TODAY", color = Color(0xFF59F5D8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
