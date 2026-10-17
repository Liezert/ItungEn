package com.liezert.itungen.work

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.ItungEnApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Notifikasi catat cepat yang diusap (delete intent) dipasang lagi hanya selama toggle aktif dan izin ada.
 * Izin notifikasi tidak bisa diberikan lewat shell di ColorOS, jadi keputusan "pasang lagi" diuji dengan izin yang
 * diganti; jalur broadcast sungguhan ke receiver diuji untuk toggle mati (tidak boleh muncul apa pun).
 */
@RunWith(AndroidJUnit4::class)
class QuickIncomeDismissTest {
    private val app = ApplicationProvider.getApplicationContext<ItungEnApp>()

    @After
    fun tearDown() {
        app.container.quickIncomeEnabled = false
        QuickIncome.cancel(app)
    }

    @Test
    fun toggleOn_reshows() = runBlocking<Unit> {
        app.container.quickIncomeEnabled = true
        assertEquals(true, QuickIncome.reshowAfterDismiss(app, permitted = { true }))
    }

    @Test
    fun toggleOff_doesNotReshow() = runBlocking<Unit> {
        app.container.quickIncomeEnabled = false
        assertEquals(false, QuickIncome.reshowAfterDismiss(app, permitted = { true }))
    }

    @Test
    fun permissionRevoked_doesNotReshow() = runBlocking<Unit> {
        app.container.quickIncomeEnabled = true
        assertEquals(false, QuickIncome.reshowAfterDismiss(app, permitted = { false }))
    }

    @Test
    fun swipeBroadcast_toggleOff_staysGone() = runBlocking<Unit> {
        app.container.quickIncomeEnabled = false
        app.sendBroadcast(Intent(app, QuickIncomeReceiver::class.java).setAction(QuickIncomeReceiver.ACTION_DISMISSED))
        delay(3_000)
        val active = app.getSystemService(NotificationManager::class.java).activeNotifications
        assertTrue(active.none { it.id == QuickIncome.NOTIFICATION_ID })
    }
}
