# Dayline Android Task Manager — Design

**Status:** Draft for user approval  
**Date:** 2026-10-18

## Intent

Build an Android task-management app for busy individuals, students, and professionals who need help remembering routines, assignments, deadlines, and other commitments. The motivating use case is losing track of a project submission and a client meeting while absorbed in debugging. Dayline should keep the most important commitments visible, bring them back to attention at useful times, and make daily progress feel manageable rather than noisy.

## Goals

- Capture tasks quickly and see today's commitments, priorities, and recurring habits in one place.
- Surface important work in a short morning summary, with an option to see the full list.
- Use progressive, actionable deadline reminders and a persistent glanceable widget.
- Track recurring habits and streaks with a forgiving grace rule.
- Make the app feel distinctive, clean, legible outdoors, and satisfying to use.

## Non-goals for the first release

- Social feeds, shared team workspaces, leaderboards, or collaborative task editing.
- Replacing a full calendar application.
- Cloud accounts or cross-device synchronization. **This is an implementation assumption, not a user decision**; see “Implementation calls.”

## Product experience

The approved direction is a modern, minimal, futuristic dark interface: near-black surfaces, rounded frosted-glass cards, bold typography, neon lime and violet accents, teal for habits, and warm/pink accents for urgency. Use clear labels and icons as well as color so category and priority remain understandable. Secondary text must stay high-contrast for outdoor readability.

The Today screen contains:

1. A morning greeting and circular daily progress ring.
2. The next time-based commitment.
3. A **Hard Deadlines** section for fixed-time tasks, priority badges, completion progress, and reminder times.
4. A separate **Daily Habits** section with check-offs and streak cues.
5. A prominent thumb-reachable floating **+** action for fast task capture.
6. Navigation for Today, Tasks, Habits, and Profile.

The morning summary and persistent notification use the same visual hierarchy. Keep the daily summary, next event, next hard deadline, and relevant upcoming alert accessible at a glance. Use an ongoing notification in the system tray as the reliable fallback, accessible from the notification shade and standard Android lock screen when lock-screen notification settings permit.

## Task and summary behavior

- Tasks can be manually flagged **High Priority** and can have a fixed due date/time.
- Include timed commitments such as a client meeting in the day's upcoming items. Support both manual entry of ad-hoc commitments and optional, secure, one-way import from the native Android Calendar. Request `READ_CALENDAR` only for calendar integration; do not write or modify calendar events.
- The summary orders items as follows:
  1. Overdue tasks from previous days.
  2. Today's hard deadlines, sorted chronologically by due time.
  3. Manually flagged High Priority tasks.
  4. Habits scheduled for today.
- Show only the top 3–5 critical items, with **View All** for the rest.
- The morning summary's default delivery time is 8:00 AM local time and is configurable in settings.
- **Smart Wake:** set the unlock window to start one hour before the configured summary time. Present the summary at the first device unlock on or after that point. If the device remains locked, deliver the fallback notification exactly at the configured summary time.
- Example: for a 9:00 AM configured time, show the summary on the first unlock after 8:00 AM or, if the device remains locked, send a standard notification at 9:00 AM.

## Deadline reminders and actions

- Send a standard reminder two hours before a task deadline.
- During the final 30 minutes, send high-priority sound/vibration alerts every 10 minutes while the alert remains unacknowledged.
- Keep a high-priority lock-screen banner visible until the user explicitly completes, snoozes, or dismisses the alert.
- Let the user choose quiet-mode behavior per task. Urgent alerts can bypass silent/Do Not Disturb settings only when enabled for that task and permitted by Android.
- **Done:** atomically mark the task `COMPLETED` in the local database, immediately clear the active notification, and cancel all pending AlarmManager/WorkManager reminder intents for that task.
- **Snooze:** suppress the active alert and schedule a one-off high-priority alarm for a preset delay (10 minutes by default). Do not shift or cancel downstream deadline milestone alerts.
- **Dismiss:** clear the active system notification/heads-up banner but keep all remaining scheduled future escalation alerts and deadline triggers intact.
- At the deadline, stop active deadline/escalation alarms to prevent repeated alert spam. If the task remains incomplete, transition it to `OVERDUE` and post one persistent overdue notification, updating the persistent surface. Keep it visible until the user acknowledges, reschedules, or completes the task.

## Habit behavior

- Support Daily, Specific Days (including weekday/weekend selections), and Custom Intervals (every X days).
- Checking off a habit completes it for the current local calendar day; it resets for the next day at local midnight.
- A completion on a scheduled day increments the current streak.
- One missed scheduled day freezes the streak as a one-day grace period. Missing two consecutive scheduled days resets the current streak to zero.
- Preserve the best/longest streak in profile stats even after a current streak resets.
- Habit nudges must not use the urgent hard-deadline escalation behavior.

## Constraints and quality bar

- Android is the target platform; the current workspace is empty and has no existing application or stack to preserve.
- Task completion and reminder cancellation must stay consistent with the local task state.
- Reminder scheduling, reboot recovery, notification permissions, exact-alarm restrictions, DND access, lock-screen visibility, and background execution must follow Android's actual OS/device capabilities. Do not claim a reminder or widget is persistent if the platform can silently suppress it.
- Register a `BroadcastReceiver` for `ACTION_BOOT_COMPLETED` and restore pending AlarmManager/WorkManager reminders after device restart without waiting for the user to reopen the app. Request `RECEIVE_BOOT_COMPLETED` and exact-alarm permissions as applicable, and explain required setup in the UI.
- Keep task and habit content private and usable without network access.
- Test the ranking and top-3-to-5 summary limit, local-day habit reset and streak cases, each reminder milestone, quiet-mode choices, and the Done/Snooze/Dismiss effects.

## Implementation calls (not user decisions)

- Use a native Android implementation with Kotlin and Jetpack Compose rather than a cross-platform toolkit, because the core experience depends on Android notifications, alarms, device unlock behavior, and widgets.
- Treat the first release as single-device and local-first, without accounts or sync. Revisit this if multi-device use is required.
- Use a clear text/icon label in addition to each neon color for category, status, and priority.
- Preserve the approved mockup's dark glass visual style while adapting it to native Android sizing, accessibility settings, and system UI.

## Remaining implementation details

- Choose reliable, permission-aware Android implementations for calendar reads, Smart Wake unlock detection, persistent notifications, and reminder restoration.
- Provide fallbacks for OS/device restrictions without implying that a notification or alarm can override Android settings when permission or user authorization is missing.
