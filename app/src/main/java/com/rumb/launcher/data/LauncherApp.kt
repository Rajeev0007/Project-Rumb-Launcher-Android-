package com.rumb.launcher.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
) {
    val componentName: ComponentName = ComponentName(packageName, activityName)
    val key: String = componentName.flattenToString()
}
