package com.rumb.launcher.domain

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{Mn}+")
private val wordSeparators = charArrayOf(' ', '-', '_', '.', '&', '+', ':', '/')

/** Lowercases and strips accents so "Café" matches "cafe". */
fun normalizeForSearch(value: String): String =
    combiningMarks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT)
        .trim()

/**
 * Ranks apps for a query. Exact and prefix label matches come first, then word-prefix matches,
 * initials ("gm" → "Google Maps"), substring matches, and finally package-name matches.
 * Apps with equal rank keep the incoming (alphabetical) order. Non-matching apps are dropped.
 */
fun rankApps(apps: List<InstalledApp>, rawQuery: String): List<InstalledApp> {
    val query = normalizeForSearch(rawQuery)
    if (query.isEmpty()) return apps
    val compactQuery = query.filterNot(Char::isWhitespace)
    val ranked = ArrayList<Pair<Int, Int>>()
    apps.forEachIndexed { index, app ->
        matchScore(app, query, compactQuery)?.let { ranked += it to index }
    }
    ranked.sortWith(compareBy<Pair<Int, Int>>({ it.first }, { it.second }))
    return ranked.map { apps[it.second] }
}

private fun matchScore(app: InstalledApp, query: String, compactQuery: String): Int? {
    val label = normalizeForSearch(app.label)
    if (label == query) return 0
    if (label.startsWith(query)) return 1
    val words = label.split(*wordSeparators).filter(String::isNotEmpty)
    if (words.any { it.startsWith(query) }) return 2
    if (compactQuery.length >= 2 && words.size >= 2) {
        val initials = words.joinToString("") { it.take(1) }
        if (initials.startsWith(compactQuery)) return 3
    }
    if (label.contains(query)) return 4
    if (compactQuery.isNotEmpty() && label.filterNot(Char::isWhitespace).contains(compactQuery)) return 5
    if (app.packageName.lowercase(Locale.ROOT).contains(compactQuery)) return 6
    return null
}
