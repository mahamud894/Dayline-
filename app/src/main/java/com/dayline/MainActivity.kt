package com.dayline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dayline.ui.navigation.DaylineNavHost
import com.dayline.ui.theme.DaylineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaylineTheme {
                DaylineNavHost()
            }
        }
    }
}
