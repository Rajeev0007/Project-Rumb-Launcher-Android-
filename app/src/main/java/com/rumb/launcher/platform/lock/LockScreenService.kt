package com.rumb.launcher.platform.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Exists only to turn the screen off through Android's public lock-screen global action (Android 9+).
 * It subscribes to no accessibility events and cannot read window content. Android's secure lock
 * screen still protects the device; this only triggers it.
 */
class LockScreenService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: WeakReference<LockScreenService>? = null

        val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val isConnected: Boolean get() = instance?.get() != null

        /** Returns true when the screen was locked. */
        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            val service = instance?.get() ?: return false
            return runCatching { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) }.getOrDefault(false)
        }
    }
}
