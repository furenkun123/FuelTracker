package com.fueltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fueltracker.ui.theme.FuelTrackerTheme
import com.fueltracker.ui.theme.ThemePreferences
import com.fueltracker.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 启动时从 SharedPreferences 读一次
        ThemePreferences.init(applicationContext)

        // 2) 边到边(只调一次, 放 setContent 之前或之内都行)
        enableEdgeToEdge()

        // 3) 单层 setContent, 一次 FuelTrackerTheme
        setContent {
            FuelTrackerTheme(themeMode = ThemePreferences.mode) {
                FuelTrackerApp(viewModel = viewModel)
            }
        }
    }
}