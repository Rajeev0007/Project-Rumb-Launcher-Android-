package com.rumb.launcher.platform.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.rumb.launcher.domain.AppId
import com.rumb.launcher.domain.AppShortcut
import com.rumb.launcher.domain.CatalogState
import com.rumb.launcher.domain.InstalledApp
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

class LauncherAppsCatalog(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null
    private val refreshGeneration = AtomicInteger()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = refresh()
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = refresh()
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) = refresh()
    }

    init {
        launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        val generation = refreshGeneration.incrementAndGet()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val previous = mutableState.value
            runCatching { loadApps() }
                .onSuccess {
                    if (generation == refreshGeneration.get()) {
                        mutableState.value = CatalogState(apps = it, loading = false)
                    }
                }
                .onFailure {
                    if (it !is CancellationException && generation == refreshGeneration.get()) {
                        mutableState.value = CatalogState(
                            apps = previous.apps,
                            loading = false,
                            errorMessage = "Apps could not be refreshed: ${it.message ?: "Android denied access"}",
                        )
                    }
                }
        }
    }

    fun launch(app: InstalledApp) {
        val service = launcherApps ?: error("Launcher service is unavailable")
        service.startMainActivity(
            ComponentName(app.packageName, app.activityName),
            userFor(app.id.profileSerial) ?: error("The ${app.profileLabel ?: "selected"} profile is unavailable"),
            null,
            null,
        )
    }

    fun shortcuts(app: InstalledApp): List<AppShortcut> {
        val service = launcherApps ?: return emptyList()
        if (!service.hasShortcutHostPermission()) return emptyList()
        val user = userFor(app.id.profileSerial) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return service.getShortcuts(query, user).orEmpty()
            .filter { it.isEnabled }
            .map { AppShortcut(it.id, app.packageName, it.shortLabel.toString(), app.id.profileSerial) }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun launchShortcut(shortcut: AppShortcut) {
        val service = launcherApps ?: error("Launcher service is unavailable")
        service.startShortcut(
            shortcut.packageName,
            shortcut.id,
            null,
            null,
            userFor(shortcut.profileSerial) ?: error("The shortcut profile is unavailable"),
        )
    }

    fun activityInfo(appId: AppId): LauncherActivityInfo? {
        val service = launcherApps ?: return null
        val user = userFor(appId.profileSerial) ?: return null
        val component = ComponentName.unflattenFromString(appId.component) ?: return null
        return service.getActivityList(component.packageName, user).firstOrNull { it.componentName == component }
    }

    fun userFor(serial: Long): UserHandle? = launcherApps?.profiles.orEmpty()
        .firstOrNull { userManager?.getSerialNumberForUser(it) == serial }

    private fun loadApps(): List<InstalledApp> {
        val service = launcherApps ?: error("Android LauncherApps service is unavailable")
        val currentUser = Process.myUserHandle()
        val collator = Collator.getInstance(Locale.getDefault())
        return service.profiles.flatMap { user ->
            val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 } ?: return@flatMap emptyList()
            val workProfile = user != currentUser
            val profileLabel = if (workProfile) "Work" else null
            runCatching { service.getActivityList(null, user) }.getOrDefault(emptyList()).mapNotNull { info ->
                if (info.applicationInfo.packageName == appContext.packageName) return@mapNotNull null
                InstalledApp(
                    id = AppId(info.componentName.flattenToString(), serial),
                    label = info.label?.toString()?.ifBlank { info.applicationInfo.packageName } ?: info.applicationInfo.packageName,
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    profileLabel = profileLabel,
                    isWorkProfile = workProfile,
                )
            }
        }.distinctBy { it.id.stableKey }.sortedWith { left, right -> collator.compare(left.label, right.label) }
    }

    override fun close() {
        refreshGeneration.incrementAndGet()
        refreshJob?.cancel()
        launcherApps?.unregisterCallback(callback)
        scope.cancel()
    }
}
