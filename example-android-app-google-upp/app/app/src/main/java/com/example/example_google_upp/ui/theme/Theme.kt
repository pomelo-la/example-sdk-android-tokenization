package com.example.example_google_upp.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
    lightColorScheme(
        primary = PomeloMagenta,
        onPrimary = PomeloWhite,
        primaryContainer = PomeloMagentaLight,
        onPrimaryContainer = PomeloPurple,
        secondary = PomeloViolet,
        onSecondary = PomeloWhite,
        secondaryContainer = PomeloPurple,
        onSecondaryContainer = PomeloWhite,
        tertiary = PomeloPurple,
        onTertiary = PomeloWhite,
        background = PomeloWhite,
        onBackground = PomeloBlack,
        surface = PomeloWhite,
        onSurface = PomeloBlack,
        surfaceVariant = PomeloSurface,
        onSurfaceVariant = PomeloTextMuted,
        outline = PomeloOutline,
        outlineVariant = PomeloMagentaLight,
    )

@Composable
fun ExamplegoogleuppTheme(dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                dynamicLightColorScheme(context)
            }

            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as android.app.Activity).window
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    androidx.core.view.WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = true
                }
            }
            content()
        },
    )
}
