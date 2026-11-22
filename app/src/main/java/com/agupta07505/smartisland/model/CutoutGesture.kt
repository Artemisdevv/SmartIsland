/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.GestureSensitivity
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val CUTOUT_SWIPE_THRESHOLD_DP = 12f

private fun isCutoutSwipeHorizontal(dx: Float, dy: Float, sensitivity: GestureSensitivity): Boolean =
    abs(dx) > abs(dy) * sensitivity.dominance * 0.9f

/** All distances passed to a recognizer are in the same coordinate space. */
internal fun isInwardCutoutSwipe(
    startFromCenter: Float, dx: Float, dy: Float, threshold: Float, sensitivity: GestureSensitivity
): Boolean =
    isCutoutSwipeHorizontal(dx, dy, sensitivity) && startFromCenter * dx < 0f &&
        abs(startFromCenter) >= threshold / 2f && abs(dx) >= threshold

internal fun isOutwardCutoutSwipe(
    startFromCenter: Float, dx: Float, dy: Float, threshold: Float, sensitivity: GestureSensitivity
): Boolean =
    isCutoutSwipeHorizontal(dx, dy, sensitivity) && abs(dx) >= threshold &&
        (abs(startFromCenter) <= threshold / 3f || startFromCenter * dx > 0f)

internal enum class CollapsedCardTap { OpenApp, PutAway, Expand }

internal fun collapsedCardTap(appleStyle: Boolean, inCenter: Boolean): CollapsedCardTap = when {
    appleStyle -> CollapsedCardTap.OpenApp
    inCenter -> CollapsedCardTap.PutAway
    else -> CollapsedCardTap.Expand
}

internal enum class CutoutRestore { Idle, Manual }

internal fun cutoutRestore(
    hasVisibleCards: Boolean, hasManualActivity: Boolean, hasIdleCards: Boolean, hideWhenIdle: Boolean
): CutoutRestore? = when {
    hasVisibleCards -> null
    hasManualActivity -> CutoutRestore.Manual
    hasIdleCards && hideWhenIdle -> CutoutRestore.Idle
    else -> null
}

internal data class CutoutWindowBounds(val left: Int, val top: Int, val width: Int, val height: Int)

/** Physical window bounds are the hitbox. Keep them within the display even for a calibrated edge cutout. */
internal fun cutoutWindowBounds(area: CutoutArea, density: Float, screenWidthPx: Int): CutoutWindowBounds {
    val target = cutoutTouchTarget(area)
    val width = (target.widthDp * density).roundToInt().coerceIn(1, screenWidthPx.coerceAtLeast(1))
    val left = ((screenWidthPx - width) / 2f + target.centreXDp * density).roundToInt()
        .coerceIn(0, (screenWidthPx - width).coerceAtLeast(0))
    return CutoutWindowBounds(
        left,
        (target.topDp * density).roundToInt(),
        width,
        (target.heightDp * density).roundToInt().coerceAtLeast(1)
    )
}
