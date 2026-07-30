package com.bimacore.usahakecil.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class PinHashRecord(
    val saltBase64: String,
    val hashBase64: String,
    val iterations: Int,
)

@OptIn(ExperimentalEncodingApi::class)
object PinHasher {
    private const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    fun create(
        pin: String,
        iterations: Int = DEFAULT_ITERATIONS,
    ): PinHashRecord {
        validatePin(pin)
        require(iterations >= 100_000) { "Pengamanan PIN terlalu lemah" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt, iterations)
        return PinHashRecord(
            saltBase64 = Base64.encode(salt),
            hashBase64 = Base64.encode(hash),
            iterations = iterations,
        )
    }

    fun verify(
        pin: String,
        record: PinHashRecord,
    ): Boolean {
        if (!pin.matches(PIN_PATTERN)) return false
        val salt = runCatching { Base64.decode(record.saltBase64) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(record.hashBase64) }.getOrNull() ?: return false
        val actual = runCatching { derive(pin, salt, record.iterations) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(
        pin: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun validatePin(pin: String) {
        require(pin.matches(PIN_PATTERN)) { "PIN harus berisi 4 sampai 8 angka" }
    }

    private val PIN_PATTERN = Regex("\\d{4,8}")
}
