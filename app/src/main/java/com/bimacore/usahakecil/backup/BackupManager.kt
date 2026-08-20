package com.bimacore.usahakecil.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.core.content.FileProvider
import com.bimacore.usahakecil.data.DatabaseOperationCoordinator
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.security.ReportSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupPreview(
    val manifest: BackupManifest,
    val sourceUri: Uri,
    internal val decryptionPin: String? = null,
)

class BackupManager(
    private val context: Context,
    private val currentDatabase: () -> PosDatabase,
    private val closeDatabase: () -> Unit,
    private val reopenDatabase: () -> PosDatabase,
    private val ownerSession: ReportSession,
    private val databaseOperations: DatabaseOperationCoordinator = DatabaseOperationCoordinator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val databaseName: String = DEFAULT_DATABASE_NAME,
) {
    suspend fun createBackup(pin: String): Uri = databaseOperations.withOperation {
        ownerSession.requireOwner()
        BackupCrypto.validatePin(pin)
        withContext(Dispatchers.IO) {
            val database = currentDatabase()
            val profile = requireNotNull(database.profileDao().getProfile()) {
                "Profil usaha belum tersedia"
            }
            checkpoint(database)
            val source = context.getDatabasePath(databaseName)
            require(source.exists()) { "Database aktif tidak ditemukan" }
            val bytes = source.readBytes()
            val mediaBytes = createMediaArchive(database)
            val payload = createPayload(bytes, mediaBytes)
            require(payload.size.toLong() <= MAX_PAYLOAD_SIZE_BYTES) {
                "Data pada salinan terlalu besar"
            }
            val parameters = BackupCrypto.newParameters()
            val manifest = BackupManifest.create(
                businessUid = profile.businessUid,
                businessName = profile.businessName,
                businessType = profile.businessType,
                createdAt = clock(),
                schemaVersion = DATABASE_SCHEMA_VERSION,
                databaseBytes = bytes,
                mediaBytes = mediaBytes,
                formatVersion = BackupManifest.ENCRYPTED_FORMAT_VERSION,
                payloadSize = payload.size,
                payloadSha256 = BackupManifest.sha256(payload),
                encryptionSaltBase64 = BackupCrypto.encode(parameters.salt),
                encryptionNonceBase64 = BackupCrypto.encode(parameters.nonce),
            )
            val encryptedPayload = BackupCrypto.encrypt(
                payload = payload,
                pin = pin,
                parameters = parameters,
                associatedData = manifest.serialize().toByteArray(Charsets.UTF_8),
            )
            val directory = File(context.cacheDir, BACKUP_DIRECTORY).apply { mkdirs() }
            cleanupGeneratedFiles(directory, "CatatToko-", MAX_BACKUPS_TO_KEEP)
            val output = File(directory, "CatatToko-${manifest.createdAt}.ukbackup.zip")
            writePackage(output, manifest, encryptedPayload)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                output,
            )
        }
    }

    suspend fun preview(uri: Uri, pin: String? = null): BackupPreview = databaseOperations.withOperation {
        ownerSession.requireOwner()
        withContext(Dispatchers.IO) {
            val packageData = readPackage(uri, pin)
            require(packageData.manifest.verify(packageData.databaseBytes, packageData.mediaBytes)) {
                "Berkas salinan rusak atau sudah berubah"
            }
            require(packageData.manifest.schemaVersion <= DATABASE_SCHEMA_VERSION) {
                "Versi salinan lebih baru dari aplikasi"
            }
            val profile = currentDatabase().profileDao().getProfile()
            if (profile != null) {
                require(packageData.manifest.businessType == profile.businessType) {
                    "Salinan dari jenis usaha ${packageData.manifest.businessType} tidak dapat dipasang pada aplikasi ${profile.businessType}"
                }
            }
            BackupPreview(packageData.manifest, uri, pin)
        }
    }

    suspend fun restore(preview: BackupPreview) = databaseOperations.withOperation {
        ownerSession.requireOwner()
        withContext(Dispatchers.IO) {
        val incoming = readPackage(preview.sourceUri, preview.decryptionPin)
        require(incoming.manifest == preview.manifest) { "Keterangan salinan berubah" }
        require(incoming.manifest.verify(incoming.databaseBytes, incoming.mediaBytes)) {
            "Berkas salinan rusak atau sudah berubah"
        }
        val profile = currentDatabase().profileDao().getProfile()
        if (profile != null) {
            require(incoming.manifest.businessType == profile.businessType) {
                "Salinan dari jenis usaha ${incoming.manifest.businessType} tidak dapat dipasang pada aplikasi ${profile.businessType}"
            }
        }
        val active = context.getDatabasePath(databaseName)
        val safetyDir = File(context.cacheDir, BACKUP_DIRECTORY).apply { mkdirs() }
        cleanupGeneratedFiles(safetyDir, "sebelum-restore-", MAX_SAFETY_DATABASES_TO_KEEP)
        val safety = File(safetyDir, "sebelum-restore-${clock()}.db")

        val currentSecurity = currentDatabase().securityDao().getReportSecurity()

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
                "Pemeriksaan data hasil pemulihan gagal"
            }
            if (currentSecurity != null) {
                reopened.securityDao().saveReportSecurity(currentSecurity)
            }
            val restoredProfile = requireNotNull(reopened.profileDao().getProfile()) {
                "Profil usaha pada salinan tidak valid"
            }
            require(restoredProfile.businessUid == preview.manifest.businessUid) {
                "Identitas usaha pada salinan tidak sesuai dengan keterangan"
            }
            if (profile != null) {
                require(restoredProfile.businessType == profile.businessType) {
                    "Profil usaha hasil pemulihan tidak sesuai dengan aplikasi"
                }
            }
            restoreMedia(reopened, incoming.mediaBytes)
        } catch (error: Exception) {
            closeDatabase()
            clearSidecars(active)
            safety.copyTo(active, overwrite = true)
            reopenDatabase()
            throw IllegalStateException(
                "Pemulihan gagal. Data aktif sudah dikembalikan seperti semula.",
                error,
            )
        }
        }
    }

    suspend fun saveCopy(sourceUri: Uri, destinationUri: Uri) = databaseOperations.withOperation {
        ownerSession.requireOwner()
        withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Salinan sementara tidak dapat dibuka")
            val output = context.contentResolver.openOutputStream(destinationUri, "w")
                ?: throw IllegalArgumentException("Lokasi simpan tidak dapat dibuka")
            input.use { source ->
                output.use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    var read: Int
                    while (source.read(buffer).also { read = it } != -1) {
                        total += read
                        require(total <= MAX_PACKAGE_SIZE_BYTES) {
                            "Salinan terlalu besar untuk disimpan"
                        }
                        target.write(buffer, 0, read)
                    }
                    target.flush()
                }
            }
        }
    }

    private fun writePackage(
        output: File,
        manifest: BackupManifest,
        encryptedPayload: ByteArray,
    ) {
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.serialize().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(PAYLOAD_ENTRY))
            zip.write(encryptedPayload)
            zip.closeEntry()
        }
    }

    private fun createPayload(
        databaseBytes: ByteArray,
        mediaBytes: ByteArray,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            zip.write(databaseBytes)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(MEDIA_ENTRY))
            zip.write(mediaBytes)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private suspend fun createMediaArchive(database: PosDatabase): ByteArray {
        val products = database.catalogDao().getAllProducts()
        val images = products.mapNotNull { product ->
            val value = product.imageUri?.trim().orEmpty()
            if (value.isBlank()) return@mapNotNull null
            val bytes = ProductImageStorage.readUri(context, value)
                ?: throw IllegalArgumentException("Foto produk ${product.name} tidak dapat dibaca untuk backup")
            ProductMedia(product.id, bytes)
        }
        if (images.isEmpty()) return ByteArray(0)
        val output = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip ->
            images.forEach { image ->
                zip.putNextEntry(ZipEntry("$MEDIA_DIRECTORY/${image.productId}.img"))
                zip.write(image.bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray().also {
            require(it.size.toLong() <= MAX_MEDIA_SIZE_BYTES) {
                "Foto produk pada backup terlalu besar"
            }
        }
    }

    private suspend fun restoreMedia(database: PosDatabase, mediaBytes: ByteArray) {
        if (mediaBytes.isEmpty()) return
        val restored = linkedMapOf<Long, String>()
        try {
            ZipInputStream(ByteArrayInputStream(mediaBytes).buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    require(!entry.isDirectory) { "Media backup berisi folder yang tidak valid" }
                    val prefix = "$MEDIA_DIRECTORY/"
                    require(entry.name.startsWith(prefix)) {
                        "Media backup berisi bagian tidak sah"
                    }
                    val productId = entry.name.removePrefix(prefix)
                        .removeSuffix(".img")
                        .toLongOrNull()
                    require(productId != null && productId > 0L) {
                        "Identitas foto produk pada backup tidak valid"
                    }
                    require(restored[productId] == null) { "Foto produk pada backup ganda" }
                    val bytes = readBytesWithLimit(zip, ProductImageStorage.MAX_IMAGE_BYTES)
                    restored[productId] = ProductImageStorage.writeRestored(
                        context,
                        "product-$productId.img",
                        bytes,
                    )
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: ZipException) {
            throw IllegalArgumentException("Media backup rusak", error)
        }
        database.withTransaction {
            restored.forEach { (productId, imageUri) ->
                val product = requireNotNull(database.catalogDao().getProduct(productId)) {
                    "Foto backup mengarah ke produk yang tidak tersedia"
                }
                database.catalogDao().updateProduct(product.copy(imageUri = imageUri))
            }
        }
    }

    private fun checkpoint(database: PosDatabase) {
        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { cursor ->
                require(cursor.moveToFirst()) { "Checkpoint database tidak memberi hasil" }
                require(cursor.getInt(0) == 0) { "Data masih sibuk, coba buat salinan lagi" }
            }
    }

    private fun readPackage(uri: Uri, pin: String? = null): PackageData {
        var manifest: BackupManifest? = null
        var databaseBytes: ByteArray? = null
        var mediaBytes: ByteArray? = null
        var encryptedPayload: ByteArray? = null
        var entryCount = 0
        val seenEntries = mutableSetOf<String>()
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Berkas salinan tidak dapat dibuka")
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                require(entryCount <= 3) { "Berkas salinan berisi bagian yang tidak valid" }
                val name = entry.name
                require(name == MANIFEST_ENTRY ||
                    name == DATABASE_ENTRY ||
                    name == MEDIA_ENTRY ||
                    name == PAYLOAD_ENTRY
                ) {
                    "Berkas salinan berisi bagian tidak sah: $name"
                }
                require(seenEntries.add(name)) { "Berkas salinan berisi bagian ganda: $name" }
                when (name) {
                    MANIFEST_ENTRY -> {
                        val bytes = readBytesWithLimit(zip, MAX_MANIFEST_SIZE_BYTES)
                        manifest = BackupManifest.parse(bytes.toString(Charsets.UTF_8))
                    }
                    DATABASE_ENTRY -> {
                        databaseBytes = readBytesWithLimit(zip, MAX_BACKUP_SIZE_BYTES)
                    }
                    MEDIA_ENTRY -> {
                        mediaBytes = readBytesWithLimit(zip, MAX_MEDIA_SIZE_BYTES)
                    }
                    PAYLOAD_ENTRY -> {
                        encryptedPayload = readBytesWithLimit(zip, MAX_PAYLOAD_SIZE_BYTES + AES_GCM_TAG_BYTES)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val resolvedManifest = requireNotNull(manifest) { "Keterangan salinan tidak ditemukan" }
        return if (resolvedManifest.isEncrypted) {
            require(databaseBytes == null && mediaBytes == null) {
                "Berkas terenkripsi berisi data mentah"
            }
            val encrypted = requireNotNull(encryptedPayload) {
                "Payload terenkripsi tidak ditemukan"
            }
            val suppliedPin = requireNotNull(pin) {
                "Masukkan PIN Owner untuk membuka salinan"
            }
            require(encrypted.size.toLong() <= MAX_PAYLOAD_SIZE_BYTES + AES_GCM_TAG_BYTES) {
                "Payload terenkripsi terlalu besar"
            }
            require(resolvedManifest.payloadSize > 0) {
                "Ukuran payload terenkripsi tidak valid"
            }
            val payload = BackupCrypto.decrypt(
                payload = encrypted,
                pin = suppliedPin,
                saltBase64 = resolvedManifest.encryptionSaltBase64,
                nonceBase64 = resolvedManifest.encryptionNonceBase64,
                associatedData = resolvedManifest.serialize().toByteArray(Charsets.UTF_8),
            )
            require(payload.size == resolvedManifest.payloadSize) {
                "Payload terenkripsi berubah"
            }
            require(BackupManifest.sha256(payload) == resolvedManifest.payloadSha256) {
                "Payload terenkripsi berubah"
            }
            readPayload(payload, resolvedManifest)
        } else {
            PackageData(
                manifest = resolvedManifest,
                databaseBytes = requireNotNull(databaseBytes) { "Isi database tidak ditemukan" },
                mediaBytes = mediaBytes ?: ByteArray(0),
            )
        }
    }

    private fun readPayload(payload: ByteArray, manifest: BackupManifest): PackageData {
        var databaseBytes: ByteArray? = null
        var mediaBytes: ByteArray? = null
        var entryCount = 0
        val seenEntries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(payload).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                require(entryCount <= 2) { "Payload salinan berisi bagian yang tidak valid" }
                require(!entry.isDirectory) { "Payload salinan berisi folder yang tidak valid" }
                require(entry.name == DATABASE_ENTRY || entry.name == MEDIA_ENTRY) {
                    "Payload salinan berisi bagian tidak sah"
                }
                require(seenEntries.add(entry.name)) { "Payload salinan berisi bagian ganda" }
                when (entry.name) {
                    DATABASE_ENTRY -> databaseBytes = readBytesWithLimit(zip, MAX_BACKUP_SIZE_BYTES)
                    MEDIA_ENTRY -> mediaBytes = readBytesWithLimit(zip, MAX_MEDIA_SIZE_BYTES)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return PackageData(
            manifest = manifest,
            databaseBytes = requireNotNull(databaseBytes) { "Isi database tidak ditemukan" },
            mediaBytes = mediaBytes ?: ByteArray(0),
        )
    }

    private fun readBytesWithLimit(zip: ZipInputStream, maxBytes: Long): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0L
        var read: Int
        while (zip.read(buffer).also { read = it } != -1) {
            totalRead += read
            require(totalRead <= maxBytes) { "Ukuran berkas salinan melebihi batas" }
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
        check(temporary.renameTo(target)) { "Data hasil pemulihan tidak dapat dipasang" }
    }

    private fun clearSidecars(database: File) {
        File(database.path + "-wal").delete()
        File(database.path + "-shm").delete()
    }

    private data class PackageData(
        val manifest: BackupManifest,
        val databaseBytes: ByteArray,
        val mediaBytes: ByteArray,
    )

    private data class ProductMedia(
        val productId: Long,
        val bytes: ByteArray,
    )

    private fun cleanupGeneratedFiles(directory: File, prefix: String, keep: Int) {
        directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.startsWith(prefix) }
            .sortedByDescending { it.lastModified() }
            .drop(keep)
            .forEach(File::delete)
    }

    companion object {
        const val SENSITIVITY_WARNING = "Salinan data terenkripsi dengan PIN Owner dan berisi informasi usaha, transaksi, pelanggan, serta pekerja. Simpan PIN terpisah dari file dan bagikan hanya kepada pihak yang dipercaya."
        
        private const val DEFAULT_DATABASE_NAME = "usaha-kecil-pos.db"
        private const val DATABASE_SCHEMA_VERSION = 6
        private const val BACKUP_DIRECTORY = "backups"
        private const val MANIFEST_ENTRY = "manifest.txt"
        private const val DATABASE_ENTRY = "database.db"
        private const val MEDIA_ENTRY = "media.zip"
        private const val PAYLOAD_ENTRY = "payload.bin"
        private const val MEDIA_DIRECTORY = "product-images"
        private const val MAX_BACKUP_SIZE_BYTES = 256 * 1024 * 1024L
        private const val MAX_MEDIA_SIZE_BYTES = 256 * 1024 * 1024L
        private const val MAX_PAYLOAD_SIZE_BYTES = 512 * 1024 * 1024L
        private const val AES_GCM_TAG_BYTES = 16L
        private const val MAX_PACKAGE_SIZE_BYTES = MAX_PAYLOAD_SIZE_BYTES + AES_GCM_TAG_BYTES + 2 * 1024 * 1024L
        private const val MAX_MANIFEST_SIZE_BYTES = 1 * 1024 * 1024L
        private const val MAX_BACKUPS_TO_KEEP = 5
        private const val MAX_SAFETY_DATABASES_TO_KEEP = 3
    }
}
