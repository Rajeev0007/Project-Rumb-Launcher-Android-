package com.rumb.launcher.platform.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // Snapshots from the last successful load. Icon loading reads these instead of making a
    // LauncherApps IPC call for every icon, which was the main cost of scrolling the library.
    @Volatile private var activityInfoByKey: Map<String, LauncherActivityInfo> = emptyMap()
    @Volatile private var usersBySerial: Map<Long, UserHandle> = emptyMap()

    private val mutableRemovals = MutableSharedFlow<PackageRemoval>(extraBufferCapacity = 16)

    /** Emits when an app is uninstalled (not when it is updated or temporarily unavailable). */
    val removals: SharedFlow<PackageRemoval> = mutableRemovals.asSharedFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 }?.let {
                mutableRemovals.tryEmit(PackageRemoval(packageName, it))
            }
            refresh()
        }
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

    fun launch(app: InstalledApp, source: LaunchSource? = null) {
        val service = launcherApps ?: error("Launcher service is unavailable")
        service.startMainActivity(
            ComponentName(app.packageName, app.activityName),
            userFor(app.id.profileSerial) ?: error("The ${app.profileLabel ?: "selected"} profile is unavailable"),
            source?.bounds,
            source?.options,
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
        activityInfoByKey[appId.stableKey]?.let { return it }
        val service = launcherApps ?: return null
        val user = userFor(appId.profileSerial) ?: return null
        val component = ComponentName.unflattenFromString(appId.component) ?: return null
        return runCatching {
            service.getActivityList(component.packageName, user).firstOrNull { it.componentName == component }
        }.getOrNull()
    }

    fun userFor(serial: Long): UserHandle? = usersBySerial[serial] ?: launcherApps?.profiles.orEmpty()
        .firstOrNull { userManager?.getSerialNumberForUser(it) == serial }

    private fun loadApps(): List<InstalledApp> {
        val service = launcherApps ?: error("Android LauncherApps service is unavailable")
        val currentUser = Process.myUserHandle()
        val collator = Collator.getInstance(Locale.getDefault())
        val infos = HashMap<String, LauncherActivityInfo>()
        val users = HashMap<Long, UserHandle>()
        val apps = service.profiles.flatMap { user ->
            val serial = userManager?.getSerialNumberForUser(user)?.takeIf { it >= 0 } ?: return@flatMap emptyList()
            users[serial] = user
            val workProfile = user != currentUser
            val profileLabel = if (workProfile) "Work" else null
            runCatching { service.getActivityList(null, user) }.getOrDefault(emptyList()).mapNotNull { info ->
                if (info.applicationInfo.packageName == appContext.packageName) return@mapNotNull null
                val id = AppId(info.componentName.flattenToString(), serial)
                infos[id.stableKey] = info
                InstalledApp(
                    id = id,
                    label = info.label?.toString()?.ifBlank { info.applicationInfo.packageName } ?: info.applicationInfo.packageName,
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    profileLabel = profileLabel,
                    isWorkProfile = workProfile,
                    isSystemApp = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }.distinctBy { it.id.stableKey }.sortedWith { left, right -> collator.compare(left.label, right.label) }
        usersBySerial = users
        activityInfoByKey = infos
        return apps
    }

    override fun close() {
        refreshGeneration.incrementAndGet()
        refreshJob?.cancel()
        launcherApps?.unregisterCallback(callback)
        scope.cancel()
    }
}

data class PackageRemoval(val packageName: String, val profileSerial: Long)
