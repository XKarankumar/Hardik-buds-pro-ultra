package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `navigate across all primary screens and verify honest UI state`() {
        composeRule.mainClock.advanceTimeBy(1200L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("HARDIK BUDS PRO ULTRA").assertIsDisplayed()
        composeRule.onNodeWithTag("scan_toggle_button").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_audio").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("10-Band Equalizer").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_controls").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Smart Earbud Controls").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_find_my").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Location Transparency Notice").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_security").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Keystore Companion Vault").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Theme & Luxury Aesthetics").assertIsDisplayed()
    }
}
