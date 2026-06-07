package com.cofredesegredos.sync

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BluetoothSyncServer(
    context: Context,
    private val exportBackup: () -> String,
    private val importBackup: (String) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onDiagnostics: (BluetoothSyncDiagnostics) -> Unit
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter
    private var server: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var sessionNonce = ""
    private var pairingCode = ""
    private var authenticated = false
    private var incoming = ByteArrayOutputStream()
    private var expectedIncomingBytes = 0
    private var authenticationResult = "AUTH_REQUIRED"
    private var diagnostics = BluetoothSyncDiagnostics()

    fun pairingCode(): String = pairingCode

    @SuppressLint("MissingPermission")
    fun refreshDiagnostics(permissionsGranted: Boolean) {
        runCatching {
            val bluetoothAdapter = adapter
            updateDiagnostics(
                bluetoothEnabled = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false),
                permissionsGranted = permissionsGranted,
                advertisingSupported = runCatching {
                    bluetoothAdapter?.isMultipleAdvertisementSupported == true &&
                        bluetoothAdapter.bluetoothLeAdvertiser != null
                }.getOrDefault(false)
            )
        }.onFailure {
            Log.e(TAG, "Falha ao consultar diagnóstico BLE", it)
            fail("Falha ao consultar Bluetooth: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    @SuppressLint("MissingPermission")
    fun start(): String {
        return try {
            stop(updateStatus = false)
            Log.i(TAG, "Iniciando modo de sincronização BLE")
            val bluetoothAdapter = adapter ?: return fail("Bluetooth não está disponível neste aparelho.")
            if (!bluetoothAdapter.isEnabled) return fail("Bluetooth está desligado.")
            if (!diagnostics.permissionsGranted) return fail("Permissões Bluetooth não concedidas.")
            if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
                return fail("Este aparelho não suporta BLE advertising.")
            }
            if (bluetoothAdapter.bluetoothLeAdvertiser == null) {
                return fail("BluetoothLeAdvertiser indisponível neste aparelho.")
            }

            pairingCode = SecureRandom().nextInt(1_000_000).toString().padStart(6, '0')
            sessionNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
            authenticated = false
            authenticationResult = "AUTH_REQUIRED"
            incoming.reset()

            val bluetoothManager = manager
                ?: return fail("Serviço Bluetooth do Android indisponível.")
            server = bluetoothManager.openGattServer(appContext, callback)
            if (server == null) return fail("Falha ao criar o GATT Server.")
            Log.i(TAG, "GATT Server criado")
            updateDiagnostics(gattServerStarted = true, advertisingStarted = false, lastError = "")

            val control = BluetoothGattCharacteristic(
                BluetoothSyncProtocol.CONTROL_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val data = BluetoothGattCharacteristic(
                BluetoothSyncProtocol.DATA_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            ).also {
                it.addDescriptor(
                    BluetoothGattDescriptor(
                        BluetoothSyncProtocol.CCC_UUID,
                        BluetoothGattDescriptor.PERMISSION_READ or
                            BluetoothGattDescriptor.PERMISSION_WRITE
                    )
                )
            }
            dataCharacteristic = data
            val service = BluetoothGattService(
                BluetoothSyncProtocol.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            ).apply {
                addCharacteristic(control)
                addCharacteristic(data)
            }
            if (server?.addService(service) != true) {
                return fail("Falha ao registrar o serviço GATT.")
            }
            Log.i(TAG, "Registro do serviço GATT solicitado")
            onStatus("Registrando serviço GATT...")
            pairingCode
        } catch (error: Exception) {
            Log.e(TAG, "Erro ao iniciar BLE", error)
            fail(error.message ?: error.javaClass.simpleName)
        }
    }

    @SuppressLint("MissingPermission")
    fun stop(updateStatus: Boolean = true) {
        Log.i(TAG, "Parando modo de sincronização BLE")
        runCatching { adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) }
            .onFailure { Log.e(TAG, "Erro ao parar advertising", it) }
        runCatching { server?.close() }
            .onFailure { Log.e(TAG, "Erro ao fechar GATT Server", it) }
        server = null
        connectedDevice = null
        dataCharacteristic = null
        authenticated = false
        authenticationResult = "AUTH_REQUIRED"
        pairingCode = ""
        sessionNonce = ""
        incoming.reset()
        updateDiagnostics(gattServerStarted = false, advertisingStarted = false)
        if (updateStatus) onStatus("Sincronização Bluetooth parada.")
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            fail("BluetoothLeAdvertiser ficou indisponível.")
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BluetoothSyncProtocol.SERVICE_UUID))
            .build()
        try {
            Log.i(TAG, "Chamando BluetoothLeAdvertiser.startAdvertising")
            advertiser.startAdvertising(settings, advertiseData, advertiseCallback)
            onStatus("Iniciando advertising BLE...")
        } catch (error: Exception) {
            Log.e(TAG, "Falha ao chamar startAdvertising", error)
            fail(error.message ?: "Falha ao iniciar advertising BLE.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendBackup() {
        val device = connectedDevice ?: error("PC não conectado.")
        val characteristic = dataCharacteristic ?: error("Canal de dados indisponível.")
        val bytes = exportBackup().toByteArray(Charsets.UTF_8)
        val chunks = bytes.toList().chunked(BluetoothSyncProtocol.CHUNK_SIZE)
        onStatus("Enviando backup criptografado (${bytes.size} bytes)...")
        Thread {
            try {
                chunks.forEachIndexed { index, values ->
                    val header = byteArrayOf(
                        ((index shr 8) and 0xff).toByte(),
                        (index and 0xff).toByte(),
                        ((chunks.size shr 8) and 0xff).toByte(),
                        (chunks.size and 0xff).toByte()
                    )
                    notify(device, characteristic, header + values.toByteArray())
                    Thread.sleep(35)
                }
                notify(device, characteristic, byteArrayOf(0x7f))
                onStatus("Backup enviado ao PC.")
            } catch (error: Exception) {
                Log.e(TAG, "Erro no envio do backup", error)
                fail(error.message ?: "Erro no envio do backup.")
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server?.notifyCharacteristicChanged(device, characteristic, false, value)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            server?.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "Advertising BLE iniciado com sucesso")
            updateDiagnostics(advertisingStarted = true, lastError = "")
            onStatus("Aguardando PC. Código: $pairingCode")
        }

        override fun onStartFailure(errorCode: Int) {
            val message = "Falha no advertising BLE: ${advertiseError(errorCode)} ($errorCode)"
            Log.e(TAG, message)
            updateDiagnostics(advertisingStarted = false, lastError = message)
            onStatus(message)
        }
    }

    private val callback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Serviço GATT registrado: ${service?.uuid}")
                startAdvertising()
            } else {
                fail("Falha ao adicionar serviço GATT: status $status")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.i(TAG, "Conexão GATT: status=$status estado=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                onStatus("PC conectado. Aguardando autenticação.")
            } else {
                connectedDevice = null
                authenticated = false
                onStatus("PC desconectado. Advertising continua ativo.")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = if (characteristic.uuid == BluetoothSyncProtocol.CONTROL_UUID) {
                if (authenticationResult == "AUTH_REQUIRED") {
                    "HELLO:1:$sessionNonce".toByteArray()
                } else {
                    authenticationResult.toByteArray()
                }
            } else {
                byteArrayOf()
            }
            server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            try {
                if (characteristic.uuid == BluetoothSyncProtocol.CONTROL_UUID) {
                    handleControl(String(value, Charsets.UTF_8))
                } else if (characteristic.uuid == BluetoothSyncProtocol.DATA_UUID && authenticated) {
                    incoming.write(value)
                    if (expectedIncomingBytes > 0 && incoming.size() >= expectedIncomingBytes) {
                        importBackup(String(incoming.toByteArray(), Charsets.UTF_8))
                        incoming.reset()
                        expectedIncomingBytes = 0
                        onStatus("Backup recebido. O cofre foi bloqueado para validar a senha mestre.")
                    }
                }
                if (responseNeeded) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Erro de sincronização", error)
                fail(error.message ?: "Erro de sincronização.")
                if (responseNeeded) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (responseNeeded) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    private fun handleControl(command: String) {
        when {
            command.startsWith("AUTH:") -> {
                val received = command.removePrefix("AUTH:").trim().lowercase()
                val expected = hmac(pairingCode, sessionNonce)
                authenticated = MessageDigest.isEqual(
                    received.toByteArray(),
                    expected.toByteArray()
                )
                authenticationResult = if (authenticated) "AUTH_OK" else "AUTH_FAILED"
                onStatus(if (authenticated) "PC autenticado." else "Código de pareamento inválido.")
            }
            command == "PULL" && authenticated -> sendBackup()
            command.startsWith("PUSH:") && authenticated -> {
                expectedIncomingBytes = command.removePrefix("PUSH:").toInt()
                incoming.reset()
                onStatus("Recebendo backup criptografado do PC...")
            }
            else -> error("Comando Bluetooth inválido ou não autenticado.")
        }
    }

    private fun hmac(code: String, nonce: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(code.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(nonce.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun fail(message: String): String {
        Log.e(TAG, message)
        updateDiagnostics(lastError = message, advertisingStarted = false)
        onStatus(message)
        return ""
    }

    private fun updateDiagnostics(
        bluetoothEnabled: Boolean = diagnostics.bluetoothEnabled,
        permissionsGranted: Boolean = diagnostics.permissionsGranted,
        advertisingSupported: Boolean = diagnostics.advertisingSupported,
        gattServerStarted: Boolean = diagnostics.gattServerStarted,
        advertisingStarted: Boolean = diagnostics.advertisingStarted,
        lastError: String = diagnostics.lastError
    ) {
        diagnostics = BluetoothSyncDiagnostics(
            bluetoothEnabled = bluetoothEnabled,
            permissionsGranted = permissionsGranted,
            advertisingSupported = advertisingSupported,
            gattServerStarted = gattServerStarted,
            advertisingStarted = advertisingStarted,
            lastError = lastError
        )
        runCatching { onDiagnostics(diagnostics) }
            .onFailure { Log.e(TAG, "Falha ao publicar diagnóstico na UI", it) }
        Log.d(TAG, "Diagnóstico BLE: $diagnostics")
    }

    private fun advertiseError(code: Int): String = when (code) {
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "dados do anúncio grandes demais"
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "limite de anunciantes atingido"
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "advertising já iniciado"
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "erro interno do Bluetooth"
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "recurso não suportado"
        else -> "erro desconhecido"
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "CofreBleSync"
    }
}
