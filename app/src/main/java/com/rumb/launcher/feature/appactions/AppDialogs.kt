package com.rumb.launcher.feature.appactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.AppShortcut
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.InstalledApp

@Composable
fun AppActionsDialog(
    app: InstalledApp,
    pinned: Boolean,
    docked: Boolean,
    shortcuts: List<AppShortcut>,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onShortcut: (AppShortcut) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Action(if (pinned) "Remove from home" else "Add to home", Icons.Default.PushPin, onTogglePin)
            Action(if (docked) "Remove from dock" else "Add to dock", Icons.Default.Dock, onToggleDock)
            shortcuts.take(4).forEach { shortcut -> Action("Shortcut: ${shortcut.label}", Icons.Default.PushPin) { onShortcut(shortcut) } }
            Action("Hide from drawer", Icons.Default.VisibilityOff, onHide)
            Action("App info", Icons.Default.Info, onInfo)
            Action("Uninstall", Icons.Default.Delete, onUninstall)
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun FolderDialog(
    folder: HomeItem,
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.title.ifBlank { "Folder" }) },
        text = { Column {
            if (apps.isEmpty()) Text("The apps in this folder are no longer available")
            apps.forEach { app -> TextButton(onClick = { onLaunch(app) }, Modifier.fillMaxWidth()) { Text(app.label, Modifier.weight(1f)) } }
            TextButton(onClick = onRemove, Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Remove folder", Modifier.weight(1f)) }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable private fun Action(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    TextButton(onClick = action, Modifier.fillMaxWidth()) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Text(label, Modifier.weight(1f)) }
}
