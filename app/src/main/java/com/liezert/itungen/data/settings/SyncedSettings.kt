package com.liezert.itungen.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.liezert.itungen.data.sync.budgetPlanFromDoc
import com.liezert.itungen.data.sync.toDoc
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate

/**
 * Pengaturan milik pengguna (bukan milik HP): ikut cadangan JSON dan tersinkron ke dokumen
 * `users/{uid}/settings/main`. Bentrok antar-HP: perubahan terakhir menang menurut [updatedAt] (jam HP).
 * Disimpan di SharedPreferences dengan kunci lama, jadi rencana dari versi sebelumnya tetap terbaca.
 */
class SyncedSettings(private val prefs: SharedPreferences, private val clock: Clock) {
    private val _plan = MutableStateFlow(readPlan())

    /** Rencana Jatah hari ini (Pengaturan → Anggaran); null = belum diatur. */
    val plan: StateFlow<BudgetPlan?> = _plan.asStateFlow()

    private val _dirty = MutableStateFlow(prefs.getBoolean(Dirty, false))

    /** Ada perubahan di HP ini yang belum terunggah. */
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    /** Waktu perubahan terakhir (epoch ms); 0 = belum pernah diubah. */
    val updatedAt: Long get() = prefs.getLong(UpdatedAt, 0)

    init {
        // Rencana dari versi sebelum sinkron pengaturan: tandai perlu diunggah agar ikut ke akun saat masuk.
        if (_plan.value != null && !prefs.contains(UpdatedAt)) markChanged()
    }

    fun savePlan(plan: BudgetPlan) {
        writePlan(plan)
        markChanged()
    }

    /** Isi dokumen sinkron dan bagian `settings` cadangan JSON. */
    fun toDoc(): Map<String, Any?> = mapOf(
        "budgetPlan" to _plan.value?.toDoc(),
        "updatedAt" to updatedAt,
    )

    /** Menerapkan dokumen dari server (yang lebih baru) tanpa menandainya perlu diunggah lagi. */
    fun applyRemote(doc: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        writePlan((doc["budgetPlan"] as Map<String, Any?>?)?.let(::budgetPlanFromDoc))
        setState(updatedAt = (doc["updatedAt"] as Number?)?.toLong() ?: 0, dirty = false)
    }

    /** Unggahan untuk perubahan pada [at] selesai; bila ada perubahan baru selama mengunggah, tetap perlu diunggah. */
    fun markPushed(at: Long) {
        if (updatedAt == at) setState(updatedAt = at, dirty = false)
    }

    /** Keluar akun: pengaturan milik akun ikut dihapus dari HP (kembali saat masuk lagi). */
    fun clear() {
        writePlan(null)
        setState(updatedAt = 0, dirty = false)
    }

    private fun markChanged() = setState(updatedAt = clock.millis(), dirty = true)

    private fun setState(updatedAt: Long, dirty: Boolean) {
        prefs.edit {
            putLong(UpdatedAt, updatedAt)
            putBoolean(Dirty, dirty)
        }
        _dirty.value = dirty
    }

    private fun writePlan(plan: BudgetPlan?) {
        prefs.edit {
            if (plan == null) {
                PlanKeys.forEach(::remove)
            } else {
                putLong("budget_income", plan.income)
                putInt("budget_start_day", plan.cycleStartDay)
                putLong("budget_bills", plan.fixedBills)
                putLong("budget_savings", plan.savingsTarget)
                putString("budget_period", plan.period.name)
                putString("budget_started_on", plan.startedOn?.toString())
                putString("budget_next_income_on", plan.nextIncomeOn?.toString())
            }
        }
        _plan.value = plan
    }

    private fun readPlan(): BudgetPlan? =
        if (!prefs.contains("budget_income")) null
        else BudgetPlan(
            income = prefs.getLong("budget_income", 0),
            cycleStartDay = prefs.getInt("budget_start_day", 1),
            fixedBills = prefs.getLong("budget_bills", 0),
            savingsTarget = prefs.getLong("budget_savings", 0),
            period = BudgetPeriod.valueOf(prefs.getString("budget_period", null) ?: BudgetPeriod.MONTHLY.name),
            startedOn = prefs.getString("budget_started_on", null)?.let(LocalDate::parse),
            nextIncomeOn = prefs.getString("budget_next_income_on", null)?.let(LocalDate::parse),
        )

    private companion object {
        const val Dirty = "settings_dirty"
        const val UpdatedAt = "settings_updated_at"
        val PlanKeys = listOf(
            "budget_income", "budget_start_day", "budget_bills", "budget_savings",
            "budget_period", "budget_started_on", "budget_next_income_on",
        )
    }
}
