package com.familykhata.app.coaching

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CoachingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao = CoachingDatabase.get(application).dao()

    private val workspace =
        MutableStateFlow("SHOP")

    val students: StateFlow<List<CoachingStudentEntity>> =
        workspace.flatMapLatest {
            dao.observeStudents(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val batches: StateFlow<List<CoachingBatchEntity>> =
        workspace.flatMapLatest {
            dao.observeBatches(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val summaries: StateFlow<List<CoachingEnrollmentSummary>> =
        workspace.flatMapLatest {
            dao.observeEnrollmentSummaries(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setWorkspace(value: String) {
        if (workspace.value != value) {
            workspace.value = value
        }
    }

    fun addStudent(
        name: String,
        phone: String,
        guardian: String,
        address: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertStudent(
                CoachingStudentEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    guardianName = guardian.trim(),
                    address = address.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun addBatch(
        name: String,
        admissionFee: Double,
        monthlyFee: Double,
        note: String,
        workspace: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            dao.insertBatch(
                CoachingBatchEntity(
                    name = name.trim(),
                    admissionFee =
                        admissionFee.coerceAtLeast(0.0),
                    monthlyFee =
                        monthlyFee.coerceAtLeast(0.0),
                    note = note.trim(),
                    workspace = workspace
                )
            )
        }
    }

    fun enroll(
        studentId: Long,
        batchId: Long
    ) {
        if (studentId <= 0 || batchId <= 0) return

        viewModelScope.launch {
            dao.insertEnrollment(
                CoachingEnrollmentEntity(
                    studentId = studentId,
                    batchId = batchId
                )
            )
        }
    }

    fun addAdmissionCharge(
        enrollmentId: Long,
        amount: Double
    ) {
        addCharge(
            enrollmentId = enrollmentId,
            feeType = "ADMISSION",
            periodKey = "",
            amount = amount,
            dueDate = System.currentTimeMillis(),
            note = ""
        )
    }

    fun addMonthlyCharge(
        enrollmentId: Long,
        amount: Double
    ) {
        val period =
            SimpleDateFormat(
                "yyyy-MM",
                Locale.US
            ).format(Date())

        addCharge(
            enrollmentId = enrollmentId,
            feeType = "MONTHLY",
            periodKey = period,
            amount = amount,
            dueDate = System.currentTimeMillis(),
            note = ""
        )
    }

    fun addCharge(
        enrollmentId: Long,
        feeType: String,
        periodKey: String,
        amount: Double,
        dueDate: Long?,
        note: String
    ) {
        if (enrollmentId <= 0 || amount <= 0) return

        viewModelScope.launch {
            val guardedType =
                feeType == "ADMISSION" ||
                feeType == "MONTHLY"

            if (
                guardedType &&
                dao.countCharge(
                    enrollmentId = enrollmentId,
                    feeType = feeType,
                    periodKey = periodKey
                ) > 0
            ) {
                return@launch
            }

            dao.insertCharge(
                CoachingChargeEntity(
                    enrollmentId = enrollmentId,
                    feeType = feeType,
                    periodKey = periodKey,
                    amount = amount,
                    dueDate = dueDate,
                    note = note.trim()
                )
            )
        }
    }

    fun addPayment(
        enrollmentId: Long,
        amount: Double,
        note: String = ""
    ) {
        if (enrollmentId <= 0 || amount <= 0) return

        viewModelScope.launch {
            dao.insertPayment(
                CoachingPaymentEntity(
                    enrollmentId = enrollmentId,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun observeCharges(
        enrollmentId: Long
    ): Flow<List<CoachingChargeEntity>> =
        dao.observeCharges(enrollmentId)

    fun observePayments(
        enrollmentId: Long
    ): Flow<List<CoachingPaymentEntity>> =
        dao.observePayments(enrollmentId)
}
