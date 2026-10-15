package com.liezert.itungen

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.liezert.itungen.ui.lock.LockScreen
import com.liezert.itungen.ui.navigation.AppNav
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.ItungEnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Selama app terbuka, perubahan dari HP lain ditarik berkala; unggah sudah otomatis setiap ada perubahan. */
private const val ForegroundSyncMillis = 60_000L

/** Kunci aplikasi aktif lagi bila app ditinggalkan lebih lama dari ini (pemilih berkas / izin tidak mengunci ulang). */
private const val RelockMillis = 30_000L

class MainActivity : ComponentActivity() {
    /** Bertambah setiap ada permintaan membuka Tambah Transaksi (pintasan ikon / notifikasi pengingat). */
    private var addRequests by mutableIntStateOf(0)
    private var locked by mutableStateOf(false)
    private var leftAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && intent?.action == ACTION_ADD) addRequests++
        val container = (application as ItungEnApp).container
        Ink.dark = container.darkTheme
        locked = container.appLock
        // Kunci aktif = pratinjau di Recents disamarkan, agar saldo tidak terlihat dari daftar aplikasi.
        setRecentsScreenshotEnabled(!container.appLock)
        enableEdgeToEdge(statusBarStyle = barStyle(), navigationBarStyle = barStyle())
        super.onCreate(savedInstanceState)
        setContent {
            // Ikon status bar & navigasi gelap di tema terang, terang di tema gelap.
            LaunchedEffect(Ink.dark) { enableEdgeToEdge(statusBarStyle = barStyle(), navigationBarStyle = barStyle()) }
            ItungEnTheme {
                Box(Modifier.fillMaxSize()) {
                    AppNav(addRequests)
                    if (locked) LockScreen(onUnlock = { locked = false })
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    container.sync.sync() // langsung selesai bila belum masuk
                    delay(ForegroundSyncMillis)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val container = (application as ItungEnApp).container
        if (container.appLock && leftAt > 0 && SystemClock.elapsedRealtime() - leftAt > RelockMillis) locked = true
    }

    override fun onStop() {
        super.onStop()
        leftAt = SystemClock.elapsedRealtime()
    }

    // singleTop: pintasan / notifikasi saat app sudah terbuka datang ke sini.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_ADD) addRequests++
    }

    private fun barStyle() =
        if (Ink.dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)

    companion object {
        const val ACTION_ADD = "com.liezert.itungen.ADD"

        fun addTransactionIntent(context: Context) = Intent(context, MainActivity::class.java)
            .setAction(ACTION_ADD)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
