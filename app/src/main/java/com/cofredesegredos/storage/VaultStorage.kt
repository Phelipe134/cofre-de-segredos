package com.cofredesegredos.storage

import android.content.Context
import com.cofredesegredos.crypto.CryptoManager
import com.cofredesegredos.models.VaultData
import org.json.JSONObject
import javax.crypto.SecretKey

class VaultStorage(
    context: Context,
    private val crypto: CryptoManager = CryptoManager()
) : VaultRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentKey: SecretKey? = null

    override fun isInitialized(): Boolean = prefs.getString(KEY_SALT, null) != null

    override fun createStore(password: String): VaultData {
        val salt = crypto.randomBytes(16)
        val key = crypto.deriveKey(password, salt)
        val data = VaultData.empty()

        prefs.edit()
            .putString(KEY_SALT, crypto.encode(salt))
            .putString(KEY_VERIFIER, crypto.encrypt(key, JSONObject().put("ok", true).toString()))
            .putString(KEY_VAULT_DATA, crypto.encrypt(key, data.toJson().toString()))
            .apply()

        currentKey = key
        return data
    }

    override fun unlock(password: String): VaultData {
        val salt = crypto.decode(prefs.getString(KEY_SALT, null) ?: error("Store ausente."))
        val key = crypto.deriveKey(password, salt)
        crypto.validatePassword(key, prefs.getString(KEY_VERIFIER, null) ?: error("Verificador ausente."))
        val rawData = crypto.decrypt(key, prefs.getString(KEY_VAULT_DATA, null) ?: error("Dados ausentes."))
        currentKey = key
        return VaultData.fromJson(JSONObject(rawData))
    }

    override fun save(data: VaultData) {
        val key = currentKey ?: error("Cofre bloqueado.")
        prefs.edit()
            .putString(KEY_VAULT_DATA, crypto.encrypt(key, data.toJson().toString()))
            .apply()
    }

    override fun exportBackup(): String {
        return BackupPayload(
            cryptoVersion = 1,
            kdf = "PBKDF2-HMAC-SHA256",
            salt = prefs.getString(KEY_SALT, null) ?: error("Nenhum cofre configurado."),
            verifier = prefs.getString(KEY_VERIFIER, null) ?: error("Verificador ausente."),
            vaultData = prefs.getString(KEY_VAULT_DATA, null) ?: error("Dados ausentes.")
        ).toJsonString()
    }

    override fun importBackup(rawBackup: String) {
        val backup = BackupPayload.fromJsonString(rawBackup)
        prefs.edit()
            .putString(KEY_SALT, backup.salt)
            .putString(KEY_VERIFIER, backup.verifier)
            .putString(KEY_VAULT_DATA, backup.vaultData)
            .apply()
        currentKey = null
    }

    override fun clearSession() {
        currentKey = null
    }

    companion object {
        private const val PREFS_NAME = "cofre_segredos_secure_store"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_VAULT_DATA = "vaultData"
    }
}
