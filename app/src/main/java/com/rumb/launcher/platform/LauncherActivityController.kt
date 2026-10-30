package com.rumb.launcher.platform

import android.appwidget.AppWidgetHostView

data class WidgetSelection(
    val appWidgetId: Int,
    val provider: String,
    val minWidthPx: Int,
    val minHeightPx: Int,
)

interface LauncherActivityController {
    var onImportReady: ((json: String) -> Unit)?
    var onWidgetReady: ((widget: WidgetSelection) -> Boolean)?
    var onError: ((message: String) -> Unit)?
    /** Invoked when the user presses Home while Rumb is already the foreground launcher. */
    var onHomeIntent: (() -> Unit)?
    /** Invoked when Android asks apps to release memory. */
    var onMemoryPressure: (() -> Unit)?

    fun exportJson(json: String)
    fun importJson()
    fun requestWidget()
    fun requestWidgetProvider(providerFlattened: String)
    fun createWidgetView(appWidgetId: Int, expectedProvider: String): AppWidgetHostView?
    fun updateWidgetSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int)
    fun deleteWidgetId(appWidgetId: Int)
    fun reconcileWidgetIds(activeWidgetIds: Set<Int>)
}
