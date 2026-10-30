package com.rumb.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rumb.launcher.Destination
import com.rumb.launcher.LauncherViewModel
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.domain.iconAppearance
import com.rumb.launcher.feature.appactions.AppActionsSheet
import com.rumb.launcher.feature.appactions.FolderAppEntry
import com.rumb.launcher.feature.appactions.FolderDialog
import com.rumb.launcher.feature.drawer.AppDrawerScreen
import com.rumb.launcher.feature.home.HomeScreen
import com.rumb.launcher.feature.quickpanel.QuickPanelScreen
import com.rumb.launcher.feature.settings.SettingsScreen
import com.rumb.launcher.platform.LauncherActivityController
import com.rumb.launcher.ui.components.visualTokens
import com.rumb.launcher.ui.theme.RumbLauncherTheme

@Composable
fun RumbLauncherRoot(controller: LauncherActivityController) {
    val context = LocalContext.current
    val model: LauncherViewModel = viewModel(factory = LauncherViewModel.factory(context.applicationContext))
    val state by model.uiState.collectAsStateWithLifecycle()
    val widgetState by model.widgetCatalog.state.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var homePressSignal by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val drawer = remember(scope) { DrawerController(scope) }
    val drawerHidden by remember { derivedStateOf { drawer.fraction >= DrawerController.CLOSED } }
    val drawerScroll = remember(drawer, model) {
        drawer.nestedScrollConnection { open -> model.navigate(if (open) Destination.DRAWER else Destination.HOME) }
    }

    DisposableEffect(controller, model) {
        controller.onImportReady = model::importJson
        controller.onWidgetReady = model::addWidget
        controller.onError = model::showMessage
        controller.onMemoryPressure = model::onMemoryPressure
        controller.onHomeIntent = {
            val atRest = state.destination == Destination.HOME && !editMode &&
                state.selectedApp == null && state.openedFolder == null
            if (atRest) {
                homePressSignal++
            } else {
                editMode = false
                model.selectApp(null)
                model.openFolder(null)
                model.navigate(Destination.HOME)
            }
        }
        onDispose {
            controller.onImportReady = null
            controller.onWidgetReady = null
            controller.onError = null
            controller.onMemoryPressure = null
            controller.onHomeIntent = null
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        model.onStop()
        // Coming back to the launcher should show Home, not a half-forgotten library or panel.
        if (state.destination == Destination.DRAWER || state.destination == Destination.QUICK_PANEL) {
            model.navigate(Destination.HOME)
            drawer.snapTo(DrawerController.CLOSED)
        }
    }

    val activeWidgetIds = remember(state.settings.homeItems) {
        state.settings.homeItems.filter { it.type == HomeItemType.WIDGET }.mapNotNull { it.widgetId }.toSet()
    }
    LaunchedEffect(activeWidgetIds) { controller.reconcileWidgetIds(activeWidgetIds) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbars.showSnackbar(it); model.consumeMessage() }
    }
    LaunchedEffect(state.destination) {
        if (drawer.dragging) return@LaunchedEffect
        drawer.animateTo(if (state.destination == Destination.DRAWER) DrawerController.OPEN else DrawerController.CLOSED)
    }
    BackHandler(editMode || state.destination != Destination.HOME) {
        if (editMode && state.destination == Destination.HOME) editMode = false
        else model.navigate(Destination.HOME)
    }

    val requestWidget = {
        if (state.settings.layoutLocked) {
            model.showMessage("Unlock the home layout before adding a widget")
        } else {
            editMode = false
            controller.requestWidget()
        }
    }

    RumbLauncherTheme(state.settings.theme, state.settings.dynamicColor) {
        Box(Modifier.fillMaxSize().onSizeChanged { drawer.heightPx = it.height.toFloat() }) {
            HomeScreen(
                state = state,
                iconLoader = model.iconLoader,
                editMode = editMode,
                pagerScrollEnabled = !drawer.dragging,
                homePressSignal = homePressSignal,
                modifier = Modifier.graphicsLayer {
                    val progress = 1f - drawer.fraction
                    alpha = 1f - progress
                    val scale = 1f - .06f * progress
                    scaleX = scale
                    scaleY = scale
                },
                onEnterEditMode = {
                    editMode = true
                    if (state.settings.layoutLocked) {
                        model.showMessage("Layout is locked. Visual style, wallpaper, and settings remain available.")
                    }
                },
                onExitEditMode = { editMode = false },
                onVisualStyle = model::updateVisualStyle,
                onOpenDrawer = { editMode = false; model.navigate(Destination.DRAWER) },
                onOpenSearch = { editMode = false; model.openSearch() },
                onOpenSettings = { editMode = false; model.navigate(Destination.SETTINGS) },
                onWallpaper = model.systemActions::openWallpaperPicker,
                onAddWidget = requestWidget,
                onLaunch = model::launch,
                onAppMenu = model::selectApp,
                onOpenFolder = model::openFolder,
                onRefreshApps = model.catalog::refresh,
                onMoveItem = model::moveItem,
                onGesture = model::performGesture,
                onDrawerDrag = { delta -> drawer.dragBy(delta) },
                onDrawerDragEnd = { velocity ->
                    if (drawer.settle(velocity)) model.navigate(Destination.DRAWER)
                },
                onPageChanged = model::onHomePageChanged,
                onCreateWidgetView = controller::createWidgetView,
                onUpdateWidgetSize = controller::updateWidgetSize,
                onRemoveWidget = { itemId ->
                    model.removeWidget(itemId)?.let(controller::deleteWidgetId)
                },
            )

            // The library stays composed so opening it never pays a first-frame cost; while closed it
            // is drawn with zero alpha (skipped entirely) and hidden from accessibility services.
            AppDrawerScreen(
                state = state,
                iconLoader = model.iconLoader,
                widgetState = widgetState,
                widgetPreviewLoader = model.widgetPreviewLoader,
                isOpen = state.destination == Destination.DRAWER,
                focusSearch = state.searchFocusRequested,
                nestedScrollConnection = drawerScroll,
                onHeaderDrag = { delta -> drawer.dragBy(delta) },
                onHeaderDragEnd = { velocity ->
                    model.navigate(if (drawer.settle(velocity)) Destination.DRAWER else Destination.HOME)
                },
                onSearchFocused = model::consumeSearchFocus,
                onBack = { model.navigate(Destination.HOME) },
                onSearch = model::search,
                onRefreshApps = model.catalog::refresh,
                onRefreshWidgets = model.widgetCatalog::refresh,
                onLaunch = model::launch,
                onLongPress = model::selectApp,
                onRequestWidget = requestWidget,
                onRequestWidgetProvider = { provider ->
                    if (state.settings.layoutLocked) model.showMessage("Unlock the home layout before adding a widget")
                    else controller.requestWidgetProvider(provider)
                },
                onWebSearch = model::webSearch,
                onStoreSearch = model::storeSearch,
                modifier = Modifier
                    .graphicsLayer {
                        val fraction = drawer.fraction
                        translationY = fraction * drawer.heightPx
                        alpha = if (fraction >= DrawerController.CLOSED) 0f else 1f
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        clip = fraction > DrawerController.OPEN
                    }
                    .then(if (drawerHidden) Modifier.clearAndSetSemantics { } else Modifier),
            )

            AnimatedVisibility(
                visible = state.destination == Destination.QUICK_PANEL,
                enter = slideInVertically(tween(260)) { -it / 5 } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(200)) { -it / 6 } + fadeOut(tween(160)),
            ) {
                Box(Modifier.fillMaxSize().blockTouchesBelow()) {
                    QuickPanelScreen(
                        style = state.settings.visualStyle,
                        onBack = { model.navigate(Destination.HOME) },
                        onInternet = model.systemActions::openInternetSettings,
                        onBluetooth = model.systemActions::openBluetoothSettings,
                        onDisplay = model.systemActions::openDisplaySettings,
                        onSound = model.systemActions::openSoundSettings,
                        onBattery = model.systemActions::openBatterySettings,
                        onWallpaper = model.systemActions::openWallpaperPicker,
                        onHomeSettings = model.systemActions::openHomeSettings,
                    )
                }
            }

            AnimatedVisibility(
                visible = state.destination == Destination.SETTINGS,
                enter = slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(220)),
                exit = slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)),
            ) {
                Box(Modifier.fillMaxSize().blockTouchesBelow()) {
                    SettingsScreen(
                        settings = state.settings,
                        iconPacks = state.iconPacks,
                        onBack = { model.navigate(Destination.HOME) },
                        onVisualStyle = model::updateVisualStyle,
                        onTheme = model::updateTheme,
                        onDynamicColor = model::updateDynamicColor,
                        onIconShape = model::updateIconShape,
                        onThemedIcons = model::updateThemedIcons,
                        onIconPack = model::updateIconPack,
                        onColumns = model::updateColumns,
                        onRows = model::updateRows,
                        onIconSize = model::updateIconSize,
                        onPages = model::updatePageCount,
                        onShowHomeLabels = model::updateShowHomeLabels,
                        onHomeSearchBar = model::updateHomeSearchBar,
                        hiddenAppCount = state.settings.hiddenApps.size,
                        onUnhideAll = model::unhideAll,
                        onLayoutLock = model::updateLayoutLocked,
                        onAddWidget = requestWidget,
                        onDrawerSuggestions = model::updateDrawerSuggestions,
                        onDrawerAutoKeyboard = model::updateDrawerAutoKeyboard,
                        onClearSuggestions = model::clearSuggestions,
                        onClockStyle = model::updateClockStyle,
                        onClockAlignment = model::updateClockAlignment,
                        onClockTimeFormat = model::updateClockTimeFormat,
                        onClockFont = model::updateClockFont,
                        onClockSize = model::updateClockSize,
                        onShowDate = model::updateShowDate,
                        onShowSeconds = model::updateShowSeconds,
                        onSwipeUp = model::updateSwipeUp,
                        onSwipeDown = model::updateSwipeDown,
                        onDoubleTap = model::updateDoubleTap,
                        onPinch = model::updatePinch,
                        onHaptics = model::updateHaptics,
                        onHomeSettings = model.systemActions::openHomeSettings,
                        onWallpaper = model.systemActions::openWallpaperPicker,
                        onExport = { controller.exportJson(model.exportJson()) },
                        onImport = controller::importJson,
                    )
                }
            }

            SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }

        val accent = visualTokens(state.settings.visualStyle).accent
        val appearance = state.settings.iconAppearance
        state.selectedApp?.let { app ->
            AppActionsSheet(
                app = app,
                iconLoader = model.iconLoader,
                appearance = appearance,
                accent = accent,
                pinned = state.settings.homeItems.any { app.id in it.appIds },
                docked = app.id in state.settings.dockApps,
                hasCustomLabel = app.id in state.settings.customLabels,
                shortcuts = state.shortcuts,
                onDismiss = { model.selectApp(null) },
                onTogglePin = {
                    if (state.settings.homeItems.any { app.id in it.appIds }) model.unpin(app) else model.pin(app)
                },
                onToggleDock = { model.toggleDock(app) },
                onHide = { model.toggleHidden(app) },
                onInfo = { model.systemActions.openAppInfo(app) },
                onShortcut = model::launchShortcut,
                onRename = { label -> model.renameApp(app.id, label) },
            )
        }
        state.openedFolder?.let { folder ->
            FolderDialog(
                folder = folder,
                entries = folder.appIds.map { id -> FolderAppEntry(id, state.appsById[id]) },
                iconLoader = model.iconLoader,
                appearance = appearance,
                accent = accent,
                editable = !state.settings.layoutLocked,
                onDismiss = { model.openFolder(null) },
                onLaunch = { app, source ->
                    model.launch(app, source)
                    model.openFolder(null)
                },
                onRename = { model.renameFolder(folder.id, it) },
                onRemoveApp = { model.removeFolderApp(folder.id, it) },
                onRemove = { model.removeHomeItem(folder); model.openFolder(null) },
            )
        }
    }
}

/** Overlays sit above a live Home screen; make sure touches on their empty areas never reach it. */
private fun Modifier.blockTouchesBelow(): Modifier = pointerInput(Unit) {}
