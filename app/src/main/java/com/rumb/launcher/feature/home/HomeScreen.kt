@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rumb.launcher.feature.home

import android.appwidget.AppWidgetHostView
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.layoutSpanX
import com.rumb.launcher.domain.layoutSpanY
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.FolderTile
import com.rumb.launcher.ui.components.LauncherClock
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onWallpaper: () -> Unit,
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
    val appsById = remember(state.catalog.apps) { state.catalog.apps.associateBy { it.id } }
    val pagerState = rememberPagerState(pageCount = { settings.pageCount })
    val gestureDensity = LocalDensity.current
    val swipeThreshold = with(gestureDensity) { 120.dp.toPx() }
    val tapSlop = with(gestureDensity) { 24.dp.toPx() }
    val doubleTapDistance = with(gestureDensity) { 48.dp.toPx() }
    val pinchThreshold = with(gestureDensity) { 36.dp.toPx() }
    var lastBackgroundTap by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Unspecified) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .30f)).safeDrawingPadding()
            .pointerInput(
                settings.doubleTapAction,
                settings.swipeUpAction,
                settings.swipeDownAction,
                settings.pinchAction,
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    val start = down.position
                    var end = start
                    var upTime = down.uptimeMillis
                    var consumed = down.isConsumed
                    var sawMultiplePointers = false
                    var invalidPinch = false
                    var initialPinchSpan: Float? = null
                    var pinchDetected = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
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
                            upTime = change.uptimeMillis
                        }
                        if (event.changes.none { it.pressed }) break
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
                            if (nearby && upTime - lastBackgroundTap in 1..350) {
                                onGesture(settings.doubleTapAction)
                                lastBackgroundTap = 0L
                                lastTapPosition = Offset.Unspecified
                            } else {
                                lastBackgroundTap = upTime
                                lastTapPosition = start
                            }
                        }
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.Top) {
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
                    Surface(shape = CircleShape, color = Color.Black.copy(.35f)) {
                        Icon(Icons.Default.Lock, contentDescription = "Home layout locked", tint = Color.White, modifier = Modifier.padding(9.dp).size(18.dp))
                    }
                }
            }
            when {
                state.catalog.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                state.catalog.errorMessage != null && state.catalog.apps.isEmpty() -> HomeEmpty(
                    state.catalog.errorMessage,
                    Modifier.weight(1f),
                    onRefreshApps,
                )
                else -> HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    HomePage(
                        items = settings.homeItems.filter { it.page == page },
                        appsById = appsById,
                        state = state,
                        iconLoader = iconLoader,
                        onLaunch = onLaunch,
                        onOpenFolder = onOpenFolder,
                        onMoveItem = onMoveItem,
                        onCreateWidgetView = onCreateWidgetView,
                        onUpdateWidgetSize = onUpdateWidgetSize,
                        onRemoveWidget = onRemoveWidget,
                    )
                }
            }
            if (settings.pageCount > 1) Row(
                Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center,
            ) {
                repeat(settings.pageCount) { page ->
                    Box(
                        Modifier.padding(3.dp).size(if (page == pagerState.currentPage) 8.dp else 6.dp)
                            .background(Color.White.copy(if (page == pagerState.currentPage) .95f else .42f), CircleShape),
                    )
                }
            }
            Dock(
                apps = settings.dockApps.mapNotNull(appsById::get),
                state = state,
                iconLoader = iconLoader,
                onLaunch = onLaunch,
                onOpenDrawer = onOpenDrawer,
                onOpenSettings = onOpenSettings,
                onWallpaper = onWallpaper,
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
    onLaunch: (InstalledApp) -> Unit,
    onOpenFolder: (HomeItem) -> Unit,
    onMoveItem: (String, Int, Int) -> Unit,
    onCreateWidgetView: (Int, String) -> AppWidgetHostView?,
    onUpdateWidgetSize: (AppWidgetHostView, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
) {
    val settings = state.settings
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        val cellWidth = maxWidth / settings.columns
        val cellHeight = maxHeight / settings.rows
        val density = LocalDensity.current
        items.sortedBy { if (it.type == HomeItemType.WIDGET) 0 else 1 }.forEach { item ->
            key(item.id) {
                val column = item.cell % settings.columns
                val row = item.cell / settings.columns
                val spanX = item.layoutSpanX(settings.columns)
                val spanY = item.layoutSpanY(settings.rows)
                val itemWidth = cellWidth * spanX
                val itemHeight = cellHeight * spanY
                Box(
                    Modifier.offset(x = cellWidth * column, y = cellHeight * row)
                        .width(itemWidth).height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    when (item.type) {
                        HomeItemType.APP -> item.appIds.firstOrNull()?.let(appsById::get)?.let { app ->
                            DraggableHomeItem(
                                item = item,
                                locked = settings.layoutLocked,
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
                                    badgeCount = state.notificationCounts[app.packageName] ?: 0,
                                    homeStyle = true,
                                    onClick = { onLaunch(app) },
                                    onLongClick = null,
                                )
                            }
                        }
                        HomeItemType.FOLDER -> DraggableHomeItem(
                            item,
                            settings.layoutLocked,
                            with(density) { cellWidth.toPx() },
                            with(density) { cellHeight.toPx() },
                            onMoveItem,
                        ) {
                            FolderTile(item, settings.iconSize.dp, onClick = { onOpenFolder(item) }, onLongClick = null)
                        }
                        HomeItemType.WIDGET -> WidgetCell(
                            item = item,
                            locked = settings.layoutLocked,
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
    Box(Modifier.fillMaxSize().padding(3.dp)) {
        if (hostView == null) {
            Surface(Modifier.fillMaxSize(), color = Color.Black.copy(.48f), shape = RoundedCornerShape(18.dp)) {
                Box(Modifier.fillMaxSize().padding(12.dp), Alignment.Center) {
                    Text("Widget unavailable", color = Color.White.copy(.86f), textAlign = TextAlign.Center)
                }
            }
        } else {
            AndroidView(
                factory = { hostView },
                modifier = Modifier.fillMaxSize(),
                update = {
                    onUpdateWidgetSize(it, (widthDp - 6).coerceAtLeast(1), (heightDp - 6).coerceAtLeast(1))
                },
            )
        }
        if (!locked) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                shape = CircleShape,
                color = Color.Black.copy(.68f),
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
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
    val scale by animateFloatAsState(if (dx != 0f || dy != 0f) 1.08f else 1f, label = "drag scale")
    Box(
        Modifier.fillMaxSize().graphicsLayer { translationX = dx; translationY = dy; scaleX = scale; scaleY = scale }
            .pointerInput(item.id, locked) {
                if (!locked) detectDragGesturesAfterLongPress(
                    onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                    onDragCancel = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        onMove(item.id, (dx / cellWidthPx).roundToInt(), (dy / cellHeightPx).roundToInt())
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
    apps: List<InstalledApp>, state: LauncherUiState, iconLoader: AppIconLoader,
    onLaunch: (InstalledApp) -> Unit, onOpenDrawer: () -> Unit, onOpenSettings: () -> Unit, onWallpaper: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(14.dp), color = Color.Black.copy(.46f), shape = RoundedCornerShape(30.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (apps.isEmpty()) {
                DockAction(Icons.Default.Palette, "Wallpaper", onWallpaper)
                DockAction(Icons.Default.Apps, "All apps", onOpenDrawer)
                DockAction(Icons.Default.Settings, "Settings", onOpenSettings)
            } else {
                apps.forEach { app -> AppTile(
                    app, iconLoader, 44.dp, state.settings.iconShape, state.settings.themedIcons,
                    state.notificationCounts[app.packageName] ?: 0, true, Modifier.weight(1f), { onLaunch(app) }, null,
                ) }
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Apps, "All apps", tint = Color.White) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
            }
        }
    }
}

@Composable private fun DockAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, action: () -> Unit) {
    TextButton(onClick = action) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    } }
}

@Composable
private fun HomeEmpty(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.White.copy(.86f), textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
        }
    }
}
