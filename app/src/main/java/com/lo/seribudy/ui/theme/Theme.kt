package com.lo.seribudy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val TikTokRed = Color(0xFFFE2C55)
val TikTokCyan = Color(0xFF25F4EE)
val TikTokBlack = Color(0xFF010101)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkCard = Color(0xFF2A2A2A)

private val DarkColorScheme = darkColorScheme(
    primary = TikTokCyan,
    secondary = TikTokRed,
    tertiary = Color(0xFFB8F4EE),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = TikTokBlack,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
)

private val LightColorScheme = lightColorScheme(
    primary = TikTokRed,
    secondary = TikTokCyan,
    tertiary = Color(0xFF69B2AC),
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    onPrimary = Color.White,
    onSecondary = TikTokBlack,
    onBackground = TikTokBlack,
    onSurface = TikTokBlack,
)

@Composable
fun SeriBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
