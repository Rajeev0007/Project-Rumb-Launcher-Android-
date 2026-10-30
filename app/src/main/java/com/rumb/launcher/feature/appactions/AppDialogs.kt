package com.rumb.launcher.feature.appactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.AppShortcut
import com.rumb.launcher.domain.HomeItem
import com.rumb.launcher.domain.IconAppearance
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.platform.apps.LaunchSource
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppIconImage
import com.rumb.launcher.ui.components.AppTile
import kotlinx.coroutines.launch

private val UninstallColor = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: InstalledApp,
    iconLoader: AppIconLoader,
    appearance: IconAppearance,
    accent: Color,
    pinned: Boolean,
    docked: Boolean,
    hasCustomLabel: Boolean,
    shortcuts: List<AppShortcut>,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onShortcut: (AppShortcut) -> Unit,
    onRename: (String) -> Unit,
    onUninstall: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var renaming by remember(app.id) { mutableStateOf(false) }
    var name by remember(app.id) { mutableStateOf(app.label) }
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(app.id, iconLoader, 52.dp, appearance, contentDescription = null)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(app.packageName, app.profileLabel?.let { "$it profile" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (renaming) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 48) name = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true,
                    label = { Text("App name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onRename(name); renaming = false }),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (hasCustomLabel) TextButton(onClick = { onRename(""); renaming = false }) {
                        Text("Use original name")
                    }
                    TextButton(onClick = { onRename(name); renaming = false }, enabled = name.isNotBlank()) {
                        Text("Save")
                    }
                }
            }

            if (shortcuts.isNotEmpty()) {
                Text(
                    "Shortcuts",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp, start = 4.dp),
                )
                Surface(color = colors.surfaceContainerHigh, shape = RoundedCornerShape(20.dp)) {
                    Column {
                        shortcuts.take(5).forEach { shortcut ->
                            Row(
                                Modifier.fillMaxWidth().clickable(role = Role.Button) { closeThen { onShortcut(shortcut) } }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(shortcut.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            val actions = buildList {
                add(SheetActionItem(Icons.Default.PushPin, if (pinned) "Remove from Home" else "Add to Home") { closeThen(onTogglePin) })
                add(SheetActionItem(Icons.Default.Dock, if (docked) "Remove from dock" else "Add to dock") { closeThen(onToggleDock) })
                add(SheetActionItem(Icons.Default.Edit, "Rename") { renaming = !renaming })
                add(SheetActionItem(Icons.Default.VisibilityOff, "Hide") { closeThen(onHide) })
                add(SheetActionItem(Icons.Default.Info, "App info") { closeThen(onInfo) })
                if (!app.isSystemApp) add(SheetActionItem(Icons.Default.Delete, "Uninstall", destructive = true) { closeThen(onUninstall) })
            }
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { SheetAction(it) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

private class SheetActionItem(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun RowScope.SheetAction(item: SheetActionItem) {
    val colors = MaterialTheme.colorScheme
    val tint = if (item.destructive) UninstallColor else colors.primary
    Column(
        Modifier.weight(1f).heightIn(min = 84.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = item.onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (item.destructive) UninstallColor else colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

data class FolderAppEntry(val id: AppId, val app: InstalledApp?)

@Composable
fun FolderDialog(
    folder: HomeItem,
    entries: List<FolderAppEntry>,
    iconLoader: AppIconLoader,
    appearance: IconAppearance,
    accent: Color,
    editable: Boolean,
    badges: Set<String>,
    onDismiss: () -> Unit,
    onLaunch: (InstalledApp, LaunchSource?) -> Unit,
    onRename: (String) -> Unit,
    onRemoveApp: (AppId) -> Unit,
    onRemove: () -> Unit,
) {
    var editing by remember(folder.id) { mutableStateOf(false) }
    var title by remember(folder.id, folder.title) { mutableStateOf(folder.title.ifBlank { "Folder" }) }
    val normalizedTitle = title.trim()
    val validTitle = normalizedTitle.length in 1..48
    fun finishEditing() {
        if (validTitle && normalizedTitle != folder.title) onRename(normalizedTitle)
        editing = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { if (it.length <= 48) title = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = !validTitle,
                            label = { Text("Folder name") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { finishEditing() }),
                        )
                    } else {
                        Text(
                            folder.title.ifBlank { "Folder" },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (editable) {
                        IconButton(onClick = { if (editing) finishEditing() else editing = true }) {
                            Icon(
                                if (editing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (editing) "Finish editing folder" else "Edit folder",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (entries.none { it.app != null }) {
                    Text(
                        "The apps in this folder are no longer available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    entries.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { entry ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                                    val app = entry.app
                                    if (app != null) {
                                        AppTile(
                                            app = app,
                                            iconLoader = iconLoader,
                                            iconSize = 50.dp,
                                            appearance = appearance,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            labelShadow = false,
                                            badged = app.badgeKey in badges,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { source -> if (!editing) onLaunch(app, source) },
                                        )
                                    } else {
                                        Text(
                                            entry.id.component.substringBefore('/').ifBlank { "Unavailable app" },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                        )
                                    }
                                    if (editing) {
                                        Box(
                                            Modifier.padding(2.dp).size(24.dp).clip(CircleShape).background(Color(0xFFE5484D))
                                                .clickable(role = Role.Button, onClickLabel = "Remove from folder") { onRemoveApp(entry.id) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Remove from folder", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (editing) {
                    TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = UninstallColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Remove folder from Home", color = UninstallColor)
                    }
                }
            }
        }
    }
}
