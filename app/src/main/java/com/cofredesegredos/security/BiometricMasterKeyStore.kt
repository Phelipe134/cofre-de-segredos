package com.cofredesegredos.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricMasterKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean =
        prefs.getString(KEY_ENCRYPTED_MASTER_PASSWORD, null) != null

    fun enable(masterPassword: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(masterPassword.toByteArray(Charsets.UTF_8))
        val payload = JSONObject()
            .put("iv", encode(cipher.iv))
            .put("ciphertext", encode(encrypted))
            .toString()
        prefs.edit().putString(KEY_ENCRYPTED_MASTER_PASSWORD, payload).apply()
    }

    fun decryptMasterPassword(): String {
        val payload = JSONObject(
            prefs.getString(KEY_ENCRYPTED_MASTER_PASSWORD, null)
                ?: error("Biometria nao configurada.")
        )
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, decode(payload.getString("iv"))))
        return String(cipher.doFinal(decode(payload.getString("ciphertext"))), Charsets.UTF_8)
    }

    fun disable() {
        prefs.edit().remove(KEY_ENCRYPTED_MASTER_PASSWORD).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationRequired(false)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        private const val PREFS_NAME = "cofre_segredos_biometric_store"
        private const val KEY_ENCRYPTED_MASTER_PASSWORD = "encrypted_master_password"
        private const val KEY_ALIAS = "cofre_segredos_biometric_master_password"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
