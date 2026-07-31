package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RedAccent,
    secondary = OnDarkTextSecondary,
    tertiary = DarkCardBg,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = OnDarkTextPrimary,
    onBackground = OnDarkTextPrimary,
    onSurface = OnDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = RedAccentLight,
    secondary = OnLightTextSecondary,
    tertiary = LightCardBg,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = OnLightTextPrimary,
    onBackground = OnLightTextPrimary,
    onSurface = OnLightTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our high-fidelity custom brand identity
    isAdultMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (isAdultMode) {
        val orangePrimary = if (darkTheme) Color(0xFFFF5722) else Color(0xFFE64A19)
        baseScheme.copy(primary = orangePrimary)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
