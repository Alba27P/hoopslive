package com.hoopslive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hoopslive.app.ui.AppNav
import com.hoopslive.app.ui.theme.HoopsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoopsTheme {
                AppNav()
            }
        }
    }
}
