package com.rumb.launcher.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.rumb.launcher.domain.InstalledApp

sealed interface ExternalDrop {
    data class Cell(val page: Int, val cell: Int) : ExternalDrop
    data object Dock : ExternalDrop
}

/**
 * Carries an app from the library onto Home. The library starts the drag and keeps feeding root
 * coordinates while its sheet slides away; Home publishes its grid geometry and highlights the target.
 */
@Stable
class AppDragController(private val onDrop: (InstalledApp, ExternalDrop) -> Unit) {
    var app by mutableStateOf<InstalledApp?>(null)
        private set

    /** Finger position in root coordinates. Read it in layer or derived-state lambdas only. */
    var position by mutableStateOf(Offset.Zero)
        private set

    /** Untransformed Home grid bounds (inside page padding) in root coordinates, supplied by Home. */
    var gridBounds: () -> Rect? = { null }
    var dockBounds: () -> Rect? = { null }
    var columns: Int = 5
    var rows: Int = 5
    var currentPage: Int = 0

    val isDragging: Boolean get() = app != null

    fun start(app: InstalledApp, rootPosition: Offset) {
        position = rootPosition
        this.app = app
    }

    fun move(rootPosition: Offset) {
        if (app != null) position = rootPosition
    }

    fun drop() {
        val dragged = app ?: return
        val target = targetAt(position)
        app = null
        if (target != null) onDrop(dragged, target)
    }

    fun cancel() {
        app = null
    }

    /** Grid cell under [point] on the current page, or null when outside the grid. */
    fun cellAt(point: Offset): Int? {
        val bounds = gridBounds() ?: return null
        if (bounds.width <= 0f || bounds.height <= 0f || !bounds.contains(point)) return null
        val column = ((point.x - bounds.left) / (bounds.width / columns)).toInt().coerceIn(0, columns - 1)
        val row = ((point.y - bounds.top) / (bounds.height / rows)).toInt().coerceIn(0, rows - 1)
        return row * columns + column
    }

    /** -1 or 1 while the finger rests near the left or right edge of the grid, otherwise 0. */
    fun edgeDirection(point: Offset, edgePx: Float): Int {
        val bounds = gridBounds() ?: return 0
        if (point.y < bounds.top || point.y > bounds.bottom) return 0
        return when {
            point.x < bounds.left + edgePx -> -1
            point.x > bounds.right - edgePx -> 1
            else -> 0
        }
    }

    private fun targetAt(point: Offset): ExternalDrop? {
        dockBounds()?.let { if (it.contains(point)) return ExternalDrop.Dock }
        return cellAt(point)?.let { ExternalDrop.Cell(currentPage, it) }
    }
}
