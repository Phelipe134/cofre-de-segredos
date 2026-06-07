package com.cofredesegredos.models

import org.json.JSONArray
import org.json.JSONObject

data class VaultSettings(
    val lockTimeoutMs: Long = 300_000L
) {
    fun toJson(): JSONObject = JSONObject().put("lockTimeoutMs", lockTimeoutMs)

    companion object {
        fun fromJson(json: JSONObject?): VaultSettings = VaultSettings(
            lockTimeoutMs = json?.optLong("lockTimeoutMs", 300_000L) ?: 300_000L
        )
    }
}

data class TrashedVault(
    val vault: Vault,
    val deletedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("vault", vault.toJson())
        .put("deletedAt", deletedAt)

    companion object {
        fun fromJson(json: JSONObject): TrashedVault = TrashedVault(
            vault = Vault.fromJson(json.getJSONObject("vault")),
            deletedAt = json.optLong("deletedAt", System.currentTimeMillis())
        )
    }
}

data class TrashedSecret(
    val vaultId: String,
    val vaultName: String,
    val secret: Secret,
    val deletedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("vaultId", vaultId)
        .put("vaultName", vaultName)
        .put("secret", secret.toJson())
        .put("deletedAt", deletedAt)

    companion object {
        fun fromJson(json: JSONObject): TrashedSecret = TrashedSecret(
            vaultId = json.optString("vaultId"),
            vaultName = json.optString("vaultName"),
            secret = Secret.fromJson(json.getJSONObject("secret")),
            deletedAt = json.optLong("deletedAt", System.currentTimeMillis())
        )
    }
}

data class VaultData(
    val version: Int = 1,
    val settings: VaultSettings = VaultSettings(),
    val vaults: List<Vault> = emptyList(),
    val trashedVaults: List<TrashedVault> = emptyList(),
    val trashedSecrets: List<TrashedSecret> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject()
        .put("version", version)
        .put("settings", settings.toJson())
        .put("vaults", JSONArray().also { array ->
            vaults.forEach { array.put(it.toJson()) }
        })
        .put("trashedVaults", JSONArray().also { array ->
            trashedVaults.forEach { array.put(it.toJson()) }
        })
        .put("trashedSecrets", JSONArray().also { array ->
            trashedSecrets.forEach { array.put(it.toJson()) }
        })

    companion object {
        fun empty(): VaultData = VaultData()

        fun fromJson(json: JSONObject): VaultData {
            val vaultsJson = json.optJSONArray("vaults") ?: JSONArray()
            val vaults = buildList {
                for (index in 0 until vaultsJson.length()) {
                    add(Vault.fromJson(vaultsJson.getJSONObject(index)))
                }
            }
            val trashedVaultsJson = json.optJSONArray("trashedVaults") ?: JSONArray()
            val trashedVaults = buildList {
                for (index in 0 until trashedVaultsJson.length()) {
                    add(TrashedVault.fromJson(trashedVaultsJson.getJSONObject(index)))
                }
            }
            val trashedSecretsJson = json.optJSONArray("trashedSecrets") ?: JSONArray()
            val trashedSecrets = buildList {
                for (index in 0 until trashedSecretsJson.length()) {
                    add(TrashedSecret.fromJson(trashedSecretsJson.getJSONObject(index)))
                }
            }
            return VaultData(
                version = json.optInt("version", 1),
                settings = VaultSettings.fromJson(json.optJSONObject("settings")),
                vaults = vaults,
                trashedVaults = trashedVaults,
                trashedSecrets = trashedSecrets
            )
        }
    }
}
