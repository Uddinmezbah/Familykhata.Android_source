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
    suspend fun upsertBusinessProfile(
        item: BusinessProfileEntity
    )

    @Query(
        """
        SELECT *
        FROM business_profiles
        WHERE isActive = 1
        ORDER BY createdAt ASC
        """
    )
    fun observeBusinessProfiles():
        Flow<List<BusinessProfileEntity>>

    @Query(
        """
        SELECT *
        FROM business_profiles
        WHERE businessId = :businessId
        LIMIT 1
        """
    )
    suspend fun getBusinessProfile(
        businessId: String
    ): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(
        item: TransactionEntity
    ): Long

    @Delete
    suspend fun deleteTransaction(item: TransactionEntity)

    @Query(
        """
        UPDATE transactions
        SET type = :type,
            amount = :amount,
            category = :category,
            note = :note,
            financialAccountId = :financialAccountId
        WHERE id = :transactionId
        """
    )
    suspend fun updateTransaction(
        transactionId: Long,
        type: String,
        amount: Double,
        category: String,
        note: String,
        financialAccountId: Long?
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialAccount(
        account: FinancialAccountEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialAccountEntry(
        entry: FinancialAccountEntryEntity
    ): Long

    @Query(
        """
        SELECT
            a.id AS id,
            a.name AS name,
            a.type AS type,
            a.provider AS provider,
            a.openingBalance AS openingBalance,
            a.openingBalance +
                COALESCE(SUM(e.balanceDelta), 0) AS balance,
            a.workspace AS workspace,
            a.isActive AS isActive
        FROM financial_accounts a
        LEFT JOIN financial_account_entries e
            ON e.accountId = a.id
        WHERE a.workspace = :workspace
        GROUP BY a.id
        ORDER BY
            a.isActive DESC,
            a.name COLLATE NOCASE ASC
        """
    )
    fun observeFinancialAccounts(
        workspace: String
    ): Flow<List<FinancialAccountSummary>>

    @Query(
        """
        SELECT *
        FROM financial_accounts
        WHERE id = :accountId
        LIMIT 1
        """
    )
    suspend fun getFinancialAccountOnce(
        accountId: Long
    ): FinancialAccountEntity?

    @Query(
        """
        SELECT
            a.openingBalance +
            COALESCE(
                (
                    SELECT SUM(e.balanceDelta)
                    FROM financial_account_entries e
                    WHERE e.accountId = a.id
                ),
                0
            )
        FROM financial_accounts a
        WHERE a.id = :accountId
        LIMIT 1
        """
    )
    suspend fun getFinancialAccountBalanceOnce(
        accountId: Long
    ): Double?

    @Query(
        """
        SELECT *
        FROM financial_account_entries
        WHERE accountId = :accountId
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun observeFinancialAccountEntries(
        accountId: Long
    ): Flow<List<FinancialAccountEntryEntity>>

    @Query(
        """
        SELECT *
        FROM financial_account_entries
        WHERE sourceKey = :sourceKey
        LIMIT 1
        """
    )
    suspend fun getFinancialAccountEntryBySourceKey(
        sourceKey: String
    ): FinancialAccountEntryEntity?

    @Query(
        """
        DELETE FROM financial_account_entries
        WHERE sourceKey = :sourceKey
        """
    )
    suspend fun deleteFinancialAccountEntryBySourceKey(
        sourceKey: String
    )

    @Query(
        "SELECT * FROM financial_accounts ORDER BY id ASC"
    )
    suspend fun getAllFinancialAccounts():
        List<FinancialAccountEntity>

    @Query(
        "SELECT * FROM financial_account_entries ORDER BY id ASC"
    )
    suspend fun getAllFinancialAccountEntries():
        List<FinancialAccountEntryEntity>

    @Query("DELETE FROM financial_account_entries")
    suspend fun clearFinancialAccountEntries()

    @Query("DELETE FROM financial_accounts")
    suspend fun clearFinancialAccounts()

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertDigitalServiceTransaction(
        item: DigitalServiceTransactionEntity
    ): Long

    @Query(
        """
        SELECT *
        FROM digital_service_transactions
        WHERE workspace = :workspace
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun observeDigitalServiceTransactions(
        workspace: String
    ): Flow<List<DigitalServiceTransactionEntity>>

    @Query(
        """
        SELECT *
        FROM digital_service_transactions
        ORDER BY id ASC
        """
    )
    suspend fun getAllDigitalServiceTransactions():
        List<DigitalServiceTransactionEntity>

    @Query(
        "DELETE FROM digital_service_transactions"
    )
    suspend fun clearDigitalServiceTransactions()

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
