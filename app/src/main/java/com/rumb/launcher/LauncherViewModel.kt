package com.rumb.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rumb.launcher.data.LauncherStateRepository
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.AppShortcut
import com.rumb.launcher.domain.CatalogState
import com.rumb.launcher.domain.ClockStyle
import com.rumb.launcher.domain.GestureAction
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.IconShape
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.LauncherSettings
import com.rumb.launcher.domain.ThemePreference
import com.rumb.launcher.platform.SystemActions
import com.rumb.launcher.platform.apps.LauncherAppsCatalog
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.platform.notifications.NotificationDotStore
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val repository = LauncherStateRepository(appContext)
    val catalog = LauncherAppsCatalog(appContext)
    val iconLoader = AppIconLoader(appContext, catalog)
    val systemActions = SystemActions(appContext)

    private val destination = MutableStateFlow(Destination.HOME)
    private val search = MutableStateFlow("")
    private val selectedApp = MutableStateFlow<InstalledApp?>(null)
    private val openedFolder = MutableStateFlow<HomeItem?>(null)
    private val shortcuts = MutableStateFlow<List<AppShortcut>>(emptyList())
    private val message = MutableStateFlow<String?>(null)
    private var shortcutJob: Job? = null

    private val baseState = combine(repository.state, catalog.state, destination, search, selectedApp) {
            settings, catalogState, currentDestination, query, app ->
        BaseState(settings, catalogState, currentDestination, query, app)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        baseState,
        openedFolder,
        shortcuts,
        NotificationDotStore.counts,
        message,
    ) { base, folder, availableShortcuts, counts, currentMessage ->
        val visibleApps = base.catalog.apps.filterNot { it.id in base.settings.hiddenApps }
        LauncherUiState(
            settings = base.settings,
            catalog = base.catalog,
            apps = visibleApps,
            filteredApps = if (base.query.isBlank()) visibleApps else visibleApps.filter {
                it.label.contains(base.query.trim(), ignoreCase = true) ||
                    it.packageName.contains(base.query.trim(), ignoreCase = true)
            },
            destination = base.destination,
            search = base.query,
            selectedApp = base.selectedApp,
            openedFolder = folder,
            shortcuts = availableShortcuts,
            notificationCounts = if (base.settings.notificationDots) counts else emptyMap(),
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    fun navigate(value: Destination) { destination.value = value }
    fun search(value: String) { search.value = value.take(80) }
    fun selectApp(app: InstalledApp?) {
        shortcutJob?.cancel()
        selectedApp.value = app
        shortcuts.value = emptyList()
        if (app != null) shortcutJob = viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching { catalog.shortcuts(app) }.getOrDefault(emptyList())
            if (selectedApp.value?.id == app.id) shortcuts.value = result
        }
    }
    fun openFolder(folder: HomeItem?) { openedFolder.value = folder }
    fun consumeMessage() { message.value = null }
    fun showMessage(value: String) { message.value = value }

    fun launch(app: InstalledApp) = safe("App could not be opened") { catalog.launch(app) }
    fun launchShortcut(shortcut: AppShortcut) = safe("Shortcut could not be opened") { catalog.launchShortcut(shortcut) }

    fun pin(app: InstalledApp) = repository.update { state ->
        if (state.homeItems.any { app.id in it.appIds } || app.id in state.dockApps) state else {
            val capacity = state.columns * state.rows
            val occupied = state.homeItems.groupBy { it.page }.mapValues { entry -> entry.value.map(HomeItem::cell).toSet() }
            val existingPage = (0 until state.pageCount).firstOrNull { occupied[it].orEmpty().size < capacity }
            val targetPage = existingPage ?: if (state.pageCount < 9) state.pageCount else state.pageCount - 1
            val pages = maxOf(state.pageCount, targetPage + 1)
            val cell = (0 until capacity).firstOrNull { it !in occupied[targetPage].orEmpty() }
            if (cell == null) {
                showMessage("All nine home pages are full")
                return@update state
            }
            state.copy(
                pageCount = pages,
                homeItems = state.homeItems + HomeItem(
                    id = UUID.randomUUID().toString(),
                    type = HomeItemType.APP,
                    page = targetPage,
                    cell = cell,
                    appIds = listOf(app.id),
                ),
            )
        }
    }

    fun unpin(app: InstalledApp) = repository.update { state ->
        state.copy(
            homeItems = removeAppFromHome(state.homeItems, app.id),
            dockApps = state.dockApps - app.id,
        )
    }

    fun toggleHidden(app: InstalledApp) = repository.update { state ->
        val hidden = if (app.id in state.hiddenApps) state.hiddenApps - app.id else state.hiddenApps + app.id
        state.copy(
            hiddenApps = hidden,
            homeItems = if (app.id in hidden) removeAppFromHome(state.homeItems, app.id) else state.homeItems,
            dockApps = if (app.id in hidden) state.dockApps - app.id else state.dockApps,
        )
    }

    fun toggleDock(app: InstalledApp) = repository.update { state ->
        val dock = if (app.id in state.dockApps) state.dockApps - app.id else {
            if (state.dockApps.size >= 5) {
                showMessage("The dock can hold up to five apps")
                return@update state
            }
            state.dockApps + app.id
        }
        state.copy(dockApps = dock, homeItems = removeAppFromHome(state.homeItems, app.id))
    }

    fun moveItem(itemId: String, columnDelta: Int, rowDelta: Int) = repository.update { state ->
        if (state.layoutLocked) return@update state
        val item = state.homeItems.firstOrNull { it.id == itemId } ?: return@update state
        val oldRow = item.cell / state.columns
        val oldColumn = item.cell % state.columns
        val newColumn = (oldColumn + columnDelta).coerceIn(0, state.columns - 1)
        val newRow = (oldRow + rowDelta).coerceIn(0, state.rows - 1)
        val target = newRow * state.columns + newColumn
        val collision = state.homeItems.firstOrNull { it.page == item.page && it.cell == target && it.id != item.id }
        when {
            target == item.cell -> state
            collision == null -> state.copy(homeItems = state.homeItems.map { if (it.id == item.id) it.copy(cell = target) else it })
            item.type == HomeItemType.APP && collision.type == HomeItemType.APP -> state.copy(
                homeItems = state.homeItems.filterNot { it.id == item.id || it.id == collision.id } + HomeItem(
                    id = UUID.randomUUID().toString(),
                    type = HomeItemType.FOLDER,
                    page = item.page,
                    cell = target,
                    title = "Folder",
                    appIds = (collision.appIds + item.appIds).distinctBy(AppId::stableKey),
                ),
            )
            item.type == HomeItemType.APP && collision.type == HomeItemType.FOLDER -> {
                if (collision.appIds.size >= 24) {
                    showMessage("Folders can hold up to 24 apps")
                    state
                } else state.copy(
                    homeItems = state.homeItems.filterNot { it.id == item.id }.map {
                        if (it.id == collision.id) it.copy(appIds = (it.appIds + item.appIds).distinctBy(AppId::stableKey)) else it
                    },
                )
            }
            else -> state.copy(homeItems = state.homeItems.map {
                when (it.id) {
                    item.id -> it.copy(cell = collision.cell)
                    collision.id -> it.copy(cell = item.cell)
                    else -> it
                }
            })
        }
    }

    fun removeHomeItem(item: HomeItem) = repository.update { it.copy(homeItems = it.homeItems - item) }
    fun importJson(raw: String) = runCatching { repository.importJson(raw) }
        .onSuccess { showMessage("Layout imported. Widget bindings were not imported.") }
        .onFailure { showMessage(it.message ?: "Import failed") }
    fun exportJson(): String = repository.exportJson()

    fun updateTheme(value: ThemePreference) = update { it.copy(theme = value) }
    fun updateDynamicColor(value: Boolean) = update { it.copy(dynamicColor = value) }
    fun updateIconShape(value: IconShape) = update { it.copy(iconShape = value) }
    fun updateThemedIcons(value: Boolean) { iconLoader.invalidate(); update { it.copy(themedIcons = value) } }
    fun updateColumns(value: Int) = reflowGrid(value, repository.state.value.rows)
    fun updateRows(value: Int) = reflowGrid(repository.state.value.columns, value)
    fun updateIconSize(value: Int) = update { it.copy(iconSize = value) }
    fun updatePageCount(value: Int) = update { state ->
        val occupiedPages = (state.homeItems.maxOfOrNull(HomeItem::page) ?: 0) + 1
        state.copy(pageCount = value.coerceAtLeast(occupiedPages))
    }
    fun unhideAll() = update { it.copy(hiddenApps = emptySet()) }
    fun updateLayoutLocked(value: Boolean) = update { it.copy(layoutLocked = value) }
    fun updateClockStyle(value: ClockStyle) = update { it.copy(clockStyle = value) }
    fun updateShowDate(value: Boolean) = update { it.copy(showDate = value) }
    fun updateShowSeconds(value: Boolean) = update { it.copy(showSeconds = value) }
    fun updateNotificationDots(value: Boolean) = update { it.copy(notificationDots = value) }
    fun updateSwipeUp(value: GestureAction) = update { it.copy(swipeUpAction = value) }
    fun updateSwipeDown(value: GestureAction) = update { it.copy(swipeDownAction = value) }
    fun updateDoubleTap(value: GestureAction) = update { it.copy(doubleTapAction = value) }

    fun performGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> navigate(Destination.DRAWER)
            GestureAction.OPEN_SETTINGS -> navigate(Destination.SETTINGS)
            GestureAction.OPEN_NOTIFICATIONS -> showMessage("Android does not expose notification shade control to third-party launchers")
            GestureAction.LOCK_LAYOUT -> updateLayoutLocked(!repository.state.value.layoutLocked)
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
        val safeColumns = columns.coerceIn(4, 8)
        val safeRows = rows.coerceIn(4, 8)
        val capacity = safeColumns * safeRows
        if (state.homeItems.size > capacity * 9) {
            showMessage("The grid cannot be reduced without removing home items")
            return@update state
        }
        val sortedItems = state.homeItems.sortedWith(compareBy(HomeItem::page, HomeItem::cell))
        val requiredPages = ((sortedItems.size + capacity - 1) / capacity).coerceAtLeast(1)
        state.copy(
            columns = safeColumns,
            rows = safeRows,
            pageCount = maxOf(state.pageCount, requiredPages),
            homeItems = sortedItems.mapIndexed { index, item ->
                item.copy(page = index / capacity, cell = index % capacity)
            },
        )
    }

    private fun update(block: (LauncherSettings) -> LauncherSettings) = repository.update(block)
    private fun safe(fallback: String, block: () -> Unit) = runCatching(block).onFailure { showMessage(it.message ?: fallback) }

    override fun onCleared() {
        catalog.close()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LauncherViewModel(context) as T
        }
    }
}

private data class BaseState(
    val settings: LauncherSettings,
    val catalog: CatalogState,
    val destination: Destination,
    val query: String,
    val selectedApp: InstalledApp?,
)

enum class Destination { HOME, DRAWER, SETTINGS }

data class LauncherUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val catalog: CatalogState = CatalogState(),
    val apps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val destination: Destination = Destination.HOME,
    val search: String = "",
    val selectedApp: InstalledApp? = null,
    val openedFolder: HomeItem? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val notificationCounts: Map<String, Int> = emptyMap(),
    val message: String? = null,
)
