@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rumb.launcher.feature.home

import android.appwidget.AppWidgetHostView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.domain.layoutSpanX
import com.rumb.launcher.domain.layoutSpanY
import com.rumb.launcher.feature.customize.HomeCustomizeTray
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.FolderTile
import com.rumb.launcher.ui.components.LauncherClock
import com.rumb.launcher.ui.components.VisualBackdrop
import com.rumb.launcher.ui.components.VisualDockSurface
import com.rumb.launcher.ui.components.VisualSurface
import com.rumb.launcher.ui.components.visualTokens
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HomeScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    editMode: Boolean,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onVisualStyle: (VisualStyle) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onOpenFolder: (HomeItem) -> Unit,
    onRefreshApps: () -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onGesture: (GestureAction) -> Unit,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
) {
    val settings = state.settings
    val tokens = visualTokens(settings.visualStyle)
    val appsById = remember(state.catalog.apps) { state.catalog.apps.associateBy { it.id } }
    val pagerState = rememberPagerState(pageCount = { settings.pageCount })
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val interactiveBounds = remember { mutableStateMapOf<String, Rect>() }
    val swipeThreshold = with(density) { 120.dp.toPx() }
    val tapSlop = with(density) { 24.dp.toPx() }
    val doubleTapDistance = with(density) { 48.dp.toPx() }
    val pinchThreshold = with(density) { 36.dp.toPx() }
    var lastBackgroundTap by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Unspecified) }
    val previewScale by animateFloatAsState(if (editMode) .91f else 1f, label = "home preview scale")
    val previewLift = with(density) { if (editMode) (-52).dp.toPx() else 0f }

    Box(
        Modifier.fillMaxSize().pointerInput(
            editMode,
            settings.doubleTapAction,
            settings.swipeUpAction,
            settings.swipeDownAction,
            settings.pinchAction,
            pagerState.currentPage,
        ) {
            if (editMode) return@pointerInput
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
                var released = false
                val deadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
                while (!released) {
                    val waitMillis = (deadline - eventTime).coerceAtLeast(1L)
                    val event = withTimeoutOrNull(waitMillis) { awaitPointerEvent(PointerEventPass.Final) }
                    if (event == null) {
                        val overInteractiveContent = interactiveBounds.values.any { it.contains(start) }
                        if (!consumed && !movedBeyondTap && !sawMultiplePointers && !overInteractiveContent) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnterEditMode()
                        }
                        return@awaitEachGesture
                    }
                    consumed = consumed || event.changes.any { it.isConsumed }
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
                    event.changes.firstOrNull { it.id == down.id }?.let { change ->
                        end = change.position
                        eventTime = change.uptimeMillis
                        if ((end - start).getDistance() >= tapSlop) movedBeyondTap = true
                    }
                    released = event.changes.none { it.pressed }
                }
                val dx = end.x - start.x
                val dy = end.y - start.y
                when {
                    !consumed && sawMultiplePointers && pinchDetected && !invalidPinch -> {
                        onGesture(settings.pinchAction)
                        lastBackgroundTap = 0L
                        lastTapPosition = Offset.Unspecified
                    }
                    !consumed && !sawMultiplePointers && abs(dy) > swipeThreshold && abs(dy) > abs(dx) * 1.5f -> {
                        onGesture(if (dy < 0) settings.swipeUpAction else settings.swipeDownAction)
                        lastBackgroundTap = 0L
                        lastTapPosition = Offset.Unspecified
                    }
                    !consumed && !sawMultiplePointers && abs(dx) < tapSlop && abs(dy) < tapSlop -> {
                        val nearby = lastTapPosition != Offset.Unspecified &&
                            (start - lastTapPosition).getDistance() <= doubleTapDistance
                        if (nearby && eventTime - lastBackgroundTap in 1..350) {
                            onGesture(settings.doubleTapAction)
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
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = previewScale
                scaleY = previewScale
                translationY = previewLift
                shadowElevation = if (editMode) 28.dp.toPx() else 0f
                shape = RoundedCornerShape(if (editMode) 34.dp else 0.dp)
                clip = editMode
            },
        ) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 26.dp, bottom = 8.dp)
                        .onGloballyPositioned { interactiveBounds["clock"] = it.boundsInRoot() },
                    verticalAlignment = Alignment.Top,
                ) {
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
                    if (settings.layoutLocked) {
                        Surface(shape = CircleShape, color = tokens.surface, border = androidx.compose.foundation.BorderStroke(1.dp, tokens.border)) {
                            Icon(Icons.Default.Lock, contentDescription = "Home layout locked", tint = tokens.foreground, modifier = Modifier.padding(10.dp).size(18.dp))
                        }
                    }
                }
                when {
                    state.catalog.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        CircularProgressIndicator(color = tokens.accent)
                    }
                    state.catalog.errorMessage != null && state.catalog.apps.isEmpty() -> HomeEmpty(
                        state.catalog.errorMessage,
                        settings.visualStyle,
                        Modifier.weight(1f),
                        onRefreshApps,
                    )
                    else -> HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        HomePage(
                            items = settings.homeItems.filter { it.page == page },
                            appsById = appsById,
                            state = state,
                            iconLoader = iconLoader,
                            interactionsEnabled = !editMode,
                            onLaunch = onLaunch,
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
                Dock(
                    apps = settings.dockApps.mapNotNull(appsById::get),
                    state = state,
                    iconLoader = iconLoader,
                    onLaunch = onLaunch,
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

@Composable
private fun HomePage(
    items: List<HomeItem>,
    appsById: Map<com.rumb.launcher.domain.AppId, InstalledApp>,
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    interactionsEnabled: Boolean,
    onLaunch: (InstalledApp) -> Unit,
    onOpenFolder: (HomeItem) -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onBounds: (String, Rect) -> Unit,
    onRemoveBounds: (String) -> Unit,
) {
    val settings = state.settings
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        val cellWidth = maxWidth / settings.columns
        val cellHeight = maxHeight / settings.rows
        val density = LocalDensity.current
        if (items.isEmpty()) {
            VisualSurface(
                settings.visualStyle,
                Modifier.align(Alignment.Center).padding(28.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = 8.dp,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Make this space yours", color = visualTokens(settings.visualStyle).foreground, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Long-press empty space to choose a look, wallpaper, widgets, and layout.",
                        color = visualTokens(settings.visualStyle).mutedForeground,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        items.sortedBy { if (it.type == HomeItemType.WIDGET) 0 else 1 }.forEach { item ->
            key(item.id) {
                DisposableEffect(item.id) { onDispose { onRemoveBounds(item.id) } }
                val column = item.cell % settings.columns
                val row = item.cell / settings.columns
                val spanX = item.layoutSpanX(settings.columns)
                val spanY = item.layoutSpanY(settings.rows)
                val itemWidth = cellWidth * spanX
                val itemHeight = cellHeight * spanY
                Box(
                    Modifier.offset(x = cellWidth * column, y = cellHeight * row)
                        .width(itemWidth).height(itemHeight)
                        .onGloballyPositioned { onBounds(item.id, it.boundsInRoot()) },
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.type) {
                        HomeItemType.APP -> item.appIds.firstOrNull()?.let(appsById::get)?.let { app ->
                            DraggableHomeItem(
                                item = item,
                                locked = settings.layoutLocked || !interactionsEnabled,
                                cellWidthPx = with(density) { cellWidth.toPx() },
                                cellHeightPx = with(density) { cellHeight.toPx() },
                                onMove = onMoveItem,
                            ) {
                                AppTile(
                                    app = app,
                                    iconLoader = iconLoader,
                                    iconSize = settings.iconSize.dp,
                                    iconShape = settings.iconShape,
                                    themed = settings.themedIcons,
                                    homeStyle = true,
                                    onClick = { if (interactionsEnabled) onLaunch(app) },
                                    onLongClick = null,
                                )
                            }
                        }
                        HomeItemType.FOLDER -> DraggableHomeItem(
                            item,
                            settings.layoutLocked || !interactionsEnabled,
                            with(density) { cellWidth.toPx() },
                            with(density) { cellHeight.toPx() },
                            onMoveItem,
                        ) {
                            FolderTile(item, settings.iconSize.dp, onClick = { if (interactionsEnabled) onOpenFolder(item) }, onLongClick = null)
                        }
                        HomeItemType.WIDGET -> WidgetCell(
                            item = item,
                            locked = settings.layoutLocked || !interactionsEnabled,
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

@Composable
private fun WidgetCell(
    item: HomeItem,
    locked: Boolean,
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
                update = { onUpdateWidgetSize(it, (widthDp - 8).coerceAtLeast(1), (heightDp - 8).coerceAtLeast(1)) },
            )
        }
        if (!locked) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                shape = CircleShape,
                color = Color.Black.copy(.72f),
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove widget", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DraggableHomeItem(
    item: HomeItem,
    locked: Boolean,
    cellWidthPx: Float,
    cellHeightPx: Float,
    onMove: (String, Int, Int) -> Unit,
    content: @Composable () -> Unit,
) {
    var dx by remember(item.id) { mutableFloatStateOf(0f) }
    var dy by remember(item.id) { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (dx != 0f || dy != 0f) 1.08f else 1f, label = "drag scale")
    Box(
        Modifier.fillMaxSize().graphicsLayer { translationX = dx; translationY = dy; scaleX = scale; scaleY = scale }
            .pointerInput(item.id, locked) {
                if (!locked) detectDragGesturesAfterLongPress(
                    onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                    onDragCancel = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        onMove(item.id, (dx / cellWidthPx).roundToInt(), (dy / cellHeightPx).roundToInt())
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dx = 0f
                        dy = 0f
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Dock(
    apps: List<InstalledApp>,
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    onLaunch: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onBounds: (Rect) -> Unit,
) {
    val style = state.settings.visualStyle
    val tokens = visualTokens(style)
    VisualDockSurface(
        style = style,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
            .onGloballyPositioned { onBounds(it.boundsInRoot()) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                AppTile(
                    app,
                    iconLoader,
                    45.dp,
                    state.settings.iconShape,
                    state.settings.themedIcons,
                    true,
                    Modifier.weight(1f),
                    { onLaunch(app) },
                    null,
                )
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).semantics { contentDescription = "Open all apps" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(52.dp)) {
                    Box(
                        Modifier.size(46.dp).background(
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(tokens.accent, tokens.accentSecondary)),
                            RoundedCornerShape(if (style == VisualStyle.PURE_MINIMAL) 14.dp else 18.dp),
                        ),
                        Alignment.Center,
                    ) { Icon(Icons.Default.Apps, contentDescription = "All apps", tint = Color.White, modifier = Modifier.size(24.dp)) }
                }
                Text("Apps", color = tokens.foreground, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (apps.isEmpty()) Text(
        "Long-press empty space to personalize",
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        color = tokens.mutedForeground.copy(alpha = .78f),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PageIndicator(pageCount: Int, selectedPage: Int, style: VisualStyle) {
    val tokens = visualTokens(style)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = tokens.surface, shape = CircleShape, modifier = Modifier.padding(top = 3.dp)) {
            Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pageCount) { page ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(if (page == selectedPage) 16.dp else 6.dp)
                            .background(if (page == selectedPage) tokens.accent else tokens.mutedForeground.copy(.45f), CircleShape),
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
