<div align="center">

# 🌅 Dayline

**Your day, in one line — tasks, deadlines and habits that never slip through the cracks.**

[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#-getting-started)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

</div>

---

Dayline is an offline-first Android task manager for students, professionals and anyone with a busy day.
It was born from a familiar problem: getting so absorbed in work that a project submission or a client
meeting quietly slips by. Dayline keeps what matters visible, brings it back at the right moment, and
makes daily progress feel calm instead of noisy.

## ✨ Features

### 📋 Tasks & deadlines
- **Quick capture** with a thumb-friendly **+** button
- **Calendar & clock pickers** for due dates and times
- **High Priority** flag for the things that can't wait
- **Search** tasks by title or notes
- Overdue tasks are tracked automatically until you act on them

### ⏰ Reminders that actually work
- A gentle heads-up **2 hours** before a deadline
- In the **final 30 minutes**, an urgent alert every **10 minutes** until you respond
- Deadlines **ring like an alarm clock** with a full-screen page — even on the lock screen
- **Done / Snooze / Dismiss** right from the notification
- Optional per-task **Do Not Disturb bypass** (when Android permits it)
- Reminders are **restored automatically after a reboot**

### ☀️ Smart morning summary
- Your top **3–5 critical items** for the day, ranked: overdue → today's deadlines → high priority → habits
- **Smart Wake** shows the summary on your first unlock in the hour before your chosen time, with a regular
  notification as fallback
- Summary time is fully configurable (default 8:00 AM)

### 🔥 Habits & streaks
- Daily, specific weekdays, or every *X* days
- **Forgiving streaks** — one missed day is a grace period; only two in a row resets the streak
- Your **best streak** is kept forever on your profile

### 🔒 Private by design
- Works fully **offline** — no account, no cloud, no tracking
- Optional **read-only** calendar import (Dayline never writes to your calendar)
- **Backup & restore** tasks, habits and settings to a file you control

### 🌐 Languages
- English and **বাংলা (Bangla)**

## 🎨 Design

A modern, minimal dark interface: near-black surfaces, rounded glass cards, bold type, and neon lime,
violet and teal accents. Every color is paired with an icon or text label, and secondary text stays
high-contrast so the app is readable outdoors.

## 🛠️ Tech stack

| Layer | Tools |
| --- | --- |
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Storage | Room (tasks & habits), DataStore (settings) |
| Background work | AlarmManager (exact alarms), WorkManager (fallback), foreground alarm service |
| Async | Kotlin Coroutines & Flow |
| Testing | JUnit 4, Robolectric, Compose UI tests |

**Min SDK** 26 (Android 8.0) · **Target SDK** 36

## 🚀 Getting started

### Requirements
- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17 or newer

### Build & run

```bash
git clone https://github.com/mahamud894/Dayline-.git
cd Dayline-
./gradlew installDebug        # Windows: gradlew.bat installDebug
```

Or simply open the project in Android Studio and press **Run ▶**.

### Run the tests

```bash
./gradlew testDebugUnitTest
```

### Release build (optional)

To build a signed release APK, create a `keystore.properties` file in the project root:

```properties
storeFile=your-release-key.jks
storePassword=••••••
keyAlias=your-alias
keyPassword=••••••
```

Then run `./gradlew assembleRelease`. Both `keystore.properties` and `*.jks` files are git-ignored —
**never commit them**. Without this file, the release build falls back to debug signing (fine for testing,
not for Google Play).

## 📱 Permissions

Dayline asks only for what it needs, and explains each one in the app:

| Permission | Why |
| --- | --- |
| Notifications | Deadline reminders and the morning summary |
| Exact alarms | Reminders that fire on time |
| Full-screen alarms | Deadline alarm page on the lock screen |
| Do Not Disturb access | Only for tasks where you enable the bypass |
| Calendar (read-only) | Optional — show calendar events in your day |
| Run at startup | Restore reminders after the phone restarts |

## 📂 Project structure

```
app/src/main/java/com/dayline/
├── data/        # Room database, repositories, backup
├── domain/      # Models, habit streak rules, reminder planning, summary ranking
├── platform/    # Alarms, notifications, boot recovery, Smart Wake, calendar
└── ui/          # Compose screens: Today, Tasks, Habits, Profile, Settings
```

## 📄 License

Released under the [MIT License](LICENSE) © 2026 Mahamud Hassan.

<div align="center">
<sub>Made with ☕ and Kotlin</sub>
</div>
