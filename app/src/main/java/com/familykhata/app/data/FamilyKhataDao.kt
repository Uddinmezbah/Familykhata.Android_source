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

    @Query(
        """
        UPDATE transactions
        SET type = :type,
            amount = :amount,
            category = :category,
            note = :note
        WHERE id = :transactionId
        """
    )
    suspend fun updateTransaction(
        transactionId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String
    )

    @Query("SELECT * FROM transactions WHERE workspace = :workspace ORDER BY createdAt DESC")
    fun observeTransactions(workspace: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense
        FROM transactions
        WHERE workspace = :workspace
        """
    )
    fun observeDashboardTotals(workspace: String): Flow<DashboardTotals>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: BakiPersonEntity): Long

    @Query("UPDATE baki_people SET name = :name, phone = :phone WHERE id = :personId")
    suspend fun updatePerson(personId: Long, name: String, phone: String)

    @Query("DELETE FROM baki_people WHERE id = :personId")
    suspend fun deletePersonById(personId: Long)

    @Query("SELECT * FROM baki_people WHERE workspace = :workspace ORDER BY name COLLATE NOCASE ASC")
    fun observePeople(workspace: String): Flow<List<BakiPersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBakiEntry(entry: BakiEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBakiEntryIgnore(
        entry: BakiEntryEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM baki_entries
        WHERE sourceKey = :sourceKey
        LIMIT 1
        """
    )
    suspend fun getBakiEntryBySourceKey(
        sourceKey: String
    ): BakiEntryEntity?

    @Query(
        """
        DELETE FROM baki_entries
        WHERE sourceKey = :sourceKey
        """
    )
    suspend fun deleteBakiEntryBySourceKey(
        sourceKey: String
    ): Int

    @Delete
    suspend fun deleteBakiEntry(entry: BakiEntryEntity)

    @Query(
        """
        UPDATE baki_entries
        SET action = :action,
            amount = :amount,
            balanceDelta = :balanceDelta,
            note = :note,
            dueAt = :dueAt
        WHERE id = :entryId
        """
    )
    suspend fun updateBakiEntry(
        entryId: Long,
        action: String,
        amount: Double,
        balanceDelta: Double,
        note: String,
        dueAt: Long?
    )

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.phone AS phone,
               COALESCE(SUM(e.balanceDelta), 0) AS balance
        FROM baki_people p
        LEFT JOIN baki_entries e ON p.id = e.personId
        WHERE p.workspace = :workspace
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeBakiSummaries(workspace: String): Flow<List<BakiPersonSummary>>

    @Query("SELECT * FROM baki_entries WHERE personId = :personId ORDER BY createdAt DESC")
    fun observeBakiEntries(personId: Long): Flow<List<BakiEntryEntity>>

    @Query("SELECT * FROM baki_people WHERE id = :personId AND workspace = :workspace LIMIT 1")
    suspend fun getStatementPerson(personId: Long, workspace: String): BakiPersonEntity?

    @Query("SELECT * FROM baki_entries WHERE personId = :personId ORDER BY createdAt ASC, id ASC")
    suspend fun getStatementEntries(personId: Long): List<BakiEntryEntity>

    @Query(
        """
        SELECT e.*
        FROM baki_entries e
        INNER JOIN baki_people p ON p.id = e.personId
        WHERE p.workspace = :workspace
        ORDER BY e.createdAt ASC, e.id ASC
        """
    )
    fun observeWorkspaceBakiEntries(workspace: String): Flow<List<BakiEntryEntity>>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM baki_people ORDER BY id ASC")
    suspend fun getAllPeople(): List<BakiPersonEntity>

    @Query("SELECT * FROM baki_entries ORDER BY id ASC")
    suspend fun getAllBakiEntries(): List<BakiEntryEntity>

    @Query("DELETE FROM baki_entries")
    suspend fun clearBakiEntries()

    @Query("DELETE FROM baki_people")
    suspend fun clearPeople()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
}
