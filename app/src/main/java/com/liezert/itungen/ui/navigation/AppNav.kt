package com.liezert.itungen.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liezert.glass.components.GlassOverlayHost
import com.liezert.glass.components.LiquidBottomTab
import com.liezert.glass.components.LiquidBottomTabs
import com.liezert.glass.components.LiquidButton
import com.liezert.glass.components.LocalGlassBackdrop
import com.liezert.glass.components.LocalGlassEconomy
import com.liezert.glass.components.LocalGlassOverlayHost
import com.liezert.glass.components.ProgressiveBlur
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.R
import com.liezert.itungen.ui.account.LoginScreen
import com.liezert.itungen.ui.addtransaction.AddTransactionViewModel
import com.liezert.itungen.ui.budget.BudgetScreen
import com.liezert.itungen.ui.addtransaction.AddTransactionScreen
import com.liezert.itungen.ui.components.AppBackground
import com.liezert.itungen.ui.components.rememberBackgroundBackdrop
import com.liezert.itungen.ui.goals.GoalsScreen
import com.liezert.itungen.ui.home.HomeScreen
import com.liezert.itungen.ui.insight.InsightScreen
import com.liezert.itungen.ui.profile.ProfileScreen
import com.liezert.itungen.ui.settings.SettingsScreen
import com.liezert.itungen.ui.theme.Ink
import com.liezert.itungen.ui.theme.SemiBold
import com.liezert.itungen.ui.theme.appText
import com.liezert.itungen.ui.transactions.TransactionsScreen
import androidx.compose.ui.text.style.TextOverflow

private object Routes {
    const val Home = "home"
    const val Insight = "insight"
    const val Goals = "goals"
    const val Profile = "profile"
    const val Transactions = "transactions"
    /** Tambah transaksi; dengan `edit=<id>` mengubah transaksi yang sudah ada. */
    const val Add = "add?edit={edit}"
    fun edit(id: Long) = "add?edit=$id"
    const val Login = "login"
    const val Settings = "settings"
    const val Budget = "budget"
    const val LoginEmail = "login-email"
}

/** Urutan sama dengan item navbar. */
private val TabRoutes = listOf(Routes.Home, Routes.Insight, Routes.Goals, Routes.Profile)

// Transisi ala iOS: push geser dari kanan dengan paralaks, modal naik dari bawah, tab cross-fade cepat.
// Spring teredam kritis (tanpa pantul) seperti UINavigationController. Nilai awal; disetel di HP.
private val PushRoutes = setOf(Routes.Transactions, Routes.Settings, Routes.Budget)
private val Slide = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow, IntOffset.VisibilityThreshold)
private val TabIn = fadeIn(tween(220, delayMillis = 60)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), 0.97f)
private val TabOut = fadeOut(tween(120))

private val NavBackStackEntry.route get() = destination.route

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition = when (targetState.route) {
    Routes.Add -> slideInVertically(Slide) { it }
    in PushRoutes -> slideInHorizontally(Slide) { it }
    else -> TabIn
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition = when (targetState.route) {
    Routes.Add -> ExitTransition.KeepUntilTransitionsFinished // layar di bawah modal tetap terlihat
    in PushRoutes -> slideOutHorizontally(Slide) { -it / 4 } + fadeOut(tween(300))
    else -> TabOut
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition = when (initialState.route) {
    Routes.Add -> EnterTransition.None
    in PushRoutes -> slideInHorizontally(Slide) { -it / 4 } + fadeIn(tween(300))
    else -> TabIn
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition = when (initialState.route) {
    Routes.Add -> slideOutVertically(Slide) { it }
    in PushRoutes -> slideOutHorizontally(Slide) { it }
    else -> TabOut
}

private fun NavHostController.switchTab(index: Int) = navigate(TabRoutes[index]) {
    popUpTo(Routes.Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private class Tab(val label: String, @param:DrawableRes val icon: Int, val accent: Color)

private val Tabs = listOf(
    Tab("Beranda", R.drawable.ic_nav_home, Ink.NavHome),
    Tab("Insight", R.drawable.ic_nav_category, Ink.NavInsight),
    Tab("Goals", R.drawable.ic_nav_goals, Ink.NavGoals),
    Tab("Profil", R.drawable.ic_nav_profile, Ink.NavProfile),
)

@Composable
fun AppNav(
    /** Bertambah = buka Tambah Transaksi (pintasan ikon / notifikasi pengingat). */
    addRequests: Int = 0,
) {
    val nav = rememberNavController()
    val container = (LocalContext.current.applicationContext as ItungEnApp).container
    // Layar Masuk hanya saat pertama kali (belum memilih masuk / tamu); dihitung sekali karena NavHost butuh nilai tetap.
    val start = remember { if (container.onboarded || container.account.user.value != null) Routes.Home else Routes.Login }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    // Kaca di dalam layar memakai latar polos (BackgroundBackdrop: tanpa rekaman/efek); navbar, FAB, sheet, dan
    // dialog membiaskan latar + isi layar yang direkam ke [screen]. Kaca tidak boleh merekam layer yang memuat dirinya.
    val vignette = route == Routes.Home || route == Routes.Transactions
    val background = rememberBackgroundBackdrop(vignette)
    val screen = rememberLayerBackdrop()
    val overlay = remember { GlassOverlayHost() }
    val economy = LocalGlassEconomy.current

    // Sub-layar menjaga tab induknya tetap aktif (DESIGN_SPEC §6.3).
    fun tabOf(r: String?) = when (r) {
        Routes.Home, Routes.Transactions -> 0
        Routes.Insight -> 1
        Routes.Goals -> 2
        Routes.Profile, Routes.Settings -> 3
        else -> -1
    }
    // Anggaran dibuka dari Beranda maupun Pengaturan: ikut tab layar yang membukanya.
    val tabIndex = tabOf(route).takeIf { it >= 0 } ?: tabOf(nav.previousBackStackEntry?.destination?.route).coerceAtLeast(0)
    val currentTab by rememberUpdatedState(tabIndex)
    val selectTab = { index: Int ->
        if (index != currentTab) nav.switchTab(index)
        // Tab yang sedang aktif diketuk dari sub-layarnya (mis. Profil saat di Pengaturan): kembali ke akar tab.
        else if (route != TabRoutes[index]) nav.popBackStack(TabRoutes[index], inclusive = false)
    }
    val openHistory = { nav.navigate(Routes.Transactions) { launchSingleTop = true } }
    LaunchedEffect(addRequests) {
        // Hanya setelah layar Masuk dilewati; sebelum itu pengguna memilih masuk/tamu dulu.
        if (addRequests > 0 && (container.onboarded || container.account.user.value != null)) {
            nav.navigate("add") { launchSingleTop = true }
        }
    }

    CompositionLocalProvider(LocalGlassOverlayHost provides overlay) {
        Box(Modifier.fillMaxSize().background(Ink.Black)) {
            // Mode hemat: tidak ada kaca yang membiaskan isi layar, jadi layar tidak perlu direkam ke backdrop.
            Box(Modifier.then(if (economy) Modifier else Modifier.layerBackdrop(screen)).fillMaxSize()) {
                AppBackground(vignette)
                CompositionLocalProvider(LocalGlassBackdrop provides background) {
                    NavHost(
                        nav,
                        startDestination = start,
                        enterTransition = { enter() },
                        exitTransition = { exit() },
                        popEnterTransition = { popEnter() },
                        popExitTransition = { popExit() },
                    ) {
                        composable(Routes.Login) {
                            LoginScreen(onDone = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } })
                        }
                        // Dibuka dari Pengaturan: selesai (atau kembali) berarti pulang ke Pengaturan.
                        composable(Routes.LoginEmail) {
                            LoginScreen(onDone = { nav.popBackStack() }, startWithEmail = true)
                        }
                        composable(Routes.Home) {
                            HomeScreen(
                                onHistory = openHistory,
                                onSeeAll = openHistory,
                                onOpenGoals = { selectTab(2) },
                                onOpenBudget = { nav.navigate(Routes.Budget) { launchSingleTop = true } },
                                onEdit = { nav.navigate(Routes.edit(it)) },
                                onOpenInsight = { selectTab(1) },
                            )
                        }
                        composable(Routes.Budget) { BudgetScreen(onBack = { nav.popBackStack() }) }
                        composable(Routes.Insight) { InsightScreen() }
                        composable(Routes.Goals) { GoalsScreen() }
                        composable(Routes.Profile) {
                            ProfileScreen(onOpenSettings = { nav.navigate(Routes.Settings) { launchSingleTop = true } })
                        }
                        composable(Routes.Settings) {
                            SettingsScreen(
                                onBack = { nav.popBackStack() },
                                onEmailSignIn = { nav.navigate(Routes.LoginEmail) { launchSingleTop = true } },
                                onOpenBudget = { nav.navigate(Routes.Budget) { launchSingleTop = true } },
                            )
                        }
                        composable(Routes.Transactions) {
                            TransactionsScreen(
                                onBack = { nav.popBackStack() },
                                onEdit = { nav.navigate(Routes.edit(it)) },
                                onOpenGoals = { selectTab(2) },
                            )
                        }
                        composable(
                            Routes.Add,
                            arguments = listOf(navArgument(AddTransactionViewModel.EditArg) { type = NavType.LongType; defaultValue = -1L }),
                        ) { AddTransactionScreen(onClose = { nav.popBackStack() }) }
                    }
                }
            }

            // Efek tepi scroll iOS 26: isi yang discroll ke bawah status bar memudar dalam blur, tidak menabrak jam/ikon.
            // Mode hemat: tanpa blur (isi tetap terlihat di bawah status bar).
            if (!economy) ProgressiveBlur(
                backdrop = screen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp),
                // ponytail: tint gelap, bukan abu-abu demo (#808080 α0.8) yang terlalu terang di atas latar hitam.
                tint = Ink.Black,
                tintIntensity = 0.35f,
            )

            CompositionLocalProvider(LocalGlassBackdrop provides screen) {
                // Navbar hanya di tab dan Semua Transaksi; layar Masuk dan Tambah Transaksi tampil penuh (DESIGN_SPEC §6.3).
                AnimatedVisibility(
                    visible = route in TabRoutes || route in PushRoutes,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(Slide) { it } + fadeIn(),
                    exit = slideOutVertically(Slide) { it } + fadeOut(),
                ) {
                    // FAB melayang di kanan atas navbar, seperti tombol tambah kontak di Telegram.
                    Column(
                        Modifier
                            .padding(horizontal = 19.5.dp)
                            .padding(bottom = 17.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        // Hanya di Beranda: di Goals/Profil tombol ini disangka "tambah goal", padahal mencatat transaksi.
                        AnimatedVisibility(
                            visible = route == Routes.Home,
                            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.3f) + fadeIn(tween(150)),
                            exit = scaleOut(spring(stiffness = Spring.StiffnessMedium), 0.3f) + fadeOut(tween(120)),
                        ) {
                            LiquidButton(
                                onClick = { nav.navigate("add") { launchSingleTop = true } },
                                modifier = Modifier.size(56.dp).semantics { contentDescription = "Tambah transaksi" },
                                tint = Ink.Blue,
                                contentPadding = 0.dp,
                            ) {
                                Image(painterResource(R.drawable.ic_add), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(Color.White))
                            }
                        }
                        LiquidBottomTabs(
                            selectedTabIndex = { currentTab },
                            onTabSelected = selectTab,
                            onTabReselected = selectTab,
                            backdrop = screen,
                            tabsCount = Tabs.size,
                            modifier = Modifier.fillMaxWidth(),
                            // Lapisan aksen hanya satu warna: warna tab yang sedang aktif.
                            accentColor = Tabs[tabIndex].accent.copy(alpha = 1f),
                        ) {
                            Tabs.forEachIndexed { index, tab ->
                                LiquidBottomTab({ selectTab(index) }, selected = index == tabIndex) {
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .paint(painterResource(tab.icon), colorFilter = ColorFilter.tint(Ink.White))
                                    )
                                    Text(tab.label, style = appText(11f, 13f, SemiBold, Ink.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                overlay.Content()
            }
        }
    }
}
