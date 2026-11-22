/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue

internal data class CutoutAnimation(
    val screenCenter: Offset,
    val pivot: Offset? = null,
    val exitCard: IslandNotification? = null,
    val exitSettings: SmartIslandSettings? = null
)

@Composable
internal fun OverlayIsland(
    viewModel: IslandViewModel,
    statusBarHeight: Float,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, com.agupta07505.smartisland.model.AppShortcutAction) -> Unit = { _, _ -> },
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = true,
    appleGestureCenterX: Float? = null,
    canUseAppleGesture: () -> Boolean = { true },
    onHideActivity: (IslandNotification) -> Boolean = viewModel::hideActivity,
    cutoutAnimation: CutoutAnimation? = null,
    onCutoutAnimationFinished: (CutoutAnimation) -> Unit = {}
) {
    val shadeOpen by viewModel.shadeOpen.collectAsState()
    val idleSuppressed by viewModel.idleWidgetsSuppressed.collectAsState()
    val liveSettings by viewModel.displaySettings.collectAsState()
    val settings = cutoutAnimation?.exitSettings ?: liveSettings
    val portraitAppleGestures = settings.appleStyleGestures &&
        androidx.compose.ui.platform.LocalConfiguration.current.orientation !=
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val expanded by viewModel.expanded.collectAsState()
    val widgetIslandOpen by viewModel.widgetIslandOpen.collectAsState()
    val widgetIslandSnap by viewModel.widgetIslandSnap.collectAsState()
    val appsLauncherOpen by viewModel.appsLauncherOpen.collectAsState()
    val liveNotifications by viewModel.visibleNotifications.collectAsState()
    val notifications = cutoutAnimation?.exitCard?.let { listOf(it) } ?: liveNotifications
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isInputActive by viewModel.isInputActive.collectAsState()
    val hasIdleStoredCards by viewModel.hasIdleStoredCards.collectAsState()
    val siblingPickerOpen by viewModel.siblingPickerOpen.collectAsState()
    val pickerOnLeft by viewModel.pickerOnLeft.collectAsState()
    val readingExpandedText by viewModel.readingExpandedText.collectAsState()
    val expandedMedia by viewModel.expandedMedia.collectAsState()
    val cardStepState by viewModel.cardStepState.collectAsState()
    val cardDepth by viewModel.cardDepth.collectAsState()
    val landscapeDot by viewModel.idleDot.collectAsState()
    val widgetPill by viewModel.landscapeWidgetPill.collectAsState()
    val tickHold by viewModel.tickHold.collectAsState()
    val idleClosing by viewModel.idleClosing.collectAsState()
    val idleLeaving by viewModel.idleLeaving.collectAsState()
    val rotationHidden by viewModel.rotationVeil.collectAsState()
    // Portrait, static exempt app: the collapsed pill stays but is dead: nothing on it, nothing opens, no touches
    // (the window ignores them).
    val staticPill by viewModel.islandStaticForApp.collectAsState()
    // The Face ID scan is playing: the island shows nothing else and takes nothing new (cards that arrive wait and show afterwards).
    val faceIdState by com.agupta07505.smartisland.util.FaceIdController.state.collectAsState()
    val quiet = staticPill || faceIdState.active
    val context = LocalContext.current
    val cutoutProgress = remember {
        Animatable(if (cutoutAnimation != null && cutoutAnimation.exitCard == null) 0f else 1f)
    }
    LaunchedEffect(cutoutAnimation) {
        if (cutoutAnimation == null) {
            cutoutProgress.snapTo(1f)
        } else if (cutoutAnimation.pivot != null) {
            val end = if (cutoutAnimation.exitCard != null) 0f else 1f
            if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
                cutoutProgress.snapTo(end)
            } else {
                cutoutProgress.animateTo(end, tween(durationMillis = 225, easing = FastOutSlowInEasing))
            }
            onCutoutAnimationFinished(cutoutAnimation)
        }
    }

    // Rotation: everything fades out at once and returns on the pill's arrival spring and fade curve (RotationVeil).
    val veilSpeed = settings.animationSpeed(
        androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
    )
    val veilVisible = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(rotationHidden) {
        if (rotationHidden) {
            if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
                veilVisible.snapTo(0f)
            } else {
                veilVisible.animateTo(
                    0f,
                    androidx.compose.animation.core.tween(
                        veilSpeed.durationMs(com.agupta07505.smartisland.model.RotationVeil.HIDE_MS)
                    )
                )
            }
        } else if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
            veilVisible.snapTo(1f)
        } else {
            veilVisible.animateTo(
                1f,
                androidx.compose.animation.core.spring(
                    dampingRatio = com.agupta07505.smartisland.model.PILL_SPRING_DAMPING,
                    stiffness = veilSpeed.stiffness(com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS)
                )
            )
        }
    }

    val isContentRedacted = isLocked && settings.lockScreenPrivacy == "AppIconOnly"
    val processedNotifications = remember(notifications, isContentRedacted) {
        if (isContentRedacted) {
            notifications.map { notif ->
                if (notif.mode == com.agupta07505.smartisland.model.IslandMode.Notification) {
                    notif.copy(
                        title = notif.appName,
                        text = "Contents hidden",
                        actionIntents = emptyList(),
                        media = emptyList()
                    )
                } else {
                    notif
                }
            }
        } else {
            notifications
        }
    }

    val selectedApps = remember(settings.shortcutPackages, settings.enableAppShortcuts) {
        if (settings.enableAppShortcuts) {
            AppShortcutProvider.selectedApps(context, settings.shortcutPackages)
        } else {
            emptyList()
        }
    }
    val launcherApps by produceState<List<LaunchableApp>?>(
        initialValue = when {
            !settings.enableAppShortcuts -> emptyList()
            selectedApps.isNotEmpty() -> selectedApps
            settings.shortcutPackages.isEmpty() && !settings.showRecentApps -> emptyList()
            !AppShortcutProvider.hasUsageAccess(context) -> emptyList()
            else -> null
        },
        settings.enableAppShortcuts,
        settings.shortcutPackages,
        settings.showRecentApps
    ) {
        value = if (!settings.enableAppShortcuts) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                AppShortcutProvider.shortcuts(
                    context = context,
                    selectedPackages = settings.shortcutPackages,
                    includeRecent = settings.showRecentApps
                )
            }
        }
    }

    val shownCardKey = notifications.getOrNull(selectedIndex)?.key
    val selectorOpen = cardStepState.key != null && cardStepState.key == shownCardKey && cardStepState.selector
    // A step change by a tap (the arrow, a thumbnail, a picture in the selector, closing a picture) buzzes once, at the tap.
    // Drags and swipes have their own single buzz in the gesture handler.
    val hapticStrengthId by androidx.compose.runtime.rememberUpdatedState(settings.hapticStrength)
    val stepTapHaptic = remember(context) {
        {
            com.agupta07505.smartisland.util.IslandHaptics.play(
                context,
                com.agupta07505.smartisland.util.HapticStrength.fromId(hapticStrengthId),
                com.agupta07505.smartisland.util.HapticKind.Swipe
            )
        }
    }
    val mediaExpansion = remember(expandedMedia, selectorOpen, viewModel) {
        com.agupta07505.smartisland.ui.expanded.MediaExpansion(
            key = expandedMedia?.first,
            index = expandedMedia?.second ?: 0,
            selectorOpen = selectorOpen,
            onChange = { key, index ->
                stepTapHaptic()
                viewModel.setExpandedMedia(key, index)
            }
        )
    }
    val cardStepsAccess = remember(cardStepState, expandedMedia, viewModel) {
        com.agupta07505.smartisland.ui.expanded.CardStepsAccess(
            state = cardStepState,
            pictureKey = expandedMedia?.first,
            pictureIndex = expandedMedia?.second ?: 0,
            set = { key, steps ->
                stepTapHaptic()
                viewModel.setCardSteps(key, steps)
            },
            reportExpandable = { key, expandable -> viewModel.reportCardTextExpandable(key, expandable) }
        )
    }
    val readerScrollTracker = remember { com.agupta07505.smartisland.util.ReaderScrollTracker() }
    val stepDragState = remember { com.agupta07505.smartisland.ui.expanded.StepDragState() }
    androidx.compose.runtime.CompositionLocalProvider(
        com.agupta07505.smartisland.ui.expanded.LocalMediaExpansion provides mediaExpansion,
        com.agupta07505.smartisland.ui.expanded.LocalCardSteps provides cardStepsAccess,
        com.agupta07505.smartisland.ui.expanded.LocalReaderScroll provides readerScrollTracker,
        com.agupta07505.smartisland.ui.expanded.LocalIslandPalette provides com.agupta07505.smartisland.ui.expanded.rememberIslandPalette(settings),
        com.agupta07505.smartisland.ui.expanded.LocalStepDrag provides stepDragState,
        com.agupta07505.smartisland.ui.expanded.LocalReplyDrafts provides viewModel.replyDrafts,
        com.agupta07505.smartisland.ui.expanded.LocalPictureArrowSide provides
            com.agupta07505.smartisland.ui.expanded.pictureArrowSide(
                isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE,
                pickerOnLeft = pickerOnLeft
            )
    ) {
    IslandOverlayView(
        settings = settings,
        expanded = expanded && !quiet,
        // The Open Apps shortcut shows the quick-launch grid, which is what the island draws for an empty list.
        notifications = if (quiet || (appsLauncherOpen && expanded)) emptyList() else processedNotifications,
        selectedIndex = selectedIndex,
        launcherApps = launcherApps,
        onPageSelected = { index -> viewModel.setSelectedNotificationIndex(index) },
        onOpenNotification = onOpenNotification,
        onLaunchApp = onLaunchApp,
        onAppAction = onAppAction,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onDismissNotification = { viewModel.dismissCurrentNotification() },
        onDismissAllNotifications = { viewModel.dismissAllNotifications() },
        onOpenFloatingWindow = onOpenFloatingWindow,
        onOpenNotificationShade = onOpenNotificationShade,
        statusBarHeight = statusBarHeight,
        isInputActive = isInputActive,
        onReplyStateChanged = { viewModel.setInputActive(it) },
        onTextExpandedChanged = { viewModel.setAutoCollapsePaused(it) },
        cardDepth = cardDepth,
        onCardDeeper = { viewModel.cardGoDeeper() },
        onCardBack = { viewModel.cardGoBack() },
        siblingPickerOpen = siblingPickerOpen && !quiet,
        readingExpandedText = readingExpandedText,
        onSiblingSelected = { key -> viewModel.selectSibling(key) },
        onSiblingPickerClose = { viewModel.closeSiblingPicker() },
        onOpenSiblingPicker = { viewModel.openSiblingPicker() },
        isFullWidth = isFullWidth,
        hasIdleStoredCards = hasIdleStoredCards && !quiet,
        idleWidgetsSuppressed = idleSuppressed || quiet,
        tickPillHold = tickHold,
        onTickPillHold = { viewModel.setTickHold(it) },
        onRevealIdleCards = { viewModel.revealIdleCards() },
        onSwitchEvent = { forward -> viewModel.switchEvent(forward) },
        onSwitchNotification = { forward -> viewModel.switchNotification(forward) },
        onSwitchSibling = { forward -> viewModel.switchSibling(forward) },
        onTogglePillHidden = { viewModel.togglePillHidden() },
        // The island reports its rect on every frame of a size animation. A change of 2 px or less is skipped (no new
        // flow value, no touch-region work); the 8 dp touch pad hides the difference.
        onExpandedSurfaceBounds = { next ->
            val last = viewModel.expandedSurfaceBounds.value
            if (next == null || last == null ||
                kotlin.math.abs(last.left - next.left) > 2 || kotlin.math.abs(last.top - next.top) > 2 ||
                kotlin.math.abs(last.right - next.right) > 2 || kotlin.math.abs(last.bottom - next.bottom) > 2
            ) {
                viewModel.expandedSurfaceBounds.value = next
            }
        },
        onUserEngaged = { viewModel.markUserEngaged() },
        onHandleHeld = { viewModel.setAutoCollapseHeld(it) },
        pickerOnLeft = pickerOnLeft,
        landscapeDot = if (quiet) null else landscapeDot,
        widgetPill = widgetPill && !quiet,
        // One tick when the tap did something (a glyph-free pill centre or the landscape idle circle), none when it did nothing:
        // that tells the owner whether the tap counted.
        onOpenAppsLauncher = { viewModel.openAppsLauncher() },
        onIdleDotTap = {
            if (portraitAppleGestures) {
                if (canUseAppleGesture()) viewModel.collapse()
            } else if (viewModel.onIdleDotTapped() != com.agupta07505.smartisland.model.IdleDotTap.None) {
                stepTapHaptic()
            }
        },
        idleClosing = idleClosing,
        appsLauncherOpen = appsLauncherOpen && expanded,
        idleLeaving = idleLeaving,
        widgetIslandOpen = widgetIslandOpen && !quiet,
        onWidgetIslandChange = { viewModel.setWidgetIslandOpen(it) },
        onWidgetIslandShownChange = { viewModel.widgetIslandShown.value = it },
        widgetIslandSnap = widgetIslandSnap,
        appleGestureCenterX = appleGestureCenterX,
        canUseAppleGesture = canUseAppleGesture,
        onHideActivity = onHideActivity,
        onRestoreActivity = viewModel::restoreHiddenActivity,
        canRestoreActivity = { viewModel.hasManuallyHiddenActivity.value },
        modifier = modifier.graphicsLayer {
            alpha = com.agupta07505.smartisland.model.RotationVeil.alpha(veilVisible.value)
            if (cutoutAnimation != null) {
                val progress = cutoutProgress.value
                alpha *= progress
                scaleX = 0.16f + 0.84f * progress
                scaleY = scaleX
                val pivot = cutoutAnimation.pivot
                if (pivot != null && size.width > 0f && size.height > 0f) {
                    transformOrigin = TransformOrigin(pivot.x / size.width, pivot.y / size.height)
                }
            }
        }
    )
    }
}
