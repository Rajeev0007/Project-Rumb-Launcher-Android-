package com.rumb.launcher

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rumb.launcher.platform.LauncherActivityController
import com.rumb.launcher.ui.RumbLauncherRoot
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), LauncherActivityController {
    override var onImportReady: ((String) -> Unit)? = null
    override var onError: ((String) -> Unit)? = null
    private var pendingExport: String? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { RumbLauncherRoot(this) }
    }

    override fun exportJson(json: String) {
        pendingExport = json
        exportDocument.launch("rumb-launcher-backup.json")
    }

    override fun importJson() = importDocument.launch(arrayOf("application/json", "text/json", "text/plain"))

    companion object { private const val MAX_IMPORT_BYTES = 1_048_576 }
}
