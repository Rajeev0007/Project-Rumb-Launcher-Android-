package com.rumb.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rumb.launcher.data.AppUsage
import com.rumb.launcher.data.LauncherStateRepository
import com.rumb.launcher.data.UsageTracker
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.AppShortcut
import com.rumb.launcher.domain.CatalogState
import com.rumb.launcher.domain.ClockAlignment
import com.rumb.launcher.domain.ClockFont
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.ClockTimeFormat
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.domain.findFreePlacement
import com.rumb.launcher.domain.isRegionFree
import com.rumb.launcher.domain.layoutSpanX
import com.rumb.launcher.domain.layoutSpanY
import com.rumb.launcher.domain.occupiedCells
import com.rumb.launcher.domain.packHomeItems
import com.rumb.launcher.domain.rankApps
import com.rumb.launcher.platform.SystemActions
import com.rumb.launcher.platform.WidgetSelection
import com.rumb.launcher.platform.apps.LaunchSource
import com.rumb.launcher.platform.apps.LauncherAppsCatalog
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.platform.icons.IconPack
import com.rumb.launcher.platform.widgets.WidgetPreviewLoader
import com.rumb.launcher.platform.widgets.WidgetProviderCatalog
import java.text.Collator
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val repository = LauncherStateRepository(appContext)
    private val usage = UsageTracker(appContext)
    val catalog = LauncherAppsCatalog(appContext)
    val iconLoader = AppIconLoader(appContext, catalog)
    val widgetCatalog = WidgetProviderCatalog(appContext)
    val widgetPreviewLoader = WidgetPreviewLoader(appContext, widgetCatalog)
    val systemActions = SystemActions(appContext)

    private val transient = MutableStateFlow(TransientState())
    private val search = MutableStateFlow("")
    private val iconPacks = MutableStateFlow<List<IconPack>>(emptyList())
    private var shortcutJob: Job? = null
    @Volatile private var currentHomePage = 0

    private val appLists = combine(
        repository.state.map { AppListKey(it.hiddenApps, it.customLabels) }.distinctUntilChanged(),
        catalog.state,
    ) { key, catalogState -> buildAppLists(catalogState.apps, key) }

    private val searchResults = combine(appLists, search) { lists, query ->
        SearchResults(query, if (query.isBlank()) lists.visible else rankApps(lists.visible, query))
    }

    private val suggestions = combine(
        appLists,
        usage.state,
        repository.state.map { it.drawerSuggestions }.distinctUntilChanged(),
    ) { lists, usageMap, enabled ->
        if (enabled) topSuggestions(lists.visible, usageMap) else emptyList()
    }

    private val data = combine(repository.state, catalog.state, appLists, searchResults, suggestions) {
            settings, catalogState, lists, results, suggested ->
        LauncherData(settings, catalogState, lists, results, suggested)
    }

    val uiState: StateFlow<LauncherUiState> = combine(data, transient, iconPacks) { data, ui, packs ->
        LauncherUiState(
            settings = data.settings,
            catalog = data.catalog,
            allApps = data.lists.all,
            appsById = data.lists.byId,
            apps = data.lists.visible,
            filteredApps = data.results.apps,
            suggestions = data.suggestions,
            destination = ui.destination,
            search = data.results.query,
            selectedApp = ui.selectedAppId?.let(data.lists.byId::get),
            openedFolder = ui.openedFolderId?.let { id ->
                data.settings.homeItems.firstOrNull { it.id == id && it.type == HomeItemType.FOLDER }
            },
            shortcuts = ui.shortcuts,
            message = ui.message,
            searchFocusRequested = ui.searchFocusRequested,
            iconPacks = packs,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    fun navigate(value: Destination) {
        transient.update { it.copy(destination = value, searchFocusRequested = if (value == Destination.DRAWER) it.searchFocusRequested else false) }
        if (value == Destination.SETTINGS) refreshIconPacks()
    }

    fun openSearch() = transient.update { it.copy(destination = Destination.DRAWER, searchFocusRequested = true) }
    fun consumeSearchFocus() = transient.update { it.copy(searchFocusRequested = false) }
    fun search(value: String) { search.value = value.take(80) }

    fun selectApp(app: InstalledApp?) {
        shortcutJob?.cancel()
        transient.update { it.copy(selectedAppId = app?.id, shortcuts = emptyList()) }
        if (app != null) shortcutJob = viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { catalog.shortcuts(app) }.getOrDefault(emptyList())
            transient.update { if (it.selectedAppId == app.id) it.copy(shortcuts = result) else it }
        }
    }

    fun openFolder(folder: HomeItem?) = transient.update { it.copy(openedFolderId = folder?.id) }
    fun consumeMessage() = transient.update { it.copy(message = null) }
    fun showMessage(value: String) = transient.update { it.copy(message = value) }
    fun onHomePageChanged(page: Int) { currentHomePage = page }

    fun launch(app: InstalledApp, source: LaunchSource? = null) = safe("App could not be opened") {
        catalog.launch(app, source)
        usage.recordLaunch(app.id)
    }

    fun launchShortcut(shortcut: AppShortcut) = safe("Shortcut could not be opened") { catalog.launchShortcut(shortcut) }
    fun webSearch(query: String) = systemActions.webSearch(query)
    fun storeSearch(query: String) = systemActions.storeSearch(query)
    fun clearSuggestions() {
        usage.clear()
        showMessage("Suggestions cleared")
    }

    /** Persists pending writes. Called when the launcher leaves the foreground. */
    fun onStop() {
        repository.flush()
        usage.flush()
    }

    fun onMemoryPressure() = iconLoader.trim()

    fun refreshIconPacks() {
        viewModelScope.launch(Dispatchers.Default) {
            iconPacks.value = runCatching { iconLoader.installedPacks() }.getOrDefault(emptyList())
        }
    }

    fun pin(app: InstalledApp) = repository.update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before adding apps")
            return@update state
        }
        if (state.homeItems.any { app.id in it.appIds } || app.id in state.dockApps) state else {
            val placement = findFreePlacement(
                state.homeItems, state.columns, state.rows, 9,
                preferredPage = currentHomePage.coerceIn(0, state.pageCount - 1),
            )
            if (placement == null) {
                showMessage("All nine home pages are full")
                return@update state
            }
            showMessage(
                if (placement.page == currentHomePage) "Added ${app.label} to Home"
                else "Added ${app.label} to page ${placement.page + 1}",
            )
            state.copy(
                pageCount = maxOf(state.pageCount, placement.page + 1),
                homeItems = state.homeItems + HomeItem(
                    id = UUID.randomUUID().toString(),
                    type = HomeItemType.APP,
                    page = placement.page,
                    cell = placement.cell,
                    appIds = listOf(app.id),
                ),
            )
        }
    }

    fun addWidget(widget: WidgetSelection): Boolean {
        var added = false
        var failure: String? = null
        repository.update { state ->
            when {
                state.layoutLocked -> {
                    failure = "Unlock the home layout before adding a widget"
                    state
                }
                state.homeItems.any { it.widgetId == widget.appWidgetId } -> {
                    added = true
                    state
                }
                else -> {
                    val density = appContext.resources.displayMetrics.density.coerceAtLeast(1f)
                    val configuration = appContext.resources.configuration
                    val cellWidthDp = configuration.screenWidthDp.coerceAtLeast(state.columns).toFloat() / state.columns
                    val usableHeightDp = (configuration.screenHeightDp - 200).coerceAtLeast(state.rows * 48)
                    val cellHeightDp = usableHeightDp.toFloat() / state.rows
                    val spanX = ceil(widget.minWidthPx / density / cellWidthDp).toInt().coerceIn(1, state.columns)
                    val spanY = ceil(widget.minHeightPx / density / cellHeightDp).toInt().coerceIn(1, state.rows)
                    val placement = findFreePlacement(
                        state.homeItems, state.columns, state.rows, 9, spanX, spanY,
                        preferredPage = currentHomePage.coerceIn(0, state.pageCount - 1),
                    )
                    if (placement == null) {
                        failure = "No free ${spanX}×$spanY area is available across nine pages"
                        state
                    } else {
                        added = true
                        state.copy(
                            pageCount = maxOf(state.pageCount, placement.page + 1),
                            homeItems = state.homeItems + HomeItem(
                                id = UUID.randomUUID().toString(),
                                type = HomeItemType.WIDGET,
                                page = placement.page,
                                cell = placement.cell,
                                spanX = spanX,
                                spanY = spanY,
                                widgetId = widget.appWidgetId,
                                widgetProvider = widget.provider,
                            ),
                        )
                    }
                }
            }
        }
        failure?.let(::showMessage)
        if (added) navigate(Destination.HOME)
        return added
    }

    fun unpin(app: InstalledApp) = repository.update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before removing apps")
            return@update state
        }
        state.copy(
            homeItems = removeAppFromHome(state.homeItems, app.id),
            dockApps = state.dockApps - app.id,
        )
    }

    fun toggleHidden(app: InstalledApp) = repository.update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before hiding apps")
            return@update state
        }
        val hidden = if (app.id in state.hiddenApps) state.hiddenApps - app.id else state.hiddenApps + app.id
        state.copy(
            hiddenApps = hidden,
            homeItems = if (app.id in hidden) removeAppFromHome(state.homeItems, app.id) else state.homeItems,
            dockApps = if (app.id in hidden) state.dockApps - app.id else state.dockApps,
        )
    }

    fun toggleDock(app: InstalledApp) = repository.update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before changing the dock")
            return@update state
        }
        val dock = if (app.id in state.dockApps) state.dockApps - app.id else {
            if (state.dockApps.size >= 5) {
                showMessage("The dock can hold up to five apps")
                return@update state
            }
            state.dockApps + app.id
        }
        state.copy(dockApps = dock, homeItems = removeAppFromHome(state.homeItems, app.id))
    }

    /** Sets a custom label. A blank value or the original label restores the app's own name. */
    fun renameApp(appId: AppId, rawLabel: String) {
        val label = rawLabel.trim().take(48)
        val original = catalog.state.value.apps.firstOrNull { it.id == appId }?.label
        repository.update { state ->
            val labels = if (label.isEmpty() || label == original) state.customLabels - appId
            else state.customLabels + (appId to label)
            if (labels == state.customLabels) state else state.copy(customLabels = labels)
        }
    }

    fun moveItem(itemId: String, columnDelta: Int, rowDelta: Int) = repository.update { state ->
        if (state.layoutLocked) return@update state
        val item = state.homeItems.firstOrNull { it.id == itemId } ?: return@update state
        if (item.type == HomeItemType.WIDGET) return@update state
        val spanX = item.layoutSpanX(state.columns)
        val spanY = item.layoutSpanY(state.rows)
        val oldRow = item.cell / state.columns
        val oldColumn = item.cell % state.columns
        val newColumn = (oldColumn + columnDelta).coerceIn(0, state.columns - spanX)
        val newRow = (oldRow + rowDelta).coerceIn(0, state.rows - spanY)
        val target = newRow * state.columns + newColumn
        if (target == item.cell) return@update state
        val targetCells = occupiedCells(item.copy(cell = target), state.columns, state.rows)
        val collisions = state.homeItems.filter { other ->
            other.id != item.id && other.page == item.page && occupiedCells(other, state.columns, state.rows).any(targetCells::contains)
        }
        if (collisions.isEmpty() && isRegionFree(
                state.homeItems, item.page, target, spanX, spanY, state.columns, state.rows, item.id,
            )
        ) return@update state.copy(homeItems = state.homeItems.map { if (it.id == item.id) it.copy(cell = target) else it })

        val collision = collisions.singleOrNull()
        when {
            item.type == HomeItemType.APP && collision?.type == HomeItemType.APP && collision.cell == target -> state.copy(
                homeItems = state.homeItems.filterNot { it.id == item.id || it.id == collision.id } + HomeItem(
                    id = UUID.randomUUID().toString(),
                    type = HomeItemType.FOLDER,
                    page = item.page,
                    cell = target,
                    title = "Folder",
                    appIds = (collision.appIds + item.appIds).distinctBy(AppId::stableKey),
                ),
            )
            item.type == HomeItemType.APP && collision?.type == HomeItemType.FOLDER && collision.cell == target -> {
                if (collision.appIds.size >= 24) {
                    showMessage("Folders can hold up to 24 apps")
                    state
                } else state.copy(
                    homeItems = state.homeItems.filterNot { it.id == item.id }.map {
                        if (it.id == collision.id) it.copy(appIds = (it.appIds + item.appIds).distinctBy(AppId::stableKey)) else it
                    },
                )
            }
            collision?.type == HomeItemType.WIDGET -> {
                showMessage("Apps cannot be dropped onto widgets")
                state
            }
            else -> state
        }
    }

    fun removeWidget(itemId: String): Int? {
        val state = repository.state.value
        if (state.layoutLocked) return null
        val item = state.homeItems.firstOrNull { it.id == itemId && it.type == HomeItemType.WIDGET } ?: return null
        repository.update { it.copy(homeItems = it.homeItems.filterNot { homeItem -> homeItem.id == itemId }) }
        return item.widgetId
    }

    fun removeHomeItem(item: HomeItem) = repository.update { state ->
        if (state.layoutLocked) state else state.copy(homeItems = state.homeItems.filterNot { it.id == item.id })
    }

    fun renameFolder(folderId: String, rawTitle: String) {
        val title = rawTitle.trim()
        if (title.length !in 1..48) {
            showMessage("Folder names must be 1–48 characters")
            return
        }
        repository.update { state ->
            if (state.layoutLocked) state else state.copy(homeItems = state.homeItems.map {
                if (it.id == folderId && it.type == HomeItemType.FOLDER) it.copy(title = title) else it
            })
        }
    }

    fun removeFolderApp(folderId: String, appId: AppId) = repository.update { state ->
        if (state.layoutLocked) return@update state
        state.copy(homeItems = state.homeItems.mapNotNull { item ->
            if (item.id != folderId || item.type != HomeItemType.FOLDER) return@mapNotNull item
            val remaining = item.appIds.filterNot { it == appId }
            when {
                remaining.isEmpty() -> null
                remaining.size == 1 -> item.copy(type = HomeItemType.APP, title = "", appIds = remaining)
                else -> item.copy(appIds = remaining)
            }
        })
    }

    fun importJson(raw: String) = runCatching { repository.importJson(raw) }
        .onSuccess { iconLoader.invalidate(); showMessage("Layout imported. Widget bindings were not imported.") }
        .onFailure { showMessage(it.message ?: "Import failed") }
    fun exportJson(): String = repository.exportJson()

    fun updateTheme(value: ThemePreference) = update { it.copy(theme = value) }
    fun updateVisualStyle(value: VisualStyle) = update { it.copy(visualStyle = value) }
    fun updateDynamicColor(value: Boolean) = update { it.copy(dynamicColor = value) }
    fun updateIconShape(value: IconShape) = update { it.copy(iconShape = value) }
    fun updateThemedIcons(value: Boolean) { iconLoader.invalidate(); update { it.copy(themedIcons = value) } }
    fun updateIconPack(value: String?) { iconLoader.invalidate(); update { it.copy(iconPack = value) } }
    fun updateColumns(value: Int) = reflowGrid(value, repository.state.value.rows)
    fun updateRows(value: Int) = reflowGrid(repository.state.value.columns, value)
    fun updateIconSize(value: Int) = update { it.copy(iconSize = value) }
    fun updatePageCount(value: Int) = update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before changing page count")
            return@update state
        }
        val occupiedPages = (state.homeItems.maxOfOrNull(HomeItem::page) ?: 0) + 1
        state.copy(pageCount = value.coerceAtLeast(occupiedPages))
    }
    fun unhideAll() = update { it.copy(hiddenApps = emptySet()) }
    fun updateLayoutLocked(value: Boolean) = update { it.copy(layoutLocked = value) }
    fun updateClockStyle(value: ClockStyle) = update { it.copy(clockStyle = value) }
    fun updateClockAlignment(value: ClockAlignment) = update { it.copy(clockAlignment = value) }
    fun updateClockTimeFormat(value: ClockTimeFormat) = update { it.copy(clockTimeFormat = value) }
    fun updateClockFont(value: ClockFont) = update { it.copy(clockFont = value) }
    fun updateClockSize(value: Int) = update { it.copy(clockSize = value) }
    fun updateShowDate(value: Boolean) = update { it.copy(showDate = value) }
    fun updateShowSeconds(value: Boolean) = update { it.copy(showSeconds = value) }
    fun updateSwipeUp(value: GestureAction) = update { it.copy(swipeUpAction = value) }
    fun updateSwipeDown(value: GestureAction) = update { it.copy(swipeDownAction = value) }
    fun updateDoubleTap(value: GestureAction) = update { it.copy(doubleTapAction = value) }
    fun updatePinch(value: GestureAction) = update { it.copy(pinchAction = value) }
    fun updateShowHomeLabels(value: Boolean) = update { it.copy(showHomeLabels = value) }
    fun updateHomeSearchBar(value: Boolean) = update { it.copy(homeSearchBar = value) }
    fun updateDrawerSuggestions(value: Boolean) = update { it.copy(drawerSuggestions = value) }
    fun updateDrawerAutoKeyboard(value: Boolean) = update { it.copy(drawerAutoKeyboard = value) }
    fun updateHaptics(value: Boolean) = update { it.copy(haptics = value) }

    fun performGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> navigate(Destination.DRAWER)
            GestureAction.OPEN_SEARCH -> openSearch()
            GestureAction.OPEN_QUICK_PANEL -> navigate(Destination.QUICK_PANEL)
            GestureAction.OPEN_SETTINGS -> navigate(Destination.SETTINGS)
            GestureAction.LOCK_LAYOUT -> {
                val locked = !repository.state.value.layoutLocked
                updateLayoutLocked(locked)
                showMessage(if (locked) "Home layout locked" else "Home layout unlocked")
            }
        }
    }

    private fun removeAppFromHome(items: List<HomeItem>, appId: AppId): List<HomeItem> = items.mapNotNull { item ->
        if (appId !in item.appIds) return@mapNotNull item
        if (item.type != HomeItemType.FOLDER) return@mapNotNull null
        val remaining = item.appIds - appId
        when {
            remaining.isEmpty() -> null
            remaining.size == 1 -> item.copy(type = HomeItemType.APP, title = "", appIds = remaining)
            else -> item.copy(appIds = remaining)
        }
    }

    private fun reflowGrid(columns: Int, rows: Int) = repository.update { state ->
        if (state.layoutLocked) {
            showMessage("Unlock the home layout before changing the grid")
            return@update state
        }
        val safeColumns = columns.coerceIn(4, 8)
        val safeRows = rows.coerceIn(4, 8)
        val resized = state.homeItems.map { item ->
            item.copy(spanX = item.layoutSpanX(safeColumns), spanY = item.layoutSpanY(safeRows))
        }
        val packed = packHomeItems(resized, safeColumns, safeRows, 9, preservePreferredPositions = false)
        if (packed == null) {
            showMessage("The grid cannot be reduced without removing home items")
            return@update state
        }
        val requiredPages = ((packed.maxOfOrNull(HomeItem::page) ?: 0) + 1).coerceAtLeast(1)
        state.copy(
            columns = safeColumns,
            rows = safeRows,
            pageCount = maxOf(state.pageCount, requiredPages),
            homeItems = packed,
        )
    }

    private fun update(block: (LauncherSettings) -> LauncherSettings) = repository.update(block)
    private fun safe(fallback: String, block: () -> Unit) = runCatching(block).onFailure { showMessage(it.message ?: fallback) }

    override fun onCleared() {
        repository.flush()
        usage.flush()
        catalog.close()
        widgetCatalog.close()
        super.onCleared()
    }

    companion object {
        private const val MAX_SUGGESTIONS = 8

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LauncherViewModel(context) as T
        }

        private fun buildAppLists(source: List<InstalledApp>, key: AppListKey): AppLists {
            val labeled = if (key.labels.isEmpty()) source else {
                val collator = Collator.getInstance(Locale.getDefault())
                source.map { app -> key.labels[app.id]?.let { app.copy(label = it) } ?: app }
                    .sortedWith { left, right -> collator.compare(left.label, right.label) }
            }
            return AppLists(
                all = labeled,
                byId = labeled.associateBy(InstalledApp::id),
                visible = if (key.hidden.isEmpty()) labeled else labeled.filterNot { it.id in key.hidden },
            )
        }

        private fun topSuggestions(apps: List<InstalledApp>, usage: Map<String, AppUsage>): List<InstalledApp> {
            if (usage.isEmpty()) return emptyList()
            val now = System.currentTimeMillis()
            return apps.mapNotNull { app -> usage[app.id.stableKey]?.let { app to it.score(now) } }
                .sortedByDescending { it.second }
                .take(MAX_SUGGESTIONS)
                .map { it.first }
        }
    }
}

private data class AppListKey(val hidden: Set<AppId>, val labels: Map<AppId, String>)
private data class AppLists(
    val all: List<InstalledApp>,
    val byId: Map<AppId, InstalledApp>,
    val visible: List<InstalledApp>,
)
private data class SearchResults(val query: String, val apps: List<InstalledApp>)
private data class LauncherData(
    val settings: LauncherSettings,
    val catalog: CatalogState,
    val lists: AppLists,
    val results: SearchResults,
    val suggestions: List<InstalledApp>,
)
private data class TransientState(
    val destination: Destination = Destination.HOME,
    val selectedAppId: AppId? = null,
    val openedFolderId: String? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val message: String? = null,
    val searchFocusRequested: Boolean = false,
)

enum class Destination { HOME, DRAWER, QUICK_PANEL, SETTINGS }

data class LauncherUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val catalog: CatalogState = CatalogState(),
    /** Every launchable app with custom labels applied, including hidden apps. */
    val allApps: List<InstalledApp> = emptyList(),
    val appsById: Map<AppId, InstalledApp> = emptyMap(),
    /** Apps shown in the library, alphabetical. */
    val apps: List<InstalledApp> = emptyList(),
    /** Ranked search results, or [apps] when the query is blank. */
    val filteredApps: List<InstalledApp> = emptyList(),
    val suggestions: List<InstalledApp> = emptyList(),
    val destination: Destination = Destination.HOME,
    val search: String = "",
    val selectedApp: InstalledApp? = null,
    val openedFolder: HomeItem? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val message: String? = null,
    val searchFocusRequested: Boolean = false,
    val iconPacks: List<IconPack> = emptyList(),
)
