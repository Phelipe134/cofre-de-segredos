package com.cofredesegredos.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.util.Arrays

class UsbPasswordTransmitter(context: Context) {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var permissionReceiver: BroadcastReceiver? = null

    fun sendPassword(password: String, callback: (UsbSendResult) -> Unit) {
        sendValues(listOf(password), callback)
    }

    fun sendValues(values: List<String>, callback: (UsbSendResult) -> Unit) {
        val nonEmptyValues = values.filter { it.isNotBlank() }
        if (nonEmptyValues.isEmpty()) {
            callback(UsbSendResult(false, "Nao ha valores preenchidos para enviar."))
            return
        }

        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            callback(UsbSendResult(false, "Este Android nao informa suporte a USB Host/OTG."))
            return
        }

        if (usbManager.deviceList.isEmpty()) {
            callback(UsbSendResult(false, "CP2102 nao encontrado. Verifique o cabo USB OTG e a conexao."))
            return
        }

        val unsupportedCharacter = nonEmptyValues
            .asSequence()
            .flatMap { it.asSequence() }
            .firstOrNull { hidKey(it) == null }
        if (unsupportedCharacter != null) {
            callback(
                UsbSendResult(
                    false,
                    "O caractere '$unsupportedCharacter' nao e suportado pelo protocolo HID atual."
                )
            )
            return
        }

        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        if (driver == null) {
            callback(UsbSendResult(false, "Nenhuma porta serial compativel com o CP2102 foi encontrada."))
            return
        }

        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            requestPermission(device) { granted ->
                if (granted) {
                    writeValues(driver, nonEmptyValues, callback)
                } else {
                    callback(UsbSendResult(false, "Permissao USB negada. Nenhum dado foi enviado."))
                }
            }
            return
        }

        writeValues(driver, nonEmptyValues, callback)
    }

    fun release() {
        unregisterPermissionReceiver()
    }

    private fun writeValues(
        driver: UsbSerialDriver,
        values: List<String>,
        callback: (UsbSendResult) -> Unit
    ) {
        Thread {
            val result = try {
                val connection = usbManager.openDevice(driver.device)
                    ?: error("Falha ao abrir o dispositivo USB.")
                val port = driver.ports.firstOrNull()
                    ?: error("Dispositivo USB sem porta serial disponivel.")

                try {
                    port.open(connection)
                    port.setParameters(
                        BAUD_RATE,
                        DATA_BITS,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )

                    values.forEachIndexed { valueIndex, value ->
                        value.forEach { character ->
                            sendCharacter(port, character)
                            Thread.sleep(KEY_DELAY_MS)
                        }
                        if (valueIndex < values.lastIndex) {
                            Thread.sleep(FIELD_DELAY_MS)
                        }
                    }

                    Thread.sleep(PORT_DRAIN_DELAY_MS)
                    UsbSendResult(
                        true,
                        if (values.size == 1) "Campo enviado para o PC." else "Campos enviados para o PC."
                    )
                } finally {
                    try {
                        port.close()
                    } catch (_: Exception) {
                    }
                    connection.close()
                }
            } catch (error: Exception) {
                UsbSendResult(
                    false,
                    "Falha ao transmitir para o PC: ${error.message ?: "erro desconhecido"}."
                )
            }
            postResult(result, callback)
        }.start()
    }

    private fun sendCharacter(port: UsbSerialPort, character: Char) {
        val key = hidKey(character)
            ?: error("Caractere '$character' nao suportado pelo protocolo HID.")

        if (key.requiresShift) {
            writePacket(port, TYPE_MODIFIER, MODIFIER_LEFT_SHIFT)
        }
        writePacket(port, TYPE_KEY_DOWN, key.keycode)
        writePacket(port, TYPE_KEY_UP, key.keycode)
        if (key.requiresShift) {
            writePacket(port, TYPE_MODIFIER, MODIFIER_NONE)
        }
    }

    private fun writePacket(port: UsbSerialPort, type: Int, payload: Int) {
        val packet = byteArrayOf(
            PACKET_START.toByte(),
            type.toByte(),
            payload.toByte(),
            (PACKET_START xor type xor payload).toByte()
        )
        try {
            port.write(packet, WRITE_TIMEOUT_MS)
        } finally {
            Arrays.fill(packet, 0)
        }
    }

    private fun hidKey(character: Char): HidKey? = when (character) {
        in 'a'..'z' -> HidKey(HID_A + (character - 'a'))
        in 'A'..'Z' -> HidKey(HID_A + (character - 'A'), requiresShift = true)
        in '1'..'9' -> HidKey(HID_1 + (character - '1'))
        '0' -> HidKey(HID_0)
        '!' -> HidKey(0x1E, requiresShift = true)
        '@' -> HidKey(0x1F, requiresShift = true)
        '#' -> HidKey(0x20, requiresShift = true)
        '$' -> HidKey(0x21, requiresShift = true)
        '%' -> HidKey(0x22, requiresShift = true)
        '^' -> HidKey(0x23, requiresShift = true)
        '&' -> HidKey(0x24, requiresShift = true)
        '*' -> HidKey(0x25, requiresShift = true)
        '(' -> HidKey(0x26, requiresShift = true)
        ')' -> HidKey(0x27, requiresShift = true)
        '-' -> HidKey(0x2D)
        '_' -> HidKey(0x2D, requiresShift = true)
        '=' -> HidKey(0x2E)
        '+' -> HidKey(0x2E, requiresShift = true)
        '[' -> HidKey(0x2F)
        '{' -> HidKey(0x2F, requiresShift = true)
        ']' -> HidKey(0x30)
        '}' -> HidKey(0x30, requiresShift = true)
        '\\' -> HidKey(0x31)
        '|' -> HidKey(0x31, requiresShift = true)
        ';' -> HidKey(0x33)
        ':' -> HidKey(0x33, requiresShift = true)
        '\'' -> HidKey(0x34)
        '"' -> HidKey(0x34, requiresShift = true)
        '`' -> HidKey(0x35)
        '~' -> HidKey(0x35, requiresShift = true)
        ',' -> HidKey(0x36)
        '<' -> HidKey(0x36, requiresShift = true)
        '.' -> HidKey(0x37)
        '>' -> HidKey(0x37, requiresShift = true)
        '/' -> HidKey(0x38)
        '?' -> HidKey(0x38, requiresShift = true)
        ' ' -> HidKey(HID_SPACE)
        '\n', '\r' -> HidKey(HID_ENTER)
        else -> null
    }

    private data class HidKey(
        val keycode: Int,
        val requiresShift: Boolean = false
    )

    private fun postResult(result: UsbSendResult, callback: (UsbSendResult) -> Unit) {
        mainHandler.post { callback(result) }
    }

    private fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        unregisterPermissionReceiver()
        val action = "${appContext.packageName}.USB_PASSWORD_PERMISSION"
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != action) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                unregisterPermissionReceiver()
                callback(granted)
            }
        }

        val filter = IntentFilter(action)
        val receiver = permissionReceiver ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val permissionIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(action).setPackage(appContext.packageName),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun unregisterPermissionReceiver() {
        val receiver = permissionReceiver ?: return
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        } finally {
            permissionReceiver = null
        }
    }

    companion object {
        private const val BAUD_RATE = 115_200
        private const val DATA_BITS = 8
        private const val WRITE_TIMEOUT_MS = 2_000
        private const val KEY_DELAY_MS = 20L
        private const val FIELD_DELAY_MS = 300L
        private const val PORT_DRAIN_DELAY_MS = 200L

        private const val PACKET_START = 0xAA
        private const val TYPE_KEY_DOWN = 0x01
        private const val TYPE_KEY_UP = 0x02
        private const val TYPE_MODIFIER = 0x03
        private const val MODIFIER_NONE = 0x00
        private const val MODIFIER_LEFT_SHIFT = 0x02

        private const val HID_A = 0x04
        private const val HID_1 = 0x1E
        private const val HID_0 = 0x27
        private const val HID_ENTER = 0x28
        private const val HID_SPACE = 0x2C
    }
}

data class UsbSendResult(
    val success: Boolean,
    val message: String
)
