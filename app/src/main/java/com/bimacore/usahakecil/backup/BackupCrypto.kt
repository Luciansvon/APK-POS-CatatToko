package com.bimacore.usahakecil.backup

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object BackupCrypto {
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    const val KEY_ITERATIONS = 120_000

    data class Parameters(
        val salt: ByteArray,
        val nonce: ByteArray,
    )

    fun newParameters(): Parameters = Parameters(
        salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes),
        nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes),
    )

    fun validatePin(pin: String) {
        require(pin.matches(Regex("\\d{4,8}"))) {
            "PIN backup harus berisi 4 sampai 8 angka"
        }
    }

    fun encrypt(
        payload: ByteArray,
        pin: String,
        parameters: Parameters,
        associatedData: ByteArray,
    ): ByteArray {
        validatePin(pin)
        validateParameters(parameters)
        return cipher(Cipher.ENCRYPT_MODE, pin, parameters, associatedData).doFinal(payload)
    }

    fun decrypt(
        payload: ByteArray,
        pin: String,
        saltBase64: String,
        nonceBase64: String,
        associatedData: ByteArray,
    ): ByteArray {
        validatePin(pin)
        val parameters = runCatching {
            Parameters(Base64.decode(saltBase64), Base64.decode(nonceBase64))
        }.getOrElse { throw IllegalArgumentException("Metadata enkripsi salinan rusak", it) }
        validateParameters(parameters)
        return try {
            cipher(Cipher.DECRYPT_MODE, pin, parameters, associatedData).doFinal(payload)
        } catch (error: AEADBadTagException) {
            throw IllegalArgumentException("PIN backup salah atau salinan rusak", error)
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("Salinan terenkripsi tidak dapat dibuka", error)
        }
    }

    fun encode(bytes: ByteArray): String = Base64.encode(bytes)

    private fun cipher(
        mode: Int,
        pin: String,
        parameters: Parameters,
        associatedData: ByteArray,
    ): Cipher {
        val keySpec = PBEKeySpec(
            pin.toCharArray(),
            parameters.salt,
            KEY_ITERATIONS,
            256,
        )
        val key = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(keySpec)
                .encoded
        } finally {
            keySpec.clearPassword()
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                mode,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, parameters.nonce),
            )
            updateAAD(associatedData)
        }
    }

    private fun validateParameters(parameters: Parameters) {
        require(parameters.salt.size == SALT_BYTES) { "Salt enkripsi salinan tidak valid" }
        require(parameters.nonce.size == NONCE_BYTES) { "Nonce enkripsi salinan tidak valid" }
    }
}
