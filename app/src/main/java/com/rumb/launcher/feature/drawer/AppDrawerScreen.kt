package com.rumb.launcher.feature.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.platform.widgets.InstalledWidgetProvider
import com.rumb.launcher.platform.widgets.WidgetCatalogState
import com.rumb.launcher.platform.widgets.WidgetPreviewLoader
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.visualTokens
import java.text.Collator
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.launch

private enum class LibraryTab { APPS, WIDGETS }

@Composable
fun AppDrawerScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    widgetState: WidgetCatalogState,
    widgetPreviewLoader: WidgetPreviewLoader,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRefreshApps: () -> Unit,
    onRefreshWidgets: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
    onRequestWidget: () -> Unit,
    onRequestWidgetProvider: (String) -> Unit,
) {
    val tokens = visualTokens(state.settings.visualStyle)
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.APPS) }
    var ascending by rememberSaveable { mutableStateOf(true) }
    val collator = remember { Collator.getInstance(Locale.getDefault()) }
    val sortedApps = remember(state.filteredApps, ascending) {
        state.filteredApps.sortedWith { left, right -> collator.compare(left.label, right.label) }
            .let { if (ascending) it else it.asReversed() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 8.dp),
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
                            contentDescription = if (ascending) "Sort apps descending" else "Sort apps ascending"
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
                LibraryTabButton("Widgets", selectedTab == LibraryTab.WIDGETS, tokens.accent) { selectedTab = LibraryTab.WIDGETS }
            }
            if (selectedTab == LibraryTab.APPS) {
                AppsLibrary(
                    state = state,
                    apps = sortedApps,
                    iconLoader = iconLoader,
                    ascending = ascending,
                    onSearch = onSearch,
                    onRefresh = onRefreshApps,
                    onLaunch = onLaunch,
                    onLongPress = onLongPress,
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

@Composable
private fun RowScope.LibraryTabButton(label: String, isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = if (isSelected) Color.White else Color.White.copy(.55f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Box(
            Modifier.padding(top = 10.dp).height(3.dp).width(56.dp)
                .background(if (isSelected) accent else Color.Transparent, CircleShape),
        )
    }
}

@Composable
private fun AppsLibrary(
    state: LauncherUiState,
    apps: List<InstalledApp>,
    iconLoader: AppIconLoader,
    ascending: Boolean,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
) {
    val tokens = visualTokens(state.settings.visualStyle)
    OutlinedTextField(
        value = state.search,
        onValueChange = onSearch,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        singleLine = true,
        placeholder = { Text("Search apps", color = Color.White.copy(.5f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tokens.accent) },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF19191C),
            unfocusedContainerColor = Color(0xFF19191C),
            focusedIndicatorColor = tokens.accent,
            unfocusedIndicatorColor = Color.White.copy(.14f),
            cursorColor = tokens.accent,
        ),
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 34.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (state.search.isBlank()) "${apps.size} apps" else "${apps.size} matches",
            color = Color.White.copy(.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (ascending) "A–Z" else "Z–A",
            color = tokens.accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
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
            TextButton(onClick = onRefresh) { Text("Retry", color = tokens.accent) }
        }
    }
    when {
        state.catalog.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = tokens.accent)
        }
        apps.isEmpty() -> EmptyLibrary(
            message = state.catalog.errorMessage
                ?: if (state.search.isBlank()) "No launchable apps are available" else "No apps match your search",
            showRetry = state.catalog.errorMessage != null,
            onRetry = onRefresh,
        )
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val columnCount = if (maxWidth < 600.dp) 4 else (maxWidth.value / 104f).toInt().coerceIn(5, 8)
            val gridState = rememberLazyGridState()
            val scope = rememberCoroutineScope()
            val buckets = remember(apps) {
                buildMap<Char, Int> {
                    apps.forEachIndexed { index, app -> putIfAbsent(appBucket(app.label), index) }
                }
            }
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(end = 27.dp),
                    contentPadding = PaddingValues(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(apps, key = { it.id.stableKey }, contentType = { "app" }) { app ->
                        AppTile(
                            app = app,
                            iconLoader = iconLoader,
                            iconSize = state.settings.iconSize.dp,
                            iconShape = state.settings.iconShape,
                            themed = state.settings.themedIcons,
                            homeStyle = state.settings.visualStyle != VisualStyle.SOFT_PANEL,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                            labelColor = Color.White,
                        )
                    }
                }
                FastIndex(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(28.dp),
                    accent = tokens.accent,
                    available = buckets.keys,
                    onLetter = { letter ->
                        buckets[letter]?.let { index ->
                            val firstInRow = (index / columnCount) * columnCount
                            scope.launch { gridState.animateScrollToItem(firstInRow) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FastIndex(
    modifier: Modifier,
    accent: Color,
    available: Set<Char>,
    onLetter: (Char) -> Unit,
) {
    val letters = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    Column(
        modifier.background(Color.Black.copy(.74f), CircleShape).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { letter ->
            val enabled = letter in available
            Text(
                letter.toString(),
                modifier = Modifier.weight(1f).fillMaxWidth().clickable(enabled = enabled) { onLetter(letter) }
                    .semantics { contentDescription = "Jump to $letter" },
                color = if (enabled) accent else Color.White.copy(.2f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
            )
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
                item(key = "group:$providerLabel", span = { GridItemSpan(maxLineSpan) }) {
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
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
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
                if (preview != null) {
                    Image(
                        bitmap = preview!!,
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
