package com.example.fitnesstracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePageUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMainUIComponentsDisplayed() {
        // Check TopHeader text
        composeTestRule.onNodeWithText("Fitness Tracker").assertIsDisplayed()

        // Check Tracking button
        composeTestRule.onNodeWithText("Enable Tracking").assertIsDisplayed()

        // Check Weekly Progress header
        composeTestRule.onNodeWithText("Weekly Progress").performScrollTo().assertIsDisplayed()

        // Check Daily Summary header
        composeTestRule.onNodeWithText("Daily Summary").performScrollTo().assertIsDisplayed()

    }

    @Test
    fun testBottomNavBarButtonsDisplayedAndClickable() {
        // Wait for the activity and compose content to be ready
        composeTestRule.waitForIdle()

        // Check that all three bottom nav items are displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Click the Stats button
        composeTestRule.onNodeWithText("Stats").performClick()

        // Click the Settings button
        composeTestRule.onNodeWithText("Settings").performClick()

        // Click the Home button
        composeTestRule.onNodeWithText("Home").performClick()
    }
}
