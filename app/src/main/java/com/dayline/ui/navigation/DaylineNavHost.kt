package com.dayline.ui.navigation

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayline.AppContainer
import com.dayline.platform.calendar.CalendarAccessUnavailableException
import com.dayline.platform.calendar.CalendarCommitment
import com.dayline.domain.model.Task
import com.dayline.ui.habits.HabitEditorScreen
import com.dayline.ui.habits.HabitViewModel
import com.dayline.ui.habits.HabitsScreen
import com.dayline.ui.profile.ProfileScreen
import com.dayline.ui.settings.SettingsScreen
import com.dayline.ui.settings.SettingsViewModel
import com.dayline.ui.tasks.TaskEditorScreen
import com.dayline.ui.tasks.TaskViewModel
import com.dayline.ui.tasks.TasksScreen
import com.dayline.ui.today.TodayScreen
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

private enum class Destination(val label: String) {
    TODAY("Today"),
    TASKS("Tasks"),
    HABITS("Habits"),
    PROFILE("Profile"),
    SETTINGS("Settings")
}

private data class CalendarCommitmentsState(
    val commitments: List<CalendarCommitment> = emptyList(),
    val unavailableMessage: String? = null
)

@Composable
fun DaylineNavHost(container: AppContainer) {
    val navigationDestinations = remember { Destination.entries.filter { it != Destination.SETTINGS } }
    val taskViewModel: TaskViewModel = viewModel(
        factory = remember(container) {
            TaskViewModel.Factory(
                container.taskRepository,
                container.reminderScheduler,
                container.reminderActionHandler
            )
        }
    )
    val habitViewModel: HabitViewModel = viewModel(
        factory = remember(container) { HabitViewModel.Factory(container.habitRepository) }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(container) {
            SettingsViewModel.Factory(
                container.userSettingsStore,
                container.morningSummaryScheduler,
                container.notificationFactory,
                container.backup
            )
        }
    )
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var calendarRefreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                calendarRefreshKey++
                habitViewModel.refreshToday()
                taskViewModel.refreshReminderScheduleStatus()
                settingsViewModel.refreshSummaryScheduleStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            settingsViewModel.setCalendarImportEnabled(true)
        } else {
            settingsViewModel.reportCalendarPermissionDenied()
        }
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            settingsViewModel.exportBackup { json ->
                checkNotNull(context.contentResolver.openOutputStream(uri, "wt")) {
                    "the file couldn't be opened"
                }.bufferedWriter().use { it.write(json) }
            }
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.restoreBackup {
                checkNotNull(context.contentResolver.openInputStream(uri)) {
                    "the file couldn't be opened"
                }.bufferedReader().use { it.readText() }
            }
        }
    }
    val backupMessage by settingsViewModel.backupMessage.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val habits by habitViewModel.habits.collectAsState()
    val today by habitViewModel.today.collectAsState()
    val completedHabitIds by habitViewModel.completedHabitIds.collectAsState()
    val saveError by taskViewModel.saveError.collectAsState()
    val reminderScheduleStatus by taskViewModel.reminderScheduleStatus.collectAsState()
    val habitSaveError by habitViewModel.saveError.collectAsState()
    val userSettings by settingsViewModel.settings.collectAsState()
    val summaryScheduleStatus by settingsViewModel.scheduleStatus.collectAsState()
    val settingsError by settingsViewModel.error.collectAsState()
    // Re-read on every resume, since the user may have just changed it in system settings.
    val alarmFullScreenAllowed = remember(calendarRefreshKey) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val isXiaomiDevice = remember {
        Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")
    }
    val calendarPermissionGranted =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
    var selected by rememberSaveable { mutableStateOf(Destination.TODAY) }
    var isEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var isHabitEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingHabitId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingTask = editingTaskId?.let { id -> tasks.firstOrNull { it.id == id } }
    val editingHabit = editingHabitId?.let { id -> habits.firstOrNull { it.id == id } }
    val calendarState by produceState(
        initialValue = CalendarCommitmentsState(),
        userSettings.calendarImportEnabled,
        selected,
        today,
        calendarRefreshKey
    ) {
        if (!userSettings.calendarImportEnabled || selected != Destination.TODAY) {
            value = CalendarCommitmentsState()
        } else {
            val zoneId = ZoneId.systemDefault()
            val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            try {
                value = CalendarCommitmentsState(
                    commitments = container.calendarReader.readBetween(start, end)
                )
            } catch (exception: CalendarAccessUnavailableException) {
                value = CalendarCommitmentsState(unavailableMessage = exception.message)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                value = CalendarCommitmentsState(
                    unavailableMessage =
                        "Calendar commitments couldn't be loaded: ${exception.message ?: "unknown error"}"
                )
            }
        }
    }

    // System Back returns to the previous screen instead of closing the app.
    BackHandler(enabled = isHabitEditorVisible) {
        isHabitEditorVisible = false
        habitViewModel.clearSaveError()
    }
    BackHandler(enabled = !isHabitEditorVisible && isEditorVisible) {
        isEditorVisible = false
        taskViewModel.clearSaveError()
    }
    BackHandler(
        enabled = !isHabitEditorVisible && !isEditorVisible && selected != Destination.TODAY
    ) {
        selected = if (selected == Destination.SETTINGS) Destination.PROFILE else Destination.TODAY
    }

    if (isHabitEditorVisible) {
        HabitEditorScreen(
            habit = editingHabit,
            saveError = habitSaveError,
            today = today,
            onBack = {
                isHabitEditorVisible = false
                habitViewModel.clearSaveError()
            },
            onClearError = habitViewModel::clearSaveError,
            onSave = { title, schedule ->
                habitViewModel.saveHabit(
                    existing = editingHabit,
                    title = title,
                    schedule = schedule
                ) {
                    isHabitEditorVisible = false
                    editingHabitId = null
                }
            },
            onDelete = {
                editingHabit?.let { habit ->
                    habitViewModel.deleteHabit(habit.id) {
                        isHabitEditorVisible = false
                        editingHabitId = null
                    }
                }
            }
        )
    } else if (isEditorVisible) {
        TaskEditorScreen(
            task = editingTask,
            saveError = saveError,
            onBack = {
                isEditorVisible = false
                taskViewModel.clearSaveError()
            },
            onClearError = taskViewModel::clearSaveError,
            onSave = { title, notes, dueAtText, priority, quietModeOverrideAllowed ->
                taskViewModel.saveTask(
                    existing = editingTask,
                    title = title,
                    notes = notes,
                    dueAtText = dueAtText,
                    priority = priority,
                    quietModeOverrideAllowed = quietModeOverrideAllowed
                ) {
                    isEditorVisible = false
                    editingTaskId = null
                }
            },
            onDelete = {
                editingTask?.let { task ->
                    taskViewModel.deleteTask(task.id) {
                        isEditorVisible = false
                        editingTaskId = null
                    }
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0B0D14),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111620)) {
                    navigationDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            modifier = Modifier.testTag("nav-${destination.label.lowercase()}"),
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        Destination.TODAY -> Icons.Rounded.Home
                                        Destination.TASKS -> Icons.Rounded.CheckCircle
                                        Destination.HABITS -> Icons.Rounded.Star
                                        Destination.PROFILE -> Icons.Rounded.Person
                                        Destination.SETTINGS -> Icons.Rounded.Settings
                                    },
                                    contentDescription = null
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF10150B),
                                selectedTextColor = Color(0xFFB7FF4A),
                                indicatorColor = Color(0xFFB7FF4A),
                                unselectedIconColor = Color(0xFFC2C7D3),
                                unselectedTextColor = Color(0xFFC2C7D3)
                            ),
                            label = { Text(destination.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selected == Destination.TODAY || selected == Destination.TASKS) {
                    FloatingActionButton(
                        onClick = {
                            editingTaskId = null
                            taskViewModel.clearSaveError()
                            isEditorVisible = true
                        },
                        modifier = Modifier
                            .testTag("add-task-fab")
                            .semantics { contentDescription = "Add task" },
                        shape = CircleShape,
                        containerColor = Color(0xFFB7FF4A),
                        contentColor = Color(0xFF10150B)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                }
            }
        ) { contentPadding ->
            when (selected) {
                Destination.TODAY -> TodayScreen(
                    tasks = tasks,
                    habits = habits,
                    today = today,
                    completedHabitIds = completedHabitIds,
                    calendarCommitments = calendarState.commitments,
                    calendarImportEnabled = userSettings.calendarImportEnabled,
                    calendarUnavailableMessage = calendarState.unavailableMessage,
                    reminderScheduleStatus = reminderScheduleStatus,
                    onCompleteHabit = { habitViewModel.completeHabit(it.id) },
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.TASKS -> TasksScreen(
                    tasks = tasks,
                    reminderScheduleStatus = reminderScheduleStatus,
                    onEditTask = { task ->
                        editingTaskId = task.id
                        taskViewModel.clearSaveError()
                        isEditorVisible = true
                    },
                    onCompleteTask = { taskViewModel.completeTask(it.id) },
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.HABITS -> HabitsScreen(
                    habits = habits,
                    completedHabitIds = completedHabitIds,
                    onAddHabit = {
                        editingHabitId = null
                        habitViewModel.clearSaveError()
                        isHabitEditorVisible = true
                    },
                    onEditHabit = { habit ->
                        editingHabitId = habit.id
                        habitViewModel.clearSaveError()
                        isHabitEditorVisible = true
                    },
                    onCompleteHabit = { habitViewModel.completeHabit(it.id) },
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.PROFILE -> ProfileScreen(
                    habits = habits,
                    onOpenSettings = { selected = Destination.SETTINGS },
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.SETTINGS -> SettingsScreen(
                    settings = userSettings,
                    calendarPermissionGranted = calendarPermissionGranted,
                    scheduleStatus = summaryScheduleStatus,
                    error = settingsError,
                    onSummaryEnabledChange = settingsViewModel::setSummaryEnabled,
                    onSaveSummaryTime = settingsViewModel::saveSummaryTime,
                    onCalendarImportChange = { enabled ->
                        if (enabled) {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        } else {
                            settingsViewModel.setCalendarImportEnabled(false)
                        }
                    },
                    onClearError = settingsViewModel::clearError,
                    onBack = { selected = Destination.PROFILE },
                    onOpenNotificationSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    },
                    onOpenExactAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            }
                        }
                    },
                    onOpenDndSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        )
                    },
                    onOpenCalendarPermissionSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    },
                    modifier = Modifier.padding(contentPadding),
                    alarmFullScreenAllowed = alarmFullScreenAllowed,
                    isXiaomiDevice = isXiaomiDevice,
                    onOpenFullScreenAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            context.startActivityOrAppDetails(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    },
                    onOpenXiaomiPermissions = {
                        context.startActivityOrAppDetails(
                            Intent("miui.intent.action.APP_PERM_EDITOR")
                                .setClassName(
                                    "com.miui.securitycenter",
                                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                                )
                                .putExtra("extra_pkgname", context.packageName)
                        )
                    },
                    onOpenXiaomiAutostart = {
                        context.startActivityOrAppDetails(
                            Intent().setClassName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                            )
                        )
                    },
                    backupMessage = backupMessage,
                    onExportBackup = {
                        exportBackupLauncher.launch("Dayline-backup-${LocalDate.now()}.json")
                    },
                    onRestoreBackup = {
                        // Some file managers label .json as text or octet-stream.
                        restoreBackupLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream")
                        )
                    }
                )
            }
        }
    }
}

/** Opens a system screen, or the app's info page on phones that don't have it. */
private fun Context.startActivityOrAppDetails(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
        )
    } catch (_: SecurityException) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))
        )
    }
}
