package com.rumb.launcher.ui

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rumb.launcher.data.AppRepository
import com.rumb.launcher.data.LauncherApp
import com.rumb.launcher.data.LauncherPreferences
import com.rumb.launcher.data.LauncherTheme
import com.rumb.launcher.ui.theme.RumbLauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class LauncherScreen { HOME, DRAWER, SETTINGS }

@Composable
fun RumbLauncherRoot() {
    val context = LocalContext.current
    val preferences = remember { LauncherPreferences(context) }
    var theme by remember { mutableStateOf(preferences.theme) }

    RumbLauncherTheme(theme) {
        LauncherContent(
            preferences = preferences,
            theme = theme,
            onThemeChange = {
                theme = it
                preferences.theme = it
            },
        )
    }
}

@Composable
private fun LauncherContent(
    preferences: LauncherPreferences,
    theme: LauncherTheme,
    onThemeChange: (LauncherTheme) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(LauncherScreen.HOME) }
    var favoriteKeys by remember { mutableStateOf(preferences.favoriteKeys) }
    var columns by remember { mutableIntStateOf(preferences.columns) }
    var iconSize by remember { mutableIntStateOf(preferences.iconSize) }
    var selectedApp by remember { mutableStateOf<LauncherApp?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshKey) {
        isLoading = true
        apps = withContext(Dispatchers.IO) { repository.loadApps() }
        val validKeys = apps.mapTo(mutableSetOf()) { it.key }
        val cleanedFavorites = favoriteKeys.intersect(validKeys)
        if (cleanedFavorites != favoriteKeys) {
            favoriteKeys = cleanedFavorites
            preferences.favoriteKeys = cleanedFavorites
        }
        isLoading = false
    }

    BackHandler(enabled = screen != LauncherScreen.HOME) {
        screen = LauncherScreen.HOME
    }

    when (screen) {
        LauncherScreen.HOME -> HomeScreen(
            favorites = apps.filter { it.key in favoriteKeys },
            columns = columns,
            iconSize = iconSize,
            isLoading = isLoading,
            onOpenDrawer = { screen = LauncherScreen.DRAWER },
            onOpenSettings = { screen = LauncherScreen.SETTINGS },
            onWallpaper = { safeAction(context, repository::openWallpaperPicker) },
            onLaunch = { safeAction(context) { repository.launch(it) } },
            onLongPress = { selectedApp = it },
        )
        LauncherScreen.DRAWER -> AppDrawerScreen(
            apps = apps,
            columns = columns,
            iconSize = iconSize,
            isLoading = isLoading,
            onBack = { screen = LauncherScreen.HOME },
            onLaunch = { safeAction(context) { repository.launch(it) } },
            onLongPress = { selectedApp = it },
        )
        LauncherScreen.SETTINGS -> SettingsScreen(
            columns = columns,
            iconSize = iconSize,
            theme = theme,
            onBack = { screen = LauncherScreen.HOME },
            onColumnsChange = {
                columns = it
                preferences.columns = it
            },
            onIconSizeChange = {
                iconSize = it
                preferences.iconSize = it
            },
            onThemeChange = onThemeChange,
            onHomeSettings = { safeAction(context, repository::openHomeSettings) },
            onWallpaper = { safeAction(context, repository::openWallpaperPicker) },
        )
    }

    selectedApp?.let { app ->
        AppActionsDialog(
            app = app,
            isFavorite = app.key in favoriteKeys,
            onDismiss = { selectedApp = null },
            onToggleFavorite = {
                favoriteKeys = if (app.key in favoriteKeys) {
                    favoriteKeys - app.key
                } else {
                    favoriteKeys + app.key
                }
                preferences.favoriteKeys = favoriteKeys
                selectedApp = null
            },
            onAppInfo = {
                safeAction(context) { repository.openAppInfo(app) }
                selectedApp = null
            },
            onUninstall = {
                safeAction(context) { repository.requestUninstall(app) }
                selectedApp = null
            },
        )
    }
}

@Composable
private fun HomeScreen(
    favorites: List<LauncherApp>,
    columns: Int,
    iconSize: Int,
    isLoading: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Clock(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp))

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No favorites yet\nLong-press an app in the drawer to pin it",
                        color = Color.White.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                AppGrid(
                    apps = favorites,
                    columns = columns,
                    iconSize = iconSize,
                    homeStyle = true,
                    modifier = Modifier.weight(1f),
                    onLaunch = onLaunch,
                    onLongPress = onLongPress,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    HomeAction(Icons.Default.Palette, "Wallpaper", onWallpaper)
                    HomeAction(Icons.Default.Apps, "Apps", onOpenDrawer)
                    HomeAction(Icons.Default.Settings, "Settings", onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }

    Column(modifier = modifier) {
        Text(
            text = now.format(timeFormatter),
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = now.format(dateFormatter),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun HomeAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color.White)
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawerScreen(
    apps: List<LauncherApp>,
    columns: Int,
    iconSize: Int,
    isLoading: Boolean,
    onBack: () -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val filteredApps = remember(apps, search) {
        if (search.isBlank()) apps else apps.filter { it.label.contains(search, ignoreCase = true) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(24.dp),
                )
            }

            Text(
                text = if (search.isBlank()) "All apps" else "${filteredApps.size} results",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                filteredApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching apps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> AppGrid(
                    apps = filteredApps,
                    columns = columns,
                    iconSize = iconSize,
                    modifier = Modifier.weight(1f),
                    onLaunch = onLaunch,
                    onLongPress = onLongPress,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    columns: Int,
    iconSize: Int,
    theme: LauncherTheme,
    onBack: () -> Unit,
    onColumnsChange: (Int) -> Unit,
    onIconSizeChange: (Int) -> Unit,
    onThemeChange: (LauncherTheme) -> Unit,
    onHomeSettings: () -> Unit,
    onWallpaper: () -> Unit,
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Rumb Launcher settings", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsHeading("Home screen grid")
            OptionRow(
                options = listOf(4, 5, 6),
                selected = columns,
                label = { "$it columns" },
                onSelect = onColumnsChange,
            )

            SettingsHeading("App icon size")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(44, 52, 60, 68).forEach { size ->
                    ChoiceButton(
                        selected = iconSize == size,
                        label = "$size dp",
                        onClick = { onIconSizeChange(size) },
                    )
                }
            }

            SettingsHeading("Theme")
            OptionRow(
                options = LauncherTheme.entries,
                selected = theme,
                label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onSelect = onThemeChange,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            Button(onClick = onHomeSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose default home app")
            }
            OutlinedButton(
                onClick = onWallpaper,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change wallpaper")
            }
            Text(
                text = "Rumb Launcher 1.0.0",
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun <T> OptionRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            ChoiceButton(
                selected = option == selected,
                label = label(option),
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun ChoiceButton(selected: Boolean, label: String, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(onClick = onClick) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun AppGrid(
    apps: List<LauncherApp>,
    columns: Int,
    iconSize: Int,
    modifier: Modifier = Modifier,
    homeStyle: Boolean = false,
    onLaunch: (LauncherApp) -> Unit,
    onLongPress: (LauncherApp) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            AppItem(
                app = app,
                iconSize = iconSize,
                homeStyle = homeStyle,
                onClick = { onLaunch(app) },
                onLongPress = { onLongPress(app) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    app: LauncherApp,
    iconSize: Int,
    homeStyle: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val bitmap = remember(app.key, app.icon) { drawableToBitmap(app.icon).asImageBitmap() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(iconSize.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (homeStyle) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun AppActionsDialog(
    app: LauncherApp,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PushPin, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (isFavorite) "Remove from home" else "Pin to home", Modifier.weight(1f))
                }
                TextButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("App info", Modifier.weight(1f))
                }
                TextButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Uninstall", Modifier.weight(1f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun drawableToBitmap(drawable: Drawable, size: Int = 144): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val oldBounds = android.graphics.Rect(drawable.bounds)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    drawable.bounds = oldBounds
    return bitmap
}

private fun safeAction(context: android.content.Context, action: () -> Unit) {
    try {
        action()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "This action is not available on this device", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Android blocked this action", Toast.LENGTH_SHORT).show()
    }
}
