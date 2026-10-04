package com.example.videodownloader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.videodownloader.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = TitaniumBlueDark,
    onPrimary = OnTitaniumBlueDark,
    primaryContainer = TitaniumContainerDark,
    onPrimaryContainer = OnTitaniumContainerDark,

    secondary = TealSteelDark,
    onSecondary = OnTealSteelDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,

    tertiary = SlateSteelDark,
    onTertiary = OnSlateSteelDark,
    tertiaryContainer = SlateContainerDark,
    onTertiaryContainer = OnSlateContainerDark,

    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = TitaniumBlueLight,
    onPrimary = OnTitaniumBlueLight,
    primaryContainer = TitaniumContainerLight,
    onPrimaryContainer = OnTitaniumContainerLight,

    secondary = TealSteelLight,
    onSecondary = OnTealSteelLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = OnTealContainerLight,

    tertiary = SlateSteelLight,
    onTertiary = OnSlateSteelLight,
    tertiaryContainer = SlateContainerLight,
    onTertiaryContainer = OnSlateContainerLight,

    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)



@Composable
fun VideoDownloaderTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    // 🚀 ВЫСОКОПРОИЗВОДИТЕЛЬНЫЙ 120 FPS GPU CROSSFADE ПЕРЕХОД ТЕМЫ
    val themeKey = remember(isDark, dynamicColor) { "$isDark-$dynamicColor" }

    Crossfade(
        targetState = themeKey,
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing
        ),
        label = "ThemeKeyCrossfade"
    ) { key ->
        val keyIsDark = key.startsWith("true")
        val keyDynamic = key.endsWith("true")
        val context = LocalContext.current

        val activeColorScheme = remember(keyIsDark, keyDynamic, context) {
            when {
                keyDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (keyIsDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                keyIsDark -> DarkColorScheme
                else -> LightColorScheme
            }
        }

        MaterialTheme(
            colorScheme = activeColorScheme,
            typography = Typography
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
fun getAppCardBorder(isSelected: Boolean = false): androidx.compose.foundation.BorderStroke {
    if (isSelected) return androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)

    val appTheme by com.example.videodownloader.SettingsManager.appTheme.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    return if (isDark) {
        androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
}