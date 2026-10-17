package com.liezert.itungen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.liezert.glass.components.GlassCard
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.domain.usecase.LimitLevel
import com.liezert.itungen.domain.usecase.LimitWarning
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.ui.addtransaction.AddTransactionContent
import com.liezert.itungen.ui.addtransaction.AddTransactionUiState
import com.liezert.itungen.ui.components.rememberBackgroundBackdrop
import com.liezert.itungen.ui.home.HomeContent
import com.liezert.itungen.ui.home.HomeUiState
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.ItungEnTheme
import com.liezert.itungen.ui.transactions.TransactionsContent
import com.liezert.itungen.ui.transactions.TransactionsUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

/** Bug dari uji di HP (docs/PROMPT_PERBAIKAN_BUG.md). Dijalankan di layar HP sungguhan (360 × 800 dp). */
@RunWith(AndroidJUnit4::class)
class BugFixUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 10, 17)
    private val expense = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Pendidikan", "Lainnya")
        .mapIndexed { i, name -> Category(i + 1L, name, "other", 0, CategoryType.EXPENSE) }

    @After
    fun resetTheme() {
        Ink.dark = true
    }

    private fun addScreen(state: AddTransactionUiState = AddTransactionUiState(categories = expense, categoryId = 1, date = today, today = today)) {
        val host = GlassOverlayHost()
        // Seperti MainActivity: edge-to-edge, jadi status bar dan navigasi HP ikut memakan tinggi layar.
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides host) {
                    Box(Modifier.fillMaxSize()) {
                        AddTransactionContent(state, onClose = {}, onType = {}, onCategory = {}, onDate = {}, onSave = { _, _ -> })
                        host.Content()
                    }
                }
            }
        }
    }

    /** A1: kotak kategori dulu tertutup keypad (di luar layar) tanpa tanda bisa di-scroll. */
    @Test
    fun addTransaction_categoryIsVisibleWithoutScrolling() {
        addScreen()
        compose.onNodeWithText("Makanan").assertIsDisplayed()
        // assertIsDisplayed sudah lolos bila seiris tombol terlihat; tombol kategori harus terlihat utuh.
        val category = compose.onNodeWithText("Makanan").fetchSemanticsNode()
        assertEquals("tinggi tombol kategori yang terlihat", category.size.height.toFloat(), category.boundsInRoot.height)
        compose.onNodeWithContentDescription("Sama dengan, hitung hasil").assertIsDisplayed()
    }

    /** A6: "Nominal terlalu besar." dulu baru muncul setelah Simpan, tidak hilang setelah C, dan menggeser keypad. */
    @Test
    fun addTransaction_amountErrorIsLive_andKeypadStaysPut() {
        addScreen()
        val key = { d: String -> compose.onNodeWithContentDescription(d).performClick() }
        val keypadTop = { compose.onNodeWithContentDescription("1").fetchSemanticsNode().boundsInRoot.top }
        val before = keypadTop()
        repeat(12) { key("9") }
        key("kali")
        key("9")
        key("9")
        compose.onNodeWithText("Nominal terlalu besar.").assertIsDisplayed()
        assertEquals(before, keypadTop())
        key("Kosongkan nominal")
        compose.onAllNodesWithText("Nominal terlalu besar.").assertCountEquals(0)
    }

    /** A7: nama bulan dan hari di pemilih tanggal dulu mengikuti bahasa HP (Inggris), bukan Indonesia. */
    @Test
    fun datePicker_isIndonesian() {
        addScreen()
        compose.onNodeWithText("Sabtu, 17 Okt 2026", substring = true).performClick()
        compose.onAllNodesWithText("Oktober 2026", useUnmergedTree = true).onFirst().assertExists()
        compose.onAllNodesWithText("October 2026", useUnmergedTree = true).assertCountEquals(0)
    }

    /** A5: ketukan di sela tombol navbar dulu tembus ke konten di belakangnya. */
    @Test
    fun navbarGap_doesNotTapThrough() {
        var behind = 0
        compose.setContent {
            Box(Modifier.size(300.dp, 200.dp)) {
                Box(Modifier.fillMaxSize().clickable { behind++ })
                LiquidBottomTabs(
                    selectedTabIndex = { 0 },
                    onTabSelected = {},
                    backdrop = emptyBackdrop(),
                    tabsCount = 3,
                    modifier = Modifier.width(300.dp).align(Alignment.BottomCenter).testTag("tabs"),
                ) {
                    LiquidBottomTab({}) { Text("A") }
                    LiquidBottomTab({}) { Text("B") }
                    LiquidBottomTab({}) { Text("C") }
                }
            }
        }
        // Batas tab ke-2 dan ke-3, dekat tepi atas: di luar kapsul tab, tetapi masih di dalam panel navbar.
        compose.onNodeWithTag("tabs").performTouchInput { click(Offset((4 + 292f * 2 / 3).dp.toPx(), 10.dp.toPx())) }
        compose.waitForIdle()
        assertEquals(0, behind)
    }

    /** B2: "Sembunyikan saldo" dulu hanya menyamarkan kartu saldo; jatah, batas, target, dan riwayat tetap terlihat. */
    @Test
    fun hiddenBalance_hidesEveryAmountOnHome() {
        val food = expense[0]
        val plan = BudgetPlan(income = 3_000_000, cycleStartDay = 25, fixedBills = 500_000, savingsTarget = 300_000)
        val recent = listOf(Transaction(1, 1, food, TransactionType.EXPENSE, 55_000, "Makan siang", today))
        val state = HomeUiState(
            today = today,
            zone = ZoneOffset.UTC,
            loading = false,
            balance = 475_000,
            budget = computeBudget(plan, recent, 0, today),
            recent = recent,
            goal = Goal("Laptop baru", saved = 1_000_000, target = 2_000_000),
            limitWarnings = listOf(LimitWarning(CategorySpend(food, 1, 327_000), 30_000, LimitLevel.OVER)),
        )
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop()) {
                    HomeContent(state, onHistory = {}, onSeeAll = {}, balanceHidden = true)
                }
            }
        }
        val amount = Regex("Rp[\\s\\u00A0]?\\d")
        val showsAmount = SemanticsMatcher("menampilkan nominal") { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { amount.containsMatchIn(it.text) }
        }
        compose.onAllNodes(showsAmount, useUnmergedTree = true).assertCountEquals(0)
    }

    /** B5: setelah pencarian dikosongkan, daftar dulu tetap di posisi hasil terakhir (September), bukan hari ini. */
    @Test
    fun clearingSearch_returnsListToToday() {
        val food = expense[0]
        val rows = (0 until 40).map { Transaction(100L - it, 1, food, TransactionType.EXPENSE, 10_000, "Catatan $it", today.minusDays(it.toLong())) }
        var state by mutableStateOf(
            TransactionsUiState(today, ZoneOffset.UTC, loading = false, transactions = rows.subList(20, 30), query = "hiburan", hasAny = true)
        )
        compose.setContent {
            ItungEnTheme {
                CompositionLocalProvider(LocalGlassBackdrop provides emptyBackdrop(), LocalGlassOverlayHost provides GlassOverlayHost()) {
                    TransactionsContent(state, onBack = {})
                }
            }
        }
        compose.onNodeWithText("10 transaksi cocok", substring = true).assertExists()
        state = state.copy(transactions = rows, query = "")
        compose.onNodeWithText("Catatan 0").assertIsDisplayed()
    }

    /** B6: kartu kaca berisi tombol kaca dulu tertinggal satu tema setelah Mode gelap diganti. */
    @Test
    fun themeSwitch_repaintsEveryCard() {
        // Seperti Pengaturan: layar direkam ke backdrop (untuk navbar), kartu di dalam kolom yang bisa di-scroll.
        compose.setContent {
            ItungEnTheme {
                val screen = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize().layerBackdrop(screen)) {
                    CompositionLocalProvider(LocalGlassBackdrop provides rememberBackgroundBackdrop(false)) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            repeat(4) { GlassCard(Modifier.fillMaxWidth().size(120.dp)) { Text("Pengaturan $it") } }
                            GlassCard(Modifier.size(200.dp, 120.dp).testTag("plain")) { Text("Kartu") }
                            GlassCard(Modifier.size(200.dp, 120.dp).testTag("buttons")) {
                                LiquidButton({}) { Text("Ekspor") }
                                LiquidButton({}) { Text("Pulihkan") }
                            }
                        }
                    }
                }
            }
        }
        val pixel = { tag: String -> compose.onNodeWithTag(tag).captureToImage().toPixelMap()[100, 12] }
        compose.waitForIdle()
        val dark = pixel("buttons")
        val darkPlain = pixel("plain")
        compose.runOnUiThread { Ink.dark = false }
        compose.waitForIdle()
        assertNotEquals("kartu biasa ikut berganti tema", darkPlain, pixel("plain"))
        // Dulu permukaan kartu berisi tombol tetap gelap (≈ 0,03) setelah pindah ke mode terang.
        assertNotEquals(dark, pixel("buttons"))
        assert(pixel("buttons").red > 0.8f) { "kartu berisi tombol masih gelap: ${pixel("buttons")}" }
    }
}
