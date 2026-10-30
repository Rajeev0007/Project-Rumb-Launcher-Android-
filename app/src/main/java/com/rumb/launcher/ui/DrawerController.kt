package com.rumb.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Position of the app library sheet, driven directly by the finger and settled with a spring.
 *
 * [fraction] is 1 when the sheet is fully below the screen and 0 when it covers Home. Read it only
 * inside graphics-layer or draw lambdas so dragging redraws without recomposing.
 */
@Stable
class DrawerController(private val scope: CoroutineScope) {
    var fraction by mutableFloatStateOf(CLOSED)
        private set

    /** True while a finger is moving the sheet. Home pages stop scrolling during the drag. */
    var dragging by mutableStateOf(false)
        private set

    /** Measured screen height. Snapshot state so layers re-read it once the real size is known. */
    var heightPx by mutableFloatStateOf(4096f)

    private var job: Job? = null
    private var target = CLOSED

    /** Moves the sheet by [deltaPx] (negative opens). Returns the pixels actually consumed. */
    fun dragBy(deltaPx: Float): Float {
        job?.cancel()
        if (!dragging) dragging = true
        val height = heightPx.coerceAtLeast(1f)
        val before = fraction
        fraction = (fraction + deltaPx / height).coerceIn(OPEN, CLOSED)
        return (fraction - before) * height
    }

    /** Ends a drag, flinging in the gesture direction or snapping to the nearer edge. Returns true if it opens. */
    fun settle(velocityPx: Float): Boolean {
        dragging = false
        val velocity = velocityPx / heightPx.coerceAtLeast(1f)
        val open = when {
            velocity < -FLING_THRESHOLD -> true
            velocity > FLING_THRESHOLD -> false
            else -> fraction < .5f
        }
        animateTo(if (open) OPEN else CLOSED, velocity)
        return open
    }

    fun animateTo(value: Float, velocity: Float = 0f) {
        if (value == target && job?.isActive == true) return
        target = value
        if (value == fraction) return
        job?.cancel()
        job = scope.launch {
            animate(
                initialValue = fraction,
                targetValue = value,
                initialVelocity = velocity,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 520f),
            ) { current, _ -> fraction = current }
        }
    }

    fun snapTo(value: Float) {
        job?.cancel()
        dragging = false
        target = value
        fraction = value
    }

    /** Lets the library list pull the sheet down once it is scrolled to the top, then flings it shut. */
    fun nestedScrollConnection(onSettled: (open: Boolean) -> Unit): NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y < 0f && fraction > OPEN) {
                val consumed = dragBy(available.y)
                // Fully reopened: hand the rest of the gesture (and its fling) back to the list.
                if (fraction <= OPEN) dragging = false
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y > 0f) return Offset(0f, dragBy(available.y))
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            onSettled(settle(available.y))
            return available
        }
    }

    companion object {
        const val OPEN = 0f
        const val CLOSED = 1f
        private const val FLING_THRESHOLD = 1.1f
    }
}
