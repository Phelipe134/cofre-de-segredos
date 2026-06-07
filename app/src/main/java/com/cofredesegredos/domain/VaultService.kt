package com.cofredesegredos.domain

import com.cofredesegredos.models.Secret
import com.cofredesegredos.models.TrashedSecret
import com.cofredesegredos.models.TrashedVault
import com.cofredesegredos.models.Vault
import com.cofredesegredos.models.VaultData
import com.cofredesegredos.storage.VaultRepository

class VaultService(
    private val storage: VaultRepository
) {
    private var data: VaultData? = null

    fun isInitialized(): Boolean = storage.isInitialized()

    private var sessionMasterPassword: String? = null

    fun createMasterPassword(password: String, confirmation: String): VaultData {
        require(password.length >= 8) { "Use pelo menos 8 caracteres." }
        require(password == confirmation) { "As senhas nao conferem." }
        return storage.createStore(password).also {
            data = it
            sessionMasterPassword = password
        }
    }

    fun unlock(password: String): VaultData =
        storage.unlock(password).also {
            data = it
            sessionMasterPassword = password
        }

    fun lock() {
        data = null
        sessionMasterPassword = null
        storage.clearSession()
    }

    fun currentMasterPasswordForBiometricSetup(): String =
        sessionMasterPassword ?: error("Desbloqueie com a senha mestre antes de ativar biometria.")

    fun currentData(): VaultData? = data

    fun lockTimeoutMs(): Long = data?.settings?.lockTimeoutMs ?: 300_000L

    fun vaults(): List<Vault> = requireData().vaults

    fun findVault(vaultId: String): Vault? = requireData().vaults.firstOrNull { it.id == vaultId }

    fun createVault(
        name: String,
        description: String = "",
        iconKey: String = "keys",
        colorKey: String = "blue"
    ): Vault {
        val cleanedName = name.trim()
        require(cleanedName.isNotBlank()) { "Informe o nome do cofre." }
        val vault = Vault(
            name = cleanedName,
            description = description.trim(),
            iconKey = iconKey,
            colorKey = colorKey
        )
        updateData(requireData().copy(vaults = requireData().vaults + vault))
        return vault
    }

    fun updateVault(
        vaultId: String,
        name: String,
        description: String,
        iconKey: String? = null,
        colorKey: String? = null
    ): Vault {
        val cleanedName = name.trim()
        require(cleanedName.isNotBlank()) { "Informe o nome do cofre." }
        var updatedVault: Vault? = null
        updateVault(vaultId) { vault ->
            vault.copy(
                name = cleanedName,
                description = description.trim(),
                iconKey = iconKey ?: vault.iconKey,
                colorKey = colorKey ?: vault.colorKey
            ).also {
                updatedVault = it
            }
        }
        return updatedVault ?: error("Cofre nao encontrado.")
    }

    fun deleteVault(vaultId: String) {
        val current = requireData()
        val vault = current.vaults.firstOrNull { it.id == vaultId }
            ?: error("Cofre nao encontrado.")
        updateData(
            current.copy(
                vaults = current.vaults.filterNot { it.id == vaultId },
                trashedVaults = current.trashedVaults + TrashedVault(vault)
            )
        )
    }

    fun createSecret(
        vaultId: String,
        title: String,
        login: String,
        password: String,
        notes: String,
        customLabel: String,
        customValue: String
    ): Secret {
        val cleanedTitle = title.trim()
        require(cleanedTitle.isNotBlank()) { "Informe o titulo do segredo." }
        val secret = Secret(
            title = cleanedTitle,
            login = login.trim(),
            password = password,
            notes = notes.trim(),
            customLabel = customLabel.trim(),
            customValue = customValue.trim()
        )
        updateVault(vaultId) { vault -> vault.copy(secrets = vault.secrets + secret) }
        return secret
    }

    fun updateSecret(
        vaultId: String,
        secretId: String,
        title: String,
        login: String,
        password: String,
        notes: String,
        customLabel: String,
        customValue: String
    ): Secret {
        val cleanedTitle = title.trim()
        require(cleanedTitle.isNotBlank()) { "Informe o titulo do segredo." }
        var updatedSecret: Secret? = null
        updateVault(vaultId) { vault ->
            vault.copy(secrets = vault.secrets.map { secret ->
                if (secret.id == secretId) {
                    secret.copy(
                        title = cleanedTitle,
                        login = login.trim(),
                        password = password,
                        notes = notes.trim(),
                        customLabel = customLabel.trim(),
                        customValue = customValue.trim()
                    ).also { updatedSecret = it }
                } else {
                    secret
                }
            })
        }
        return updatedSecret ?: error("Segredo nao encontrado.")
    }

    fun deleteSecret(vaultId: String, secretId: String) {
        val current = requireData()
        val vault = current.vaults.firstOrNull { it.id == vaultId }
            ?: error("Cofre nao encontrado.")
        val secret = vault.secrets.firstOrNull { it.id == secretId }
            ?: error("Segredo nao encontrado.")
        updateData(
            current.copy(
                vaults = current.vaults.map {
                    if (it.id == vaultId) it.copy(secrets = it.secrets.filterNot { item -> item.id == secretId })
                    else it
                },
                trashedSecrets = current.trashedSecrets + TrashedSecret(vault.id, vault.name, secret)
            )
        )
    }

    fun trashedVaults(): List<TrashedVault> {
        purgeExpiredTrash()
        return requireData().trashedVaults
    }

    fun trashedSecrets(): List<TrashedSecret> {
        purgeExpiredTrash()
        return requireData().trashedSecrets
    }

    fun restoreVault(vaultId: String) {
        val current = requireData()
        val item = current.trashedVaults.firstOrNull { it.vault.id == vaultId }
            ?: error("Cofre excluido nao encontrado.")
        updateData(
            current.copy(
                vaults = current.vaults + item.vault,
                trashedVaults = current.trashedVaults.filterNot { it.vault.id == vaultId }
            )
        )
    }

    fun permanentlyDeleteVault(vaultId: String) {
        val current = requireData()
        updateData(current.copy(trashedVaults = current.trashedVaults.filterNot { it.vault.id == vaultId }))
    }

    fun restoreSecret(secretId: String) {
        val current = requireData()
        val item = current.trashedSecrets.firstOrNull { it.secret.id == secretId }
            ?: error("Segredo excluido nao encontrado.")
        val targetExists = current.vaults.any { it.id == item.vaultId }
        val restoredVaults = if (targetExists) {
            current.vaults.map { vault ->
                if (vault.id == item.vaultId) vault.copy(secrets = vault.secrets + item.secret) else vault
            }
        } else {
            current.vaults + Vault(
                id = item.vaultId,
                name = item.vaultName.ifBlank { "Recuperados" },
                secrets = listOf(item.secret)
            )
        }
        updateData(
            current.copy(
                vaults = restoredVaults,
                trashedSecrets = current.trashedSecrets.filterNot { it.secret.id == secretId }
            )
        )
    }

    fun permanentlyDeleteSecret(secretId: String) {
        val current = requireData()
        updateData(current.copy(trashedSecrets = current.trashedSecrets.filterNot { it.secret.id == secretId }))
    }

    fun searchSecrets(vaultId: String, query: String): List<Secret> {
        val vault = findVault(vaultId) ?: return emptyList()
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return vault.secrets
        return vault.secrets.filter { secret ->
            listOf(
                secret.title,
                secret.login,
                secret.notes,
                secret.customLabel,
                secret.customValue
            ).any { value -> value.lowercase().contains(normalizedQuery) }
        }
    }

    fun exportBackup(): String = storage.exportBackup()

    fun importBackup(rawBackup: String) {
        storage.importBackup(rawBackup)
        data = null
        sessionMasterPassword = null
    }

    private fun updateVault(vaultId: String, transform: (Vault) -> Vault) {
        val current = requireData()
        val updatedVaults = current.vaults.map { vault ->
            if (vault.id == vaultId) transform(vault) else vault
        }
        require(updatedVaults.any { it.id == vaultId }) { "Cofre nao encontrado." }
        updateData(current.copy(vaults = updatedVaults))
    }

    private fun updateData(updated: VaultData) {
        data = updated
        storage.save(updated)
    }

    private fun purgeExpiredTrash() {
        val current = requireData()
        val cutoff = System.currentTimeMillis() - TRASH_RETENTION_MS
        val activeVaults = current.trashedVaults.filter { it.deletedAt >= cutoff }
        val activeSecrets = current.trashedSecrets.filter { it.deletedAt >= cutoff }
        if (activeVaults.size != current.trashedVaults.size ||
            activeSecrets.size != current.trashedSecrets.size
        ) {
            updateData(current.copy(trashedVaults = activeVaults, trashedSecrets = activeSecrets))
        }
    }

    private fun requireData(): VaultData = data ?: error("Cofre bloqueado.")

    companion object {
        private const val TRASH_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
