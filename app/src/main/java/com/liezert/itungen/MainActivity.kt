package com.liezert.itungen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liezert.itungen.ui.navigation.AppNav
import com.liezert.itungen.ui.theme.ItungEnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { ItungEnTheme { AppNav() } }
    }

    override fun onStart() {
        super.onStart()
        (application as ItungEnApp).container.syncInBackground()
    }
}
