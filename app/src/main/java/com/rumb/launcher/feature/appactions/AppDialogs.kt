package com.rumb.launcher.feature.appactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rumb.launcher.domain.AppId
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

data class FolderAppEntry(val id: AppId, val app: InstalledApp?)

@Composable
fun FolderDialog(
    folder: HomeItem,
    entries: List<FolderAppEntry>,
    editable: Boolean,
    onDismiss: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (AppId) -> Unit,
    onRemove: () -> Unit,
) {
    var title by remember(folder.id, folder.title) { mutableStateOf(folder.title.ifBlank { "Folder" }) }
    val normalizedTitle = title.trim()
    val validTitle = normalizedTitle.length in 1..48
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.title.ifBlank { "Folder" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = { Column {
            if (editable) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 48) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Folder name") },
                    supportingText = { Text("1–48 characters") },
                    singleLine = true,
                    isError = title.isNotEmpty() && !validTitle,
                )
                TextButton(
                    onClick = { onRename(normalizedTitle) },
                    modifier = Modifier.align(Alignment.End),
                    enabled = validTitle && normalizedTitle != folder.title,
                ) { Text("Save name") }
            }
            if (entries.none { it.app != null }) Text("Some or all apps in this folder are no longer available")
            entries.forEach { entry ->
                val app = entry.app
                val label = app?.label ?: entry.id.component.substringBefore('/').ifBlank { "Unavailable app" }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { app?.let(onLaunch) },
                        modifier = Modifier.weight(1f),
                        enabled = app != null,
                    ) {
                        Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (editable) IconButton(onClick = { onRemoveApp(entry.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove $label from folder")
                    }
                }
            }
            if (editable) TextButton(onClick = onRemove, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Remove folder", Modifier.weight(1f))
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable private fun Action(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    TextButton(onClick = action, Modifier.fillMaxWidth()) { Icon(icon, null); Spacer(Modifier.width(10.dp)); Text(label, Modifier.weight(1f)) }
}
