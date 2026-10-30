@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rumb.launcher.feature.home

import android.appwidget.AppWidgetHostView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.IconAppearance
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.domain.iconAppearance
import com.rumb.launcher.domain.layoutSpanX
import com.rumb.launcher.domain.layoutSpanY
import com.rumb.launcher.domain.occupiedCells
import com.rumb.launcher.feature.customize.HomeCustomizeTray
import com.rumb.launcher.platform.apps.LaunchSource
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.FolderTile
import com.rumb.launcher.ui.components.LauncherClock
import com.rumb.launcher.ui.components.VisualBackdrop
import com.rumb.launcher.ui.components.VisualDockSurface
import com.rumb.launcher.ui.components.VisualSurface
import com.rumb.launcher.ui.components.rememberLaunchSourceTracker
import com.rumb.launcher.ui.components.trackLaunchSource
import com.rumb.launcher.ui.components.visualTokens
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    editMode: Boolean,
    pagerScrollEnabled: Boolean,
    homePressSignal: Int,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onVisualStyle: (VisualStyle) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    onOpenFolder: (HomeItem) -> Unit,
    onRefreshApps: () -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onGesture: (GestureAction) -> Unit,
    onDrawerDrag: (Float) -> Unit,
    onDrawerDragEnd: (Float) -> Unit,
    onPageChanged: (Int) -> Unit,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val tokens = visualTokens(settings.visualStyle)
    val appearance = settings.iconAppearance
    val pagerState = rememberPagerState(pageCount = { settings.pageCount })
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // Plain map: bounds are read only by the gesture handler, so writes must not trigger recomposition.
    val interactiveBounds = remember { HashMap<String, Rect>() }
    val itemsByPage = remember(settings.homeItems) {
        settings.homeItems.groupBy(HomeItem::page)
            .mapValues { (_, items) -> items.sortedBy { if (it.type == HomeItemType.WIDGET) 0 else 1 } }
    }
    val previewScale by animateFloatAsState(
        if (editMode) .9f else 1f,
        spring(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow),
        label = "home preview scale",
    )
    val previewLift = with(density) { (-52).dp.toPx() }

    val currentSettings by rememberUpdatedState(settings)
    val currentOnGesture by rememberUpdatedState(onGesture)
    val currentOnEnterEditMode by rememberUpdatedState(onEnterEditMode)
    val currentOnDrawerDrag by rememberUpdatedState(onDrawerDrag)
    val currentOnDrawerDragEnd by rememberUpdatedState(onDrawerDragEnd)

    LaunchedEffect(settings.clockStyle, settings.layoutLocked) {
        if (settings.clockStyle == ClockStyle.HIDDEN && !settings.layoutLocked) interactiveBounds.remove("clock")
    }
    LaunchedEffect(settings.homeSearchBar) { if (!settings.homeSearchBar) interactiveBounds.remove("search") }
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.currentPage }.collect { onPageChanged(it) } }
    val dockLaunch: (InstalledApp, LaunchSource?) -> Unit = if (editMode) { _, _ -> onExitEditMode() } else onLaunch
    LaunchedEffect(homePressSignal) {
        if (homePressSignal > 0 && pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
    }

    Box(
        modifier.fillMaxSize()
            .pointerInput(editMode) {
                if (editMode) return@pointerInput
                val swipeThreshold = 96.dp.toPx()
                val tapSlop = 24.dp.toPx()
                val drawerSlop = viewConfiguration.touchSlop * 1.5f
                val doubleTapDistance = 48.dp.toPx()
                val pinchThreshold = 36.dp.toPx()
                var lastBackgroundTap = 0L
                var lastTapPosition = Offset.Unspecified
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    val start = down.position
                    var end = start
                    var eventTime = down.uptimeMillis
                    var consumed = down.isConsumed
                    var movedBeyondTap = false
                    var sawMultiplePointers = false
                    var invalidPinch = false
                    var initialPinchSpan: Float? = null
                    var pinchDetected = false
                    var drawerDragging = false
                    var drawerSettled = false
                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    val deadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
                    try {
                        while (true) {
                            val longPressPossible = !consumed && !movedBeyondTap && !sawMultiplePointers
                            // Only time out while a long press is still possible, so slow swipes are never dropped.
                            val event = if (longPressPossible) {
                                withTimeoutOrNull((deadline - eventTime).coerceAtLeast(1L)) {
                                    awaitPointerEvent(PointerEventPass.Final)
                                }
                            } else {
                                awaitPointerEvent(PointerEventPass.Final)
                            }
                            if (event == null) {
                                val overInteractiveContent = interactiveBounds.values.any { it.contains(start) }
                                if (!overInteractiveContent) {
                                    if (currentSettings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnEnterEditMode()
                                }
                                return@awaitEachGesture
                            }
                            if (!drawerDragging) consumed = consumed || event.changes.any { it.isConsumed }
                            if (event.changes.size > 2) invalidPinch = true
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size == 2) {
                                sawMultiplePointers = true
                                val span = (pressed[0].position - pressed[1].position).getDistance()
                                val initial = initialPinchSpan
                                if (initial == null) initialPinchSpan = span
                                else if (abs(span - initial) >= maxOf(pinchThreshold, initial * .12f)) pinchDetected = true
                            } else if (pressed.size > 2) {
                                sawMultiplePointers = true
                                invalidPinch = true
                            }
                            val primary = event.changes.firstOrNull { it.id == down.id }
                            if (primary != null) {
                                end = primary.position
                                eventTime = primary.uptimeMillis
                                velocity.addPosition(primary.uptimeMillis, primary.position)
                                if ((end - start).getDistance() >= tapSlop) movedBeyondTap = true
                                if (drawerDragging) {
                                    val delta = primary.position.y - primary.previousPosition.y
                                    if (delta != 0f) currentOnDrawerDrag(delta)
                                    primary.consume()
                                } else if (
                                    !consumed && !sawMultiplePointers &&
                                    currentSettings.swipeUpAction == GestureAction.OPEN_DRAWER
                                ) {
                                    val dx = end.x - start.x
                                    val dy = end.y - start.y
                                    if (-dy > drawerSlop && abs(dy) > abs(dx) * 1.4f) {
                                        drawerDragging = true
                                        currentOnDrawerDrag(dy + drawerSlop)
                                        primary.consume()
                                    }
                                }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                        if (drawerDragging) {
                            drawerSettled = true
                            currentOnDrawerDragEnd(velocity.calculateVelocity().y)
                            lastBackgroundTap = 0L
                            return@awaitEachGesture
                        }
                    } finally {
                        if (drawerDragging && !drawerSettled) currentOnDrawerDragEnd(0f)
                    }
                    val dx = end.x - start.x
                    val dy = end.y - start.y
                    val gestureSettings = currentSettings
                    when {
                        !consumed && sawMultiplePointers && pinchDetected && !invalidPinch -> {
                            currentOnGesture(gestureSettings.pinchAction)
                            lastBackgroundTap = 0L
                            lastTapPosition = Offset.Unspecified
                        }
                        !consumed && !sawMultiplePointers && abs(dy) > swipeThreshold && abs(dy) > abs(dx) * 1.5f -> {
                            currentOnGesture(if (dy < 0) gestureSettings.swipeUpAction else gestureSettings.swipeDownAction)
                            lastBackgroundTap = 0L
                            lastTapPosition = Offset.Unspecified
                        }
                        !consumed && !sawMultiplePointers && abs(dx) < tapSlop && abs(dy) < tapSlop -> {
                            val nearby = lastTapPosition != Offset.Unspecified &&
                                (start - lastTapPosition).getDistance() <= doubleTapDistance
                            if (nearby && eventTime - lastBackgroundTap in 1..350) {
                                currentOnGesture(gestureSettings.doubleTapAction)
                                lastBackgroundTap = 0L
                                lastTapPosition = Offset.Unspecified
                            } else {
                                lastBackgroundTap = eventTime
                                lastTapPosition = start
                            }
                        }
                    }
                }
            },
    ) {
        VisualBackdrop(
            style = settings.visualStyle,
            modifier = Modifier.fillMaxSize()
                .pointerInput(editMode) {
                    // In edit mode, tapping the Home preview (not the tray) finishes editing.
                    if (editMode) detectTapGestures { onExitEditMode() }
                }
                .graphicsLayer {
                    scaleX = previewScale
                    scaleY = previewScale
                    val progress = ((1f - previewScale) / .1f).coerceIn(0f, 1f)
                    translationY = previewLift * progress
                    shadowElevation = 28.dp.toPx() * progress
                    shape = RoundedCornerShape(34.dp)
                    clip = progress > 0f
                },
        ) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (settings.clockStyle != ClockStyle.HIDDEN || settings.layoutLocked) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
                            .onGloballyPositioned { interactiveBounds["clock"] = it.boundsInRoot() },
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (settings.clockStyle != ClockStyle.HIDDEN) {
                            LauncherClock(
                                style = settings.clockStyle,
                                alignment = settings.clockAlignment,
                                timeFormat = settings.clockTimeFormat,
                                font = settings.clockFont,
                                size = settings.clockSize,
                                showDate = settings.showDate,
                                showSeconds = settings.showSeconds,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }
                        if (settings.layoutLocked) {
                            Surface(shape = CircleShape, color = tokens.surface, border = BorderStroke(1.dp, tokens.border)) {
                                Icon(Icons.Default.Lock, contentDescription = "Home layout locked", tint = tokens.foreground, modifier = Modifier.padding(10.dp).size(18.dp))
                            }
                        }
                    }
                }
                when {
                    state.catalog.loading && state.catalog.apps.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        CircularProgressIndicator(color = tokens.accent)
                    }
                    state.catalog.errorMessage != null && state.catalog.apps.isEmpty() -> HomeEmpty(
                        state.catalog.errorMessage,
                        settings.visualStyle,
                        Modifier.weight(1f),
                        onRefreshApps,
                    )
                    else -> HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = pagerScrollEnabled,
                    ) { page ->
                        HomePage(
                            items = itemsByPage[page].orEmpty(),
                            appsById = state.appsById,
                            settings = settings,
                            appearance = appearance,
                            iconLoader = iconLoader,
                            interactionsEnabled = !editMode,
                            showRemoveWidget = editMode && !settings.layoutLocked,
                            onLaunch = onLaunch,
                            onAppMenu = onAppMenu,
                            onOpenFolder = onOpenFolder,
                            onMoveItem = onMoveItem,
                            onCreateWidgetView = onCreateWidgetView,
                            onUpdateWidgetSize = onUpdateWidgetSize,
                            onRemoveWidget = onRemoveWidget,
                            onBounds = { id, bounds -> interactiveBounds[id] = bounds },
                            onRemoveBounds = { id -> interactiveBounds.remove(id) },
                        )
                    }
                }
                if (settings.pageCount > 1) PageIndicator(settings.pageCount, pagerState.currentPage, settings.visualStyle)
                if (settings.homeSearchBar) {
                    HomeSearchBar(
                        style = settings.visualStyle,
                        onClick = onOpenSearch,
                        modifier = Modifier.onGloballyPositioned { interactiveBounds["search"] = it.boundsInRoot() },
                    )
                }
                Dock(
                    apps = settings.dockApps.mapNotNull(state.appsById::get),
                    style = settings.visualStyle,
                    appearance = appearance,
                    iconLoader = iconLoader,
                    onLaunch = dockLaunch,
                    onAppMenu = onAppMenu,
                    onOpenDrawer = onOpenDrawer,
                    onBounds = { interactiveBounds["dock"] = it },
                )
            }
        }
        AnimatedVisibility(
            visible = editMode,
            modifier = Modifier.align(Alignment.BottomCenter).onGloballyPositioned { interactiveBounds["edit-tray"] = it.boundsInRoot() },
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            HomeCustomizeTray(
                selectedStyle = settings.visualStyle,
                layoutLocked = settings.layoutLocked,
                onStyle = onVisualStyle,
                onWallpaper = onWallpaper,
                onWidgets = onAddWidget,
                onLayoutAndClock = onOpenSettings,
                onDone = onExitEditMode,
            )
        }
    }
}

private enum class DropKind { MOVE, GROUP, BLOCKED }
private data class DropTarget(val itemId: String, val cell: Int, val kind: DropKind)

@Composable
private fun HomePage(
    items: List<HomeItem>,
    appsById: Map<AppId, InstalledApp>,
    settings: LauncherSettings,
    appearance: IconAppearance,
    iconLoader: AppIconLoader,
    interactionsEnabled: Boolean,
    showRemoveWidget: Boolean,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    onOpenFolder: (HomeItem) -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onBounds: (String, Rect) -> Unit,
    onRemoveBounds: (String) -> Unit,
) {
    val columns = settings.columns
    val rows = settings.rows
    val tokens = visualTokens(settings.visualStyle)
    var dropTarget by remember { mutableStateOf<DropTarget?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows
        val density = LocalDensity.current
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }

        dropTarget?.let { target ->
            val color = when (target.kind) {
                DropKind.MOVE -> Color.White.copy(alpha = .16f)
                DropKind.GROUP -> tokens.accent.copy(alpha = .38f)
                DropKind.BLOCKED -> Color(0x55FF5A5A)
            }
            Box(
                Modifier.offset(x = cellWidth * (target.cell % columns), y = cellHeight * (target.cell / columns))
                    .size(cellWidth, cellHeight)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(color)
                    .border(1.5.dp, Color.White.copy(alpha = .45f), RoundedCornerShape(22.dp)),
            )
        }

        items.forEach { item ->
            key(item.id) {
                DisposableEffect(item.id) { onDispose { onRemoveBounds(item.id) } }
                val column = item.cell % columns
                val row = item.cell / columns
                val spanX = item.layoutSpanX(columns)
                val spanY = item.layoutSpanY(rows)
                val itemWidth = cellWidth * spanX
                val itemHeight = cellHeight * spanY
                Box(
                    Modifier.offset(x = cellWidth * column, y = cellHeight * row)
                        .size(itemWidth, itemHeight)
                        .zIndex(if (dropTarget?.itemId == item.id) 1f else 0f)
                        .onGloballyPositioned { onBounds(item.id, it.boundsInRoot()) },
                    contentAlignment = Alignment.Center,
                ) {
                    val onTarget: (Int?) -> Unit = { cell ->
                        dropTarget = cell?.let { DropTarget(item.id, it, classifyDrop(items, item, it, columns, rows)) }
                    }
                    when (item.type) {
                        HomeItemType.APP -> item.appIds.firstOrNull()?.let(appsById::get)?.let { app ->
                            val tracker = rememberLaunchSourceTracker()
                            HomeItemGestures(
                                item = item,
                                columns = columns,
                                rows = rows,
                                enabled = interactionsEnabled,
                                canDrag = !settings.layoutLocked,
                                hapticsEnabled = settings.haptics,
                                cellWidthPx = cellWidthPx,
                                cellHeightPx = cellHeightPx,
                                label = app.label,
                                onTap = { onLaunch(app, tracker.source()) },
                                onMenu = { onAppMenu(app) },
                                onTarget = onTarget,
                                onMove = { dc, dr -> onMoveItem(item.id, dc, dr) },
                                modifier = Modifier.trackLaunchSource(tracker),
                            ) {
                                AppTile(
                                    app = app,
                                    iconLoader = iconLoader,
                                    iconSize = settings.iconSize.dp,
                                    appearance = appearance,
                                    showLabel = settings.showHomeLabels,
                                )
                            }
                        }
                        HomeItemType.FOLDER -> HomeItemGestures(
                            item = item,
                            columns = columns,
                            rows = rows,
                            enabled = interactionsEnabled,
                            canDrag = !settings.layoutLocked,
                            hapticsEnabled = settings.haptics,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            label = "${item.title.ifBlank { "Folder" }} folder",
                            onTap = { onOpenFolder(item) },
                            onMenu = { onOpenFolder(item) },
                            onTarget = onTarget,
                            onMove = { dc, dr -> onMoveItem(item.id, dc, dr) },
                        ) {
                            FolderTile(
                                folder = item,
                                apps = item.appIds.mapNotNull(appsById::get),
                                iconLoader = iconLoader,
                                iconSize = settings.iconSize.dp,
                                appearance = appearance,
                                showLabel = settings.showHomeLabels,
                            )
                        }
                        HomeItemType.WIDGET -> WidgetCell(
                            item = item,
                            showRemove = showRemoveWidget,
                            widthDp = itemWidth.value.roundToInt(),
                            heightDp = itemHeight.value.roundToInt(),
                            onCreateWidgetView = onCreateWidgetView,
                            onUpdateWidgetSize = onUpdateWidgetSize,
                            onRemove = { onRemoveWidget(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Mirrors [com.rumb.launcher.LauncherViewModel.moveItem] so the highlight predicts the drop result. */
private fun classifyDrop(items: List<HomeItem>, item: HomeItem, cell: Int, columns: Int, rows: Int): DropKind {
    if (cell == item.cell) return DropKind.MOVE
    val occupant = items.firstOrNull { it.id != item.id && cell in occupiedCells(it, columns, rows) } ?: return DropKind.MOVE
    return if (item.type == HomeItemType.APP && occupant.cell == cell && occupant.type != HomeItemType.WIDGET) {
        DropKind.GROUP
    } else DropKind.BLOCKED
}

private enum class PressOutcome { TAP, CANCEL }

/**
 * One gesture model for Home items: tap opens, long-press and release shows the item menu,
 * and long-press then move drags the item across the grid.
 */
@Composable
private fun HomeItemGestures(
    item: HomeItem,
    columns: Int,
    rows: Int,
    enabled: Boolean,
    canDrag: Boolean,
    hapticsEnabled: Boolean,
    cellWidthPx: Float,
    cellHeightPx: Float,
    label: String,
    onTap: () -> Unit,
    onMenu: () -> Unit,
    onTarget: (Int?) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var dragOffset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember(item.id) { mutableStateOf(false) }
    var pressed by remember(item.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            dragging -> 1.12f
            pressed -> .9f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = .62f, stiffness = Spring.StiffnessMedium),
        label = "home item scale",
    )
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMenu by rememberUpdatedState(onMenu)
    val currentOnTarget by rememberUpdatedState(onTarget)
    val currentOnMove by rememberUpdatedState(onMove)
    val startColumn = item.cell % columns
    val startRow = item.cell / columns

    Box(
        modifier.fillMaxSize()
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                scaleX = scale
                scaleY = scale
            }
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                onClick(label = "Open") { currentOnTap(); true }
                onLongClick(label = "More options") { currentOnMenu(); true }
            }
            .pointerInput(item.id, enabled, canDrag, cellWidthPx, cellHeightPx, startColumn, startRow) {
                if (!enabled) return@pointerInput
                fun targetCell(total: Offset): Int {
                    val column = (startColumn + (total.x / cellWidthPx).roundToInt()).coerceIn(0, columns - 1)
                    val row = (startRow + (total.y / cellHeightPx).roundToInt()).coerceIn(0, rows - 1)
                    return row * columns + column
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var result = PressOutcome.CANCEL
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                result = PressOutcome.TAP
                                break
                            }
                            val movedTooFar = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            if (change.isConsumed || movedTooFar || event.changes.count { it.pressed } > 1) break
                        }
                        result
                    }
                    pressed = false
                    when (outcome) {
                        PressOutcome.TAP -> {
                            currentOnTap()
                            return@awaitEachGesture
                        }
                        PressOutcome.CANCEL -> return@awaitEachGesture
                        null -> Unit
                    }
                    if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var moved = false
                    var total = Offset.Zero
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            total += change.position - change.previousPosition
                            change.consume()
                            if (!moved && canDrag && total.getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                                dragging = true
                            }
                            if (moved) {
                                dragOffset = total
                                currentOnTarget(targetCell(total))
                            }
                        }
                        if (moved) {
                            val target = targetCell(total)
                            currentOnMove(target % columns - startColumn, target / columns - startRow)
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            currentOnMenu()
                        }
                    } finally {
                        dragging = false
                        dragOffset = Offset.Zero
                        currentOnTarget(null)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun WidgetCell(
    item: HomeItem,
    showRemove: Boolean,
    widthDp: Int,
    heightDp: Int,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemove: () -> Unit,
) {
    val widgetId = item.widgetId
    val provider = item.widgetProvider
    val hostView = remember(widgetId, provider) {
        if (widgetId == null || provider == null) null else onCreateWidgetView(widgetId, provider)
    }
    // Report size only when it changes. Each report makes the provider re-render the widget.
    LaunchedEffect(hostView, widthDp, heightDp) {
        hostView?.let { onUpdateWidgetSize(it, (widthDp - 8).coerceAtLeast(1), (heightDp - 8).coerceAtLeast(1)) }
    }
    Box(Modifier.fillMaxSize().padding(4.dp)) {
        if (hostView == null) {
            Surface(Modifier.fillMaxSize(), color = Color.Black.copy(.54f), shape = RoundedCornerShape(22.dp)) {
                Box(Modifier.fillMaxSize().padding(12.dp), Alignment.Center) {
                    Text("Widget unavailable", color = Color.White.copy(.88f), textAlign = TextAlign.Center)
                }
            }
        } else {
            AndroidView(
                factory = { hostView },
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)),
            )
        }
        AnimatedVisibility(
            visible = showRemove,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(.72f)) {
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove widget", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeSearchBar(style: VisualStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = visualTokens(style)
    Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().height(50.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Search apps and the web", onClick = onClick),
            shape = CircleShape,
            color = tokens.surface,
            border = BorderStroke(1.dp, tokens.border),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = tokens.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Search apps and web",
                    color = tokens.mutedForeground,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Dock(
    apps: List<InstalledApp>,
    style: VisualStyle,
    appearance: IconAppearance,
    iconLoader: AppIconLoader,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onBounds: (Rect) -> Unit,
) {
    val tokens = visualTokens(style)
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        VisualDockSurface(
            style = style,
            modifier = Modifier.wrapContentWidth().onGloballyPositioned { onBounds(it.boundsInRoot()) },
        ) {
            Row(
                Modifier.wrapContentWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                apps.forEach { app ->
                    AppTile(
                        app = app,
                        iconLoader = iconLoader,
                        iconSize = 44.dp,
                        appearance = appearance,
                        showLabel = false,
                        modifier = Modifier.width(54.dp),
                        onClick = { source -> onLaunch(app, source) },
                        onLongClick = { onAppMenu(app) },
                    )
                }
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                        .clickable(role = Role.Button, onClickLabel = "Open all apps", onClick = onOpenDrawer)
                        .semantics { contentDescription = "All apps" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(44.dp).background(
                            Brush.linearGradient(listOf(tokens.accent, tokens.accentSecondary)),
                            RoundedCornerShape(if (style == VisualStyle.PURE_MINIMAL) 13.dp else 16.dp),
                        ),
                        Alignment.Center,
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, selectedPage: Int, style: VisualStyle) {
    val tokens = visualTokens(style)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = tokens.surface, shape = CircleShape, modifier = Modifier.padding(top = 3.dp)) {
            Row(
                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { page ->
                    val selected = page == selectedPage
                    val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "page dot width")
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(6.dp).width(width)
                            .background(if (selected) tokens.accent else tokens.mutedForeground.copy(.45f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeEmpty(message: String, style: VisualStyle, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    val tokens = visualTokens(style)
    Box(modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        VisualSurface(style, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = tokens.foreground, textAlign = TextAlign.Center)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
            }
        }
    }
}
