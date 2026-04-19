package com.kitwlshcom.kdailyutil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kitwlshcom.kdailyutil.ui.MainScreen
import com.kitwlshcom.kdailyutil.ui.theme.KDailyUtilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KDailyUtilTheme {
                MainScreen()
            }
        }
    }
}