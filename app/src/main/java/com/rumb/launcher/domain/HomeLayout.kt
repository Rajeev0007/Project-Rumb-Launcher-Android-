package com.rumb.launcher.domain

data class HomePlacement(val page: Int, val cell: Int)

fun HomeItem.layoutSpanX(columns: Int): Int =
    if (type == HomeItemType.WIDGET) spanX.coerceIn(1, columns) else 1

fun HomeItem.layoutSpanY(rows: Int): Int =
    if (type == HomeItemType.WIDGET) spanY.coerceIn(1, rows) else 1

fun occupiedCells(item: HomeItem, columns: Int, rows: Int): Set<Int> {
    val spanX = item.layoutSpanX(columns)
    val spanY = item.layoutSpanY(rows)
    val column = item.cell % columns
    val row = item.cell / columns
    if (column < 0 || row < 0 || column + spanX > columns || row + spanY > rows) return emptySet()
    return buildSet {
        repeat(spanY) { y -> repeat(spanX) { x -> add((row + y) * columns + column + x) } }
    }
}

fun isRegionFree(
    items: List<HomeItem>,
    page: Int,
    cell: Int,
    spanX: Int,
    spanY: Int,
    columns: Int,
    rows: Int,
    ignoredItemId: String? = null,
): Boolean {
    val candidate = HomeItem("candidate", HomeItemType.WIDGET, page, cell, spanX, spanY)
    val cells = occupiedCells(candidate, columns, rows)
    if (cells.size != spanX * spanY) return false
    return items.none { item ->
        item.id != ignoredItemId && item.page == page && occupiedCells(item, columns, rows).any(cells::contains)
    }
}

fun findFreePlacement(
    items: List<HomeItem>,
    columns: Int,
    rows: Int,
    pageCount: Int,
    spanX: Int = 1,
    spanY: Int = 1,
    ignoredItemId: String? = null,
    preferredPage: Int = 0,
): HomePlacement? {
    val safeSpanX = spanX.coerceIn(1, columns)
    val safeSpanY = spanY.coerceIn(1, rows)
    val pages = pageCount.coerceIn(1, 9)
    val pageOrder = buildList {
        if (preferredPage in 0 until pages) add(preferredPage)
        for (page in 0 until pages) if (page != preferredPage) add(page)
    }
    for (page in pageOrder) {
        for (row in 0..rows - safeSpanY) {
            for (column in 0..columns - safeSpanX) {
                val cell = row * columns + column
                if (isRegionFree(items, page, cell, safeSpanX, safeSpanY, columns, rows, ignoredItemId)) {
                    return HomePlacement(page, cell)
                }
            }
        }
    }
    return null
}

fun packHomeItems(
    source: List<HomeItem>,
    columns: Int,
    rows: Int,
    maxPages: Int,
    preservePreferredPositions: Boolean,
): List<HomeItem>? = packHomeItemsInternal(
    source,
    columns,
    rows,
    maxPages,
    preservePreferredPositions,
    dropOverflow = false,
)

fun normalizeHomePlacements(
    source: List<HomeItem>,
    columns: Int,
    rows: Int,
    maxPages: Int,
): List<HomeItem> = packHomeItemsInternal(
    source,
    columns,
    rows,
    maxPages,
    preservePreferredPositions = true,
    dropOverflow = true,
).orEmpty()

private fun packHomeItemsInternal(
    source: List<HomeItem>,
    columns: Int,
    rows: Int,
    maxPages: Int,
    preservePreferredPositions: Boolean,
    dropOverflow: Boolean,
): List<HomeItem>? {
    val placed = mutableListOf<HomeItem>()
    val ordered = if (preservePreferredPositions) {
        source.sortedWith(compareBy(HomeItem::page, HomeItem::cell, HomeItem::id))
    } else {
        source.sortedWith(
            compareByDescending<HomeItem> { it.layoutSpanX(columns) * it.layoutSpanY(rows) }
                .thenBy(HomeItem::page)
                .thenBy(HomeItem::cell)
                .thenBy(HomeItem::id),
        )
    }
    ordered.forEach { original ->
        val item = original.copy(
            spanX = original.layoutSpanX(columns),
            spanY = original.layoutSpanY(rows),
        )
        val preferredCell = item.cell.coerceAtLeast(0)
        val canKeep = preservePreferredPositions && item.page in 0 until maxPages &&
            isRegionFree(placed, item.page, preferredCell, item.spanX, item.spanY, columns, rows)
        val placement = if (canKeep) HomePlacement(item.page, preferredCell) else findFreePlacement(
            items = placed,
            columns = columns,
            rows = rows,
            pageCount = maxPages,
            spanX = item.spanX,
            spanY = item.spanY,
        )
        if (placement == null) {
            if (!dropOverflow) return null
        } else {
            placed += item.copy(page = placement.page, cell = placement.cell)
        }
    }
    return placed
}
