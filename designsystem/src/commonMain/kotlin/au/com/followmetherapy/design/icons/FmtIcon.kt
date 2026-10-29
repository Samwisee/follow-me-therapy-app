package au.com.followmetherapy.design.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import au.com.followmetherapy.design.tokens.FmtSize


/** Draws an icon in [tint]. Decorative unless [label] is given. */
@Composable
fun FmtIcon(icon: ImageVector, tint: Color, size: Dp = FmtSize.icon, label: String? = null, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = label,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
