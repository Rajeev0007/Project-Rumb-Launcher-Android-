package com.rumb.launcher.feature.drawer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rumb.launcher.LauncherUiState
import com.rumb.launcher.domain.InstalledApp
import com.rumb.launcher.domain.VisualStyle
import com.rumb.launcher.platform.icons.AppIconLoader
import com.rumb.launcher.ui.components.AppTile
import com.rumb.launcher.ui.components.VisualBackdrop
import com.rumb.launcher.ui.components.VisualDrawerSurface
import com.rumb.launcher.ui.components.visualTokens

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
    val style = state.settings.visualStyle
    val tokens = visualTokens(style)
    VisualBackdrop(style = style, modifier = Modifier.fillMaxSize(), opaque = true) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(top = 10.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape).background(tokens.mutedForeground.copy(alpha = .42f)),
            )
            VisualDrawerSurface(
                style = style,
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 10.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home", tint = tokens.foreground)
                        }
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text("App Library", color = tokens.foreground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.search.isBlank()) "${state.filteredApps.size} apps" else "${state.filteredApps.size} matches",
                                color = tokens.mutedForeground,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.search,
                        onValueChange = onSearch,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        singleLine = true,
                        placeholder = { Text("Search apps and packages", color = tokens.mutedForeground) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = tokens.accent) },
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = tokens.foreground,
                            unfocusedTextColor = tokens.foreground,
                            focusedContainerColor = tokens.surface,
                            unfocusedContainerColor = tokens.surface,
                            focusedIndicatorColor = tokens.accent,
                            unfocusedIndicatorColor = tokens.border,
                            cursorColor = tokens.accent,
                        ),
                    )
                    Text(
                        if (state.search.isBlank()) "All apps" else "Search results",
                        Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        color = tokens.foreground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    when {
                        state.catalog.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = tokens.accent)
                        }
                        state.filteredApps.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (state.catalog.errorMessage != null) state.catalog.errorMessage
                                    else if (state.search.isBlank()) "No launchable apps are available"
                                    else "No apps match your search",
                                    color = tokens.foreground,
                                    textAlign = TextAlign.Center,
                                )
                                if (state.catalog.errorMessage != null) {
                                    Button(onClick = onRefresh, modifier = Modifier.padding(top = 14.dp)) { Text("Retry") }
                                }
                            }
                        }
                        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                            val adaptiveMinimum = when {
                                maxWidth >= 840.dp -> 124.dp
                                maxWidth >= 600.dp -> 104.dp
                                else -> 84.dp
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(adaptiveMinimum),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.filteredApps, key = { it.id.stableKey }, contentType = { "app" }) { app ->
                                    AppTile(
                                        app = app,
                                        iconLoader = iconLoader,
                                        iconSize = state.settings.iconSize.dp,
                                        iconShape = state.settings.iconShape,
                                        themed = state.settings.themedIcons,
                                        homeStyle = style != VisualStyle.SOFT_PANEL,
                                        onClick = { onLaunch(app) },
                                        onLongClick = { onLongPress(app) },
                                        labelColor = tokens.foreground,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
