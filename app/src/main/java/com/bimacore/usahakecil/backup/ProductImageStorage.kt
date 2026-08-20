package com.bimacore.usahakecil.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object ProductImageStorage {
    const val MAX_IMAGE_BYTES = 8 * 1024 * 1024L
    private const val DIRECTORY = "product-images"

    fun copyFromUri(context: Context, source: Uri): String {
        val directory = directory(context).apply { mkdirs() }
        val target = File(directory, "image-${UUID.randomUUID()}.bin")
        val input = context.contentResolver.openInputStream(source)
            ?: throw IllegalArgumentException("Foto produk tidak dapat dibaca")
        input.use { sourceStream ->
            FileOutputStream(target).use { targetStream ->
                copyWithLimit(sourceStream, targetStream, MAX_IMAGE_BYTES)
                targetStream.fd.sync()
            }
        }
        return uriForFile(context, target)
    }

    fun readUri(context: Context, value: String): ByteArray? {
        val uri = Uri.parse(value)
        val input = if (uri.authority == authority(context)) {
            resolveOwnedFile(context, uri)?.let(::FileInputStream)
        } else {
            context.contentResolver.openInputStream(uri)
        } ?: return null
        return input.use { source ->
            ByteArrayOutputStream().use { output ->
                copyWithLimit(source, output, MAX_IMAGE_BYTES)
                output.toByteArray()
            }
        }
    }

    fun writeRestored(context: Context, fileName: String, bytes: ByteArray): String {
        validateFileName(fileName)
        require(bytes.size.toLong() <= MAX_IMAGE_BYTES) { "Foto produk terlalu besar" }
        val directory = directory(context).apply { mkdirs() }
        val target = File(directory, "restore-${UUID.randomUUID()}-$fileName")
        FileOutputStream(target).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        return uriForFile(context, target)
    }

    fun isOwnedUri(context: Context, value: String): Boolean =
        resolveOwnedFile(context, Uri.parse(value)) != null

    private fun resolveOwnedFile(context: Context, uri: Uri): File? {
        if (uri.scheme != "content" || uri.authority != authority(context)) return null
        val name = uri.lastPathSegment ?: return null
        if (name != uri.pathSegments.lastOrNull() || name == "." || name == "..") return null
        validateFileNameOrNull(name) ?: return null
        val directory = directory(context).canonicalFile
        val target = File(directory, name).canonicalFile
        if (target.parentFile != directory || !target.isFile) return null
        return target
    }

    private fun uriForFile(context: Context, file: File): String =
        FileProvider.getUriForFile(context, authority(context), file).toString()

    private fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    private fun authority(context: Context): String = "${context.packageName}.fileprovider"

    private fun copyWithLimit(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        limit: Long,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "Foto produk terlalu besar" }
            output.write(buffer, 0, read)
        }
        require(total > 0L) { "Foto produk kosong" }
    }

    private fun validateFileName(value: String) {
        require(validateFileNameOrNull(value) != null) { "Nama foto produk tidak valid" }
    }

    private fun validateFileNameOrNull(value: String): String? =
        value.takeIf { it.isNotBlank() && it.length <= 128 &&
            it.all { character -> character.isLetterOrDigit() || character == '.' || character == '-' || character == '_' }
        }
}
