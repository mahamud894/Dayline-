package com.dayline.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Destination(val label: String) {
    TODAY("Today"),
    TASKS("Tasks"),
    HABITS("Habits"),
    PROFILE("Profile")
}

@Composable
fun DaylineNavHost() {
    var selected by rememberSaveable { mutableStateOf(Destination.TODAY) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = {},
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        Text(
            text = "${selected.label} coming soon",
            modifier = Modifier.padding(contentPadding)
        )
    }
}
