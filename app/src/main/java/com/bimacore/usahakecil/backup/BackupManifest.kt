package com.bimacore.usahakecil.backup

import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
data class BackupManifest(
    val formatVersion: Int,
    val schemaVersion: Int,
    val businessUid: String,
    val businessName: String,
    val businessType: String,
    val createdAt: Long,
    val databaseSize: Int,
    val databaseSha256: String,
    val mediaSize: Int = 0,
    val mediaSha256: String = "",
    val payloadSize: Int = 0,
    val payloadSha256: String = "",
    val encryptionSaltBase64: String = "",
    val encryptionNonceBase64: String = "",
) {
    val isEncrypted: Boolean
        get() = formatVersion >= ENCRYPTED_FORMAT_VERSION

    fun serialize(): String = listOf(
        MAGIC,
        "formatVersion=$formatVersion",
        "schemaVersion=$schemaVersion",
        "businessUid=$businessUid",
        "businessNameBase64=${Base64.encode(businessName.encodeToByteArray())}",
        "businessType=$businessType",
        "createdAt=$createdAt",
        "databaseSize=$databaseSize",
        "databaseSha256=$databaseSha256",
        "mediaSize=$mediaSize",
        "mediaSha256=$mediaSha256",
        "payloadSize=$payloadSize",
        "payloadSha256=$payloadSha256",
        "encryptionSaltBase64=$encryptionSaltBase64",
        "encryptionNonceBase64=$encryptionNonceBase64",
    ).joinToString("\n")

    fun verify(databaseBytes: ByteArray, mediaBytes: ByteArray = ByteArray(0)): Boolean =
        databaseBytes.size == databaseSize &&
            MessageDigest.isEqual(
                databaseSha256.encodeToByteArray(),
                sha256(databaseBytes).encodeToByteArray(),
            ) &&
            (formatVersion < 2 || (
                mediaBytes.size == mediaSize &&
                    MessageDigest.isEqual(
                        mediaSha256.encodeToByteArray(),
                        sha256(mediaBytes).encodeToByteArray(),
                    )
                ))

    companion object {
        private const val MAGIC = "USKS_BACKUP"

        fun create(
            schemaVersion: Int,
            businessUid: String,
            businessName: String,
            businessType: String,
            createdAt: Long,
            databaseBytes: ByteArray,
            mediaBytes: ByteArray = ByteArray(0),
            formatVersion: Int = LEGACY_FORMAT_VERSION,
            payloadSize: Int = 0,
            payloadSha256: String = "",
            encryptionSaltBase64: String = "",
            encryptionNonceBase64: String = "",
        ): BackupManifest {
            require(formatVersion in 1..CURRENT_FORMAT_VERSION) {
                "Versi format salinan belum didukung"
            }
            require(schemaVersion > 0) { "Versi data salinan tidak valid" }
            require(businessUid.isNotBlank()) { "Identitas usaha pada salinan kosong" }
            require(businessName.isNotBlank()) { "Nama usaha pada salinan kosong" }
            require(businessType.isNotBlank()) { "Jenis usaha pada salinan kosong" }
            require(createdAt > 0) { "Waktu salinan tidak valid" }
            require(databaseBytes.isNotEmpty()) { "Data pada salinan kosong" }
            require(mediaBytes.size.toLong() <= MAX_MEDIA_SIZE_BYTES) { "Media pada salinan terlalu besar" }
            if (formatVersion >= ENCRYPTED_FORMAT_VERSION) {
                require(payloadSize > 0) { "Payload terenkripsi kosong" }
                require(payloadSha256.isNotBlank()) { "Hash payload terenkripsi kosong" }
                require(encryptionSaltBase64.isNotBlank()) { "Salt enkripsi salinan kosong" }
                require(encryptionNonceBase64.isNotBlank()) { "Nonce enkripsi salinan kosong" }
            }
            return BackupManifest(
                formatVersion = formatVersion,
                schemaVersion = schemaVersion,
                businessUid = businessUid,
                businessName = businessName,
                businessType = businessType,
                createdAt = createdAt,
                databaseSize = databaseBytes.size,
                databaseSha256 = sha256(databaseBytes),
                mediaSize = mediaBytes.size,
                mediaSha256 = sha256(mediaBytes),
                payloadSize = payloadSize,
                payloadSha256 = payloadSha256,
                encryptionSaltBase64 = encryptionSaltBase64,
                encryptionNonceBase64 = encryptionNonceBase64,
            )
        }

        fun parse(serialized: String): BackupManifest {
            val lines = serialized.lineSequence().filter { it.isNotBlank() }.toList()
            require(lines.firstOrNull() == MAGIC) { "Format salinan tidak dikenali" }
            val values = lines.drop(1).associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Keterangan salinan rusak" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
            val formatVersion = values.requiredInt("formatVersion")
            require(formatVersion in 1..CURRENT_FORMAT_VERSION) {
                "Versi format salinan belum didukung"
            }
            val businessName = runCatching {
                Base64.decode(values.required("businessNameBase64")).decodeToString()
            }.getOrElse {
                throw IllegalArgumentException("Nama usaha pada salinan rusak")
            }
            return BackupManifest(
                formatVersion = formatVersion,
                schemaVersion = values.requiredInt("schemaVersion"),
                businessUid = values.required("businessUid"),
                businessName = businessName,
                businessType = values.required("businessType"),
                createdAt = values.requiredLong("createdAt"),
                databaseSize = values.requiredInt("databaseSize"),
                databaseSha256 = values.required("databaseSha256"),
                mediaSize = values["mediaSize"]?.toIntOrNull() ?: 0,
                mediaSha256 = values["mediaSha256"].orEmpty(),
                payloadSize = values["payloadSize"]?.toIntOrNull() ?: 0,
                payloadSha256 = values["payloadSha256"].orEmpty(),
                encryptionSaltBase64 = values["encryptionSaltBase64"].orEmpty(),
                encryptionNonceBase64 = values["encryptionNonceBase64"].orEmpty(),
            )
        }

        internal fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }

        const val CURRENT_FORMAT_VERSION = 3
        const val ENCRYPTED_FORMAT_VERSION = 3
        private const val LEGACY_FORMAT_VERSION = 2
        private const val MAX_MEDIA_SIZE_BYTES = 256 * 1024 * 1024L
    }
}

private fun Map<String, String>.required(key: String): String =
    requireNotNull(this[key]).also { require(it.isNotBlank()) { "Metadata $key kosong" } }

private fun Map<String, String>.requiredInt(key: String): Int =
    required(key).toIntOrNull() ?: throw IllegalArgumentException("Metadata $key tidak valid")

private fun Map<String, String>.requiredLong(key: String): Long =
    required(key).toLongOrNull() ?: throw IllegalArgumentException("Metadata $key tidak valid")
