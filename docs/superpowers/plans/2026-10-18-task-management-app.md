# Dayline Android Task Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Dayline, an offline-capable Android task manager with fast capture, ranked daily summaries, progressive deadline alerts, recurring habits, calendar read-integration, and a distinctive dark neon interface.

**Architecture:** Create a single native Android app module in Kotlin with Jetpack Compose. Keep domain rules pure and unit-testable; persist tasks, habits, completions, and settings locally with Room and DataStore. Android adapters own alarm scheduling, notifications, boot/unlock receivers, and optional read-only calendar access; Compose screens consume repositories and domain use cases through a small app container.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Room, DataStore Preferences, AlarmManager, WorkManager, Android Calendar Provider, JUnit, kotlinx-coroutines-test, Robolectric, and Compose UI testing. Use Gradle wrapper and compatible stable dependency versions selected when scaffolding.

**Spec:** [../specs/2026-10-18-task-management-app-design.md](../specs/2026-10-18-task-management-app-design.md)

## Global Constraints

- Android is the target platform; the current workspace is empty and has no existing application or stack to preserve.
- Use a native Android implementation with Kotlin and Jetpack Compose rather than a cross-platform toolkit.
- Treat the first release as single-device and local-first, without accounts or sync.
- Keep task and habit content private and usable without network access.
- Use clear text/icon labels in addition to color for category, status, and priority.
- Reminder scheduling, reboot recovery, notification permissions, exact-alarm restrictions, DND access, lock-screen visibility, and background execution must follow Android's actual OS/device capabilities.
- Request `READ_CALENDAR` only for calendar integration; do not write or modify calendar events.
- Use the configurable summary time as the fallback; Smart Wake begins one hour before that time.
- Done cancels all pending reminders for the task; Snooze schedules a one-off alarm (10 minutes by default) without shifting later milestones; Dismiss clears only the current notification.
- At a missed deadline, stop escalation alarms and mark the task `OVERDUE`; retain one persistent overdue notification until acknowledged, rescheduled, or completed.
- Restore pending AlarmManager/WorkManager reminders after `ACTION_BOOT_COMPLETED`, with any required Android permissions and user-facing setup.
- Habit completion is for the current local calendar day; one missed scheduled day freezes a streak and two consecutive missed scheduled days reset it while preserving the best streak.
- Git was initialized at the user's request for worktree isolation; commit each completed task separately with the required Co-authored-by trailer.

## Review Focus

- **Permission-denied or revoked exact-alarm/notification access:** scheduling must report unavailable capability and present the correct setup/fallback instead of claiming a reminder is active. Test denied and subsequently granted permission.
- **Local date boundaries, timezone changes, and DST:** due dates, midnight habit resets, and streak schedules must use local calendar days without double completion or skipped scheduled days. Test DST transitions and a timezone change with an injected clock/zone.
- **Races between completion and firing reminder receivers:** a completed task must never re-post an alert or transition back to overdue. Test simultaneous completion/receiver delivery and repeated action intents.
- **Smart Wake timing edges:** the first unlock at the one-hour cutoff should deliver once; an unlock and fallback firing together should not duplicate the summary; changing the configured time must recalculate the window. Test before/at/after cutoff and locked fallback.
- **Calendar permission and changing events:** denial, revocation, deleted events, and duplicate provider reads must not affect manual tasks or mutate calendar data. Test read-only behavior and empty/denied results.

---

## File Structure

All paths below are relative to the workspace root. Keep one app module and package by responsibility:

- `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradlew.bat`: Gradle project and dependency versions.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`: app configuration, permissions, and receivers.
- `app/src/main/java/com/dayline/DaylineApp.kt`, `app/src/main/java/com/dayline/AppContainer.kt`: application entry and dependency construction.
- `app/src/main/java/com/dayline/domain/model/Task.kt`, `Habit.kt`, `HabitSchedule.kt`, `UserSettings.kt`: domain data and enums.
- `app/src/main/java/com/dayline/domain/summary/DailySummaryRanker.kt`: deterministic priority ordering and top-five selection.
- `app/src/main/java/com/dayline/domain/habits/HabitStreakCalculator.kt`: schedule eligibility and streak calculations.
- `app/src/main/java/com/dayline/domain/reminders/ReminderPlan.kt`, `ReminderPlanner.kt`: pure deadline milestone and overdue planning.
- `app/src/main/java/com/dayline/data/local/DaylineDatabase.kt`, `TaskEntity.kt`, `HabitEntity.kt`, `HabitCompletionEntity.kt`, `UserSettingsStore.kt`: Room entities/DAOs/database and DataStore settings adapter.
- `app/src/main/java/com/dayline/data/TaskRepository.kt`, `HabitRepository.kt`: persistence and domain-facing operations.
- `app/src/main/java/com/dayline/platform/reminders/AndroidReminderScheduler.kt`, `ReminderReceiver.kt`, `ReminderActionReceiver.kt`, `BootReceiver.kt`: Android alarm/work integration and action/reboot handling.
- `app/src/main/java/com/dayline/platform/notifications/DaylineNotificationFactory.kt`: summary, urgent, ongoing, and overdue notifications.
- `app/src/main/java/com/dayline/platform/summary/SmartWakeReceiver.kt`, `MorningSummaryScheduler.kt`: unlock-trigger and configured-time fallback.
- `app/src/main/java/com/dayline/platform/calendar/AndroidCalendarReader.kt`: optional read-only Calendar Provider adapter.
- `app/src/main/java/com/dayline/ui/navigation/DaylineNavHost.kt`, `ui/theme/DaylineTheme.kt`: app navigation, color, typography, and shape system.
- `app/src/main/java/com/dayline/ui/today/TodayScreen.kt`, `TodayViewModel.kt`: priorities, next commitment, progress ring, deadline/habit sections, and floating add action.
- `app/src/main/java/com/dayline/ui/tasks/TasksScreen.kt`, `TaskEditorScreen.kt`, `TaskViewModel.kt`: task lists and create/edit/complete/reschedule actions.
- `app/src/main/java/com/dayline/ui/habits/HabitsScreen.kt`, `HabitEditorScreen.kt`, `HabitViewModel.kt`: schedule editing, daily completion, and streak display.
- `app/src/main/java/com/dayline/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`: summary time, calendar opt-in, and notification/quiet-mode setup.
- `app/src/main/java/com/dayline/ui/profile/ProfileScreen.kt`: best streak/profile summary.
- `app/src/test/java/com/dayline/domain/summary/DailySummaryRankerTest.kt`, `domain/habits/HabitStreakCalculatorTest.kt`, `domain/reminders/ReminderPlannerTest.kt`: pure domain tests.
- `app/src/test/java/com/dayline/domain/model/DomainModelTest.kt`: domain model validation tests.
- `app/src/test/java/com/dayline/data/TaskRepositoryTest.kt`, `HabitRepositoryTest.kt`: persistence and state-transition tests.
- `app/src/test/java/com/dayline/platform/reminders/ReminderActionReceiverTest.kt`, `BootReceiverTest.kt`, `platform/summary/SmartWakeReceiverTest.kt`, `platform/calendar/AndroidCalendarReaderTest.kt`: Android adapter behavior tests using Robolectric/fakes.
- `app/src/androidTest/java/com/dayline/ui/TodayScreenTest.kt`, `TaskFlowTest.kt`, `HabitFlowTest.kt`, `SettingsFlowTest.kt`: Compose UI and end-to-end interaction tests.

## Interfaces

Use these interfaces between plan tasks:

```kotlin
enum class TaskStatus { ACTIVE, COMPLETED, OVERDUE }
enum class TaskPriority { NORMAL, HIGH }

data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val dueAtEpochMillis: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val quietModeOverrideAllowed: Boolean,
    val completedAtEpochMillis: Long?
)

data class Habit(
    val id: String,
    val title: String,
    val schedule: HabitSchedule,
    val createdAtLocalDate: LocalDate,
    val currentStreak: Int,
    val bestStreak: Int
)

sealed interface HabitSchedule {
    data object Daily : HabitSchedule
    data class Weekdays(val days: Set<DayOfWeek>) : HabitSchedule
    data class EveryDays(val interval: Int, val anchorDate: LocalDate) : HabitSchedule
}

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun saveTask(task: Task)
    suspend fun setTaskStatus(id: String, status: TaskStatus, atEpochMillis: Long)
    suspend fun getTask(id: String): Task?
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    suspend fun saveHabit(habit: Habit)
    suspend fun completeForDate(id: String, date: LocalDate)
}

data class SummaryItem(
    val task: Task? = null,
    val habit: Habit? = null,
    val sortDueAtEpochMillis: Long? = null,
    val category: SummaryCategory
)
enum class SummaryCategory { OVERDUE, DEADLINE, HIGH_PRIORITY, HABIT }

fun rankDailySummary(
    tasks: List<Task>,
    habits: List<Habit>,
    now: ZonedDateTime,
    zoneId: ZoneId,
    limit: Int = 5
): List<SummaryItem>

data class ReminderMilestone(val triggerAtEpochMillis: Long, val kind: ReminderKind)
enum class ReminderKind { TWO_HOURS, FINAL_THIRTY_MINUTES, SNOOZE, OVERDUE }
fun planReminders(task: Task, nowEpochMillis: Long): List<ReminderMilestone>

interface ReminderScheduler {
    suspend fun schedule(task: Task)
    suspend fun cancel(taskId: String)
    suspend fun scheduleSnooze(task: Task, triggerAtEpochMillis: Long)
    suspend fun restorePending()
}
```

Use injected `Clock`/`ZoneId` in domain calculations rather than reading wall-clock time directly. Make the summary ranker deterministic: overdue first, same-day deadlines by due time, high-priority tasks after deadlines, and scheduled habits last; keep stable ordering for equal-ranked items. Clamp summary `limit` to 3–5 and default it to 5.

## Task 1: Scaffold the Android app and testable domain foundation

**Files:**
- Create: Gradle wrapper and project files listed in File Structure.
- Create: `app/src/main/AndroidManifest.xml`.
- Create: `app/src/main/java/com/dayline/DaylineApp.kt`, `AppContainer.kt`.
- Create: `app/src/main/java/com/dayline/domain/model/Task.kt`, `Habit.kt`, `HabitSchedule.kt`, `UserSettings.kt`.
- Create: `app/src/main/java/com/dayline/ui/navigation/DaylineNavHost.kt`, `ui/theme/DaylineTheme.kt`.
- Test: `app/src/test/java/com/dayline/domain/model/DomainModelTest.kt`.

**Interfaces:**
- Produces: domain models/enums and app/test configuration used by every later task.
- Use `minSdk 26` to support the required baseline local-date/time and notification APIs; set `compileSdk`/`targetSdk` to the latest stable installed/available at implementation, documenting permission behavior when target SDK changes.

- [ ] **Step 1: Create the Android project and verify the empty app builds**

Create the Gradle wrapper, one `:app` module, version catalog, Kotlin Android/Compose and KSP plugins, Material 3 Compose, lifecycle ViewModel Compose, Room runtime/compiler, DataStore Preferences, WorkManager, JUnit, kotlinx-coroutines-test, Robolectric, and Compose UI test dependencies. Choose mutually compatible stable versions; enable Compose and Java/Kotlin 17. Create a minimal Activity with a Compose content host.

- [ ] **Step 2: Run the initial build**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Write domain model tests**

In `DomainModelTest`, assert that `TaskStatus` distinguishes `ACTIVE`, `COMPLETED`, and `OVERDUE`; `TaskPriority` distinguishes `NORMAL` and `HIGH`; and each `HabitSchedule` retains its days or positive interval/anchor date.

- [ ] **Step 4: Implement typed domain models**

Create the exact domain types and fields in Interfaces. Validate `EveryDays.interval >= 1` at construction. Do not put Android framework types in domain models.

- [ ] **Step 5: Implement the base theme and navigation shell**

Build `DaylineTheme` with near-black backgrounds, neon lime/violet, teal habit status, warm urgency, high-contrast secondary text, rounded card shapes, and bold typography. Add destinations for Today, Tasks, Habits, and Profile with placeholder screens.

- [ ] **Step 6: Run unit and UI build checks**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`; `DomainModelTest` passes.

## Task 2: Persist tasks, habits, and local settings

**Files:**
- Create: `data/local/TaskEntity.kt`, `HabitEntity.kt`, `HabitCompletionEntity.kt`, `DaylineDatabase.kt`, `UserSettingsStore.kt`.
- Create: `data/TaskRepository.kt`, `HabitRepository.kt`.
- Modify: `DaylineApp.kt`, `AppContainer.kt`.
- Test: `data/TaskRepositoryTest.kt`, `HabitRepositoryTest.kt`.

**Interfaces:**
- Consumes: Task 1 domain models.
- Produces: `TaskRepository` and `HabitRepository` interfaces from Interfaces, Room-backed implementations, and a `UserSettings` DataStore flow.

- [ ] **Step 1: Write repository persistence tests**

`TaskRepositoryTest` must verify observing an inserted task, updating status and completion time, and retrieving a missing id as `null`. `HabitRepositoryTest` must verify observed habit updates and that completing the same habit/date twice creates only one completion record.

- [ ] **Step 2: Run repository tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.data.TaskRepositoryTest" --tests "com.dayline.data.HabitRepositoryTest"`
Expected: FAIL because repository/database implementations do not exist.

- [ ] **Step 3: Implement Room entities, DAOs, and database**

Create task, habit, and unique `(habitId, localDate)` completion tables with DAO methods for observation, insert/update, status changes, and completion lookup. Store due instants as epoch milliseconds and habit dates as ISO local-date strings.

- [ ] **Step 4: Implement repositories and DataStore**

Implement repository contracts using Room and coroutine Flows. Add DataStore-backed `UserSettings` fields for summary local time (default `08:00`), snooze minutes (default `10`), and calendar-import enabled state.

- [ ] **Step 5: Run persistence tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.data.TaskRepositoryTest" --tests "com.dayline.data.HabitRepositoryTest"`
Expected: PASS, including idempotent habit completion.

## Task 3: Implement daily summary ranking and deadline planning

**Files:**
- Create: `domain/summary/DailySummaryRanker.kt`, `domain/reminders/ReminderPlan.kt`, `ReminderPlanner.kt`.
- Test: `domain/summary/DailySummaryRankerTest.kt`, `domain/reminders/ReminderPlannerTest.kt`.

**Interfaces:**
- Consumes: Task 1 models and Task 2 repository models.
- Produces: `rankDailySummary(...)`, `SummaryItem`, `SummaryCategory`, `ReminderMilestone`, `ReminderKind`, and `planReminders(...)` exactly as defined in Interfaces.

- [ ] **Step 1: Write ranking tests**

Assert the exact order: all `OVERDUE` tasks (including a task whose deadline passed earlier today); current-day future hard deadlines sorted by timestamp; remaining `HIGH` tasks; habits scheduled for today. Assert a maximum of five items by default, explicit `limit = 3` returns three, and limits below 3 or above 5 are clamped to the inclusive 3–5 range. Assert equal-ranked tasks retain stable input order.

- [ ] **Step 2: Run ranking tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.domain.summary.DailySummaryRankerTest"`
Expected: FAIL because ranker is not implemented.

- [ ] **Step 3: Implement the pure summary ranker**

Use the injected `now` and `zoneId` to identify local-day boundaries and scheduled habits; do not treat a task due later today as overdue. Return sorted items before applying the clamped limit.

- [ ] **Step 4: Write reminder planner tests**

Assert a future task receives a two-hour milestone and final-window milestones at 30, 20, and 10 minutes before due time when unacknowledged; assert no milestones in the past are scheduled; assert an incomplete task past due produces an overdue transition plan and no repeating escalation after its due instant; assert completed tasks produce no reminders.

- [ ] **Step 5: Implement the pure reminder planner**

Implement `planReminders(task, nowEpochMillis)` with exact milestone instants, deterministic ordering, and no Android dependencies. Keep one-off snooze scheduling in the scheduler adapter, not in the deadline planner.

- [ ] **Step 6: Run domain tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.domain.summary.DailySummaryRankerTest" --tests "com.dayline.domain.reminders.ReminderPlannerTest"`
Expected: PASS for ordering, limits, local-day handling, and milestone timing.

## Task 4: Implement habit schedules, daily completion, and streak grace

**Files:**
- Create: `domain/habits/HabitStreakCalculator.kt`.
- Modify: `data/HabitRepository.kt`, `data/local/HabitCompletionEntity.kt` as needed.
- Test: `domain/habits/HabitStreakCalculatorTest.kt`, `data/HabitRepositoryTest.kt`.

**Interfaces:**
- Consumes: Habit and HabitSchedule models plus local-date completion history.
- Produces: `isScheduledFor(habit: Habit, date: LocalDate): Boolean` and `calculateStreak(habit: Habit, completedDates: Set<LocalDate>, today: LocalDate): StreakState`, where `StreakState` contains `current: Int`, `best: Int`, and `graceDayUsed: Boolean`.

- [ ] **Step 1: Write schedule and streak tests**

Cover Daily, selected weekdays/weekends, and every-X-days from the anchor date; a completion increments a streak; one missed scheduled day uses grace without losing the current streak; two consecutive missed scheduled days reset current streak to zero; best streak never decreases; unscheduled days do not consume grace or break a streak.

- [ ] **Step 2: Run habit tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.domain.habits.HabitStreakCalculatorTest"`
Expected: FAIL because streak calculation is not implemented.

- [ ] **Step 3: Implement schedule and streak calculations**

Use `LocalDate` and schedule dates only. Calculate streaks from scheduled dates, not elapsed 24-hour durations. Preserve `best = max(previousBest, calculatedCurrent)`.

- [ ] **Step 4: Integrate idempotent local-day completion**

Wire repository completion to a unique habit/date record and recalculate streak state from persisted completions. Ensure checking a habit twice in the same local date cannot increment twice.

- [ ] **Step 5: Run habit tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.domain.habits.HabitStreakCalculatorTest" --tests "com.dayline.data.HabitRepositoryTest"`
Expected: PASS for schedules, grace, reset, best-streak retention, and duplicate completion.

## Task 5: Build task capture, editing, and Today/Tasks screens

**Files:**
- Create: `ui/today/TodayScreen.kt`, `TodayViewModel.kt`, `ui/tasks/TasksScreen.kt`, `TaskEditorScreen.kt`, `TaskViewModel.kt`.
- Modify: `ui/navigation/DaylineNavHost.kt`, `AppContainer.kt`.
- Test: `androidTest/java/com/dayline/ui/TodayScreenTest.kt`, `TaskFlowTest.kt`.

**Interfaces:**
- Consumes: repositories, `rankDailySummary(...)`, and `DaylineTheme`.
- Produces: usable task CRUD/completion flows, day summary, and the approved Today screen.

- [ ] **Step 1: Write Compose task-flow tests**

`TaskFlowTest` must create a task with title, due time, and priority; verify it appears in the task list and summary in the correct rank; edit its title; and complete it. `TodayScreenTest` must assert distinct **Hard Deadlines** and **Daily Habits** headings, the progress ring, next commitment, and a thumb-reachable floating `+`.

- [ ] **Step 2: Run UI tests to verify they fail**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.TaskFlowTest`
Expected: FAIL because task screens and flows do not exist.

- [ ] **Step 3: Implement task ViewModel and editor**

Use repository operations for add/edit/complete. Require a non-empty title, support optional notes/due instant, and expose `NORMAL`/`HIGH` priority and the per-task quiet-mode choice.

- [ ] **Step 4: Implement Today and Tasks screens**

Use the ranked summary and repository flows. Render overdue/deadline/high-priority/habit content with text/icon and category-specific status styling. Add the neon floating `+` action, progress indicator derived from today's completed tasks, and accessible high-contrast card text.

- [ ] **Step 5: Run task UI tests**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.TodayScreenTest,com.dayline.ui.TaskFlowTest`
Expected: PASS for task create/edit/complete and Today screen hierarchy.

## Task 6: Build habit editing, completion, and profile streak display

**Files:**
- Create: `ui/habits/HabitsScreen.kt`, `HabitEditorScreen.kt`, `HabitViewModel.kt`, `ui/profile/ProfileScreen.kt`.
- Modify: `ui/navigation/DaylineNavHost.kt`, `ui/today/TodayScreen.kt`.
- Test: `androidTest/java/com/dayline/ui/HabitFlowTest.kt`.

**Interfaces:**
- Consumes: HabitRepository, `isScheduledFor(...)`, `calculateStreak(...)`, and Task 5 navigation.
- Produces: habit CRUD/check-off experience, today's scheduled-habit summary cards, and profile best-streak display.

- [ ] **Step 1: Write habit UI tests**

Assert creation/editing for Daily, chosen weekdays/weekends, and every-X-days schedule; checking off marks the current local day complete; the Today section shows flexible habits separately from hard deadlines; profile displays the best streak after current streak reset.

- [ ] **Step 2: Run habit UI test to verify it fails**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.HabitFlowTest`
Expected: FAIL because habit screens are not implemented.

- [ ] **Step 3: Implement habit ViewModel and editor**

Persist schedule selection and expose current-day completion/streak state; make today's completion idempotent.

- [ ] **Step 4: Implement Habits and Profile screens**

Show schedule, completion action, current/best streak, and accessible teal habit styling. Integrate scheduled habits into Today without deadline alert controls.

- [ ] **Step 5: Run habit UI test**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.HabitFlowTest`
Expected: PASS for schedule selection, daily completion, separation, and best streak.

## Task 7: Implement deadline alarms, notification actions, and reboot restoration

**Files:**
- Create: `domain/reminders/ReminderScheduler.kt`, `platform/reminders/AndroidReminderScheduler.kt`, `ReminderReceiver.kt`, `ReminderActionReceiver.kt`, `BootReceiver.kt`, `platform/notifications/DaylineNotificationFactory.kt`.
- Modify: `AndroidManifest.xml`, `TaskRepository.kt`, `AppContainer.kt`, task completion flows.
- Test: `platform/reminders/ReminderActionReceiverTest.kt`, `BootReceiverTest.kt`, `data/TaskRepositoryTest.kt`.

**Interfaces:**
- Consumes: `planReminders(...)`, `ReminderScheduler`, TaskRepository, DataStore snooze value, and notification content.
- Produces: alarm registration/cancellation, high-priority notification channels, Done/Snooze/Dismiss actions, deadline-to-overdue transition, and boot restore.

- [ ] **Step 1: Write scheduler/action tests**

Assert normal two-hour and unacknowledged 30/20/10-minute urgent milestones; `Done` marks `COMPLETED`, clears notification, and cancels both AlarmManager and WorkManager requests for that task; `Snooze` schedules one alarm after the configured default 10 minutes without removing later milestones; `Dismiss` clears only the current notification; overdue stops escalations and creates one persistent notification; duplicate alarm/action delivery is idempotent.

- [ ] **Step 2: Run adapter tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.reminders.ReminderActionReceiverTest" --tests "com.dayline.platform.reminders.BootReceiverTest"`
Expected: FAIL because the Android scheduler and receivers do not exist.

- [ ] **Step 3: Implement `AndroidReminderScheduler`**

Use stable explicit PendingIntent identities keyed by task id and milestone. Use exact AlarmManager scheduling only when platform permission/access is available; surface the missing setup state and use an allowed WorkManager fallback for inexact work. Make `cancel(taskId)` cancel both alarm and work identifiers.

- [ ] **Step 4: Implement notification factory and receivers**

Create separate standard, urgent, summary/ongoing, and overdue channels. Apply per-task quiet-mode choice only through Android-authorized capabilities. For Done, update the repository before canceling reminders and notifications. Snooze must create one alarm at `now + configuredMinutes` without changing milestone identities. Dismiss must not cancel future milestone work.

- [ ] **Step 5: Implement deadline transition and boot recovery**

At the due-time receiver, atomically transition only `ACTIVE` tasks to `OVERDUE`, cancel active escalation work, and post/update a single persistent notification. Register `BootReceiver` for `ACTION_BOOT_COMPLETED`; `restorePending()` reloads active future-due tasks and overdue tasks, recalculates future alarms, and restores persistent notifications.

- [ ] **Step 6: Add manifest permissions and run adapter tests**

Declare `RECEIVE_BOOT_COMPLETED`; request notification and exact-alarm access using the target-API-appropriate flow. Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.reminders.ReminderActionReceiverTest" --tests "com.dayline.platform.reminders.BootReceiverTest" --tests "com.dayline.data.TaskRepositoryTest"`

Expected: PASS; denied permission tests show setup/fallback and never claim an exact alarm is active.

## Task 8: Implement configurable Smart Wake and morning summary

**Files:**
- Create: `platform/summary/SmartWakeReceiver.kt`, `MorningSummaryScheduler.kt`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`.
- Modify: `DaylineNotificationFactory.kt`, `AndroidManifest.xml`, `AppContainer.kt`, `UserSettingsStore.kt`.
- Test: `platform/summary/SmartWakeReceiverTest.kt`, `androidTest/java/com/dayline/ui/SettingsFlowTest.kt`.

**Interfaces:**
- Consumes: summary ranker, DataStore summary time, ongoing notification factory, and Task/Habit repositories.
- Produces: configurable `08:00` default, first-unlock summary trigger from `configuredTime - 1 hour`, and fallback at configured time.

- [ ] **Step 1: Write Smart Wake tests**

For configured 09:00, assert cutoff 08:00; a first unlock at/after cutoff displays exactly one summary and cancels fallback; if still locked, fallback fires exactly at 09:00; if unlock and fallback race, only one summary is delivered; changing configured time recalculates both triggers; a new local date permits the next summary.

- [ ] **Step 2: Run Smart Wake tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.summary.SmartWakeReceiverTest"`
Expected: FAIL because scheduler/receiver do not exist.

- [ ] **Step 3: Implement summary scheduler and unlock receiver**

Register the permitted Android user-present/unlock event receiver. Persist a local-date delivery marker; schedule the configured fallback and derive the unlock cutoff dynamically. The first eligible unlock posts/opens the summary experience once and cancels the fallback. If no unlock occurs, post the standard summary notification at the configured time.

- [ ] **Step 4: Implement ongoing summary notification and settings UI**

Include top-five ranked items and a route to Today/`View All` in the summary notification. Let users change the local summary time and enable/disable the daily summary; reschedule immediately after a setting change. Keep summary accessible in the persistent notification surface.

- [ ] **Step 5: Run Smart Wake and settings tests**

Run unit tests:

`.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.summary.SmartWakeReceiverTest"`

Then run settings UI tests on a connected device/emulator:

`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.SettingsFlowTest`

Expected: PASS for cutoff/fallback/race behavior and persisted configuration.

## Task 9: Add optional, secure, read-only Android Calendar integration

**Files:**
- Create: `platform/calendar/AndroidCalendarReader.kt`.
- Modify: `SettingsScreen.kt`, `SettingsViewModel.kt`, `AndroidManifest.xml`, `TodayViewModel.kt`, `DailySummaryRanker.kt` or its input adapter.
- Test: `platform/calendar/AndroidCalendarReaderTest.kt`, `androidTest/java/com/dayline/ui/SettingsFlowTest.kt`.

**Interfaces:**
- Consumes: optional calendar setting, runtime permission result, and Android Calendar Provider.
- Produces: `CalendarCommitment(id: String, title: String, startAtEpochMillis: Long, endAtEpochMillis: Long?)`, exposed through `suspend fun readBetween(startEpochMillis: Long, endEpochMillis: Long): List<CalendarCommitment>`.

- [ ] **Step 1: Write Calendar reader tests**

Assert events in the requested time range are returned sorted by start; denied/revoked permission produces a user-visible unavailable state and does not affect manually entered tasks; duplicate reads do not duplicate commitments; events are never inserted/updated/deleted in the provider.

- [ ] **Step 2: Run calendar tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.calendar.AndroidCalendarReaderTest"`
Expected: FAIL because the adapter does not exist.

- [ ] **Step 3: Implement read-only provider adapter and consent flow**

Request `READ_CALENDAR` only when the user enables import, query only the selected time range and required columns, close cursors reliably, and map provider failures/permission revocation to an explicit settings state. Do not persist full calendar event details unless needed for display.

- [ ] **Step 4: Integrate commitments and run tests**

Merge same-day calendar commitments into the upcoming-event view without treating them as editable tasks. Run unit tests:

`.\gradlew.bat :app:testDebugUnitTest --tests "com.dayline.platform.calendar.AndroidCalendarReaderTest"`

Then run settings UI tests on a connected device/emulator:

`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayline.ui.SettingsFlowTest`

Expected: PASS for optional opt-in, read-only results, permission denial, and manual-task independence.

## Task 10: Complete app-wide accessibility, lifecycle, and release verification

**Files:**
- Modify: affected app files from Tasks 1–9.
- Test: all listed unit and instrumentation tests.

**Interfaces:**
- Consumes: complete app.
- Produces: integrated release candidate meeting the approved design and behavior requirements.

- [ ] **Step 1: Add integration tests for Review Focus**

Add tests for timezone/DST date handling, revoked notification/exact-alarm/calendar permissions, simultaneous task completion and reminder delivery, Smart Wake fallback/unlock races, and repeated boot restoration. Assert task state never regresses from `COMPLETED` and reminder IDs stay unique.

- [ ] **Step 2: Verify accessible visual behavior**

Add Compose assertions for content descriptions on progress ring and quick-add, scalable text, contrast-bearing labels, and non-color-only indicators for deadline/habit/priority. Confirm system font scaling does not hide completion or snooze actions.

- [ ] **Step 3: Run complete automated verification**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, all domain and repository tests pass.

Run with an Android emulator/device:

`.\gradlew.bat :app:connectedDebugAndroidTest`

Expected: `BUILD SUCCESSFUL`, all task/habit/settings and accessibility interaction tests pass.

- [ ] **Step 4: Perform device validation**

On at least one Android device/emulator at the selected target SDK, validate notification permission onboarding, exact-alarm denial/grant, lock-screen notification visibility, DND-per-task behavior where authorized, reboot restoration, Smart Wake unlock/fallback, calendar denial/grant, and local-time summary/habit reset. Record any manufacturer-specific restriction and its user-facing fallback.

## Self-review

- **Spec coverage:** Task capture/CRUD and Today ranking are Task 5; summary rank and top-five limit Task 3; habits/streak grace Task 4 and Task 6; alerts/actions/overdue/reboot Task 7; Smart Wake/configuration/persistent surface Task 8; manual task independence and optional Calendar read access Task 9; approved visual system/accessibility Task 1, Task 5, Task 6, and Task 10.
- **Step scan:** Every implementation task has a named file boundary, interface dependency, behavioral test, and focused verification command; no task depends on an undefined app service.
- **Type consistency:** Task, Habit, Reminder, summary and scheduler types are defined once in Interfaces and reused by dependent tasks. Keep one canonical `ReminderScheduler` interface under `domain/reminders`.
- **Review Focus coverage:** Permission states (Tasks 7 and 9); DST/timezone (Tasks 3, 4, and 10); reminder races (Tasks 7 and 10); Smart Wake timing (Tasks 8 and 10); Calendar permission/data changes (Task 9).
- **Proportion:** Ten tasks cover the connected product capabilities in independently testable increments; implementation remains one app rather than separate products.
