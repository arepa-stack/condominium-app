package com.example.condominio.ui.screens.decisions

import com.example.condominio.data.model.TallyItemDto
import com.example.condominio.ui.utils.parseAmount
import kotlin.math.roundToInt

/** Rank (1-based, competition style: ties share it) per quote id; quotes without votes get none. */
internal fun rankTallies(tallies: List<TallyItemDto>): Map<String, Int> {
    val sorted = tallies.filter { it.votes > 0 }.sortedByDescending { it.votes }
    return sorted.associate { t -> t.quoteId to sorted.indexOfFirst { it.votes == t.votes } + 1 }
}

/** The single quote ranked first, or null when there is none or several share first place. */
internal fun leaderQuoteId(ranks: Map<String, Int>): String? =
    ranks.entries.filter { it.value == 1 }.singleOrNull()?.key

/** Whole-number percentage ("33"), without the float noise of the API value. */
internal fun formatPercent(pct: Double): String = pct.roundToInt().toString()

/** Typed budget amount ("1.500", "1500,50"); null when invalid or not positive. */
internal fun parseQuoteAmount(text: String): Double? = parseAmount(text)?.takeIf { it > 0 }
