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
) {
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
    ).joinToString("\n")

    fun verify(databaseBytes: ByteArray): Boolean =
        databaseBytes.size == databaseSize &&
            MessageDigest.isEqual(
                databaseSha256.encodeToByteArray(),
                sha256(databaseBytes).encodeToByteArray(),
            )

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        private const val MAGIC = "USKS_BACKUP"

        fun create(
            schemaVersion: Int,
            businessUid: String,
            businessName: String,
            businessType: String,
            createdAt: Long,
            databaseBytes: ByteArray,
        ): BackupManifest {
            require(schemaVersion > 0) { "Versi database backup tidak valid" }
            require(businessUid.isNotBlank()) { "Identitas usaha backup kosong" }
            require(businessName.isNotBlank()) { "Nama usaha backup kosong" }
            require(businessType.isNotBlank()) { "Jenis usaha backup kosong" }
            require(createdAt > 0) { "Waktu backup tidak valid" }
            require(databaseBytes.isNotEmpty()) { "Database backup kosong" }
            return BackupManifest(
                formatVersion = CURRENT_FORMAT_VERSION,
                schemaVersion = schemaVersion,
                businessUid = businessUid,
                businessName = businessName,
                businessType = businessType,
                createdAt = createdAt,
                databaseSize = databaseBytes.size,
                databaseSha256 = sha256(databaseBytes),
            )
        }

        fun parse(serialized: String): BackupManifest {
            val lines = serialized.lineSequence().filter { it.isNotBlank() }.toList()
            require(lines.firstOrNull() == MAGIC) { "Format backup tidak dikenali" }
            val values = lines.drop(1).associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Metadata backup rusak" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
            val formatVersion = values.requiredInt("formatVersion")
            require(formatVersion == CURRENT_FORMAT_VERSION) { "Versi format backup belum didukung" }
            val businessName = runCatching {
                Base64.decode(values.required("businessNameBase64")).decodeToString()
            }.getOrElse {
                throw IllegalArgumentException("Nama usaha pada backup rusak")
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
            )
        }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
    }
}

private fun Map<String, String>.required(key: String): String =
    requireNotNull(this[key]).also { require(it.isNotBlank()) { "Metadata $key kosong" } }

private fun Map<String, String>.requiredInt(key: String): Int =
    required(key).toIntOrNull() ?: throw IllegalArgumentException("Metadata $key tidak valid")

private fun Map<String, String>.requiredLong(key: String): Long =
    required(key).toLongOrNull() ?: throw IllegalArgumentException("Metadata $key tidak valid")
