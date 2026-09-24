package com.liezert.itungen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liezert.itungen.ui.navigation.ItungEnRoot
import com.liezert.itungen.ui.theme.ItungEnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ItungEnTheme { ItungEnRoot() } }
    }

    override fun onStart() {
        super.onStart()
        (application as ItungEnApp).container.catchUpRecurring()
    }
}
