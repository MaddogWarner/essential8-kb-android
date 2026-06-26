package com.maddogwarner.essential8kb.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = E8Primary,
    onPrimary = E8OnPrimary,
    primaryContainer = E8PrimaryContainer,
    onPrimaryContainer = E8OnPrimaryContainer,
    secondary = E8Secondary,
    tertiary = E8Tertiary,
    error = E8Error,
)

private val DarkColors = darkColorScheme(
    primary = E8PrimaryContainer,
    onPrimary = E8OnPrimaryContainer,
    primaryContainer = E8Primary,
    onPrimaryContainer = E8OnPrimary,
    secondary = E8Secondary,
    tertiary = E8Tertiary,
    error = E8Error,
)

@Composable
fun Essential8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colourScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colourScheme,
        typography = AppTypography,
        content = content,
    )
}
