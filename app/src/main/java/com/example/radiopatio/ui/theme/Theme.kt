package com.example.radiopatio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AzulRadioPatioDark,
    onPrimary = TextoSobreAzulDark,
    secondary = VerdeRadioPatioDark,
    onSecondary = TextoSobreVerdeDark,
    background = FondoAppDark,
    onBackground = TextoPrincipalDark,
    surface = SuperficieCardsDark,
    onSurface = TextoPrincipalDark,
    surfaceVariant = SuperficieVarianteDark,
    onSurfaceVariant = TextoSecundarioDark,
    outline = BordeSuaveDark,
    outlineVariant = BordeVarianteDark
)

private val LightColorScheme = lightColorScheme(
    primary = AzulRadioPatioLight,
    onPrimary = BlancoPuro,
    secondary = VerdeRadioPatioLight,
    onSecondary = BlancoPuro,
    background = FondoAppLight,
    onBackground = TextoPrincipalLight,
    surface = SuperficieCardsLight,
    onSurface = TextoPrincipalLight,
    surfaceVariant = SuperficieVarianteLight,
    onSurfaceVariant = TextoSecundarioLight,
    outline = BordeSuaveLight,
    outlineVariant = BordeVarianteLight
)
@Composable
fun RadioPatioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}