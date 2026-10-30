package com.rumb.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rumb.launcher.platform.LauncherActivityController
import com.rumb.launcher.platform.WidgetSelection
import com.rumb.launcher.ui.RumbLauncherRoot
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), LauncherActivityController {
    override var onImportReady: ((String) -> Unit)? = null
    override var onError: ((String) -> Unit)? = null
    override var onWidgetReady: ((WidgetSelection) -> Boolean)? = null
        set(value) {
            field = value
            deliverReadyWidget()
        }

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var pendingExport: String? = null
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetProvider: String? = null
    private var pendingWidgetProfile: UserHandle? = null
    private var pendingWidgetStage: WidgetStage? = null
    private var pendingReadyWidget: WidgetSelection? = null
    private var activeWidgetIds: Set<Int> = emptySet()
    private var protectedWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var hasReconciledWidgetIds = false

    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExport
        pendingExport = null
        if (uri != null && json != null) lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) }
                    ?: error("Document provider did not open the file")
            } }.onFailure { onError?.invoke("Export failed: ${it.message}") }
        }
    }

    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_IMPORT_BYTES) error("Backup is larger than 1 MB")
                        output.write(buffer, 0, count)
                    }
                    String(output.toByteArray(), StandardCharsets.UTF_8)
                } ?: error("Document provider did not open the file")
            } }.onSuccess { onImportReady?.invoke(it) }
                .onFailure { onError?.invoke("Import failed: ${it.message}") }
        }
    }

    private val pickWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::handleWidgetPick)
    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::handleWidgetBind)
    private val configureWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::handleWidgetConfigure)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)
        pendingWidgetId = savedInstanceState?.getInt(STATE_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetProvider = savedInstanceState?.getString(STATE_WIDGET_PROVIDER)
        pendingWidgetProfile = savedInstanceState?.getInt(STATE_WIDGET_PROFILE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }
            ?.let(UserHandle::of)
        pendingWidgetStage = savedInstanceState?.getString(STATE_WIDGET_STAGE)?.let {
            runCatching { WidgetStage.valueOf(it) }.getOrNull()
        }
        if (pendingWidgetStage == WidgetStage.READY) {
            val info = validatedInfo(pendingWidgetId, pendingWidgetProvider)
            if (info == null) cancelPendingWidget("The completed widget is no longer available")
            else pendingReadyWidget = selectionFor(info)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { RumbLauncherRoot(this) }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
            .onFailure { onError?.invoke("Widgets could not start: ${it.message}") }
    }

    override fun onStop() {
        runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) cancelPendingWidget()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_WIDGET_ID, pendingWidgetId)
        pendingWidgetProvider?.let { outState.putString(STATE_WIDGET_PROVIDER, it) }
        pendingWidgetProfile?.let { outState.putInt(STATE_WIDGET_PROFILE, it.identifier) }
        pendingWidgetStage?.let { outState.putString(STATE_WIDGET_STAGE, it.name) }
    }

    override fun exportJson(json: String) {
        pendingExport = json
        exportDocument.launch("rumb-launcher-backup.json")
    }

    override fun importJson() = importDocument.launch(arrayOf("application/json", "text/json", "text/plain"))

    override fun requestWidget() {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) cancelPendingWidget()
        val appWidgetId = runCatching { appWidgetHost.allocateAppWidgetId() }.getOrElse {
            onError?.invoke("A widget ID could not be allocated: ${it.message}")
            return
        }
        pendingWidgetId = appWidgetId
        pendingWidgetProvider = null
        pendingWidgetProfile = null
        pendingWidgetStage = WidgetStage.PICK
        launchWidgetActivity(
            pickWidget,
            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            "The system widget picker is unavailable",
        )
    }

    override fun createWidgetView(appWidgetId: Int, expectedProvider: String): AppWidgetHostView? {
        val info = validatedInfo(appWidgetId, expectedProvider) ?: return null
        return runCatching {
            appWidgetHost.createView(this, appWidgetId, info).also { it.setAppWidget(appWidgetId, info) }
        }.getOrNull()
    }

    override fun updateWidgetSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching { view.updateAppWidgetSize(Bundle.EMPTY, widthDp, heightDp, widthDp, heightDp) }
    }

    override fun deleteWidgetId(appWidgetId: Int) {
        if (appWidgetId < 0) return
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
        if (appWidgetId == pendingWidgetId) clearPendingWidget()
        if (appWidgetId == protectedWidgetId) protectedWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun reconcileWidgetIds(activeWidgetIds: Set<Int>) {
        this.activeWidgetIds = activeWidgetIds
        if (protectedWidgetId in activeWidgetIds) protectedWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        hasReconciledWidgetIds = true
        removeOrphanWidgetIds()
    }

    private fun handleWidgetPick(result: ActivityResult) {
        if (pendingWidgetStage != WidgetStage.PICK || result.resultCode != Activity.RESULT_OK) {
            cancelPendingWidget()
            return
        }
        val returnedId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (returnedId != pendingWidgetId) {
            cancelPendingWidget("The widget picker returned an invalid widget ID")
            return
        }
        appWidgetManager.getAppWidgetInfo(pendingWidgetId)?.let {
            pendingWidgetProvider = it.provider.flattenToString()
            beginWidgetConfiguration(it)
            return
        }
        val provider = result.data?.componentExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
        if (provider == null) {
            cancelPendingWidget("The selected widget provider is unavailable")
            return
        }
        pendingWidgetProvider = provider.flattenToString()
        pendingWidgetProfile = result.data?.userHandleExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE)
        val bound = runCatching {
            val profile = pendingWidgetProfile
            if (profile == null) appWidgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, provider)
            else appWidgetManager.bindAppWidgetIdIfAllowed(pendingWidgetId, profile, provider, Bundle.EMPTY)
        }.getOrDefault(false)
        if (bound) {
            val info = validatedInfo(pendingWidgetId, pendingWidgetProvider)
            if (info == null) cancelPendingWidget("The selected widget provider is unavailable") else beginWidgetConfiguration(info)
        } else {
            pendingWidgetStage = WidgetStage.BIND
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            pendingWidgetProfile?.let { bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, it) }
            launchWidgetActivity(
                bindWidget,
                bindIntent,
                "The system widget binding screen is unavailable",
            )
        }
    }

    private fun handleWidgetBind(result: ActivityResult) {
        if (pendingWidgetStage != WidgetStage.BIND || result.resultCode != Activity.RESULT_OK) {
            cancelPendingWidget()
            return
        }
        val info = validatedInfo(pendingWidgetId, pendingWidgetProvider)
        if (info == null) cancelPendingWidget("The widget could not be bound") else beginWidgetConfiguration(info)
    }

    private fun beginWidgetConfiguration(info: AppWidgetProviderInfo) {
        if (info.provider.flattenToString() != pendingWidgetProvider) {
            cancelPendingWidget("The widget provider changed during setup")
            return
        }
        val configuration = info.configure
        if (configuration == null) {
            completeWidget(info)
            return
        }
        pendingWidgetStage = WidgetStage.CONFIGURE
        launchWidgetActivity(
            configureWidget,
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configuration)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId),
            "This widget's configuration screen is unavailable",
        )
    }

    private fun handleWidgetConfigure(result: ActivityResult) {
        if (pendingWidgetStage != WidgetStage.CONFIGURE || result.resultCode != Activity.RESULT_OK) {
            cancelPendingWidget()
            return
        }
        val info = validatedInfo(pendingWidgetId, pendingWidgetProvider)
        if (info == null) cancelPendingWidget("The configured widget is unavailable") else completeWidget(info)
    }

    private fun completeWidget(info: AppWidgetProviderInfo) {
        pendingWidgetStage = WidgetStage.READY
        pendingReadyWidget = selectionFor(info)
        deliverReadyWidget()
    }

    private fun selectionFor(info: AppWidgetProviderInfo): WidgetSelection = WidgetSelection(
        appWidgetId = pendingWidgetId,
        provider = info.provider.flattenToString(),
        minWidthPx = info.minWidth.coerceAtLeast(1),
        minHeightPx = info.minHeight.coerceAtLeast(1),
    )

    private fun deliverReadyWidget() {
        val callback = onWidgetReady ?: return
        val selection = pendingReadyWidget ?: return
        if (runCatching { callback(selection) }.getOrDefault(false)) {
            protectedWidgetId = selection.appWidgetId
            pendingReadyWidget = null
            clearPendingWidget()
        } else {
            cancelPendingWidget()
        }
    }

    private fun validatedInfo(appWidgetId: Int, expectedProvider: String?): AppWidgetProviderInfo? {
        if (appWidgetId < 0 || expectedProvider.isNullOrBlank()) return null
        val expected = ComponentName.unflattenFromString(expectedProvider) ?: return null
        return appWidgetManager.getAppWidgetInfo(appWidgetId)?.takeIf { it.provider == expected }
    }

    private fun launchWidgetActivity(
        launcher: ActivityResultLauncher<Intent>,
        intent: Intent,
        unavailableMessage: String,
    ) {
        if (intent.resolveActivity(packageManager) == null) {
            cancelPendingWidget(unavailableMessage)
            return
        }
        runCatching { launcher.launch(intent) }.onFailure { cancelPendingWidget("$unavailableMessage: ${it.message}") }
    }

    private fun cancelPendingWidget(message: String? = null) {
        val appWidgetId = pendingWidgetId
        clearPendingWidget()
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
        }
        removeOrphanWidgetIds()
        message?.let { onError?.invoke(it) }
    }

    private fun removeOrphanWidgetIds() {
        if (!hasReconciledWidgetIds) return
        runCatching { appWidgetHost.appWidgetIds.toSet() }.getOrDefault(emptySet()).forEach { appWidgetId ->
            if (
                appWidgetId != pendingWidgetId &&
                appWidgetId != protectedWidgetId &&
                appWidgetId !in activeWidgetIds
            ) {
                runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
            }
        }
    }

    private fun clearPendingWidget() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetProvider = null
        pendingWidgetProfile = null
        pendingWidgetStage = null
        pendingReadyWidget = null
    }

    @Suppress("DEPRECATION")
    private fun Intent.componentExtra(name: String): ComponentName? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(name, ComponentName::class.java)
    } else {
        getParcelableExtra(name)
    }

    @Suppress("DEPRECATION")
    private fun Intent.userHandleExtra(name: String): UserHandle? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(name, UserHandle::class.java)
    } else {
        getParcelableExtra(name)
    }

    private enum class WidgetStage { PICK, BIND, CONFIGURE, READY }

    companion object {
        private const val MAX_IMPORT_BYTES = 1_048_576
        private const val WIDGET_HOST_ID = 0x52554D42
        private const val STATE_WIDGET_ID = "pending_widget_id"
        private const val STATE_WIDGET_PROVIDER = "pending_widget_provider"
        private const val STATE_WIDGET_PROFILE = "pending_widget_profile"
        private const val STATE_WIDGET_STAGE = "pending_widget_stage"
    }
}
