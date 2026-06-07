package com.cofredesegredos.storage

import org.json.JSONObject

data class BackupPayload(
    val app: String = APP_ID,
    val backupVersion: Int = BACKUP_VERSION,
    val cryptoVersion: Int,
    val kdf: String,
    val salt: String,
    val verifier: String,
    val vaultData: String,
    val exportedAt: Long = System.currentTimeMillis()
) {
    fun toJsonString(): String = JSONObject()
        .put("app", app)
        .put("backupVersion", backupVersion)
        .put("cryptoVersion", cryptoVersion)
        .put("kdf", kdf)
        .put("salt", salt)
        .put("verifier", JSONObject(verifier))
        .put("vaultData", JSONObject(vaultData))
        .put("exportedAt", exportedAt)
        .toString()

    companion object {
        const val APP_ID = "cofre-de-segredos"
        const val BACKUP_VERSION = 1

        fun fromJsonString(raw: String): BackupPayload {
            val json = JSONObject(raw)
            require(json.optString("app") == APP_ID) { "Arquivo de backup invalido." }
            require(json.optInt("backupVersion") == BACKUP_VERSION) { "Versao de backup nao suportada." }

            val verifier = json.optJSONObject("verifier") ?: error("Backup sem verificador.")
            val vaultData = json.optJSONObject("vaultData") ?: error("Backup sem dados do cofre.")
            val salt = json.optString("salt")
            require(salt.isNotBlank()) { "Backup sem salt." }

            return BackupPayload(
                cryptoVersion = json.optInt("cryptoVersion", 1),
                kdf = json.optString("kdf", "PBKDF2-HMAC-SHA256"),
                salt = salt,
                verifier = verifier.toString(),
                vaultData = vaultData.toString(),
                exportedAt = json.optLong("exportedAt", 0L)
            )
        }
    }
}
