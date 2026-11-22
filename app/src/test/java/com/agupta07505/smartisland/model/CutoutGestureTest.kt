/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.util.GestureSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoutGestureTest {
    private val sensitivity = GestureSensitivity.NORMAL

    @Test
    fun inwardSwipeCanStartOnEitherSide() {
        assertTrue(isInwardCutoutSwipe(-36f, 23f, 4f, 16f, sensitivity))
        assertTrue(isInwardCutoutSwipe(36f, -23f, -4f, 16f, sensitivity))
        assertFalse(isInwardCutoutSwipe(-36f, -23f, 4f, 16f, sensitivity))
        assertFalse(isInwardCutoutSwipe(36f, -12f, 0f, 16f, sensitivity))
        assertFalse(isInwardCutoutSwipe(36f, -23f, 30f, 16f, sensitivity))
    }

    @Test
    fun outwardSwipeCanStartAtTheCenterOrEitherSide() {
        assertTrue(isOutwardCutoutSwipe(0f, -20f, 0f, 16f, sensitivity))
        assertTrue(isOutwardCutoutSwipe(-6f, -20f, 3f, 16f, sensitivity))
        assertTrue(isOutwardCutoutSwipe(6f, 20f, -3f, 16f, sensitivity))
        assertFalse(isOutwardCutoutSwipe(6f, -30f, 0f, 16f, sensitivity))
        assertFalse(isOutwardCutoutSwipe(6f, 15f, 0f, 16f, sensitivity))
        assertFalse(isOutwardCutoutSwipe(6f, 20f, 25f, 16f, sensitivity))
    }

    @Test
    fun cutoutSwipesAcceptShorterSlightlyDiagonalDrags() {
        val threshold = CUTOUT_SWIPE_THRESHOLD_DP
        assertTrue(isInwardCutoutSwipe(8f, -12f, 13f, threshold, sensitivity))
        assertTrue(isOutwardCutoutSwipe(-3f, 12f, 13f, threshold, sensitivity))
        assertFalse(isInwardCutoutSwipe(8f, -11f, 0f, threshold, sensitivity))
        assertFalse(isOutwardCutoutSwipe(-7f, 12f, 0f, threshold, sensitivity))
        assertFalse(isOutwardCutoutSwipe(0f, 12f, 20f, threshold, sensitivity))
        assertNull(sensitivity.pillSwipe(12f, 13f, 16f))
    }

    @Test
    fun normalTapsOpenAppsInAppleStyleAndKeepLegacyActionsOtherwise() {
        assertEquals(CollapsedCardTap.OpenApp, collapsedCardTap(appleStyle = true, inCenter = true))
        assertEquals(CollapsedCardTap.OpenApp, collapsedCardTap(appleStyle = true, inCenter = false))
        assertEquals(CollapsedCardTap.PutAway, collapsedCardTap(appleStyle = false, inCenter = true))
        assertEquals(CollapsedCardTap.Expand, collapsedCardTap(appleStyle = false, inCenter = false))
    }

    @Test
    fun idleCardHasACutoutSizedWindowEvenWithShortcutsOff() {
        val settings = SmartIslandSettings(hideWhenIdle = true, enableAppShortcuts = false)
        assertEquals(CutoutRestore.Idle, cutoutRestore(false, false, true, settings.hideWhenIdle))
        assertEquals(CutoutRestore.Manual, cutoutRestore(false, true, true, settings.hideWhenIdle))
        assertNull(cutoutRestore(false, false, false, settings.hideWhenIdle))
        assertNull(cutoutRestore(true, true, true, settings.hideWhenIdle))
        assertEquals(
            CutoutWindowBounds(left = 510, top = 23, width = 120, height = 84),
            cutoutWindowBounds(CutoutArea(10f, 12f, 17f, 19f), density = 3f, screenWidthPx = 1080)
        )
    }
}
