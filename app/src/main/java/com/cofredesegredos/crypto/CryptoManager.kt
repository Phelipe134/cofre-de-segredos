package com.cofredesegredos.crypto

import org.json.JSONObject
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {
    private val secureRandom = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also { secureRandom.nextBytes(it) }

    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(key: SecretKey, plainText: String): String {
        val iv = randomBytes(GCM_IV_BYTES)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return JSONObject()
            .put("iv", encode(iv))
            .put("ciphertext", encode(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))))
            .toString()
    }

    fun decrypt(key: SecretKey, envelope: String): String {
        val payload = JSONObject(envelope)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, decode(payload.getString("iv"))))
        return String(cipher.doFinal(decode(payload.getString("ciphertext"))), Charsets.UTF_8)
    }

    fun validatePassword(key: SecretKey, verifierEnvelope: String) {
        decrypt(key, verifierEnvelope)
    }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        const val PBKDF2_ITERATIONS = 210_000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val AES_GCM = "AES/GCM/NoPadding"
    }
}
