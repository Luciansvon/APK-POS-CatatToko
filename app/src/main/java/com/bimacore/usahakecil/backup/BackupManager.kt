package com.bimacore.usahakecil.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.bimacore.usahakecil.data.PosDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupPreview(
    val manifest: BackupManifest,
    val sourceUri: Uri,
)

class BackupManager(
    private val context: Context,
    private val currentDatabase: () -> PosDatabase,
    private val closeDatabase: () -> Unit,
    private val reopenDatabase: () -> PosDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val databaseName: String = DEFAULT_DATABASE_NAME,
) {
    suspend fun createBackup(): Uri = withContext(Dispatchers.IO) {
        val database = currentDatabase()
        val profile = requireNotNull(database.profileDao().getProfile()) {
            "Profil usaha belum tersedia"
        }
        checkpoint(database)
        val source = context.getDatabasePath(databaseName)
        require(source.exists()) { "Database aktif tidak ditemukan" }
        val bytes = source.readBytes()
        val manifest = BackupManifest.create(
            businessUid = profile.businessUid,
            businessName = profile.businessName,
            businessType = profile.businessType,
            createdAt = clock(),
            schemaVersion = DATABASE_SCHEMA_VERSION,
            databaseBytes = bytes,
        )
        val directory = File(context.cacheDir, BACKUP_DIRECTORY).apply { mkdirs() }
        val output = File(directory, "usaha-kecil-${manifest.createdAt}.ukbackup")
        writePackage(output, manifest, bytes)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
    }

    suspend fun preview(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val packageData = readPackage(uri)
        require(packageData.manifest.verify(packageData.databaseBytes)) {
            "File backup rusak atau sudah berubah"
        }
        require(packageData.manifest.schemaVersion <= DATABASE_SCHEMA_VERSION) {
            "Versi backup lebih baru dari aplikasi"
        }
        val profile = currentDatabase().profileDao().getProfile()
        if (profile != null) {
            require(packageData.manifest.businessType == profile.businessType) {
                "File backup dari jenis usaha ${packageData.manifest.businessType} tidak dapat dipasang pada aplikasi ${profile.businessType}"
            }
        }
        BackupPreview(packageData.manifest, uri)
    }

    suspend fun restore(preview: BackupPreview) = withContext(Dispatchers.IO) {
        val incoming = readPackage(preview.sourceUri)
        require(incoming.manifest == preview.manifest) { "Metadata backup berubah" }
        require(incoming.manifest.verify(incoming.databaseBytes)) {
            "File backup rusak atau sudah berubah"
        }
        val profile = currentDatabase().profileDao().getProfile()
        if (profile != null) {
            require(incoming.manifest.businessType == profile.businessType) {
                "File backup dari jenis usaha ${incoming.manifest.businessType} tidak dapat dipasang pada aplikasi ${profile.businessType}"
            }
        }
        val active = context.getDatabasePath(databaseName)
        val safetyDir = File(context.cacheDir, BACKUP_DIRECTORY).apply { mkdirs() }
        val safety = File(safetyDir, "sebelum-restore-${clock()}.db")

        checkpoint(currentDatabase())
        active.copyTo(safety, overwrite = true)
        closeDatabase()
        clearSidecars(active)
        try {
            writeAtomically(active, incoming.databaseBytes)
            val reopened = reopenDatabase()
            val integrity = reopened.openHelper.writableDatabase
                .query("PRAGMA integrity_check")
                .use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else ""
                }
            require(integrity.equals("ok", ignoreCase = true)) {
                "Pemeriksaan database hasil restore gagal"
            }
            val restoredProfile = requireNotNull(reopened.profileDao().getProfile()) {
                "Profil usaha pada backup tidak valid"
            }
            if (profile != null) {
                require(restoredProfile.businessType == profile.businessType) {
                    "Profil usaha hasil restore tidak sesuai dengan aplikasi"
                }
            }
        } catch (error: Exception) {
            closeDatabase()
            clearSidecars(active)
            safety.copyTo(active, overwrite = true)
            reopenDatabase()
            throw IllegalStateException(
                "Restore gagal. Data aktif sudah dikembalikan seperti semula.",
                error,
            )
        }
    }

    private fun writePackage(
        output: File,
        manifest: BackupManifest,
        databaseBytes: ByteArray,
    ) {
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.serialize().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            zip.write(databaseBytes)
            zip.closeEntry()
        }
    }

    private fun checkpoint(database: PosDatabase) {
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { cursor ->
                require(cursor.moveToFirst()) { "Checkpoint database tidak memberi hasil" }
                require(cursor.getInt(0) == 0) { "Database masih sibuk, coba backup lagi" }
            }
    }

    private fun readPackage(uri: Uri): PackageData {
        var manifest: BackupManifest? = null
        var databaseBytes: ByteArray? = null
        var entryCount = 0
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("File backup tidak dapat dibuka")
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                require(entryCount <= 2) { "File backup berisi entry yang tidak valid" }
                val name = entry.name
                require(name == MANIFEST_ENTRY || name == DATABASE_ENTRY) {
                    "File backup berisi entry tidak sah: $name"
                }
                when (name) {
                    MANIFEST_ENTRY -> {
                        val bytes = readBytesWithLimit(zip, MAX_MANIFEST_SIZE_BYTES)
                        manifest = BackupManifest.parse(bytes.toString(Charsets.UTF_8))
                    }
                    DATABASE_ENTRY -> {
                        databaseBytes = readBytesWithLimit(zip, MAX_BACKUP_SIZE_BYTES)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return PackageData(
            manifest = requireNotNull(manifest) { "Metadata backup tidak ditemukan" },
            databaseBytes = requireNotNull(databaseBytes) { "Isi database tidak ditemukan" },
        )
    }

    private fun readBytesWithLimit(zip: ZipInputStream, maxBytes: Long): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0L
        var read: Int
        while (zip.read(buffer).also { read = it } != -1) {
            totalRead += read
            require(totalRead <= maxBytes) { "Ukuran file backup melebihi batas" }
            baos.write(buffer, 0, read)
        }
        return baos.toByteArray()
    }

    private fun writeAtomically(
        target: File,
        bytes: ByteArray,
    ) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.restore-tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        if (target.exists() && !target.delete()) {
            temporary.delete()
            error("Database lama tidak dapat dipindahkan")
        }
        check(temporary.renameTo(target)) { "Database hasil restore tidak dapat dipasang" }
    }

    private fun clearSidecars(database: File) {
        File(database.path + "-wal").delete()
        File(database.path + "-shm").delete()
    }

    private data class PackageData(
        val manifest: BackupManifest,
        val databaseBytes: ByteArray,
    )

    companion object {
        private const val DEFAULT_DATABASE_NAME = "usaha-kecil-pos.db"
        private const val DATABASE_SCHEMA_VERSION = 4
        private const val BACKUP_DIRECTORY = "backups"
        private const val MANIFEST_ENTRY = "manifest.txt"
        private const val DATABASE_ENTRY = "database.db"
        private const val MAX_BACKUP_SIZE_BYTES = 256 * 1024 * 1024L
        private const val MAX_MANIFEST_SIZE_BYTES = 1 * 1024 * 1024L
    }
}
