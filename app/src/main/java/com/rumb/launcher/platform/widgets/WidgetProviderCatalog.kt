package com.rumb.launcher.platform.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.UserManager
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A profile-aware, display-safe description of an installed widget provider. */
data class InstalledWidgetProvider(
    val providerFlattened: String,
    val profileSerial: Long,
    val providerAppLabel: String,
    val label: String,
    val minWidthPx: Int,
    val minHeightPx: Int,
) {
    val stableKey: String = "$profileSerial|$providerFlattened"
}

data class WidgetCatalogState(
    val providers: List<InstalledWidgetProvider> = emptyList(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

class WidgetProviderCatalog(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val packageManager = appContext.packageManager
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(WidgetCatalogState())
    val state: StateFlow<WidgetCatalogState> = mutableState.asStateFlow()
    private val refreshGeneration = AtomicInteger()
    private var refreshJob: Job? = null
    @Volatile private var providerInfoByKey: Map<String, AppWidgetProviderInfo> = emptyMap()

    init { refresh() }

    fun refresh() {
        val generation = refreshGeneration.incrementAndGet()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val previous = mutableState.value
            runCatching { loadProviders() }
                .onSuccess { (providers, infoByKey) ->
                    if (generation == refreshGeneration.get()) {
                        providerInfoByKey = infoByKey
                        mutableState.value = WidgetCatalogState(providers = providers, loading = false)
                    }
                }
                .onFailure { error ->
                    if (error !is CancellationException && generation == refreshGeneration.get()) {
                        mutableState.value = WidgetCatalogState(
                            providers = previous.providers,
                            loading = false,
                            errorMessage = "Widgets could not be refreshed: ${error.message ?: "Android denied access"}",
                        )
                    }
                }
        }
    }

    fun providerInfo(provider: InstalledWidgetProvider): AppWidgetProviderInfo? = providerInfoByKey[provider.stableKey]

    private fun loadProviders(): Pair<List<InstalledWidgetProvider>, Map<String, AppWidgetProviderInfo>> {
        val collator = Collator.getInstance(Locale.getDefault())
        val pairs = manager.installedProviders.mapNotNull { info ->
            val component = info.provider ?: return@mapNotNull null
            val profile = info.profile
            val serial = userManager?.getSerialNumberForUser(profile)?.takeIf { it >= 0 } ?: return@mapNotNull null
            val appLabel = runCatching {
                val application = packageManager.getApplicationInfo(component.packageName, 0)
                packageManager.getApplicationLabel(application).toString()
            }.getOrDefault(component.packageName)
            val widgetLabel = runCatching { info.loadLabel(packageManager)?.toString() }
                .getOrNull().orEmpty().ifBlank { appLabel }
            val provider = InstalledWidgetProvider(
                providerFlattened = component.flattenToString(),
                profileSerial = serial,
                providerAppLabel = appLabel,
                label = widgetLabel,
                minWidthPx = info.minWidth.coerceAtLeast(1),
                minHeightPx = info.minHeight.coerceAtLeast(1),
            )
            provider to info
        }.distinctBy { it.first.stableKey }.sortedWith { left, right ->
            val appOrder = collator.compare(left.first.providerAppLabel, right.first.providerAppLabel)
            if (appOrder != 0) appOrder else collator.compare(left.first.label, right.first.label)
        }
        return pairs.map { it.first } to pairs.associate { it.first.stableKey to it.second }
    }

    override fun close() {
        refreshGeneration.incrementAndGet()
        refreshJob?.cancel()
        scope.cancel()
    }
}
