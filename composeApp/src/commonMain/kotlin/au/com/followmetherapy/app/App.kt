package au.com.followmetherapy.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import au.com.followmetherapy.design.tokens.FmtSpace
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import au.com.followmetherapy.design.components.FmtAppBar
import au.com.followmetherapy.design.components.FmtIconButton
import au.com.followmetherapy.design.components.FmtAppChrome
import au.com.followmetherapy.design.components.FmtActionBarHost
import au.com.followmetherapy.design.components.LocalFmtSafeBottom
import au.com.followmetherapy.design.components.LocalFmtReducedMotion
import au.com.followmetherapy.design.tokens.FmtBarTokens
import au.com.followmetherapy.design.components.FmtActionHost
import au.com.followmetherapy.design.components.FmtActionSlot
import au.com.followmetherapy.design.components.LocalFmtActionSlot
import androidx.compose.runtime.CompositionLocalProvider
import au.com.followmetherapy.design.components.FmtTab
import au.com.followmetherapy.design.components.FmtTabBar
import au.com.followmetherapy.design.components.FmtWave
import au.com.followmetherapy.design.icons.FmtIcons
import au.com.followmetherapy.design.theme.FmtTheme
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtMotion
import au.com.followmetherapy.design.tokens.FmtLayout
import androidx.compose.ui.unit.dp

/** Where the app is: an area, and for flows the step within it. */
enum class Area(val slug: String, val tab: Int?) {
    Today("today", 0), Plan("plan", 1), Activities("activities", 2), GrownUps("grown-ups", null),
    Morning("morning", null), Calm("calm", null), CalmNow("calm-now", null),
    WeekSetup("week-setup", null), Week("week", null), DesignSystem("design-system", null), Install("install", null),
    PlanImport("plan-import", null), Planner("planner", null), Task("task", null),
}

data class Route(val area: Area, val step: Int = 0) {
    /** Tabs sit at depth 0; flows are deeper, and later steps deeper still. */
    val depth: Float get() = if (area.tab != null) 0f else 1f + step * 0.01f
    companion object {
        fun parse(slug: String): Route {
            val parts = slug.split("/")
            val area = Area.entries.firstOrNull { it.slug == parts[0] } ?: Area.Today
            return Route(area, parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
    }
    val slug: String get() = if (area == Area.Today) "" else if (step == 0) area.slug else "${area.slug}/$step"
}

/** Moving around: forward slides in from the right, back from the left; tabs cross-fade. */
class Nav(start: Route, private val onChange: (String) -> Unit, private val homeRoute: Route = Route(Area.Today)) {
    var route by mutableStateOf(start)
        private set
    fun go(to: Route) { route = to; onChange(to.slug) }
    fun tab(i: Int) = go(Route(Area.entries.first { it.tab == i }))
    /** Today in the family app; the families list in Simone's planner. */
    fun home() = go(homeRoute)
}

private val tabs = listOf(
    FmtTab("Today", FmtIcons.sunrise),
    FmtTab("Plan", FmtIcons.calendar),
    FmtTab("Activities", FmtIcons.blocks),
)

@Composable
fun App(platform: Platform = MemoryPlatform, startAt: String = "", onNavigate: (String) -> Unit = {}) {
    val store = remember { AppStore(platform) }
    // A plan link (#plan=…) opens on the plan, waiting to be added.
    val inbox = remember { PlanInbox().apply { if (startAt.startsWith("plan=")) plan = Plan.fromLink(startAt) } }
    val session = remember { PlannerSession() }
    val planner = remember { platform.plannerEntry() }
    val fromLink = startAt.startsWith("plan=")
    // In a phone's or tablet's browser the app opens on its install page first
    // (it works best from the Home Screen); installed, or once skipped, it opens on Today.
    val nav = remember {
        val welcome = !planner && startAt.isEmpty() && !platform.installed() && platform.device() != Device.Computer && !store.installSkipped
        Nav(if (fromLink) Route(Area.PlanImport) else if (welcome) Route(Area.Install, WELCOME) else Route.parse(startAt), onNavigate,
            homeRoute = if (planner) Route(Area.Planner) else Route(Area.Today))
    }
    // Take the plan's code out of the address once it's read.
    LaunchedEffect(Unit) { if (fromLink) onNavigate(nav.route.slug) }
    val actions = remember { FmtActionHost() }
    val safeBottom = remember { platform.safeBottom().dp }
    val reduced = remember { platform.reducedMotion() }
    FmtTheme {
      CompositionLocalProvider(LocalFmtSafeBottom provides safeBottom, LocalFmtReducedMotion provides reduced) {
        // Opening: the page's splash (the logo on lilac) fades as the app fades and rises in.
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            platform.ready()
            enter.animateTo(1f, tween(FmtMotion.durSlow, easing = FmtMotion.easeOut))
        }
        val rise = with(LocalDensity.current) { FmtSpace.s4.toPx() }
        // One app bar and wave above every screen: they stay put while the
        // screens slide beneath, and only their parts animate.
        val bar = barFor(nav.route, store, nav, planner)
        Column(Modifier.fillMaxSize().graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * rise }.background(FmtColors.paper)) {
            FmtAppBar(
                bar.title, onBack = bar.back, brand = bar.brand, progress = bar.progress, progressLabel = bar.label,
                trailing = if (bar.menu) { { FmtIconButton(FmtIcons.menu, "Menu: for grown-ups", { nav.go(Route(Area.GrownUps)) }) } } else null,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = nav.route,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { transitionFor(initialState, targetState) },
                    contentKey = { if (it.area.tab != null) "tabs" else "${it.area}/${it.step}" },
                ) { r ->
                    val slot = remember(r) { FmtActionSlot(actions, r) }
                    CompositionLocalProvider(LocalFmtActionSlot provides slot) {
                    if (r.area.tab != null) {
                        TabShell(nav, r.area.tab) { t ->
                            when (t) {
                                1 -> PlanTab(store, nav, platform, inbox)
                                2 -> ActivitiesTab(store, nav)
                                else -> TodayTab(store, nav)
                            }
                        }
                    } else {
                        when (r.area) {
                            Area.Morning -> MorningFlow(store, nav, r.step)
                            Area.Calm -> CalmFlow(store, nav, r.step)
                            Area.CalmNow -> CalmNowFlow(store, nav, r.step)
                            Area.WeekSetup -> WeekSetupFlow(store, nav)
                            Area.Week -> WeekFlow(store, nav)
                            Area.Task -> TaskScreen(nav, r.step)
                            Area.DesignSystem -> DesignSystemScreen(nav)
                            Area.Install -> InstallScreen(platform, nav, store, welcome = r.step == WELCOME)
                            Area.GrownUps -> GrownUpsScreen(store, nav, platform)
                            Area.PlanImport -> PlanImportScreen(store, nav, platform, inbox)
                            Area.Planner -> PlannerLock(store, platform) { PlannerScreen(store, nav, platform, r.step, inbox, session) }
                            else -> Unit
                        }
                    }
                    }
                }
                // The flow's buttons stay on screen between steps, like the app bar.
                FmtActionBarHost(actions, nav.route, visible = nav.route.area.tab == null)
                FmtWave(FmtBarTokens.fill, Modifier.align(Alignment.TopCenter), down = true, height = FmtAppChrome.wave, stroke = FmtBarTokens.line)
            }
        }
      }
    }
}

/** What the app bar shows for a place in the app. */
private data class Bar(val title: String, val back: (() -> Unit)? = null, val progress: Float? = null, val label: String? = null, val brand: Boolean = false, val menu: Boolean = false)

private fun barFor(r: Route, store: AppStore, nav: Nav, planner: Boolean): Bar {
    val home = nav::home
    return when (r.area) {
        // The three tabs share one bar with the lockup: the tab bar says where you are.
        // The tabs share the lockup bar, with the menu (For grown-ups) at its end.
        Area.Today, Area.Plan, Area.Activities -> Bar(tabs[r.area.tab ?: 0].label, brand = true, menu = true)
        Area.GrownUps -> Bar("For grown-ups", home)
        Area.PlanImport -> Bar("A plan from Simone", home)
        Area.Planner -> when (r.step) {
            PLANNER_EDIT -> Bar("Plan", { nav.go(Route(Area.Planner)) })
            PLANNER_SEND -> Bar("Send the plan", { nav.go(Route(Area.Planner, PLANNER_EDIT)) })
            PLANNER_REQUEST -> Bar("Request a change", { nav.go(Route(Area.Planner)) })
            else -> if (planner) Bar("Planner", brand = true) else Bar("Planner", home)
        }
        Area.Morning -> {
            val total = store.routineSteps.size
            val last = total + 2
            when (r.step) {
                0 -> Bar("Morning routine", home)
                in 1..total -> Bar("Morning routine", home, r.step / last.toFloat(), "${r.step} of $total")
                else -> Bar("Morning routine", home, r.step.coerceAtMost(last) / last.toFloat())
            }
        }
        Area.Calm -> Bar("Calm-down plan", home, if (r.step in 1..3) r.step / 4f else null, if (r.step in 1..3) "${r.step} of 4" else null)
        Area.CalmNow -> {
            val n = store.calmChosen.size + if (store.calmIdea.isNotBlank()) 1 else 0
            if (n == 0 || r.step >= n) Bar("Calm-down plan", home)
            else Bar("Calm-down plan", home, (r.step + 1) / n.toFloat(), "${r.step + 1} of $n")
        }
        Area.WeekSetup, Area.Week -> Bar("Practice week", home)
        Area.Task -> Bar("How to", { nav.tab(2) })
        Area.DesignSystem -> Bar("Design system", { nav.go(Route(Area.GrownUps)) })
        Area.Install -> if (r.step == WELCOME) Bar("Follow Me Therapy", brand = true) else Bar("Add to your Home Screen", { nav.go(Route(Area.GrownUps)) })
    }
}

private fun transitionFor(from: Route, to: Route): ContentTransform {
    val sameArea = from.area == to.area
    val forward = if (sameArea) to.step > from.step else to.depth > from.depth
    val slideMs = FmtMotion.durSlower
    return if (from.area.tab != null && to.area.tab != null) {
        fadeIn(tween(FmtMotion.durBase, easing = FmtMotion.easeOut)) togetherWith fadeOut(tween(FmtMotion.durFast))
    } else {
        val dir = if (forward) 1 else -1
        (slideInHorizontally(tween(slideMs, easing = FmtMotion.easeOut)) { w -> dir * w / 3 } + fadeIn(tween(slideMs, easing = FmtMotion.easeOut))) togetherWith
            (slideOutHorizontally(tween(slideMs, easing = FmtMotion.easeStandard)) { w -> -dir * w / 4 } + fadeOut(tween(FmtMotion.durBase)))
    }
}

/**
 * The three tabs share a scrolling column and the lilac tab bar, whose crest
 * marks the chosen tab (the app bar and its wave sit above, in [App]).
 */
@Composable
private fun TabShell(nav: Nav, tab: Int, content: @Composable ColumnScope.(Int) -> Unit) {
    val m = FmtTheme.metrics
    Column(Modifier.fillMaxSize().background(FmtColors.paper)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // A new tab's content fades and rises in; the bars stay put.
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(FmtMotion.durSlow, easing = FmtMotion.easeOut)) +
                        slideInVertically(tween(FmtMotion.durSlow, easing = FmtMotion.easeOut)) { h -> h / 30 }) togetherWith
                        fadeOut(tween(FmtMotion.durFast))
                },
            ) { t ->
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter).padding(top = FmtAppChrome.wave + m.stack, bottom = FmtAppChrome.crest + m.stack),
                        verticalArrangement = Arrangement.spacedBy(m.stack),
                    ) { content(t) }
                }
            }
        }
        FmtTabBar(tabs, tab, nav::tab)
    }
}
