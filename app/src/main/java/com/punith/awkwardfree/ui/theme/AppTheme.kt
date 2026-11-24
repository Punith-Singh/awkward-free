package com.punith.awkwardfree.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.punith.awkwardfree.R

internal val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold)
)
internal fun appTypography(): Typography {
    val t = Typography()
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), displayMedium = t.displayMedium.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), displaySmall = t.displaySmall.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified),
        headlineLarge = t.headlineLarge.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), headlineMedium = t.headlineMedium.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), headlineSmall = t.headlineSmall.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified),
        titleLarge = t.titleLarge.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), titleMedium = t.titleMedium.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), titleSmall = t.titleSmall.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified),
        bodyLarge = t.bodyLarge.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), bodyMedium = t.bodyMedium.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), bodySmall = t.bodySmall.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified),
        labelLarge = t.labelLarge.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), labelMedium = t.labelMedium.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified), labelSmall = t.labelSmall.copy(fontFamily = Jakarta, letterSpacing = 0.sp, lineHeight = TextUnit.Unspecified)
    )
}
internal fun appShapes() = Shapes(extraSmall = RoundedCornerShape(14.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp))

internal val Background = Color(0xFFF5F4FB)
internal val Ink = Color(0xFF18152E)
internal val Muted = Color(0xFF78768D)
internal val Violet = Color(0xFF674FE8)
internal val Deep = Color(0xFF241D50)
internal val Lavender = Color(0xFFE9E4FF)
internal val Lime = Color(0xFFC9F27A)
internal val Line = Color(0xFFEBEAF2)
internal val Peach = Color(0xFFFFE5D9)
