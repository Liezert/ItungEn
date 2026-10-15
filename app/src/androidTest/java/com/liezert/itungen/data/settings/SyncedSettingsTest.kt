package com.liezert.itungen.data.settings

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.sync.toDoc
import com.liezert.itungen.domain.usecase.BudgetPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SyncedSettingsTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("settings-test", Context.MODE_PRIVATE)
    private val plan = BudgetPlan(income = 3_000_000, cycleStartDay = 25)

    @Before
    fun setUp() = prefs.edit { clear() }

    private fun settingsAt(millis: Long) = SyncedSettings(prefs, Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC))

    @Test
    fun localChange_isPending_untilPushed() {
        val settings = settingsAt(1_000)
        settings.savePlan(plan)
        assertTrue(settings.dirty.value)
        settings.markPushed(1_000)
        assertFalse(settings.dirty.value)
    }

    @Test
    fun changeDuringUpload_staysPending() {
        settingsAt(1_000).savePlan(plan)
        val settings = settingsAt(2_000)
        val uploading = settings.updatedAt // 1.000
        settings.savePlan(plan.copy(income = 4_000_000)) // berubah lagi selama unggah
        settings.markPushed(uploading)
        assertTrue(settings.dirty.value)
    }

    @Test
    fun remote_replacesLocal_withoutReupload() {
        val settings = settingsAt(1_000)
        settings.applyRemote(mapOf("budgetPlan" to plan.toDoc(), "updatedAt" to 5_000L))
        assertEquals(plan, settings.plan.value)
        assertEquals(5_000L, settings.updatedAt)
        assertFalse(settings.dirty.value)
    }

    /** Rencana yang disimpan versi lama (tanpa penanda sinkron) ikut terunggah saat pengguna masuk. */
    @Test
    fun legacyPlan_isMarkedForUpload() {
        prefs.edit {
            putLong("budget_income", 2_000_000)
            putInt("budget_start_day", 1)
        }
        val settings = settingsAt(9_000)
        assertEquals(2_000_000L, settings.plan.value?.income)
        assertTrue(settings.dirty.value)
    }

    @Test
    fun clear_removesPlan() {
        val settings = settingsAt(1_000)
        settings.savePlan(plan)
        settings.clear()
        assertEquals(null, settingsAt(1_000).plan.value)
        assertFalse(settings.dirty.value)
    }
}
