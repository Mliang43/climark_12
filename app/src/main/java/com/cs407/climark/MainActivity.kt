package com.cs407.climark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cs407.climark.ui.screens.MapScreen
import com.cs407.climark.ui.theme.ClimarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClimarkTheme { MapScreen() }
        }
    }
}