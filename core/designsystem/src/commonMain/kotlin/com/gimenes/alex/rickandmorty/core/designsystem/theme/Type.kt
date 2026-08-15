package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import rickandmorty.core.designsystem.generated.resources.Res
import rickandmorty.core.designsystem.generated.resources.space_grotesk_bold
import rickandmorty.core.designsystem.generated.resources.space_grotesk_medium
import rickandmorty.core.designsystem.generated.resources.space_grotesk_regular
import rickandmorty.core.designsystem.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

/**
 * Accessibility-first: we intentionally keep Material3's default type scale (system font,
 * default sizes/line-heights) for body/UI text rather than a custom brand type ramp, so user
 * font-scale and platform accessibility settings continue to work as expected.
 *
 * The one deliberate exception (design refinement, issue #35) is the headline tier
 * (`headlineLarge`/`headlineMedium`/`headlineSmall`) - screen titles and section headings - which
 * uses the Space Grotesk display font for on-brand emphasis. Only the typeface is swapped; sizes,
 * weights, and line-heights are untouched, so font-scale behavior for headline text is unaffected.
 * `displayLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, and
 * `labelLarge/Medium/Small` all remain on the system default font, per the same rationale.
 *
 * Space Grotesk is an OFL-licensed Google Font (see `core/designsystem/licenses/SpaceGrotesk-OFL.txt`),
 * embedded via Compose Multiplatform's font resources (`composeResources/font/`).
 */
@Composable
fun rickAndMortyTypography(): Typography {
    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, weight = FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, weight = FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, weight = FontWeight.Bold),
    )
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = spaceGrotesk),
        headlineMedium = base.headlineMedium.copy(fontFamily = spaceGrotesk),
        headlineSmall = base.headlineSmall.copy(fontFamily = spaceGrotesk),
    )
}
