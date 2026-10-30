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
import androidx.compose.material3.OutlinedTextFieldDefaults
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

private val SheetColor = Color(0xFF141416)
private val SheetCard = Color(0xFF1E1E21)
private val UninstallColor = Color(0xFFFF8A8A)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetColor,
        contentColor = Color.White,
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
                        color = Color.White.copy(.55f),
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accent,
                        cursorColor = accent,
                        focusedLabelColor = accent,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (hasCustomLabel) TextButton(onClick = { onRename(""); renaming = false }) {
                        Text("Use original name", color = Color.White.copy(.75f))
                    }
                    TextButton(onClick = { onRename(name); renaming = false }, enabled = name.isNotBlank()) {
                        Text("Save", color = accent)
                    }
                }
            }

            if (shortcuts.isNotEmpty()) {
                Text(
                    "Shortcuts",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(.6f),
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
                Surface(color = SheetCard, shape = RoundedCornerShape(20.dp)) {
                    Column {
                        shortcuts.take(5).forEach { shortcut ->
                            Row(
                                Modifier.fillMaxWidth().clickable(role = Role.Button) { closeThen { onShortcut(shortcut) } }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(shortcut.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetAction(Icons.Default.PushPin, if (pinned) "Unpin" else "Add to Home", accent) { closeThen(onTogglePin) }
                SheetAction(Icons.Default.Dock, if (docked) "Undock" else "Dock", accent) { closeThen(onToggleDock) }
                SheetAction(Icons.Default.Edit, "Rename", accent) { renaming = !renaming }
                SheetAction(Icons.Default.VisibilityOff, "Hide", accent) { closeThen(onHide) }
                SheetAction(Icons.Default.Info, "App info", accent) { closeThen(onInfo) }
            }
            if (!app.isSystemApp) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(18.dp)).background(SheetCard)
                        .clickable(role = Role.Button, onClickLabel = "Uninstall ${app.label}") { closeThen(onUninstall) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = UninstallColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Uninstall", color = UninstallColor, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun RowScope.SheetAction(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(SheetCard)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
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
            color = SheetColor.copy(alpha = .97f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(.1f)),
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accent,
                                cursorColor = accent,
                                focusedLabelColor = accent,
                            ),
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
                                tint = accent,
                            )
                        }
                    }
                }
                if (entries.none { it.app != null }) {
                    Text(
                        "The apps in this folder are no longer available",
                        color = Color.White.copy(.7f),
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
                                            labelColor = Color.White,
                                            labelShadow = false,
                                            badged = app.badgeKey in badges,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { source -> if (!editing) onLaunch(app, source) },
                                        )
                                    } else {
                                        Text(
                                            entry.id.component.substringBefore('/').ifBlank { "Unavailable app" },
                                            color = Color.White.copy(.5f),
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
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A8A))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove folder from Home", color = Color(0xFFFF8A8A))
                    }
                }
            }
        }
    }
}
