package com.cofredesegredos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cofredesegredos.domain.VaultService
import com.cofredesegredos.storage.BackupPayload
import com.cofredesegredos.storage.VaultStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Sprint41BackupInstrumentedTest {
    @Before
    fun clearAppState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteSharedPreferences("cofre_segredos_secure_store")
        context.deleteSharedPreferences("cofre_segredos_biometric_store")
    }

    @Test
    fun exportImportBackupAndLockAfterImportWork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = VaultService(VaultStorage(context))
        service.createMasterPassword(PASSWORD, PASSWORD)
        val vault = service.createVault("Banco", "contas")
        service.createSecret(vault.id, "Cartao", "cliente", "senha", "nota", "pin", "1234")

        val backup = service.exportBackup()
        val payload = BackupPayload.fromJsonString(backup)
        assertEquals("cofre-de-segredos", payload.app)
        assertTrue(payload.vaultData.contains("ciphertext"))
        assertTrue(payload.verifier.contains("ciphertext"))

        service.importBackup(backup)
        assertEquals(null, service.currentData())

        val restored = service.unlock(PASSWORD)
        assertEquals("Banco", restored.vaults.single().name)
        assertEquals("Cartao", restored.vaults.single().secrets.single().title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBackupIsRejected() {
        BackupPayload.fromJsonString("""{"app":"outro","backupVersion":1}""")
    }

    companion object {
        private const val PASSWORD = "SenhaMestre123"
    }
}
