package com.bimacore.usahakecil

import android.app.Application
import com.bimacore.usahakecil.backup.BackupManager
import com.bimacore.usahakecil.data.CulinaryRepository
import com.bimacore.usahakecil.data.InventoryRepository
import com.bimacore.usahakecil.data.OperationsRepository
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.data.PosRepository
import com.bimacore.usahakecil.data.ReportRepository
import com.bimacore.usahakecil.data.WorkforceRepository
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.export.ExcelExportManager
import com.bimacore.usahakecil.security.ReportSession

class PosApplication : Application() {
    val businessType: BusinessType by lazy {
        BusinessType.valueOf(BuildConfig.BUSINESS_TYPE)
    }
    val capabilities: BusinessCapabilities by lazy {
        BusinessCapabilities.forType(businessType)
    }
    val reportSession = ReportSession()

    @Volatile
    private var databaseInstance: PosDatabase? = null

    val database: PosDatabase
        @Synchronized get() = databaseInstance ?: PosDatabase.create(this).also {
            databaseInstance = it
        }

    fun newPosRepository() =
        PosRepository(
            database = database,
            businessType = businessType,
            businessName = getString(R.string.business_label),
        )

    fun newInventoryRepository() = InventoryRepository(database, capabilities)

    fun newOperationsRepository() = OperationsRepository(database, ownerSession = reportSession)

    fun newWorkforceRepository() = WorkforceRepository(database)

    fun newReportRepository() = ReportRepository(database, reportSession)

    fun newCulinaryRepository() = CulinaryRepository(database, capabilities)

    fun newBackupManager() = BackupManager(
        context = this,
        currentDatabase = { database },
        closeDatabase = ::closeDatabase,
        reopenDatabase = { database },
    )

    fun newExcelExportManager() = ExcelExportManager(
        context = this,
        database = database,
        ownerSession = reportSession,
        businessType = businessType.name,
    )

    @Synchronized
    private fun closeDatabase() {
        databaseInstance?.close()
        databaseInstance = null
    }
}
