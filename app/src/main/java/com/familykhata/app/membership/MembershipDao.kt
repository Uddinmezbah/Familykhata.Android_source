package com.familykhata.app.membership

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(
        item: MembershipMemberEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(
        item: MembershipPlanEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(
        item: MembershipSubscriptionEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(
        item: MembershipPaymentEntity
    ): Long

    @Query("""
        SELECT *
        FROM membership_members
        WHERE workspace = :workspace
        ORDER BY name COLLATE NOCASE
    """)
    fun observeMembers(
        workspace: String
    ): Flow<List<MembershipMemberEntity>>

    @Query("""
        SELECT *
        FROM membership_plans
        WHERE workspace = :workspace
          AND active = 1
        ORDER BY name COLLATE NOCASE
    """)
    fun observePlans(
        workspace: String
    ): Flow<List<MembershipPlanEntity>>

    @Query("""
        SELECT
            s.id AS subscriptionId,
            m.id AS memberId,
            m.name AS memberName,
            m.phone AS phone,
            p.id AS planId,
            p.name AS planName,
            s.startDate AS startDate,
            s.endDate AS endDate,
            s.totalFee AS totalFee,
            COALESCE(
                (
                    SELECT SUM(mp.amount)
                    FROM membership_payments mp
                    WHERE mp.subscriptionId = s.id
                ),
                0
            ) AS totalPaid,
            s.totalFee -
            COALESCE(
                (
                    SELECT SUM(mp.amount)
                    FROM membership_payments mp
                    WHERE mp.subscriptionId = s.id
                ),
                0
            ) AS dueAmount,
            s.status AS status
        FROM membership_subscriptions s
        INNER JOIN membership_members m
            ON m.id = s.memberId
        INNER JOIN membership_plans p
            ON p.id = s.planId
        WHERE m.workspace = :workspace
        ORDER BY
            CASE
                WHEN s.status = 'ACTIVE' THEN 0
                WHEN s.status = 'EXPIRED' THEN 1
                ELSE 2
            END,
            s.endDate ASC
    """)
    fun observeMembershipSummaries(
        workspace: String
    ): Flow<List<MembershipSummary>>

    @Query("""
        SELECT *
        FROM membership_payments
        WHERE subscriptionId = :subscriptionId
        ORDER BY paidAt DESC
    """)
    fun observePayments(
        subscriptionId: Long
    ): Flow<List<MembershipPaymentEntity>>

    @Query("""
        UPDATE membership_subscriptions
        SET status = :status
        WHERE id = :subscriptionId
    """)
    suspend fun updateSubscriptionStatus(
        subscriptionId: Long,
        status: String
    )

    @Query("""
        UPDATE membership_subscriptions
        SET status = 'EXPIRED'
        WHERE endDate < :now
          AND status = 'ACTIVE'
    """)
    suspend fun expireOldSubscriptions(
        now: Long
    )

    @Query("SELECT * FROM membership_members ORDER BY id")
    suspend fun getAllMembers():
        List<MembershipMemberEntity>

    @Query("SELECT * FROM membership_plans ORDER BY id")
    suspend fun getAllPlans():
        List<MembershipPlanEntity>

    @Query("SELECT * FROM membership_subscriptions ORDER BY id")
    suspend fun getAllSubscriptions():
        List<MembershipSubscriptionEntity>

    @Query("SELECT * FROM membership_payments ORDER BY id")
    suspend fun getAllPayments():
        List<MembershipPaymentEntity>
}
