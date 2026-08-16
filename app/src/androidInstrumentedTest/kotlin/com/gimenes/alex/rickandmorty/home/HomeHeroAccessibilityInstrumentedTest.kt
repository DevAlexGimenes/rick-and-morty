package com.gimenes.alex.rickandmorty.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyTheme
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies, via the actual Compose semantics tree (R5, issue #41's explicit "verify with an
 * actual semantics check" acceptance criterion - not just an assumption that
 * [androidx.compose.ui.semantics.clearAndSetSemantics]/
 * [androidx.compose.ui.semantics.hideFromAccessibility] behave as documented), that Home's hero
 * decorative elements are correctly excluded from what a screen reader would announce, while the
 * headline/tagline - which carry the real meaning - remain reachable.
 *
 * Mirrors `core:designsystem`'s `ModeCardAccessibilityInstrumentedTest` precedent: hard
 * assertions against the semantics tree, plus [onRoot.printToLog] for manual TalkBack-adjacent
 * inspection in logcat.
 */
class HomeHeroAccessibilityInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeHero_decorativeElementsAreHiddenAndMeaningfulTextIsReachable() {
        composeTestRule.setContent {
            RickAndMortyTheme {
                HomeScreen(onQuizClick = {}, onGuessCharacterClick = {})
            }
        }

        composeTestRule.onRoot().printToLog("HomeHeroSemanticsTree")

        // --- Decorative: kicker pill ---
        // clearAndSetSemantics{} means the kicker's own node carries no Text property (its label
        // is not merged/exposed), and the label string itself is not reachable anywhere in the
        // tree via a text query - i.e. a screen reader query for it finds nothing.
        val kickerNode = composeTestRule.onNodeWithTag(HERO_KICKER_TEST_TAG).fetchSemanticsNode()
        assertFalse(
            kickerNode.config.contains(SemanticsProperties.Text),
            "Kicker pill's semantics node must not expose a Text property (label should be " +
                "cleared, not merged, per clearAndSetSemantics)."
        )
        composeTestRule.onNodeWithText("Multiverse Arcade").assertDoesNotExist()

        // --- Decorative: hero PortalRingGlyph ---
        // hideFromAccessibility() (applied inside PortalRingGlyph itself when contentDescription
        // is null, which Home's hero usage relies on) sets HideFromAccessibility on the node -
        // confirm that property is actually present, not merely that no contentDescription was
        // set (a node can lack a description and still be exposed as an unlabeled focusable stop
        // otherwise).
        val glyphNode = composeTestRule.onNodeWithTag(HERO_GLYPH_TEST_TAG).fetchSemanticsNode()
        assertTrue(
            glyphNode.config.contains(SemanticsProperties.HideFromAccessibility),
            "Hero glyph's semantics node must carry HideFromAccessibility - it is purely " +
                "decorative and the headline already carries the meaning."
        )
        assertFalse(
            glyphNode.config.contains(SemanticsProperties.ContentDescription),
            "Hero glyph must not carry a content description - contentDescription = null per " +
                "the spec."
        )

        // --- Meaningful: headline + tagline remain reachable ---
        composeTestRule.onNodeWithText("Rick and Morty").assertExists()
        composeTestRule.onNodeWithText("Mini Games").assertExists()
        composeTestRule
            .onNodeWithText("Step through the portal. Prove you know the multiverse.")
            .assertExists()
    }
}
