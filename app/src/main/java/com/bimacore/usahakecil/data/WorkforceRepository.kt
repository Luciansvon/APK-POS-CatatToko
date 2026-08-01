package com.bimacore.usahakecil.data

import androidx.room.withTransaction
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.LedgerRules
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.WorkforceRules
import kotlinx.coroutines.flow.Flow

enum class WorkerScheme {
    DAILY,
    FREELANCE,
}

class WorkforceRepository(
    private val database: PosDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val workforceDao = database.workforceDao()
    private val operationsDao = database.operationsDao()

    val employees: Flow<List<EmployeeEntity>> = workforceDao.observeEmployees()
    val attendance: Flow<List<AttendanceEntity>> = workforceDao.observeAttendance()
    val freelanceJobs: Flow<List<FreelanceJobEntity>> = workforceDao.observeFreelanceJobs()

    fun observeRates(employeeId: Long): Flow<List<WageRateEntity>> =
        workforceDao.observeRates(employeeId)

    fun observePayments(employeeId: Long): Flow<List<WorkerPaymentEntity>> =
        workforceDao.observeWorkerPayments(employeeId)

    suspend fun saveEmployee(
        id: Long?,
        name: String,
        phone: String,
        scheme: WorkerScheme,
    ): Long {
        require(name.isNotBlank()) { "Nama pekerja wajib diisi" }
        val now = clock()
        return if (id == null) {
            workforceDao.insertEmployee(
                EmployeeEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    scheme = scheme.name,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val current = requireNotNull(workforceDao.getEmployee(id)) {
                "Pekerja tidak tersedia"
            }
            workforceDao.updateEmployee(
                current.copy(
                    name = name.trim(),
                    phone = phone.trim(),
                    scheme = scheme.name,
                    updatedAt = now,
                ),
            )
            id
        }
    }

    suspend fun saveEmployeeWithInitialRate(
        name: String,
        phone: String,
        scheme: WorkerScheme,
        dailyRate: Long?,
        effectiveAt: Long,
    ): Long = database.withTransaction {
        val validatedRate = if (scheme == WorkerScheme.DAILY) {
            requireNotNull(dailyRate) { "Tarif harian wajib diisi" }.also {
                require(it in 1..MoneyMath.MAX_MONEY) { "Tarif harian tidak valid" }
            }
        } else {
            null
        }
        val employeeId = saveEmployee(null, name, phone, scheme)
        if (validatedRate != null) {
            addDailyRate(employeeId, validatedRate, effectiveAt)
        }
        employeeId
    }

    suspend fun setEmployeeActive(id: Long, active: Boolean) {
        val current = requireNotNull(workforceDao.getEmployee(id)) { "Pekerja tidak tersedia" }
        workforceDao.updateEmployee(current.copy(isActive = active, updatedAt = clock()))
    }

    suspend fun addDailyRate(
        employeeId: Long,
        amount: Long,
        effectiveAt: Long,
    ): Long {
        val employee = requireNotNull(workforceDao.getEmployee(employeeId)) {
            "Pekerja tidak tersedia"
        }
        require(employee.scheme == WorkerScheme.DAILY.name) { "Tarif harian hanya untuk pekerja harian" }
        require(amount in 1..MoneyMath.MAX_MONEY) { "Tarif harian tidak valid" }
        require(effectiveAt > 0) { "Tanggal berlaku tidak valid" }
        return workforceDao.insertRate(
            WageRateEntity(
                employeeId = employeeId,
                amount = amount,
                effectiveAt = effectiveAt,
                createdAt = clock(),
            ),
        )
    }

    suspend fun recordAttendance(
        employeeId: Long,
        workDate: Long,
        status: AttendanceStatus,
        overtime: Long,
        bonus: Long,
        deduction: Long,
        advance: Long,
        note: String,
    ): Long {
        val employee = requireNotNull(workforceDao.getEmployee(employeeId)) {
            "Pekerja tidak tersedia"
        }
        require(employee.scheme == WorkerScheme.DAILY.name) {
            "Kehadiran hanya untuk pekerja harian"
        }
        val rate = requireNotNull(workforceDao.getEffectiveRate(employeeId, workDate)) {
            "Tarif harian belum dibuat untuk tanggal ini"
        }
        val netPay = WorkforceRules.dailyPay(
            rate = rate.amount,
            attendance = status,
            overtime = overtime,
            bonus = bonus,
            deduction = deduction,
            advance = advance,
        )
        val now = clock()
        return workforceDao.insertAttendance(
            AttendanceEntity(
                employeeId = employeeId,
                workDate = workDate,
                status = status.name,
                rateSnapshot = rate.amount,
                overtime = overtime,
                bonus = bonus,
                deduction = deduction,
                advance = advance,
                netPay = netPay,
                note = note.trim(),
                isPaid = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun payAttendance(
        attendanceId: Long,
        note: String,
    ) = database.withTransaction {
        val attendance = requireNotNull(workforceDao.getAttendance(attendanceId)) {
            "Catatan kehadiran tidak tersedia"
        }
        require(!attendance.isPaid) { "Upah kehadiran ini sudah dibayar" }
        val now = clock()
        workforceDao.insertWorkerPayment(
            WorkerPaymentEntity(
                employeeId = attendance.employeeId,
                referenceType = "ATTENDANCE",
                referenceId = attendance.id,
                amount = attendance.netPay,
                note = note.trim(),
                paidAt = now,
            ),
        )
        workforceDao.updateAttendance(attendance.copy(isPaid = true, updatedAt = now))
        if (attendance.netPay > 0) {
            operationsDao.insertCashEntry(
                CashEntryEntity(
                    type = "WAGE_OUT",
                    amount = attendance.netPay,
                    category = "Upah harian",
                    note = note.trim().ifBlank { "Pembayaran upah harian" },
                    paymentMethod = "CASH",
                    referenceType = "ATTENDANCE",
                    referenceId = attendance.id,
                    createdAt = now,
                    shiftId = database.shiftDao().getOpenShift()?.id,
                ),
            )
        }
    }

    suspend fun createFreelanceJob(
        employeeId: Long,
        title: String,
        agreedAmount: Long,
        workDate: Long,
        note: String,
    ): Long {
        val employee = requireNotNull(workforceDao.getEmployee(employeeId)) {
            "Pekerja tidak tersedia"
        }
        require(employee.scheme == WorkerScheme.FREELANCE.name) {
            "Pekerjaan panggilan hanya untuk freelancer"
        }
        require(title.isNotBlank()) { "Nama pekerjaan wajib diisi" }
        require(agreedAmount in 1..MoneyMath.MAX_MONEY) { "Nilai pekerjaan tidak valid" }
        val now = clock()
        return workforceDao.insertFreelanceJob(
            FreelanceJobEntity(
                employeeId = employeeId,
                title = title.trim(),
                agreedAmount = agreedAmount,
                paidAmount = 0,
                status = "OPEN",
                workDate = workDate,
                note = note.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun payFreelanceJob(
        jobId: Long,
        amount: Long,
        note: String,
    ) = database.withTransaction {
        val job = requireNotNull(workforceDao.getFreelanceJob(jobId)) {
            "Pekerjaan tidak tersedia"
        }
        require(amount in 1..MoneyMath.MAX_MONEY) { "Nominal pembayaran tidak valid" }
        val totalPaid = Math.addExact(job.paidAmount, amount)
        require(totalPaid <= job.agreedAmount) { "Pembayaran melebihi nilai pekerjaan" }
        val now = clock()
        val status = LedgerRules.status(job.agreedAmount, listOf(totalPaid)).name
        workforceDao.insertWorkerPayment(
            WorkerPaymentEntity(
                employeeId = job.employeeId,
                referenceType = "FREELANCE_JOB",
                referenceId = job.id,
                amount = amount,
                note = note.trim(),
                paidAt = now,
            ),
        )
        workforceDao.updateFreelanceJob(
            job.copy(
                paidAmount = totalPaid,
                status = status,
                updatedAt = now,
            ),
        )
        operationsDao.insertCashEntry(
            CashEntryEntity(
                type = "WAGE_OUT",
                amount = amount,
                category = "Bayaran freelancer",
                note = note.trim().ifBlank { job.title },
                paymentMethod = "CASH",
                referenceType = "FREELANCE_JOB",
                referenceId = job.id,
                createdAt = now,
                shiftId = database.shiftDao().getOpenShift()?.id,
            ),
        )
    }
}
