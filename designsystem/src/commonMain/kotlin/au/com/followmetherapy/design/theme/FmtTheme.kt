package au.com.followmetherapy.design.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.followmetherapy.design.resources.Res
import au.com.followmetherapy.design.resources.fredoka_400
import au.com.followmetherapy.design.resources.fredoka_500
import au.com.followmetherapy.design.resources.fredoka_600
import au.com.followmetherapy.design.resources.fredoka_700
import au.com.followmetherapy.design.resources.quicksand_500
import au.com.followmetherapy.design.resources.quicksand_600
import au.com.followmetherapy.design.resources.quicksand_700
import au.com.followmetherapy.design.tokens.FmtBorder
import au.com.followmetherapy.design.tokens.FmtColors
import au.com.followmetherapy.design.tokens.FmtFamily
import au.com.followmetherapy.design.tokens.FmtLayout
import au.com.followmetherapy.design.tokens.FmtLeading
import au.com.followmetherapy.design.tokens.FmtText
import au.com.followmetherapy.design.tokens.FmtTracking
import au.com.followmetherapy.design.tokens.FmtTypeToken
import au.com.followmetherapy.design.tokens.FmtTypography
import au.com.followmetherapy.design.tokens.FmtWeight
import au.com.followmetherapy.design.tokens.Fluid
import org.jetbrains.compose.resources.Font

/**
 * The semantic tokens resolved for the current window width. On screens they
 * are fluid (phone 360dp → laptop 1440dp), the same values the website uses,
 * so a component looks the same in the app as on the site at that width.
 */
@Immutable
data class FmtMetrics(
    val widthDp: Float,
    val textXs: TextUnit,
    val textSm: TextUnit,
    val textBase: TextUnit,
    val textMd: TextUnit,
    val textLg: TextUnit,
    val textXl: TextUnit,
    val textKid: TextUnit,
    val text2xl: TextUnit,
    val text3xl: TextUnit,
    val text4xl: TextUnit,
    val textDisplay: TextUnit,
    val gutter: Dp,
    val sectionSpace: Dp,
    val sectionTop: Dp,
    val stack: Dp,
    val groupGap: Dp,
    val cardPad: Dp,
    val gridGap: Dp,
    val content: Dp,
    val measure: Dp,
) {
    /** Where words outside containers start: card-pad + outline in, so they line up with words inside the header and notes. */
    val textInset: Dp get() = cardPad + FmtBorder.outline

    /** Corner of a container holding a radius-tile thing card-pad in from its edge, with an outline. */
    val containerRadius: Dp get() = au.com.followmetherapy.design.tokens.FmtRadius.tile + cardPad + FmtBorder.outline

    companion object {
        fun forWidth(widthDp: Float): FmtMetrics {
            fun t(f: Fluid) = f.at(widthDp).sp
            fun d(f: Fluid) = f.at(widthDp).dp
            return FmtMetrics(
                widthDp = widthDp,
                textXs = t(FmtText.xs), textSm = t(FmtText.sm), textBase = t(FmtText.base), textMd = t(FmtText.md),
                textLg = t(FmtText.lg), textXl = t(FmtText.xl), textKid = t(FmtText.kid), text2xl = t(FmtText.t2xl),
                text3xl = t(FmtText.t3xl), text4xl = t(FmtText.t4xl), textDisplay = t(FmtText.display),
                gutter = d(FmtLayout.gutter), sectionSpace = d(FmtLayout.sectionSpace), sectionTop = d(FmtLayout.sectionTop),
                stack = d(FmtLayout.stack), groupGap = d(FmtLayout.groupGap), cardPad = d(FmtLayout.cardPad),
                gridGap = d(FmtLayout.gridGap), content = d(FmtLayout.content), measure = d(FmtLayout.measure),
            )
        }
    }
}

/** The two families: Fredoka for headings, buttons and the menu; Quicksand for everything else. */
@Immutable
data class FmtFonts(val display: FontFamily, val body: FontFamily)

/** Text styles built from the typography tokens at the current size. */
@Immutable
data class FmtTypeScale(
    val title: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val lead: TextStyle,
    val body: TextStyle,
    val chip: TextStyle,
    val small: TextStyle,
    /** Section-group headings on activity screens (h2 on a worksheet). */
    val groupTitle: TextStyle,
    /** Anything a child reads. */
    val kid: TextStyle,
    /** The practice name beside the logo, at weight-brand. */
    val brand: TextStyle,
    val button: TextStyle,
)

val LocalFmtMetrics = staticCompositionLocalOf { FmtMetrics.forWidth(390f) }
val LocalFmtType = staticCompositionLocalOf<FmtTypeScale> { error("Wrap your UI in FmtTheme { }") }
val LocalFmtFonts = staticCompositionLocalOf<FmtFonts> { error("Wrap your UI in FmtTheme { }") }

/** Shortcuts: FmtTheme.metrics, FmtTheme.type. */
object FmtTheme {
    val metrics: FmtMetrics @Composable get() = LocalFmtMetrics.current
    val type: FmtTypeScale @Composable get() = LocalFmtType.current
    val fonts: FmtFonts @Composable get() = LocalFmtFonts.current
}

@Composable
fun rememberFmtFonts(): FmtFonts {
    val display = FontFamily(
        Font(Res.font.fredoka_400, FontWeight.Normal),
        Font(Res.font.fredoka_500, FontWeight.Medium),
        Font(Res.font.fredoka_600, FontWeight.SemiBold),
        Font(Res.font.fredoka_700, FontWeight.Bold),
    )
    val body = FontFamily(
        Font(Res.font.quicksand_500, FontWeight.Medium),
        Font(Res.font.quicksand_600, FontWeight.SemiBold),
        Font(Res.font.quicksand_700, FontWeight.Bold),
    )
    return remember(display, body) { FmtFonts(display, body) }
}

private fun style(token: FmtTypeToken, fonts: FmtFonts, widthDp: Float, color: androidx.compose.ui.graphics.Color = FmtColors.ink): TextStyle {
    val size = token.size.at(widthDp)
    return TextStyle(
        fontFamily = if (token.family == FmtFamily.Display) fonts.display else fonts.body,
        fontWeight = FontWeight(token.weight),
        fontSize = size.sp,
        lineHeight = (size * token.lineHeight).sp,
        letterSpacing = token.letterSpacingEm.em,
        color = color,
    )
}

fun fmtTypeScale(m: FmtMetrics, fonts: FmtFonts): FmtTypeScale {
    val w = m.widthDp
    val kidSize = FmtText.kid.at(w)
    val xl = FmtText.xl.at(w)
    val md = FmtText.md.at(w)
    return FmtTypeScale(
        title = style(FmtTypography.title, fonts, w),
        heading = style(FmtTypography.heading, fonts, w),
        subheading = style(FmtTypography.subheading, fonts, w),
        lead = style(FmtTypography.lead, fonts, w),
        body = style(FmtTypography.body, fonts, w, FmtColors.inkSoft),
        chip = style(FmtTypography.chip, fonts, w),
        small = style(FmtTypography.small, fonts, w, FmtColors.inkSoft),
        groupTitle = TextStyle(
            fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = xl.sp,
            lineHeight = (xl * FmtLeading.snug).sp, color = FmtColors.ink,
        ),
        kid = TextStyle(
            fontFamily = fonts.body, fontWeight = FontWeight.SemiBold, fontSize = kidSize.sp,
            lineHeight = (kidSize * FmtLeading.normal).sp, color = FmtColors.ink,
        ),
        brand = TextStyle(
            fontFamily = fonts.display, fontWeight = FontWeight(FmtWeight.brand), fontSize = xl.sp,
            lineHeight = (xl * FmtLeading.tight).sp, letterSpacing = FmtTracking.tight.em, color = FmtColors.brandInk,
        ),
        button = TextStyle(
            fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = md.sp,
            lineHeight = (md * FmtLeading.snug).sp, color = FmtColors.paper,
        ),
    )
}

/**
 * Provides the design system to everything inside. Measures the window and
 * resolves the fluid tokens for its width, so type and spacing grow smoothly
 * from phone to laptop, as on the website.
 */
@Composable
fun FmtTheme(content: @Composable () -> Unit) {
    val fonts = rememberFmtFonts()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth.value
        val metrics = remember(width) { FmtMetrics.forWidth(width) }
        val type = remember(metrics, fonts) { fmtTypeScale(metrics, fonts) }
        CompositionLocalProvider(
            LocalFmtMetrics provides metrics,
            LocalFmtType provides type,
            LocalFmtFonts provides fonts,
        ) {
            content()
        }
    }
}
