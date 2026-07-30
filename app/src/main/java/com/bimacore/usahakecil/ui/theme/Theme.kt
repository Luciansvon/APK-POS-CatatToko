package com.bimacore.usahakecil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bimacore.usahakecil.BuildConfig

object BrandColors {
    val Warning = Color(0xFFA15C00)
    val Success = Color(0xFF2E7D32)
}

private val RetailColors = lightColorScheme(
    primary = Color(0xFF0B6B5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F3ED),
    onPrimaryContainer = Color(0xFF083B36),
    secondary = Color(0xFF47645D),
    onSecondary = Color.White,
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF18201E),
    surface = Color.White,
    onSurface = Color(0xFF18201E),
    surfaceVariant = Color(0xFFE8EFEC),
    onSurfaceVariant = Color(0xFF5F6B67),
    outline = Color(0xFFB9C6C1),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
)

private val WholesaleColors = lightColorScheme(
    primary = Color(0xFF2457C5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF102C67),
    secondary = Color(0xFF52658F),
    onSecondary = Color.White,
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF171C27),
    surface = Color.White,
    onSurface = Color(0xFF171C27),
    surfaceVariant = Color(0xFFE8EDF7),
    onSurfaceVariant = Color(0xFF5C6577),
    outline = Color(0xFFB7C1D5),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
)

private val CulinaryColors = lightColorScheme(
    primary = Color(0xFFA44322),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFADCCF),
    onPrimaryContainer = Color(0xFF4A1708),
    secondary = Color(0xFF805548),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF241A17),
    surface = Color.White,
    onSurface = Color(0xFF241A17),
    surfaceVariant = Color(0xFFF3E7E1),
    onSurfaceVariant = Color(0xFF735E56),
    outline = Color(0xFFD2BBB2),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
)

@Composable
fun UsahaKecilTheme(content: @Composable () -> Unit) {
    val colors = when (BuildConfig.FLAVOR) {
        "wholesale" -> WholesaleColors
        "culinary" -> CulinaryColors
        else -> RetailColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
