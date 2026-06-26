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

private val AnimeColorScheme =
  darkColorScheme(
    primary = NeonPink,
    secondary = NeonCyan,
    tertiary = MysticPurple,
    background = DarkBackground,
    surface = CardSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // We default to dark theme for the beautiful neon vibe
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our custom artwork palette
  content: @Composable () -> Unit,
) {
  val colorScheme = AnimeColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
