package com.familykhata.app.report

import com.familykhata.app.data.BakiEntryEntity
import com.familykhata.app.data.BakiPersonEntity
import java.math.BigDecimal
import java.util.Calendar

data class LedgerStatementRow(val entry: BakiEntryEntity, val balance: BigDecimal)

data class LedgerStatement(
    val person: BakiPersonEntity,
    val startInclusive: Long,
    val endExclusive: Long,
    val openingBalance: BigDecimal,
    val closingBalance: BigDecimal,
    val rows: List<LedgerStatementRow>
)

/** Read-only snapshot. Positive balances mean the app owner is owed money. */
fun buildLedgerStatement(
    person: BakiPersonEntity,
    entries: List<BakiEntryEntity>,
    startInclusive: Long,
    endExclusive: Long
): LedgerStatement {
    require(startInclusive < endExclusive) { "Invalid statement date range" }
    val ordered = entries.filter { it.personId == person.id && it.createdAt < endExclusive }
        .sortedWith(compareBy<BakiEntryEntity> { it.createdAt }.thenBy { it.id })
    require(ordered.all { it.balanceDelta.isFinite() && it.amount.isFinite() }) {
        "Invalid ledger amount"
    }
    // Use persisted balanceDelta, exactly as the existing ledger does. Never reinterpret actions.
    val opening = ordered.filter { it.createdAt < startInclusive }
        .fold(BigDecimal.ZERO) { total, entry -> total + BigDecimal.valueOf(entry.balanceDelta) }
    var running = opening
    val rows = ordered.filter { it.createdAt >= startInclusive }.map { entry ->
        running += BigDecimal.valueOf(entry.balanceDelta)
        LedgerStatementRow(entry, running)
    }
    return LedgerStatement(person, startInclusive, endExclusive, opening, running, rows)
}

/** Calendar arithmetic preserves local dates across daylight-saving changes. */
fun statementDayStart(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun statementNextDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = statementDayStart(timestamp)
    add(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis
