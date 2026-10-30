/*
 * Copyright 2026, Rajeev0007 (Rumb Launcher)
 *
 * Licensed under the GNU General Public License, version 3 or later.
 */

package app.lawnchair.ui.preferences.destinations

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.hotseat.HotseatMode
import app.lawnchair.icons.shape.IconShape
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.theme.color.ColorOption
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.WallpaperAccessPermissionDialog
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.util.FileAccessManager
import app.lawnchair.util.FileAccessState
import com.android.launcher3.R
import com.patrykmichalik.opto.core.setBlocking

/** A one-tap look: icon and folder shape, dock, labels, folders, drawer, and accent together. */
private data class RumbStyle(
    val name: String,
    val description: String,
    val iconShape: String,
    val dockBackground: Boolean,
    val dockColor: String,
    val dockAlpha: Int,
    val dockRadius: Float,
    val dockGradient: Boolean,
    val dockSearch: String,
    val labels: Boolean,
    val folderOpacity: Float,
    val drawerOpacity: Float,
    val accent: String,
    val wantsBlur: Boolean,
    val preview: List<Color>,
)

private val RumbStylePresets = listOf(
    RumbStyle(
        name = "Pixel",
        description = "Clean circles, search in the dock, wallpaper colours",
        iconShape = "circle", dockBackground = false, dockColor = "default", dockAlpha = 0,
        dockRadius = 24f, dockGradient = false, dockSearch = "lawnchair", labels = true,
        folderOpacity = 1f, drawerOpacity = 1f, accent = "system_accent", wantsBlur = false,
        preview = listOf(Color(0xFF1B6EF3), Color(0xFF7BB3FF)),
    ),
    RumbStyle(
        name = "Glass",
        description = "iPhone-style frosted dock, rounded icons, see-through folders",
        iconShape = "cupertino", dockBackground = true, dockColor = "custom|#ffffffff", dockAlpha = 22,
        dockRadius = 32f, dockGradient = false, dockSearch = "disabled", labels = true,
        folderOpacity = 0.6f, drawerOpacity = 0.55f, accent = "system_accent", wantsBlur = true,
        preview = listOf(Color(0xFF6A8CFF), Color(0xFFFF9FD2)),
    ),
    RumbStyle(
        name = "One UI",
        description = "Soft squircles, calm dock, solid drawer",
        iconShape = "squircle", dockBackground = false, dockColor = "default", dockAlpha = 0,
        dockRadius = 28f, dockGradient = false, dockSearch = "disabled", labels = true,
        folderOpacity = 0.9f, drawerOpacity = 1f, accent = "custom|#ff3e8bff", wantsBlur = false,
        preview = listOf(Color(0xFF0F3D7A), Color(0xFF3E8BFF)),
    ),
    RumbStyle(
        name = "Gradient",
        description = "Rumb violet accent with a colour-fade dock",
        iconShape = "squircle", dockBackground = true, dockColor = "custom|#ff7b5cff", dockAlpha = 70,
        dockRadius = 30f, dockGradient = true, dockSearch = "disabled", labels = true,
        folderOpacity = 0.85f, drawerOpacity = 0.92f, accent = "custom|#ff5b48c9", wantsBlur = false,
        preview = listOf(Color(0xFF7B5CFF), Color(0xFF1FB6FF)),
    ),
    RumbStyle(
        name = "Minimal",
        description = "No labels, no dock chrome, nothing in the way",
        iconShape = "roundedSquare", dockBackground = false, dockColor = "default", dockAlpha = 0,
        dockRadius = 20f, dockGradient = false, dockSearch = "disabled", labels = false,
        folderOpacity = 1f, drawerOpacity = 1f, accent = "custom|#ff6e7687", wantsBlur = false,
        preview = listOf(Color(0xFF26282E), Color(0xFF575B66)),
    ),
)

private fun applyStyle(
    context: Context,
    style: RumbStyle,
    prefs: PreferenceManager,
    prefs2: PreferenceManager2,
    canBlur: Boolean,
) {
    IconShape.fromString(style.iconShape, context)?.let {
        prefs2.iconShape.setBlocking(it)
        prefs2.folderShape.setBlocking(it)
    }
    prefs.hotseatBG.set(style.dockBackground)
    prefs.hotseatBGAlpha.set(style.dockAlpha)
    prefs.rumbDockGradient.set(style.dockGradient)
    prefs2.hotseatBackgroundColor.setBlocking(ColorOption.fromString(style.dockColor))
    prefs2.hotseatBackgroundCornerRadius.setBlocking(style.dockRadius)
    prefs2.hotseatMode.setBlocking(HotseatMode.fromString(style.dockSearch))
    prefs2.showIconLabelsOnHomeScreen.setBlocking(style.labels)
    prefs2.folderBackgroundOpacity.setBlocking(style.folderOpacity)
    prefs2.folderPreviewBackgroundOpacity.setBlocking(style.folderOpacity)
    // A see-through drawer only looks like glass over a blurred wallpaper; without blur
    // permission, keep it mostly solid so text stays readable.
    prefs.drawerOpacity.set(if (style.wantsBlur && !canBlur) 0.88f else style.drawerOpacity)
    if (style.wantsBlur && canBlur) {
        prefs.enableWallpaperBlur.set(true)
        prefs.wallpaperBlur.set(40)
    } else if (!style.wantsBlur) {
        prefs.enableWallpaperBlur.set(false)
    }
    // Accent last: changing it restarts the launcher to rebuild colours.
    prefs2.accentColor.setBlocking(ColorOption.fromString(style.accent))
}

@Composable
fun RumbStylesPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val context = LocalContext.current
    val fileAccessManager = remember { FileAccessManager.getInstance(context) }
    val allFilesAccessState by fileAccessManager.allFilesAccessState.collectAsStateWithLifecycle()
    val wallpaperAccessState by fileAccessManager.wallpaperAccessState.collectAsStateWithLifecycle()
    val hasPermission = wallpaperAccessState != FileAccessState.Denied
    var showPermissionDialog by remember { mutableStateOf(false) }

    PreferenceLayout(
        label = "Styles",
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup(
            heading = "One-tap looks",
            description = "Applies icon shape, dock, folders, drawer and accent together. Rumb restarts once to apply colours.",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RumbStylePresets.forEach { style ->
                    StyleCard(style) {
                        if (style.wantsBlur && !hasPermission) showPermissionDialog = true
                        applyStyle(context, style, prefs, prefs2, hasPermission)
                        Toast.makeText(context, "${style.name} style applied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val enableWallpaperBlur = prefs.enableWallpaperBlur.getAdapter()
        PreferenceGroup(heading = "Blur & glass") {
            SwitchPreference(
                checked = hasPermission && enableWallpaperBlur.state.value,
                onCheckedChange = {
                    if (!hasPermission) showPermissionDialog = true else enableWallpaperBlur.onChange(it)
                },
                label = stringResource(id = R.string.wallpaper_blur),
                description = "Frosted wallpaper behind the app drawer and folders",
            )
            val canBlur = hasPermission && enableWallpaperBlur.state.value
            ExpandAndShrink(visible = canBlur) {
                SliderPreference(
                    label = stringResource(id = R.string.wallpaper_background_blur),
                    adapter = prefs.wallpaperBlur.getAdapter(),
                    step = 5,
                    valueRange = 0..100,
                    showUnit = "%",
                )
            }
            SliderPreference(
                label = "App drawer opacity (lower shows the wallpaper)",
                adapter = prefs.drawerOpacity.getAdapter(),
                valueRange = 0f..1f,
                step = 0.05f,
                showAsPercentage = true,
            )
            SliderPreference(
                label = "Folder opacity",
                adapter = prefs2.folderBackgroundOpacity.getAdapter(),
                valueRange = 0f..1f,
                step = 0.05f,
                showAsPercentage = true,
            )
            SwitchPreference(
                adapter = prefs.recentsTranslucentBackground.getAdapter(),
                label = "Translucent recents",
                description = "See your wallpaper behind recent apps",
            )
        }

        val dockBackground = prefs.hotseatBG.getAdapter()
        PreferenceGroup(heading = "Dock & gradient") {
            SwitchPreference(
                adapter = dockBackground,
                label = "Dock background",
                description = "A rounded panel behind the dock icons",
            )
            ExpandAndShrink(visible = dockBackground.state.value) {
                Column {
                    SwitchPreference(
                        adapter = prefs.rumbDockGradient.getAdapter(),
                        label = "Gradient dock",
                        description = "Fades the dock colour into your accent colour",
                    )
                    SliderPreference(
                        label = "Dock transparency",
                        adapter = prefs.hotseatBGAlpha.getAdapter(),
                        step = 5,
                        valueRange = 0..100,
                        showUnit = "%",
                    )
                    SliderPreference(
                        label = "Dock corner radius",
                        adapter = prefs2.hotseatBackgroundCornerRadius.getAdapter(),
                        valueRange = 0f..48f,
                        step = 1f,
                        showUnit = "dp",
                    )
                }
            }
        }

        PreferenceGroup(
            heading = "Icon changer",
            description = "Long-press any app on Home or in the drawer, tap Edit, then pick a new icon from any installed icon pack or rename the app. Shapes and icon packs are in General.",
        ) {}

        if (showPermissionDialog) {
            WallpaperAccessPermissionDialog(
                managedFilesChecked = allFilesAccessState != FileAccessState.Denied,
                onDismiss = { showPermissionDialog = false },
                onPermissionRequest = { fileAccessManager.refresh() },
            )
        }
        LifecycleResumeEffect(Unit) {
            showPermissionDialog = false
            fileAccessManager.refresh()
            onPauseOrDispose { }
        }
    }
}

@Composable
private fun StyleCard(style: RumbStyle, onClick: () -> Unit) {
    val iconShape: Shape = when (style.iconShape) {
        "circle" -> CircleShape
        "cupertino" -> RoundedCornerShape(30)
        "squircle" -> RoundedCornerShape(36)
        else -> RoundedCornerShape(18)
    }
    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(style.preview)),
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    Box(Modifier.size(22.dp).clip(iconShape).background(Color.White.copy(alpha = 0.92f)))
                }
            }
            val dockModifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .width(124.dp)
                .height(34.dp)
                .clip(RoundedCornerShape((style.dockRadius / 2).dp))
            Box(
                modifier = when {
                    style.dockGradient -> dockModifier.background(Brush.horizontalGradient(listOf(Color(0xCC7B5CFF), Color(0xCC1FB6FF))))
                    style.dockBackground -> dockModifier
                        .background(Color.White.copy(alpha = 0.28f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape((style.dockRadius / 2).dp))
                    else -> dockModifier
                },
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) {
                        Box(Modifier.size(18.dp).clip(iconShape).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(style.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            style.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
