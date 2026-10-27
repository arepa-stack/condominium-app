package com.example.condominio.ui.screens.decisions

import com.example.condominio.data.model.TallyItemDto
import com.example.condominio.ui.components.formatDecisionDate
import com.example.condominio.ui.components.formatDecisionDeadline
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecisionFormattingTest {
    private fun t(id: String, votes: Int, pct: Double = 0.0) = TallyItemDto(id, id, 100.0, votes, pct)

    // --- C-08 ---
    @Test fun `deadline shows the year`() =
        assertEquals("05/03/2026 14:30", formatDecisionDeadline("2026-03-05T14:30:00Z", TimeZone.UTC))

    @Test fun `deadline null is a dash`() = assertEquals("—", formatDecisionDeadline(null, TimeZone.UTC))

    @Test fun `finalized date keeps dd MM yyyy`() {
        val s = formatDecisionDate("2026-03-05T12:00:00Z")
        assertEquals(10, s.length)
        assertEquals("2026", s.takeLast(4))
    }

    // --- A-13 ---
    @Test fun `percentage never shows float noise`() {
        assertEquals("33", formatPercent(33.333333333333336))
        assertEquals("67", formatPercent(66.66666666666667))
        assertEquals("50", formatPercent(50.0))
    }

    @Test fun `tied quotes share the same rank`() {
        val ranks = rankTallies(listOf(t("a", 2), t("b", 2), t("c", 1), t("d", 0)))
        assertEquals(1, ranks["a"])
        assertEquals(1, ranks["b"])
        assertEquals(3, ranks["c"])
        assertNull(ranks["d"])
    }

    @Test fun `no leader when first place is tied`() =
        assertNull(leaderQuoteId(rankTallies(listOf(t("a", 2), t("b", 2)))))

    @Test fun `leader is the single first place`() =
        assertEquals("a", leaderQuoteId(rankTallies(listOf(t("b", 1), t("a", 3)))))

    // --- A-04 ---
    @Test fun `thousands separator is not read as decimal`() = assertEquals(1500.0, parseQuoteAmount("1.500"))
    @Test fun `comma decimal is accepted`() = assertEquals(1500.5, parseQuoteAmount("1500,50"))
    @Test fun `plain dot decimal still works`() = assertEquals(150.5, parseQuoteAmount("150.50"))
    @Test fun `zero and garbage are rejected`() {
        assertNull(parseQuoteAmount("0"))
        assertNull(parseQuoteAmount("abc"))
        assertNull(parseQuoteAmount(""))
    }
}
