package com.rumb.launcher.feature.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppTile

@Composable
fun AppDrawerScreen(
    state: LauncherUiState,
    iconLoader: AppIconLoader,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home") }
                OutlinedTextField(
                    value = state.search, onValueChange = onSearch, modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Search apps") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(26.dp),
                )
            }
            Text(
                if (state.search.isBlank()) "All apps" else "${state.filteredApps.size} results",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            )
            when {
                state.catalog.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.filteredApps.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (state.catalog.errorMessage != null) state.catalog.errorMessage else if (state.search.isBlank()) "No launchable apps are available" else "No matching apps")
                        if (state.catalog.errorMessage != null) {
                            Button(onClick = onRefresh, modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
                        }
                    }
                }
                else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val adaptiveMinimum = if (maxWidth >= 720.dp) 112.dp else 82.dp
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(adaptiveMinimum), modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.filteredApps, key = { it.id.stableKey }, contentType = { "app" }) { app ->
                            AppTile(
                                app = app, iconLoader = iconLoader, iconSize = state.settings.iconSize.dp,
                                iconShape = state.settings.iconShape, themed = state.settings.themedIcons,
                                homeStyle = false,
                                onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}
