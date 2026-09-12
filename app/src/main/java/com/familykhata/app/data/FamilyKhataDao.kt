package com.familykhata.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyKhataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(item: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(item: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        """
    )
    fun observeDashboardTotals(): Flow<DashboardTotals>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: BakiPersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBakiEntry(entry: BakiEntryEntity)

    @Delete
    suspend fun deleteBakiEntry(entry: BakiEntryEntity)

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.phone AS phone,
               COALESCE(SUM(e.balanceDelta), 0) AS balance
        FROM baki_people p
        LEFT JOIN baki_entries e ON p.id = e.personId
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeBakiSummaries(): Flow<List<BakiPersonSummary>>

    @Query("SELECT * FROM baki_entries WHERE personId = :personId ORDER BY createdAt DESC")
    fun observeBakiEntries(personId: Long): Flow<List<BakiEntryEntity>>
}
