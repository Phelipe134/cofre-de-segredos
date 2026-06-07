package com.cofredesegredos.domain

import com.cofredesegredos.models.VaultData
import com.cofredesegredos.storage.BackupPayload
import com.cofredesegredos.storage.VaultRepository
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultServiceTest {
    @Test
    fun editVaultPreservesIdAndSecrets() {
        val service = VaultService(FakeVaultRepository())
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        val vault = service.createVault("Banco")
        val secret = service.createSecret(vault.id, "Cartao", "user", "pass", "nota", "PIN", "1234")

        val edited = service.updateVault(vault.id, "Banco Principal", "contas e cartoes")

        assertEquals(vault.id, edited.id)
        assertEquals("Banco Principal", edited.name)
        assertEquals("contas e cartoes", edited.description)
        assertEquals(secret.id, service.findVault(vault.id)!!.secrets.single().id)
    }

    @Test
    fun editSecretDoesNotCreateDuplicate() {
        val service = VaultService(FakeVaultRepository())
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        val vault = service.createVault("Trabalho")
        val secret = service.createSecret(vault.id, "Email", "a@b.com", "old", "antiga", "token", "111")

        val edited = service.updateSecret(vault.id, secret.id, "Email Corp", "corp@b.com", "new", "nova", "2FA", "222")
        val secrets = service.findVault(vault.id)!!.secrets

        assertEquals(secret.id, edited.id)
        assertEquals(1, secrets.size)
        assertEquals("Email Corp", secrets.single().title)
        assertEquals("corp@b.com", secrets.single().login)
        assertEquals("nova", secrets.single().notes)
        assertEquals("222", secrets.single().customValue)
    }

    @Test
    fun searchFindsTitleLoginNotesAndCustomField() {
        val service = VaultService(FakeVaultRepository())
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        val vault = service.createVault("Social")
        service.createSecret(vault.id, "Instagram", "perfil123", "pass", "rede principal", "backup", "codigo azul")

        assertEquals(1, service.searchSecrets(vault.id, "insta").size)
        assertEquals(1, service.searchSecrets(vault.id, "perfil123").size)
        assertEquals(1, service.searchSecrets(vault.id, "principal").size)
        assertEquals(1, service.searchSecrets(vault.id, "azul").size)
    }

    @Test
    fun oldVaultJsonWithoutDescriptionRemainsCompatible() {
        val oldVaultJson = JSONObject()
            .put("id", "legacy-id")
            .put("name", "Legado")
            .put("createdAt", 1L)
            .put("secrets", org.json.JSONArray())

        val data = VaultData.fromJson(
            JSONObject()
                .put("version", 1)
                .put("settings", JSONObject().put("lockTimeoutMs", 300000L))
                .put("vaults", org.json.JSONArray().put(oldVaultJson))
        )

        assertEquals("legacy-id", data.vaults.single().id)
        assertEquals("", data.vaults.single().description)
        assertTrue(data.vaults.single().secrets.isEmpty())
        assertTrue(data.trashedVaults.isEmpty())
        assertTrue(data.trashedSecrets.isEmpty())
    }

    @Test
    fun deletedVaultMovesToTrashAndCanBeRestored() {
        val service = VaultService(FakeVaultRepository())
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        val vault = service.createVault("Banco", iconKey = "bank", colorKey = "amber")
        service.createSecret(vault.id, "Conta", "login", "senha", "", "", "")

        service.deleteVault(vault.id)

        assertTrue(service.vaults().isEmpty())
        assertEquals(vault.id, service.trashedVaults().single().vault.id)
        service.restoreVault(vault.id)
        assertEquals("amber", service.findVault(vault.id)!!.colorKey)
        assertEquals(1, service.findVault(vault.id)!!.secrets.size)
        assertTrue(service.trashedVaults().isEmpty())
    }

    @Test
    fun deletedSecretCanBeRestoredOrPermanentlyRemoved() {
        val service = VaultService(FakeVaultRepository())
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        val vault = service.createVault("Trabalho")
        val secret = service.createSecret(vault.id, "Email", "login", "senha", "", "", "")

        service.deleteSecret(vault.id, secret.id)

        assertTrue(service.findVault(vault.id)!!.secrets.isEmpty())
        assertEquals(secret.id, service.trashedSecrets().single().secret.id)
        service.restoreSecret(secret.id)
        assertEquals(secret.id, service.findVault(vault.id)!!.secrets.single().id)

        service.deleteSecret(vault.id, secret.id)
        service.permanentlyDeleteSecret(secret.id)
        assertTrue(service.trashedSecrets().isEmpty())
    }

    @Test
    fun backupPayloadRoundTripPreservesEncryptedEnvelope() {
        val payload = BackupPayload(
            cryptoVersion = 1,
            kdf = "PBKDF2-HMAC-SHA256",
            salt = "salt-base64",
            verifier = JSONObject().put("iv", "iv1").put("ciphertext", "cipher1").toString(),
            vaultData = JSONObject().put("iv", "iv2").put("ciphertext", "cipher2").toString(),
            exportedAt = 123L
        )

        val restored = BackupPayload.fromJsonString(payload.toJsonString())

        assertEquals("salt-base64", restored.salt)
        assertEquals("cipher1", JSONObject(restored.verifier).getString("ciphertext"))
        assertEquals("cipher2", JSONObject(restored.vaultData).getString("ciphertext"))
        assertEquals(1, restored.backupVersion)
    }

    @Test
    fun importBackupReplacesCurrentDataAndLocksSession() {
        val repository = FakeVaultRepository()
        val service = VaultService(repository)
        service.createMasterPassword("SenhaMestre123", "SenhaMestre123")
        service.createVault("Antes")

        service.importBackup("backup")

        assertEquals(null, service.currentData())
        assertTrue(repository.imported)
    }

    private class FakeVaultRepository : VaultRepository {
        private var initialized = false
        private var data = VaultData.empty()
        var imported = false

        override fun isInitialized(): Boolean = initialized

        override fun createStore(password: String): VaultData {
            initialized = true
            data = VaultData.empty()
            return data
        }

        override fun unlock(password: String): VaultData = data

        override fun save(data: VaultData) {
            this.data = data
        }

        override fun exportBackup(): String = data.toJson().toString()

        override fun importBackup(rawBackup: String) {
            data = VaultData.empty()
            initialized = true
            imported = true
        }

        override fun clearSession() = Unit
    }
}
