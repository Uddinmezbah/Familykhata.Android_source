package com.familykhata.app.report

import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonEntity
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class LedgerStatementTest {
    private val person = BakiPersonEntity(id = 7, name = "রহিম", workspace = "SHOP")
    private fun entry(id: Long, at: Long, action: String, amount: Double, delta: Double, owner: Long = 7) =
        BakiEntryEntity(id = id, personId = owner, createdAt = at, action = action, amount = amount, balanceDelta = delta)
    private fun money(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun openingAllFourActionsAndDateBoundaries() {
        val statement = buildLedgerStatement(person, listOf(
            entry(1, 99, "GAVE", 1000.0, 1000.0),
            entry(2, 100, "RECEIVED_BACK", 200.0, -200.0),
            entry(3, 130, "TOOK", 1200.0, -1200.0),
            entry(4, 150, "PAID_BACK", 300.0, 300.0),
            entry(5, 199, "GAVE", 50.0, 50.0),
            entry(6, 200, "GAVE", 999.0, 999.0),
            entry(7, 50, "GAVE", 5000.0, 5000.0, owner = 8)
        ).reversed(), 100, 200)
        money("1000", statement.openingBalance)
        money("-50", statement.closingBalance)
        assertEquals(listOf(2L, 3L, 4L, 5L), statement.rows.map { it.entry.id })
        listOf("800", "-400", "-100", "-50").forEachIndexed { i, value -> money(value, statement.rows[i].balance) }
    }

    @Test fun correctedActionKeepsOneRecordAndUsesStoredDelta() {
        val original = entry(1, 100, "GAVE", 1000.0, 1000.0)
        val corrected = original.copy(action = "TOOK", balanceDelta = -1000.0)
        val statement = buildLedgerStatement(person, listOf(corrected), 100, 101)
        assertEquals(1, statement.rows.size)
        assertEquals(original.id, statement.rows.single().entry.id)
        assertEquals(original.createdAt, statement.rows.single().entry.createdAt)
        money("-1000", statement.closingBalance)
    }

    @Test fun noActivityCarriesOpeningBalanceAndEmptyLedgerIsZero() {
        val statement = buildLedgerStatement(person, listOf(entry(1, 1, "TOOK", 25.0, -25.0)), 100, 200)
        assertTrue(statement.rows.isEmpty())
        money("-25", statement.openingBalance)
        money("-25", statement.closingBalance)
        money("0", buildLedgerStatement(person, emptyList(), 100, 200).closingBalance)
    }

    @Test fun decimalsAndEqualTimestampsHaveStableOrdering() {
        val entries = listOf(entry(3, 100, "RECEIVED_BACK", 0.3, -0.3), entry(2, 100, "GAVE", 0.2, 0.2), entry(1, 100, "GAVE", 0.1, 0.1))
        val statement = buildLedgerStatement(person, entries, 100, 101)
        assertEquals(listOf(1L, 2L, 3L), statement.rows.map { it.entry.id })
        money("0", statement.closingBalance)
        assertEquals(listOf(3L, 2L, 1L), entries.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class) fun invalidRangeIsRejected() {
        buildLedgerStatement(person, emptyList(), 200, 100)
    }

    @Test fun endDateIncludesWholeLocalDayAcrossDst() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val noon = Calendar.getInstance().apply { clear(); set(2026, Calendar.MARCH, 8, 12, 0) }.timeInMillis
            val start = statementDayStart(noon)
            val end = statementNextDay(noon)
            assertEquals(23L * 60 * 60 * 1000, end - start)
            val statement = buildLedgerStatement(person, listOf(entry(1, end - 1, "GAVE", 5.0, 5.0), entry(2, end, "GAVE", 9.0, 9.0)), start, end)
            money("5", statement.closingBalance)
        } finally { TimeZone.setDefault(original) }
    }
}
