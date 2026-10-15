package com.liezert.itungen.ui.lock

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.itungen.ui.components.AppBackground
import com.liezert.itungen.ui.theme.Bold
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.Regular
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText

/** Sidik jari / wajah, atau PIN/pola HP sebagai cadangan (tanpa library: BiometricPrompt bawaan API 30+). */
private const val Authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

object AppLock {
    /** false = HP belum punya kunci layar, jadi tidak ada yang bisa dipakai untuk membuka. */
    fun available(context: Context): Boolean =
        context.getSystemService(BiometricManager::class.java).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(context: Context, title: String, onResult: (Boolean) -> Unit) {
        BiometricPrompt.Builder(context)
            .setTitle(title)
            .setAllowedAuthenticators(Authenticators)
            .build()
            .authenticate(
                CancellationSignal(),
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
                },
            )
    }
}

/** Menutupi seluruh app sampai pemilik HP membuka kunci; prompt muncul sendiri sekali. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prompt = { AppLock.authenticate(context, "Buka ItungEn") { if (it) onUnlock() } }
    LaunchedEffect(Unit) { prompt() }
    BackHandler { activity?.moveTaskToBack(true) }
    val backdrop = rememberLayerBackdrop()
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.Black)
            .pointerInput(Unit) { detectTapGestures { } }, // sentuhan tidak tembus ke app di bawahnya
    ) {
        AppBackground(backdrop, vignette = true)
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) { LockContent(prompt) }
    }
}

@Composable
private fun BoxScope.LockContent(prompt: () -> Unit) {
    Column(
        Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ItungEn terkunci", style = appText(22f, 28f, Bold))
        Text(
            "Gunakan sidik jari, wajah, atau PIN HP untuk membuka.",
            style = appText(14f, 20f, Regular, Ink.Gray),
            textAlign = TextAlign.Center,
        )
        LiquidButton(onClick = prompt, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp)) {
            Text("Buka kunci", style = appText(16f, 22f, SemiBold))
        }
    }
}
