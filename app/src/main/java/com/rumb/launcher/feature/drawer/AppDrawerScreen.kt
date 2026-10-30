package com.rumb.launcher.feature.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.IconAppearance
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.iconAppearance
import com.rumb.launcher.platform.apps.LaunchSource
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.platform.widgets.InstalledWidgetProvider
import com.rumb.launcher.platform.widgets.WidgetCatalogState
import com.rumb.launcher.platform.widgets.WidgetPreviewLoader
import com.rumb.launcher.ui.AppDragController
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.rememberLaunchSourceTracker
import com.rumb.launcher.ui.components.trackLaunchSource
import com.rumb.launcher.ui.components.visualTokens
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LibraryTab { APPS, WIDGETS }

private const val INDEX_LETTERS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private val SearchFieldColor = Color(0xFF19191C)
private val CardColor = Color(0xFF161618)

@Composable
fun AppDrawerScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    widgetState: WidgetCatalogState,
    widgetPreviewLoader: WidgetPreviewLoader,
    isOpen: Boolean,
    focusSearch: Boolean,
    nestedScrollConnection: NestedScrollConnection,
    onHeaderDrag: (Float) -> Unit,
    onHeaderDragEnd: (Float) -> Unit,
    onSearchFocused: () -> Unit,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRefreshApps: () -> Unit,
    onRefreshWidgets: () -> Unit,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
    onRequestWidget: () -> Unit,
    onRequestWidgetProvider: (String) -> Unit,
    onWebSearch: (String) -> Unit,
    onStoreSearch: (String) -> Unit,
    dragController: AppDragController,
    onDragToHomeStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = visualTokens(state.settings.visualStyle)
    val dragActive = dragController.isDragging
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.APPS) }
    var ascending by rememberSaveable { mutableStateOf(true) }
    // The text field owns its text locally so typing never waits on the background search pipeline.
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnSearchFocused by rememberUpdatedState(onSearchFocused)
    val wantsFocus = focusSearch || state.settings.drawerAutoKeyboard

    LaunchedEffect(isOpen, wantsFocus, dragActive) {
        if (!isOpen && dragActive) {
            // The dragged tile must stay composed until the drop, so only dismiss the keyboard now.
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            return@LaunchedEffect
        }
        if (isOpen) {
            if (wantsFocus) {
                selectedTab = LibraryTab.APPS
                delay(120)
                runCatching { focusRequester.requestFocus() }
                currentOnSearchFocused()
            }
        } else {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            if (query.isNotEmpty()) {
                query = ""
                currentOnSearch("")
            }
            selectedTab = LibraryTab.APPS
            if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }.collect { scrolling -> if (scrolling) keyboard?.hide() }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                Modifier.fillMaxWidth().draggable(
                    state = rememberDraggableState { delta -> onHeaderDrag(delta) },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> onHeaderDragEnd(velocity) },
                ),
            ) {
                Box(
                    Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = .28f), CircleShape),
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home", tint = Color.White)
                    }
                    Text(
                        "Library",
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (selectedTab == LibraryTab.APPS) {
                        IconButton(
                            onClick = { ascending = !ascending },
                            modifier = Modifier.semantics {
                                contentDescription = if (ascending) "Sort apps Z to A" else "Sort apps A to Z"
                            },
                        ) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = tokens.accent)
                        }
                    } else {
                        IconButton(
                            onClick = onRefreshWidgets,
                            modifier = Modifier.semantics { contentDescription = "Refresh widgets" },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = tokens.accent)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    LibraryTabButton("Apps", selectedTab == LibraryTab.APPS, tokens.accent) { selectedTab = LibraryTab.APPS }
                    LibraryTabButton("Widgets", selectedTab == LibraryTab.WIDGETS, tokens.accent) {
                        focusManager.clearFocus()
                        selectedTab = LibraryTab.WIDGETS
                    }
                }
            }
            Column(Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
                if (selectedTab == LibraryTab.APPS) {
                    AppsLibrary(
                        state = state,
                        query = query,
                        onQueryChange = { value ->
                            query = value.take(80)
                            onSearch(query)
                        },
                        focusRequester = focusRequester,
                        gridState = gridState,
                        iconLoader = iconLoader,
                        ascending = ascending,
                        accent = tokens.accent,
                        onRefresh = onRefreshApps,
                        onLaunch = onLaunch,
                        onLongPress = onLongPress,
                        onWebSearch = onWebSearch,
                        onStoreSearch = onStoreSearch,
                        dragController = dragController,
                        onDragToHomeStarted = onDragToHomeStarted,
                    )
                } else {
                    WidgetsLibrary(
                        state = widgetState,
                        previewLoader = widgetPreviewLoader,
                        accent = tokens.accent,
                        onRefresh = onRefreshWidgets,
                        onSystemPicker = onRequestWidget,
                        onSelect = onRequestWidgetProvider,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LibraryTabButton(label: String, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    val indicatorWidth by animateDpAsState(if (isSelected) 56.dp else 0.dp, label = "tab indicator")
    val textColor by animateColorAsState(if (isSelected) Color.White else Color.White.copy(.55f), label = "tab color")
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Box(Modifier.padding(top = 10.dp).height(3.dp).width(indicatorWidth).background(accent, CircleShape))
    }
}

@Composable
private fun AppsLibrary(
    state: LauncherUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    iconLoader: AppIconLoader,
    ascending: Boolean,
    accent: Color,
    onRefresh: () -> Unit,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
    onWebSearch: (String) -> Unit,
    onStoreSearch: (String) -> Unit,
    dragController: AppDragController,
    onDragToHomeStarted: () -> Unit,
) {
    val searching = query.isNotBlank()
    val tileConfig = LibraryTileConfig(
        iconLoader = iconLoader,
        iconSize = state.settings.iconSize.dp,
        appearance = state.settings.iconAppearance,
        badges = state.badges,
        canDrag = !state.settings.layoutLocked,
        haptics = state.settings.haptics,
        dragController = dragController,
        onLaunch = onLaunch,
        onMenu = onLongPress,
        onDragStarted = onDragToHomeStarted,
    )
    // Results come from the background pipeline; until it catches up with the typed query, keep the last list.
    val apps = remember(state.filteredApps, state.apps, ascending, searching) {
        when {
            searching -> state.filteredApps
            ascending -> state.apps
            else -> state.apps.asReversed()
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text("Search apps and web", color = Color.White.copy(.5f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.White.copy(.7f))
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            val first = state.filteredApps.firstOrNull()
            if (searching && first != null) onLaunch(first, null) else if (searching) onWebSearch(query)
        }),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = SearchFieldColor,
            unfocusedContainerColor = SearchFieldColor,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = Color.White.copy(.14f),
            cursorColor = accent,
        ),
    )
    if (state.catalog.errorMessage != null && state.catalog.apps.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF251C1C)).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.catalog.errorMessage,
                modifier = Modifier.weight(1f),
                color = Color.White.copy(.78f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRefresh) { Text("Retry", color = accent) }
        }
    }
    when {
        state.catalog.loading && state.catalog.apps.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        !searching && apps.isEmpty() -> EmptyLibrary(
            message = state.catalog.errorMessage ?: "No launchable apps are available",
            showRetry = state.catalog.errorMessage != null,
            onRetry = onRefresh,
        )
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val columnCount = if (maxWidth < 600.dp) 4 else (maxWidth.value / 104f).toInt().coerceIn(5, 8)
            val scope = rememberCoroutineScope()
            val suggestions = if (!searching && ascending) state.suggestions.take(columnCount) else emptyList()
            val headerCount = if (suggestions.isEmpty()) 0 else suggestions.size + 2
            val buckets = remember(apps, searching) {
                if (searching) emptyMap() else buildMap<Char, Int> {
                    apps.forEachIndexed { index, app -> putIfAbsent(appBucket(app.label), index) }
                }
            }
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(end = if (searching) 0.dp else 27.dp),
                    contentPadding = PaddingValues(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (suggestions.isNotEmpty()) {
                        sectionHeader("header:suggested", "Suggested")
                        items(suggestions, key = { "suggested:${it.id.stableKey}" }, contentType = { "app" }) { app ->
                            LibraryTile(app, tileConfig)
                        }
                        sectionHeader("header:all", "All apps")
                    }
                    if (searching) {
                        item(key = "header:results", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                            Text(
                                if (apps.isEmpty()) "No apps match “$query”" else "${apps.size} ${if (apps.size == 1) "app" else "apps"}",
                                color = Color.White.copy(.6f),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                    items(apps, key = { it.id.stableKey }, contentType = { "app" }) { app ->
                        LibraryTile(app, tileConfig)
                    }
                    if (searching) {
                        item(key = "action:web", span = { GridItemSpan(maxLineSpan) }, contentType = "action") {
                            SearchActionRow(Icons.Default.Language, "Search the web for “$query”", accent) { onWebSearch(query) }
                        }
                        item(key = "action:store", span = { GridItemSpan(maxLineSpan) }, contentType = "action") {
                            SearchActionRow(Icons.Default.Shop, "Find “$query” in the app store", accent) { onStoreSearch(query) }
                        }
                    }
                }
                if (!searching) {
                    FastIndex(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp),
                        accent = accent,
                        available = buckets.keys,
                        onLetter = { letter ->
                            val index = buckets[letter] ?: nearestBucket(buckets, letter)
                            if (index != null) scope.launch { gridState.scrollToItem(headerCount + index) }
                        },
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.sectionHeader(key: String, title: String) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Text(
            title,
            color = Color.White.copy(.6f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
        )
    }
}

private class LibraryTileConfig(
    val iconLoader: AppIconLoader,
    val iconSize: androidx.compose.ui.unit.Dp,
    val appearance: IconAppearance,
    val badges: Set<String>,
    val canDrag: Boolean,
    val haptics: Boolean,
    val dragController: AppDragController,
    val onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    val onMenu: (InstalledApp) -> Unit,
    val onDragStarted: () -> Unit,
)

private enum class TilePress { TAP, CANCEL }

/** Holds the tile's layout coordinates so pointer positions can be mapped to root space. */
private class TileCoordinates {
    var value: LayoutCoordinates? = null
}

/**
 * Tap opens. Long-press and release opens the app sheet. Long-press and move lifts the app out of
 * the library: the sheet slides away and the app follows the finger until it is dropped on Home.
 */
@Composable
private fun LibraryTile(app: InstalledApp, config: LibraryTileConfig) {
    val haptics = LocalHapticFeedback.current
    val tracker = rememberLaunchSourceTracker()
    val coordinates = remember { TileCoordinates() }
    var pressed by remember { mutableStateOf(false) }
    val currentApp by rememberUpdatedState(app)
    val currentConfig by rememberUpdatedState(config)
    Box(
        Modifier.onPlaced { coordinates.value = it }
            .trackLaunchSource(tracker)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = "Open ${app.label}") { currentConfig.onLaunch(currentApp, tracker.source()); true }
                onLongClick(label = "More options") { currentConfig.onMenu(currentApp); true }
            }
            .pointerInput(app.id) {
                fun toRoot(local: Offset): Offset =
                    coordinates.value?.takeIf { it.isAttached }?.localToRoot(local) ?: local
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var result = TilePress.CANCEL
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                result = TilePress.TAP
                                break
                            }
                            val moved = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            if (change.isConsumed || moved || event.changes.count { it.pressed } > 1) break
                        }
                        result
                    }
                    pressed = false
                    when (outcome) {
                        TilePress.TAP -> {
                            currentConfig.onLaunch(currentApp, tracker.source())
                            return@awaitEachGesture
                        }
                        TilePress.CANCEL -> return@awaitEachGesture
                        null -> Unit
                    }
                    val config = currentConfig
                    if (config.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var dragging = false
                    var finished = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            if (!dragging && config.canDrag &&
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            ) {
                                dragging = true
                                config.dragController.start(currentApp, toRoot(change.position))
                                config.onDragStarted()
                            }
                            if (dragging) config.dragController.move(toRoot(change.position))
                        }
                        if (dragging) {
                            config.dragController.drop()
                            if (config.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            config.onMenu(currentApp)
                        }
                        finished = true
                    } finally {
                        if (dragging && !finished) config.dragController.cancel()
                    }
                }
            },
    ) {
        AppTile(
            app = app,
            iconLoader = config.iconLoader,
            iconSize = config.iconSize,
            appearance = config.appearance,
            labelColor = Color.White,
            labelShadow = false,
            badged = app.badgeKey in config.badges,
            pressed = pressed,
        )
    }
}

@Composable
private fun SearchActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).clip(RoundedCornerShape(20.dp))
            .background(CardColor).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tap or slide along the index to jump. A bubble shows the active letter while scrubbing. */
@Composable
private fun FastIndex(
    modifier: Modifier,
    accent: Color,
    available: Set<Char>,
    onLetter: (Char) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var active by remember { mutableStateOf<Char?>(null) }
    var activeY by remember { mutableFloatStateOf(0f) }
    val currentOnLetter by rememberUpdatedState(onLetter)
    Box(modifier) {
        Column(
            Modifier.fillMaxSize().background(Color.White.copy(alpha = if (active != null) .08f else 0f), CircleShape)
                .padding(vertical = 3.dp)
                .semantics { contentDescription = "Alphabet index" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun letterAt(y: Float): Char {
                            val index = (y / size.height * INDEX_LETTERS.length).toInt().coerceIn(0, INDEX_LETTERS.length - 1)
                            return INDEX_LETTERS[index]
                        }
                        fun select(y: Float) {
                            val letter = letterAt(y)
                            activeY = y.coerceIn(0f, size.height.toFloat())
                            if (letter != active) {
                                active = letter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnLetter(letter)
                            }
                        }
                        down.consume()
                        select(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            select(change.position.y)
                        }
                        active = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            INDEX_LETTERS.forEach { letter ->
                val enabled = letter in available
                Text(
                    letter.toString(),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = when {
                        letter == active -> Color.White
                        enabled -> accent
                        else -> Color.White.copy(.2f)
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        AnimatedVisibility(
            visible = active != null,
            enter = fadeIn() + scaleIn(initialScale = .7f),
            exit = fadeOut() + scaleOut(targetScale = .7f),
            modifier = Modifier.offset {
                val bubble = with(density) { 64.dp.roundToPx() }
                IntOffset(-bubble - with(density) { 14.dp.roundToPx() }, activeY.roundToInt() - bubble / 2)
            },
        ) {
            Box(
                Modifier.size(64.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (active ?: ' ').toString(),
                    color = Color.Black,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WidgetsLibrary(
    state: WidgetCatalogState,
    previewLoader: WidgetPreviewLoader,
    accent: Color,
    onRefresh: () -> Unit,
    onSystemPicker: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val groups = remember(state.providers) { state.providers.groupBy(InstalledWidgetProvider::providerAppLabel) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Widgets", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${state.providers.size} installed providers",
                color = Color.White.copy(.58f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onSystemPicker) { Text("System picker", color = accent) }
    }
    if (state.errorMessage != null && state.providers.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF251C1C)).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.errorMessage,
                modifier = Modifier.weight(1f),
                color = Color.White.copy(.78f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRefresh) { Text("Retry", color = accent) }
        }
    }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = accent) }
        state.providers.isEmpty() -> EmptyLibrary(
            message = state.errorMessage ?: "No widget providers are available",
            showRetry = true,
            onRetry = onRefresh,
        )
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth < 600.dp) 2 else (maxWidth.value / 190f).toInt().coerceIn(3, 5)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                groups.forEach { (providerLabel, providers) ->
                    item(key = "group:$providerLabel", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        Text(
                            providerLabel,
                            color = Color.White.copy(.72f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }
                    items(providers, key = InstalledWidgetProvider::stableKey, contentType = { "widget" }) { provider ->
                        WidgetProviderCard(provider, previewLoader, accent) { onSelect(provider.providerFlattened) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderCard(
    provider: InstalledWidgetProvider,
    previewLoader: WidgetPreviewLoader,
    accent: Color,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val densityDpi = LocalConfiguration.current.densityDpi
    val widthPx = with(density) { 220.dp.roundToPx() }
    val heightPx = with(density) { 132.dp.roundToPx() }
    val preview by produceState<ImageBitmap?>(null, provider.stableKey, widthPx, heightPx, densityDpi) {
        value = previewLoader.load(provider, widthPx, heightPx, densityDpi)
    }
    val spanX = ceil(provider.minWidthPx / density.density / 72f).toInt().coerceIn(1, 8)
    val spanY = ceil(provider.minHeightPx / density.density / 72f).toInt().coerceIn(1, 8)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Add ${provider.label} widget, approximately $spanX by $spanY" },
        color = Color(0xFF171719),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(.1f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.65f).clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF09090A)),
                contentAlignment = Alignment.Center,
            ) {
                val image = preview
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = accent, modifier = Modifier.size(42.dp))
                }
            }
            Text(
                provider.providerAppLabel,
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                provider.label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "About $spanX × $spanY",
                color = Color.White.copy(.52f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun EmptyLibrary(message: String, showRetry: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Android, contentDescription = null, tint = Color.White.copy(.35f), modifier = Modifier.size(42.dp))
            Text(message, color = Color.White.copy(.82f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            if (showRetry) Button(onClick = onRetry, modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
        }
    }
}

private fun appBucket(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}

/** For letters with no apps, jump to the next letter that has some (or the last one). */
private fun nearestBucket(buckets: Map<Char, Int>, letter: Char): Int? {
    val start = INDEX_LETTERS.indexOf(letter).coerceAtLeast(0)
    for (index in start until INDEX_LETTERS.length) buckets[INDEX_LETTERS[index]]?.let { return it }
    for (index in start downTo 0) buckets[INDEX_LETTERS[index]]?.let { return it }
    return null
}
