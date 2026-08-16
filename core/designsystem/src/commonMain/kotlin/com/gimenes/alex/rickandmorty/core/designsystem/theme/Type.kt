package com.gimenes.alex.rickandmorty.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import rickandmorty.core.designsystem.generated.resources.Res
import rickandmorty.core.designsystem.generated.resources.dm_sans_medium
import rickandmorty.core.designsystem.generated.resources.dm_sans_regular
import rickandmorty.core.designsystem.generated.resources.space_grotesk_bold
import rickandmorty.core.designsystem.generated.resources.space_grotesk_medium
import rickandmorty.core.designsystem.generated.resources.space_grotesk_regular
import rickandmorty.core.designsystem.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

/**
 * Deliberate brand-typography departure (issue #35, expanded in issue #36): rather than keeping
 * Material3's default type scale on the system font, the app embeds fixed brand fonts across
 * nearly the entire type scale. This is an explicit, accepted tradeoff per repeated brand
 * direction - not an oversight. It means the app no longer honors an OS-level "user's preferred
 * system font" override on platforms that offer one (e.g. some OEM Android skins let a user swap
 * the system-wide font; that preference is no longer reflected in this app's text). This does
 * NOT affect font-scale/accessibility: Compose `sp` scaling (including the OS "font size" /
 * "display size" setting, tested up to 200%) is completely independent of font family - only
 * literal font-family substitution preference is affected, and only on platforms that expose one.
 *
 * - `displayLarge/Medium/Small` and `headlineLarge/Medium/Small` (issue #35's original scope,
 *   expanded to include display here) use Space Grotesk. Display and headline are both reserved
 *   for the largest, most prominent on-brand text (e.g. quiz score/streak call-outs, screen
 *   titles), so they share the same display-face treatment for visual consistency.
 * - `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, and `labelLarge/Medium/Small` (issue #36)
 *   use DM Sans, a text-optimized companion face, for everything else: body copy, list items,
 *   labels, and UI chrome.
 *
 * In all cases only the typeface is swapped; sizes, weights, and line-heights from Material3's
 * default `Typography()` are untouched, so layout and font-scale behavior are otherwise
 * unaffected.
 *
 * Both fonts are OFL-licensed Google Fonts, embedded via Compose Multiplatform's font resources
 * (`composeResources/font/`):
 * - Space Grotesk: `core/designsystem/licenses/SpaceGrotesk-OFL.txt`
 * - DM Sans: `core/designsystem/licenses/DMSans-OFL.txt`
 */
@Composable
fun rickAndMortyTypography(): Typography {
    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, weight = FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, weight = FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, weight = FontWeight.Bold),
    )
    val dmSans = FontFamily(
        Font(Res.font.dm_sans_regular, weight = FontWeight.Normal),
        Font(Res.font.dm_sans_medium, weight = FontWeight.Medium),
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = spaceGrotesk),
        displayMedium = base.displayMedium.copy(fontFamily = spaceGrotesk),
        displaySmall = base.displaySmall.copy(fontFamily = spaceGrotesk),
        headlineLarge = base.headlineLarge.copy(fontFamily = spaceGrotesk),
        headlineMedium = base.headlineMedium.copy(fontFamily = spaceGrotesk),
        headlineSmall = base.headlineSmall.copy(fontFamily = spaceGrotesk),
        titleLarge = base.titleLarge.copy(fontFamily = dmSans),
        titleMedium = base.titleMedium.copy(fontFamily = dmSans),
        titleSmall = base.titleSmall.copy(fontFamily = dmSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = dmSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = dmSans),
        bodySmall = base.bodySmall.copy(fontFamily = dmSans),
        labelLarge = base.labelLarge.copy(fontFamily = dmSans),
        labelMedium = base.labelMedium.copy(fontFamily = dmSans),
        labelSmall = base.labelSmall.copy(fontFamily = dmSans),
    )
}
