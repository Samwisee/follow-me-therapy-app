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
import au.com.followmetherapy.design.components.FmtAppBar
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
    Today("today", 0), Activities("activities", 1), GrownUps("grown-ups", 2),
    Morning("morning", null), Calm("calm", null), CalmNow("calm-now", null),
    WeekSetup("week-setup", null), Week("week", null), DesignSystem("design-system", null),
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
class Nav(start: Route, private val onChange: (String) -> Unit) {
    var route by mutableStateOf(start)
        private set
    fun go(to: Route) { route = to; onChange(to.slug) }
    fun tab(i: Int) = go(Route(Area.entries.first { it.tab == i }))
    fun home() = go(Route(Area.Today))
}

private val tabs = listOf(
    FmtTab("Today", FmtIcons.sunrise),
    FmtTab("Activities", FmtIcons.blocks),
    FmtTab("For grown-ups", FmtIcons.heart),
)

@Composable
fun App(platform: Platform = MemoryPlatform, startAt: String = "", onNavigate: (String) -> Unit = {}) {
    val store = remember { AppStore(platform) }
    val nav = remember { Nav(Route.parse(startAt), onNavigate) }
    FmtTheme {
        AnimatedContent(
            targetState = nav.route,
            modifier = Modifier.fillMaxSize().background(FmtColors.paper),
            transitionSpec = { transitionFor(initialState, targetState) },
            contentKey = { if (it.area.tab != null) "tabs" else "${it.area}/${it.step}" },
        ) { r ->
            if (r.area.tab != null) {
                TabShell(nav, r.area.tab) { t ->
                    when (t) {
                        1 -> ActivitiesTab(store, nav)
                        2 -> GrownUpsTab(store, nav)
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
                    Area.DesignSystem -> DesignSystemScreen(nav)
                    else -> Unit
                }
            }
        }
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

/** The three tabs share a lilac app bar, the wave, a scrolling column, and the tab bar. */
@Composable
private fun TabShell(nav: Nav, tab: Int, content: @Composable ColumnScope.(Int) -> Unit) {
    val m = FmtTheme.metrics
    Column(Modifier.fillMaxSize().background(FmtColors.paper)) {
        FmtAppBar(title = tabs[tab].label)
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
                        Modifier.widthIn(max = m.content).fillMaxWidth().padding(horizontal = m.gutter).padding(top = FmtLayout.wave.max.dp, bottom = m.stack),
                        verticalArrangement = Arrangement.spacedBy(m.stack),
                    ) { content(t) }
                }
            }
            // The wave floats over the content, so it scrolls up behind the lilac curve.
            FmtWave(FmtColors.purpleTint, Modifier.align(Alignment.TopCenter), down = true)
        }
        FmtTabBar(tabs, tab, nav::tab)
    }
}
