/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CUTOUT_SWIPE_THRESHOLD_DP
import com.agupta07505.smartisland.model.CutoutRestore
import com.agupta07505.smartisland.model.isOutwardCutoutSwipe
import com.agupta07505.smartisland.util.GestureSensitivity

/**
 * Invisible touch target over the pill's spot while the pill is hidden, so tapping the area reveals the pill (auto-
 * hide), brings back put-away idle cards, or opens the shortcuts.
 */
@Composable
internal fun HiddenPillTouchTarget(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    cb: IslandCallbacks,
    ui: IslandInteractionState
) {
    if (layout.isHiding && !cb.currentExpanded) {
        Box(
            modifier = Modifier
                .width(settings.width.dp)
                .height(settings.height.dp)
                .graphicsLayer {
                    translationX = layout.collapsedMainOffset.toPx()
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (settings.autoHidePill && ui.isAutoHidden) {
                            ui.isAutoHidden = false
                            ui.touch()
                        } else if (cb.currentNotifications.isEmpty() && cb.currentHasIdleStoredCards) {
                            cb.currentOnRevealIdleCards()
                        } else if (cb.currentNotifications.isNotEmpty()) {
                            cb.currentOnToggle()
                        }
                    }
                }
        )
    }
}

/** This view fills a window already sized to the cutout hitbox. */
@Composable
internal fun CutoutRestoreTarget(
    restore: CutoutRestore,
    centerX: Float,
    sensitivity: GestureSensitivity,
    onRestore: () -> Unit
) {
    val currentOnRestore by rememberUpdatedState(onRestore)
    Box(Modifier.fillMaxSize().pointerInput(restore, centerX, sensitivity) {
        if (restore == CutoutRestore.Idle) {
            detectTapGestures { currentOnRestore() }
        } else {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val threshold = sensitivity.scaled(CUTOUT_SWIPE_THRESHOLD_DP).dp.toPx()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.singleOrNull()?.takeIf { it.id == down.id } ?: break
                    if (change.isConsumed) break
                    if (change.changedToUp()) {
                        val drag = change.position - down.position
                        if (isOutwardCutoutSwipe(down.position.x - centerX, drag.x, drag.y, threshold, sensitivity)) {
                            currentOnRestore()
                        }
                        change.consume()
                        break
                    }
                    change.consume()
                }
            }
        }
    })
}
