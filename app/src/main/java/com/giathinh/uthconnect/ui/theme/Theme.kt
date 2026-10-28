package com.giathinh.uthconnect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = UthIndigo,
    secondary = UthIndigo,
    background = UthLightBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
)
private val DarkColors = darkColorScheme(
    primary = UthIndigoDark,
    secondary = UthIndigoDark,
    background = UthDarkBackground,
)

/** Dynamic colors are intentionally disabled to preserve UTH brand #4F46E5. */
@Composable
fun UthConnectTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
