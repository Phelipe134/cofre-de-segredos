package com.example.cp2102serialproof

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Arrays

class MainActivity : Activity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var usbManager: UsbManager
    private lateinit var status: TextView
    private lateinit var payloadInput: EditText
    private var permissionReceiver: BroadcastReceiver? = null
    private var driver: UsbSerialDriver? = null
    private var port: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null
    private var baudRate = 115_200
    private var lastBytes = 0
    private var lastError = "nenhum"
    private var lastAction = "App iniciado."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        setContentView(buildUi())
        updateStatus("App iniciado.")
    }

    override fun onDestroy() {
        closePortInternal()
        unregisterPermissionReceiver()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "CP2102 Serial Proof"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(text("App separado: detectar CP2102, abrir serial e enviar texto falso."))

        payloadInput = EditText(this).apply {
            setText("TESTE_COFRE_123")
            hint = "Texto para enviar"
            setSingleLine(false)
            minLines = 2
        }
        root.addView(payloadInput, fullWidth())

        val baudRow = row()
        baudRow.addView(button("9600") { changeBaudRate(9_600) }, weight())
        baudRow.addView(button("19200") { changeBaudRate(19_200) }, weight())
        baudRow.addView(button("38400") { changeBaudRate(38_400) }, weight())
        root.addView(baudRow)

        val baudRow2 = row()
        baudRow2.addView(button("57600") { changeBaudRate(57_600) }, weight())
        baudRow2.addView(button("115200") { changeBaudRate(115_200) }, weight())
        baudRow2.addView(button("230400") { changeBaudRate(230_400) }, weight())
        root.addView(baudRow2)

        root.addView(button("Atualizar") { refresh() }, fullWidth())
        root.addView(button("Solicitar permissao") { requestPermissionForSelected() }, fullWidth())
        root.addView(button("Abrir porta") { openPort() }, fullWidth())
        root.addView(button("Enviar texto") { send(commandPacket(0x01, payloadInput.text.toString()), "AA 01 + tamanho + texto") }, fullWidth())
        root.addView(button("Cmd 1 (digitar)") { send(commandPacket(0x01, payloadInput.text.toString()), "AA 01 + tamanho + texto") }, fullWidth())
        root.addView(button("Cmd 2") { send(commandPacket(0x02, payloadInput.text.toString()), "AA 02 + tamanho + texto") }, fullWidth())
        root.addView(button("Cmd 3") { send(commandPacket(0x03, payloadInput.text.toString()), "AA 03 + tamanho + texto") }, fullWidth())
        root.addView(button("Cmd 0x00") { send(protocolPayload(0x00, payloadInput.text.toString()), "AA FF 00 + texto + LF") }, fullWidth())
        root.addView(button("Cmd 0x01") { send(protocolPayload(0x01, payloadInput.text.toString()), "AA FF 01 + texto + LF") }, fullWidth())
        root.addView(button("Cmd 0x02") { send(protocolPayload(0x02, payloadInput.text.toString()), "AA FF 02 + texto + LF") }, fullWidth())
        root.addView(button("Enviar com Enter") { send("${payloadInput.text}\r\n".toByteArray(Charsets.UTF_8), "texto + CRLF") }, fullWidth())
        root.addView(button("Enviar lento") { sendSlow(payloadInput.text.toString()) }, fullWidth())
        root.addView(button("Brute Force") { bruteForceAfterAA() }, fullWidth())
        root.addView(button("Fechar porta") { closePort(); updateStatus("Porta fechada.") }, fullWidth())

        status = text("").apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(status)

        return ScrollView(this).apply { addView(root) }
    }

    private fun refresh() {
        selectDriver()
        updateStatus("Dispositivos atualizados.")
    }

    private fun requestPermissionForSelected() {
        val device = selectedDevice()
        if (device == null) {
            updateStatus("Nenhum dispositivo USB encontrado.")
            return
        }
        if (usbManager.hasPermission(device)) {
            updateStatus("Permissao USB ja concedida.")
            return
        }
        requestPermission(device) { granted ->
            updateStatus(if (granted) "Permissao USB concedida." else "Permissao USB negada.")
        }
    }

    private fun changeBaudRate(value: Int) {
        val wasOpen = port != null
        baudRate = value
        if (wasOpen) {
            updateStatus("Baud $baudRate selecionado. Reabrindo porta...")
            openPort()
        } else {
            updateStatus("Baud $baudRate selecionado. Porta fechada.")
        }
    }

    private fun protocolPayload(command: Int, text: String): ByteArray =
        byteArrayOf(0xAA.toByte(), 0xFF.toByte(), command.toByte()) +
            text.toByteArray(Charsets.UTF_8) +
            byteArrayOf(0x0A)

    private fun commandPacket(command: Int, text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8)
        return byteArrayOf(0xAA.toByte(), command.toByte(), payload.size.toByte()) + payload
    }

    private fun buildPacket(text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8)
        val checksum = payload.fold(0.toByte()) { acc, b ->
            (acc.toInt() xor b.toInt()).toByte()
        }
        return byteArrayOf(0xAA.toByte(), payload.size.toByte()) + payload + byteArrayOf(checksum)
    }

    private fun openPort() {
        Thread {
            try {
                closePortInternal()
                val selectedDriver = selectDriver()
                    ?: return@Thread postStatus("Nenhum driver serial encontrado.")
                val device = selectedDriver.device
                if (!usbManager.hasPermission(device)) {
                    return@Thread postStatus("Permissao USB ausente.")
                }
                val opened = usbManager.openDevice(device)
                    ?: return@Thread postStatus("Falha ao abrir dispositivo USB.")
                val selectedPort = selectedDriver.ports.firstOrNull()
                    ?: return@Thread postStatus("Driver sem porta serial.")
                selectedPort.open(opened)
                selectedPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                connection = opened
                port = selectedPort
                lastError = "nenhum"
                postStatus("Porta aberta.")
            } catch (error: Exception) {
                closePortInternal()
                lastError = error.fullStackTrace()
                postStatus("Erro ao abrir porta.")
            }
        }.start()
    }

    private fun send(bytes: ByteArray, label: String) {
        Thread {
            val payload = bytes.copyOf()
            try {
                val selectedPort = port
                    ?: return@Thread postStatus("Porta nao esta aberta.")
                selectedPort.write(payload, WRITE_TIMEOUT_MS)
                lastBytes = payload.size
                lastError = "nenhum"
                postStatus("Payload enviado: $label.")
            } catch (error: Exception) {
                lastBytes = 0
                lastError = error.fullStackTrace()
                postStatus("Erro ao enviar payload.")
            } finally {
                Arrays.fill(payload, 0)
            }
        }.start()
    }

    private fun sendSlow(text: String) {
        Thread {
            val payload = text.toByteArray(Charsets.UTF_8)
            try {
                val selectedPort = port
                    ?: return@Thread postStatus("Porta nao esta aberta.")
                payload.forEach {
                    selectedPort.write(byteArrayOf(it), WRITE_TIMEOUT_MS)
                    Thread.sleep(80)
                }
                lastBytes = payload.size
                lastError = "nenhum"
                postStatus("Payload enviado lentamente.")
            } catch (error: Exception) {
                lastBytes = 0
                lastError = error.fullStackTrace()
                postStatus("Erro ao enviar lento.")
            } finally {
                Arrays.fill(payload, 0)
            }
        }.start()
    }

    private fun bruteForceAfterAA() {
        Thread {
            try {
                baudRate = 9_600
                postStatus("Brute Force: abrindo porta em 9600.")
                closePortInternal()
                val selectedDriver = selectDriver()
                    ?: return@Thread postStatus("Nenhum driver serial encontrado.")
                val device = selectedDriver.device
                if (!usbManager.hasPermission(device)) {
                    return@Thread postStatus("Permissao USB ausente.")
                }
                val opened = usbManager.openDevice(device)
                    ?: return@Thread postStatus("Falha ao abrir dispositivo USB.")
                val selectedPort = selectedDriver.ports.firstOrNull()
                    ?: return@Thread postStatus("Driver sem porta serial.")
                selectedPort.open(opened)
                selectedPort.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                connection = opened
                port = selectedPort

                val packet1 = commandPacket(0x01, "A")
                selectedPort.write(packet1, WRITE_TIMEOUT_MS)
                lastBytes = packet1.size
                lastError = "nenhum"
                postStatus("Enviando pacote: AA 01 01 41")
                Thread.sleep(500)

                val packet2 = commandPacket(0x02, "A")
                selectedPort.write(packet2, WRITE_TIMEOUT_MS)
                lastBytes += packet2.size
                lastError = "nenhum"
                postStatus("Enviando pacote: AA 02 01 41")
                Thread.sleep(500)

                val packet3 = commandPacket(0x03, "A")
                selectedPort.write(packet3, WRITE_TIMEOUT_MS)
                lastBytes += packet3.size
                lastError = "nenhum"
                postStatus("Enviando pacote: AA 03 01 41")
                postStatus("Brute Force concluido.")
            } catch (error: Exception) {
                lastBytes = 0
                lastError = error.fullStackTrace()
                postStatus("Erro no Brute Force.")
            }
        }.start()
    }

    private fun closePort() {
        closePortInternal()
    }

    private fun closePortInternal() {
        try {
            port?.close()
        } catch (_: Exception) {
        } finally {
            port = null
        }
        try {
            connection?.close()
        } catch (_: Exception) {
        } finally {
            connection = null
        }
    }

    private fun selectDriver(): UsbSerialDriver? {
        driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        return driver
    }

    private fun selectedDevice(): UsbDevice? =
        driver?.device ?: selectDriver()?.device ?: usbManager.deviceList.values.firstOrNull()

    private fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        unregisterPermissionReceiver()
        val action = "$packageName.USB_PERMISSION"
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
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = PendingIntent.getBroadcast(this, 0, Intent(action).setPackage(packageName), flags)
        usbManager.requestPermission(device, intent)
    }

    private fun unregisterPermissionReceiver() {
        val receiver = permissionReceiver ?: return
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        } finally {
            permissionReceiver = null
        }
    }

    private fun updateStatus(action: String) {
        lastAction = action
        status.text = report()
    }

    private fun postStatus(action: String) {
        mainHandler.post {
            Toast.makeText(this, action, Toast.LENGTH_SHORT).show()
            updateStatus(action)
        }
    }

    private fun report(): String {
        val devices = usbManager.deviceList.values.toList()
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val selected = driver
        val device = selected?.device ?: devices.firstOrNull()
        return buildString {
            appendLine("Ultima acao: $lastAction")
            appendLine("USB Host: ${packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}")
            appendLine("Dispositivos USB: ${devices.size}")
            devices.forEachIndexed { index, usbDevice ->
                appendLine("${index + 1}. ${usbDevice.deviceName}")
                appendLine("   product: ${usbDevice.productName ?: "indisponivel"}")
                appendLine("   manufacturer: ${usbDevice.manufacturerName ?: "indisponivel"}")
                appendLine("   VID/PID: ${hex(usbDevice.vendorId)} / ${hex(usbDevice.productId)}")
            }
            appendLine("Drivers seriais: ${drivers.size}")
            drivers.forEachIndexed { index, serialDriver ->
                appendLine("${index + 1}. ${serialDriver.javaClass.simpleName} portas=${serialDriver.ports.size}")
            }
            appendLine("Driver selecionado: ${selected?.javaClass?.simpleName ?: "nenhum"}")
            appendLine("Permissao: ${if (device != null && usbManager.hasPermission(device)) "concedida" else "ausente"}")
            appendLine("Porta aberta: ${port != null}")
            appendLine("Baud: $baudRate")
            appendLine("Ultimos bytes enviados: $lastBytes")
            appendLine("Erro:")
            appendLine(lastError)
        }
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun text(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 15f
    }

    private fun fullWidth(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hex(value: Int): String = "0x${value.toString(16).uppercase().padStart(4, '0')}"

    private fun Exception.fullStackTrace(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString().trim()
    }

    companion object {
        private const val WRITE_TIMEOUT_MS = 2_000
    }
}
