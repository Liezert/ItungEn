package com.liezert.itungen.data.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.liezert.itungen.data.sync.budgetPlanFromDoc
import com.liezert.itungen.data.sync.toDoc
import com.liezert.itungen.domain.usecase.BudgetPeriod
import com.liezert.itungen.domain.usecase.BudgetPlan
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate

/**
 * Pengaturan milik pengguna (bukan milik HP): rencana anggaran, batas per kategori, dan hari tidak jajan.
 * Ikut cadangan JSON dan tersinkron ke dokumen
 * `users/{uid}/settings/main`. Bentrok antar-HP: perubahan terakhir menang menurut [updatedAt] (jam HP).
 * Disimpan di SharedPreferences dengan kunci lama, jadi rencana dari versi sebelumnya tetap terbaca.
 */
class SyncedSettings(private val prefs: SharedPreferences, private val clock: Clock) {
    private val _plan = MutableStateFlow(readPlan())

    /** Rencana Jatah hari ini (Pengaturan → Anggaran); null = belum diatur. */
    val plan: StateFlow<BudgetPlan?> = _plan.asStateFlow()

    private val _limits = MutableStateFlow(readLimits())

    /** Batas pengeluaran per bulan per kategori: uuid kategori → nominal. */
    val categoryLimits: StateFlow<Map<String, Long>> = _limits.asStateFlow()

    private val _noSpendDays = MutableStateFlow(readNoSpendDays())

    /** Hari yang ditandai "tidak jajan" (dari notifikasi pengingat atau kalender Insight). */
    val noSpendDays: StateFlow<Set<LocalDate>> = _noSpendDays.asStateFlow()

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

    /** [limit] null atau ≤ 0 = hapus batas kategori itu. */
    fun setCategoryLimit(categoryUuid: String, limit: Long?) {
        writeLimits(if (limit == null || limit <= 0) _limits.value - categoryUuid else _limits.value + (categoryUuid to limit))
        markChanged()
    }

    fun setNoSpendDay(day: LocalDate, marked: Boolean) {
        val days = if (marked) _noSpendDays.value + day else _noSpendDays.value - day
        if (days == _noSpendDays.value) return
        writeNoSpendDays(days)
        markChanged()
    }

    /** Isi dokumen sinkron dan bagian `settings` cadangan JSON. */
    fun toDoc(): Map<String, Any?> = mapOf(
        "budgetPlan" to _plan.value?.toDoc(),
        "categoryLimits" to _limits.value,
        // Hanya tahun terakhir: cukup untuk kalender & streak, dokumen tidak tumbuh tanpa batas.
        "noSpendDays" to _noSpendDays.value.map { it.toEpochDay() }.sorted().takeLast(MaxNoSpendDays),
        "updatedAt" to updatedAt,
    )

    /**
     * Menerapkan dokumen dari server yang lebih baru. Rencana dan batas: milik server. Hari tidak jajan: gabungan
     * keduanya (menandai di dua HP tidak saling menghapus); bila gabungan lebih banyak, perlu diunggah lagi.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyRemote(doc: Map<String, Any?>) {
        writePlan((doc["budgetPlan"] as Map<String, Any?>?)?.let(::budgetPlanFromDoc))
        writeLimits((doc["categoryLimits"] as Map<String, Any?>?).orEmpty().mapValues { (it.value as Number).toLong() })
        val remoteDays = (doc["noSpendDays"] as List<Any?>?).orEmpty().map { LocalDate.ofEpochDay((it as Number).toLong()) }.toSet()
        val merged = _noSpendDays.value + remoteDays
        writeNoSpendDays(merged)
        val remoteAt = (doc["updatedAt"] as Number?)?.toLong() ?: 0
        if (merged == remoteDays) setState(updatedAt = remoteAt, dirty = false) else markChanged()
    }

    /** Cadangan JSON: batas hanya diisi bila HP ini belum punya, hari tidak jajan digabung. */
    @Suppress("UNCHECKED_CAST")
    fun restoreBackup(doc: Map<String, Any?>) {
        val limits = (doc["categoryLimits"] as Map<String, Any?>?).orEmpty().mapValues { (it.value as Number).toLong() }
        val days = (doc["noSpendDays"] as List<Any?>?).orEmpty().map { LocalDate.ofEpochDay((it as Number).toLong()) }
        var changed = false
        if (_limits.value.isEmpty() && limits.isNotEmpty()) {
            writeLimits(limits)
            changed = true
        }
        if (!_noSpendDays.value.containsAll(days)) {
            writeNoSpendDays(_noSpendDays.value + days)
            changed = true
        }
        if (changed) markChanged()
    }

    /** Unggahan untuk perubahan pada [at] selesai; bila ada perubahan baru selama mengunggah, tetap perlu diunggah. */
    fun markPushed(at: Long) {
        if (updatedAt == at) setState(updatedAt = at, dirty = false)
    }

    /** Keluar akun: pengaturan milik akun ikut dihapus dari HP (kembali saat masuk lagi). */
    fun clear() {
        writePlan(null)
        writeLimits(emptyMap())
        writeNoSpendDays(emptySet())
        setState(updatedAt = 0, dirty = false)
    }

    private fun markChanged() = setState(updatedAt = clock.millis(), dirty = true)

    private fun writeLimits(limits: Map<String, Long>) {
        prefs.edit { putString(Limits, JSONObject(limits).toString()) }
        _limits.value = limits
    }

    private fun readLimits(): Map<String, Long> {
        val json = JSONObject(prefs.getString(Limits, null) ?: return emptyMap())
        return json.keys().asSequence().associateWith { json.getLong(it) }
    }

    private fun writeNoSpendDays(days: Set<LocalDate>) {
        prefs.edit { putStringSet(NoSpendDays, days.mapTo(HashSet()) { it.toString() }) }
        _noSpendDays.value = days
    }

    private fun readNoSpendDays(): Set<LocalDate> {
        val days = prefs.getStringSet(NoSpendDays, emptySet()).orEmpty().mapTo(HashSet(), LocalDate::parse)
        // Versi sebelumnya hanya menyimpan satu hari ("no_spend_day"): pindahkan ke himpunan.
        prefs.getString("no_spend_day", null)?.let { legacy ->
            days += LocalDate.parse(legacy)
            prefs.edit {
                putStringSet(NoSpendDays, days.mapTo(HashSet()) { it.toString() })
                remove("no_spend_day")
            }
        }
        return days
    }

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
        const val Limits = "category_limits"
        const val NoSpendDays = "no_spend_days"
        const val MaxNoSpendDays = 400
        const val UpdatedAt = "settings_updated_at"
        val PlanKeys = listOf(
            "budget_income", "budget_start_day", "budget_bills", "budget_savings",
            "budget_period", "budget_started_on", "budget_next_income_on",
        )
    }
}
