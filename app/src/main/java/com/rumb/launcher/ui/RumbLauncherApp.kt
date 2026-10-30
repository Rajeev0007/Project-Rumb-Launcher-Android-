package com.rumb.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rumb.launcher.Destination
import com.rumb.launcher.LauncherViewModel
import com.rumb.launcher.domain.HomeItemType
import com.rumb.launcher.feature.appactions.AppActionsDialog
import com.rumb.launcher.feature.appactions.FolderAppEntry
import com.rumb.launcher.feature.appactions.FolderDialog
import com.rumb.launcher.feature.drawer.AppDrawerScreen
import com.rumb.launcher.feature.home.HomeScreen
import com.rumb.launcher.feature.settings.SettingsScreen
import com.rumb.launcher.platform.LauncherActivityController
import com.rumb.launcher.ui.theme.RumbLauncherTheme

@Composable
fun RumbLauncherRoot(controller: LauncherActivityController) {
    val context = LocalContext.current
    val model: LauncherViewModel = viewModel(factory = LauncherViewModel.factory(context.applicationContext))
    val state by model.uiState.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }

    DisposableEffect(controller, model) {
        controller.onImportReady = model::importJson
        controller.onWidgetReady = model::addWidget
        controller.onError = model::showMessage
        onDispose {
            controller.onImportReady = null
            controller.onWidgetReady = null
            controller.onError = null
        }
    }
    val activeWidgetIds = state.settings.homeItems
        .filter { it.type == HomeItemType.WIDGET }
        .mapNotNull { it.widgetId }
        .toSet()
    LaunchedEffect(activeWidgetIds) { controller.reconcileWidgetIds(activeWidgetIds) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbars.showSnackbar(it); model.consumeMessage() }
    }
    BackHandler(state.destination != Destination.HOME) { model.navigate(Destination.HOME) }

    RumbLauncherTheme(state.settings.theme, state.settings.dynamicColor) {
        Box(Modifier.fillMaxSize()) {
            when (state.destination) {
                Destination.HOME -> HomeScreen(
                    state,
                    model.iconLoader,
                    onOpenDrawer = { model.navigate(Destination.DRAWER) },
                    onOpenSettings = { model.navigate(Destination.SETTINGS) },
                    onWallpaper = model.systemActions::openWallpaperPicker,
                    onLaunch = model::launch,
                    onOpenFolder = model::openFolder,
                    onRefreshApps = model.catalog::refresh,
                    onMoveItem = model::moveItem,
                    onGesture = model::performGesture,
                    onCreateWidgetView = controller::createWidgetView,
                    onUpdateWidgetSize = controller::updateWidgetSize,
                    onRemoveWidget = { itemId ->
                        model.removeWidget(itemId)?.let(controller::deleteWidgetId)
                    },
                )
                Destination.DRAWER -> AppDrawerScreen(
                    state,
                    model.iconLoader,
                    onBack = { model.navigate(Destination.HOME) },
                    onSearch = model::search,
                    onRefresh = model.catalog::refresh,
                    onLaunch = model::launch,
                    onLongPress = model::selectApp,
                )
                Destination.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    onBack = { model.navigate(Destination.HOME) },
                    onTheme = model::updateTheme,
                    onDynamicColor = model::updateDynamicColor,
                    onIconShape = model::updateIconShape,
                    onThemedIcons = model::updateThemedIcons,
                    onColumns = model::updateColumns,
                    onRows = model::updateRows,
                    onIconSize = model::updateIconSize,
                    onPages = model::updatePageCount,
                    hiddenAppCount = state.settings.hiddenApps.size,
                    onUnhideAll = model::unhideAll,
                    onLayoutLock = model::updateLayoutLocked,
                    onAddWidget = controller::requestWidget,
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
                    onHomeSettings = model.systemActions::openHomeSettings,
                    onWallpaper = model.systemActions::openWallpaperPicker,
                    onExport = { controller.exportJson(model.exportJson()) },
                    onImport = controller::importJson,
                )
            }
            SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
        }

        state.selectedApp?.let { app ->
            val pinned = state.settings.homeItems.any { app.id in it.appIds }
            AppActionsDialog(
                app = app,
                pinned = pinned,
                docked = app.id in state.settings.dockApps,
                shortcuts = state.shortcuts,
                onDismiss = { model.selectApp(null) },
                onTogglePin = { if (pinned) model.unpin(app) else model.pin(app); model.selectApp(null) },
                onToggleDock = { model.toggleDock(app); model.selectApp(null) },
                onHide = { model.toggleHidden(app); model.selectApp(null) },
                onInfo = { model.systemActions.openAppInfo(app); model.selectApp(null) },
                onShortcut = { model.launchShortcut(it); model.selectApp(null) },
            )
        }
        state.openedFolder?.let { folder ->
            FolderDialog(
                folder = folder,
                entries = folder.appIds.map { id ->
                    FolderAppEntry(id, state.catalog.apps.firstOrNull { it.id == id })
                },
                editable = !state.settings.layoutLocked,
                onDismiss = { model.openFolder(null) },
                onLaunch = model::launch,
                onRename = { model.renameFolder(folder.id, it) },
                onRemoveApp = { model.removeFolderApp(folder.id, it) },
                onRemove = { model.removeHomeItem(folder); model.openFolder(null) },
            )
        }
    }
}
