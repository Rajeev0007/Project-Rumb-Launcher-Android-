package com.rumb.launcher.platform

interface LauncherActivityController {
    var onImportReady: ((json: String) -> Unit)?
    var onError: ((message: String) -> Unit)?

    fun exportJson(json: String)
    fun importJson()
}
