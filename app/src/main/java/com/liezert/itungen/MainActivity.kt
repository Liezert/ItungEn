package com.liezert.itungen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.liezert.glass.tokens.GlassColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HelloScreen() }
    }
}

@Preview
@Composable
private fun HelloScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(GlassColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Text("Hello, ItungEn", color = GlassColors.TextPrimary)
    }
}
