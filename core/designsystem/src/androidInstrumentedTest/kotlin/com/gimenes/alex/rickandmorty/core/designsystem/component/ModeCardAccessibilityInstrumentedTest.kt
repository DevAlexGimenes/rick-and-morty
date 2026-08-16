package com.gimenes.alex.rickandmorty.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import com.gimenes.alex.rickandmorty.core.designsystem.theme.RickAndMortyTheme
import org.junit.Rule
import org.junit.Test

/**
 * Verifies, via the actual Compose semantics tree (issue #39's explicit requirement - not just an
 * assumption that `clickable`'s default `mergeDescendants` behavior holds), that [ModeCard]'s
 * embedded "Play now" [LinkCtaText] CTA does NOT introduce a second, separately-focusable
 * accessibility node distinct from the card's own single clickable node - mirroring how
 * `AnswerLetterBadge` already stays merged into `AnswerOptionButton`'s single node in
 * `feature:quiz`'s `QuizScreen` (issue #35).
 *
 * [onRoot.printToLog] is included so the full semantics tree is also visible in logcat for manual
 * TalkBack-adjacent inspection, on top of the hard assertions below.
 */
class ModeCardAccessibilityInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun modeCard_playNowCta_mergesIntoTheCardsSingleClickableNode() {
        composeTestRule.setContent {
            RickAndMortyTheme {
                ModeCard(
                    title = "Trivia Quiz",
                    description = "Answer questions about the multiverse.",
                    onClick = {},
                )
            }
        }

        composeTestRule.onRoot().printToLog("ModeCardSemanticsTree")

        // Exactly one clickable node in the whole composition - the card itself. If "Play now"
        // had regressed into its own clickable/focusable node, this would find 2.
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)

        // That single clickable node's merged semantics still surface the CTA text, proving
        // "Play now" is announced as part of the card's own accessibility node rather than being
        // silently dropped or requiring a separate node to be reachable at all.
        composeTestRule
            .onNode(hasClickAction() and hasText("Play now", substring = true))
            .assertExists()
    }
}
