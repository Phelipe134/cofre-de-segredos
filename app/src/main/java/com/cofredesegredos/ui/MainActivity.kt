package com.cofredesegredos.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.cofredesegredos.domain.VaultService
import com.cofredesegredos.hardware.UsbPasswordTransmitter
import com.cofredesegredos.models.Secret
import com.cofredesegredos.models.Vault
import com.cofredesegredos.security.BiometricMasterKeyStore
import com.cofredesegredos.storage.VaultStorage
import com.cofredesegredos.sync.BluetoothSyncDiagnostics
import com.cofredesegredos.sync.BluetoothSyncServer
import com.cofredesegredos.ui.theme.Background
import com.cofredesegredos.ui.theme.Border
import com.cofredesegredos.ui.theme.CofreDeSegredosTheme
import com.cofredesegredos.ui.theme.Destructive
import com.cofredesegredos.ui.theme.Gold
import com.cofredesegredos.ui.theme.GoldSoft
import com.cofredesegredos.ui.theme.Mono
import com.cofredesegredos.ui.theme.Muted
import com.cofredesegredos.ui.theme.MutedForeground
import com.cofredesegredos.ui.theme.OnForeground
import com.cofredesegredos.ui.theme.Sizes
import com.cofredesegredos.ui.theme.Spacing
import com.cofredesegredos.ui.theme.Success
import com.cofredesegredos.ui.theme.VaultAmber
import com.cofredesegredos.ui.theme.VaultBlue
import com.cofredesegredos.ui.theme.VaultEmerald
import com.cofredesegredos.ui.theme.VaultPurple
import com.cofredesegredos.ui.theme.VaultRose
import com.cofredesegredos.ui.theme.VaultTeal
import com.cofredesegredos.ui.theme.VaultTone
import com.cofredesegredos.ui.theme.vaultToneByKey
import java.io.OutputStreamWriter

private sealed interface AppScreen {
    data object Auth : AppScreen
    data object Home : AppScreen
    data object Search : AppScreen
    data object Settings : AppScreen
    data object Trash : AppScreen
    data object Sync : AppScreen
    data class VaultDetail(val vaultId: String) : AppScreen
    data class SecretDetail(val vaultId: String, val secretId: String) : AppScreen
}

class MainActivity : FragmentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val vaultService by lazy { VaultService(VaultStorage(this)) }
    private val biometricStore by lazy { BiometricMasterKeyStore(this) }
    private val usbTransmitter by lazy { UsbPasswordTransmitter(this) }
    private var screen by mutableStateOf<AppScreen>(AppScreen.Auth)
    private var revision by mutableIntStateOf(0)
    private var pendingBackup: String? = null
    private var syncStatus by mutableStateOf("Sincronização Bluetooth desligada.")
    private var syncDiagnostics by mutableStateOf(BluetoothSyncDiagnostics())
    private var syncModeActive by mutableStateOf(false)
    private var syncCode by mutableStateOf("")
    private val bluetoothSyncServerDelegate = lazy {
        BluetoothSyncServer(
            context = this,
            exportBackup = { vaultService.exportBackup() },
            importBackup = { raw ->
                vaultService.importBackup(raw)
                runOnUiThread { lockApp() }
            },
            onStatus = { status -> runOnUiThread { syncStatus = status } },
            onDiagnostics = { diagnostics ->
                runOnUiThread { syncDiagnostics = diagnostics }
            }
        )
    }
    private val bluetoothSyncServer by bluetoothSyncServerDelegate
    private val lockRunnable = object : Runnable {
        override fun run() {
            if (syncModeActive) {
                handler.postDelayed(this, vaultService.lockTimeoutMs())
            } else {
                lockApp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(248, 247, 244)
        window.navigationBarColor = android.graphics.Color.rgb(248, 247, 244)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            CofreDeSegredosTheme(darkTheme = false) {
                CofreApp()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        resetAutoLock()
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        usbTransmitter.release()
        if (bluetoothSyncServerDelegate.isInitialized()) {
            bluetoothSyncServer.stop()
        }
        super.onDestroy()
    }

    @Composable
    private fun CofreApp() {
        revision
        var showVaultDialog by remember { mutableStateOf<Vault?>(null) }
        var creatingVault by remember { mutableStateOf(false) }
        var secretDialog by remember { mutableStateOf<Pair<String, Secret?>?>(null) }
        var confirmDeleteVault by remember { mutableStateOf<Vault?>(null) }
        var confirmDeleteSecret by remember { mutableStateOf<Pair<String, Secret>?>(null) }

        when (val current = screen) {
            AppScreen.Auth -> AuthScreen()
            else -> AppScaffold(
                screen = current,
                onNavigate = { screen = it },
                onAdd = {
                    when (current) {
                        is AppScreen.VaultDetail -> secretDialog = current.vaultId to null
                        else -> creatingVault = true
                    }
                }
            ) {
                when (current) {
                    AppScreen.Home -> HomeScreen(
                        onOpenVault = { screen = AppScreen.VaultDetail(it) },
                        onCreateVault = { creatingVault = true },
                        onSearch = { screen = AppScreen.Search }
                    )
                    AppScreen.Search -> SearchScreen {
                        screen = AppScreen.SecretDetail(it.first, it.second)
                    }
                    AppScreen.Settings -> SettingsScreen()
                    AppScreen.Trash -> TrashScreen()
                    AppScreen.Sync -> SyncScreen()
                    is AppScreen.VaultDetail -> VaultDetailScreen(
                        vaultId = current.vaultId,
                        onBack = { screen = AppScreen.Home },
                        onEdit = { vaultService.findVault(current.vaultId)?.let { showVaultDialog = it } },
                        onDelete = { vaultService.findVault(current.vaultId)?.let { confirmDeleteVault = it } },
                        onAddSecret = { secretDialog = current.vaultId to null },
                        onOpenSecret = {
                            screen = AppScreen.SecretDetail(current.vaultId, it)
                        }
                    )
                    is AppScreen.SecretDetail -> SecretDetailScreen(
                        vaultId = current.vaultId,
                        secretId = current.secretId,
                        onBack = { screen = AppScreen.VaultDetail(current.vaultId) },
                        onEdit = { secret ->
                            secretDialog = current.vaultId to secret
                        },
                        onDelete = { secret ->
                            confirmDeleteSecret = current.vaultId to secret
                        }
                    )
                    AppScreen.Auth -> Unit
                }
            }
        }

        if (creatingVault || showVaultDialog != null) {
            VaultEditorDialog(
                vault = showVaultDialog,
                onDismiss = {
                    creatingVault = false
                    showVaultDialog = null
                },
                onSave = { name, description, iconKey, colorKey ->
                    runAction {
                        val editing = showVaultDialog
                        if (editing == null) {
                            vaultService.createVault(name, description, iconKey, colorKey)
                        } else {
                            vaultService.updateVault(editing.id, name, description, iconKey, colorKey)
                        }
                        creatingVault = false
                        showVaultDialog = null
                        refresh()
                    }
                }
            )
        }

        secretDialog?.let { (vaultId, secret) ->
            SecretEditorDialog(
                secret = secret,
                onDismiss = { secretDialog = null },
                onSave = { title, login, password, notes, customLabel, customValue ->
                    runAction {
                        if (secret == null) {
                            vaultService.createSecret(
                                vaultId, title, login, password, notes, customLabel, customValue
                            )
                        } else {
                            vaultService.updateSecret(
                                vaultId, secret.id, title, login, password,
                                notes, customLabel, customValue
                            )
                        }
                        secretDialog = null
                        refresh()
                    }
                }
            )
        }

        confirmDeleteVault?.let { vault ->
            ConfirmDialog(
                title = "Excluir cofre?",
                message = "O cofre ${vault.name} e seus segredos serão movidos para a lixeira.",
                onDismiss = { confirmDeleteVault = null },
                onConfirm = {
                    vaultService.deleteVault(vault.id)
                    confirmDeleteVault = null
                    screen = AppScreen.Home
                    refresh()
                }
            )
        }

        confirmDeleteSecret?.let { (vaultId, secret) ->
            ConfirmDialog(
                title = "Excluir segredo?",
                message = "O segredo ${secret.title} será movido para a lixeira.",
                onDismiss = { confirmDeleteSecret = null },
                onConfirm = {
                    vaultService.deleteSecret(vaultId, secret.id)
                    confirmDeleteSecret = null
                    screen = AppScreen.VaultDetail(vaultId)
                    refresh()
                }
            )
        }
    }

    @Composable
    private fun AuthScreen() {
        val firstAccess = !vaultService.isInitialized()
        var password by remember { mutableStateOf("") }
        var confirmation by remember { mutableStateOf("") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .padding(Sizes.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircleIcon(Icons.Outlined.Shield, Gold, GoldSoft, 64.dp)
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    if (firstAccess) "Crie seu cofre" else "Cifra",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    if (firstAccess) "Defina uma senha mestre para proteger seus dados."
                    else "Seus dados protegidos apenas neste aparelho.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedForeground
                )
                Spacer(Modifier.height(Spacing.xxl))
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "SENHA MESTRE",
                    placeholder = "Digite sua senha",
                    password = true
                )
                if (firstAccess) {
                    Spacer(Modifier.height(Spacing.base))
                    AppTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = "CONFIRMAR SENHA",
                        placeholder = "Repita sua senha",
                        password = true
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(if (firstAccess) "Criar cofre" else "Entrar") {
                    runAction {
                        if (firstAccess) {
                            vaultService.createMasterPassword(password, confirmation)
                        } else {
                            vaultService.unlock(password)
                        }
                        screen = AppScreen.Home
                        refresh()
                        resetAutoLock()
                    }
                }
                if (!firstAccess && biometricStore.isEnabled()) {
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedButton(
                        onClick = { authenticateWithBiometrics() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Fingerprint, null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Usar biometria")
                    }
                }
            }
        }
    }

    @Composable
    private fun AppScaffold(
        screen: AppScreen,
        onNavigate: (AppScreen) -> Unit,
        onAdd: () -> Unit,
        content: @Composable () -> Unit
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                FloatingBottomNav(screen, onNavigate, onAdd)
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                content()
            }
        }
    }

    @Composable
    private fun HomeScreen(
        onOpenVault: (String) -> Unit,
        onCreateVault: () -> Unit,
        onSearch: () -> Unit
    ) {
        val vaults = vaultService.vaults()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("MEUS COFRES")
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Cofre", style = MaterialTheme.typography.displayLarge)
                }
                CircleIconButton(Icons.Outlined.Shield, "Segurança") {
                    screen = AppScreen.Settings
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            SearchField("Buscar segredo...", onSearch)
            Spacer(Modifier.height(Spacing.xl))
            if (vaults.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Shield,
                    title = "Seu primeiro cofre",
                    description = "Organize seus segredos em\ncofres como Bancos, Jogos ou\nTrabalho.",
                    action = "Criar cofre",
                    onAction = onCreateVault,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(Spacing.base),
                    contentPadding = PaddingValues(bottom = Spacing.xl),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(vaults) { vault ->
                        VaultCard(
                            vault = vault,
                            tone = vaultToneByKey(vault.colorKey),
                            icon = vaultIcon(vault.iconKey),
                            onClick = { onOpenVault(vault.id) }
                        )
                    }
                    item {
                        NewVaultCard(onCreateVault)
                    }
                }
            }
        }
    }

    @Composable
    private fun VaultDetailScreen(
        vaultId: String,
        onBack: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onAddSecret: () -> Unit,
        onOpenSecret: (String) -> Unit
    ) {
        val vault = vaultService.findVault(vaultId) ?: return
        val tone = vaultToneByKey(vault.colorKey)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Outlined.ArrowBack, "Voltar", onBack)
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Outlined.Edit, "Editar cofre", onEdit)
                Spacer(Modifier.width(Spacing.sm))
                CircleIconButton(Icons.Outlined.Delete, "Excluir cofre", onDelete)
            }
            Spacer(Modifier.height(Spacing.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(vaultIcon(vault.iconKey), tone.fg, tone.bg, Sizes.avatarLg)
                Spacer(Modifier.width(Spacing.base))
                Column {
                    Eyebrow("COFRE")
                    Text(vault.name, style = MaterialTheme.typography.displayMedium)
                    Text(
                        "${vault.secrets.size} ${if (vault.secrets.size == 1) "segredo" else "segredos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                }
            }
            if (vault.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.base))
                Text(vault.description, style = MaterialTheme.typography.bodyLarge, color = MutedForeground)
            }
            Spacer(Modifier.height(Spacing.xxl))
            if (vault.secrets.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp)
                ) {
                    Text("Este cofre ainda está vazio.", color = MutedForeground)
                    Spacer(Modifier.height(Spacing.base))
                    CompactPrimaryButton("Adicionar segredo", Icons.Outlined.Add, onAddSecret)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(vault.secrets) { secret ->
                        SecretListItem(secret) { onOpenSecret(secret.id) }
                    }
                }
            }
        }
    }

    @Composable
    private fun SecretDetailScreen(
        vaultId: String,
        secretId: String,
        onBack: () -> Unit,
        onEdit: (Secret) -> Unit,
        onDelete: (Secret) -> Unit
    ) {
        val secret = vaultService.findVault(vaultId)?.secrets?.firstOrNull { it.id == secretId } ?: return
        var revealed by remember { mutableStateOf(false) }
        var extrasOpen by remember { mutableStateOf(false) }
        var pendingSend by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Outlined.ArrowBack, "Voltar", onBack)
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Outlined.Edit, "Editar segredo") { onEdit(secret) }
                Spacer(Modifier.width(Spacing.sm))
                CircleIconButton(Icons.Outlined.Delete, "Excluir segredo") { onDelete(secret) }
            }
            Spacer(Modifier.height(Spacing.xl))
            Eyebrow("SEGREDO")
            Spacer(Modifier.height(Spacing.sm))
            Text(
                secret.title,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(Spacing.xl))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(28.dp)) {
                    DarkField(
                        label = "LOGIN",
                        value = secret.login.ifBlank { "Não informado" },
                        enabled = secret.login.isNotBlank(),
                        onCopy = { copySensitive("Login", secret.login) },
                        onSend = { pendingSend = "login" to listOf(secret.login) }
                    )
                    Divider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = Spacing.lg)
                    )
                    Text("SENHA", style = MaterialTheme.typography.labelSmall, color = Gold, letterSpacing = 2.2.sp)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (revealed) secret.password else "••••••••••••",
                            fontFamily = Mono,
                            fontSize = 16.sp,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { revealed = !revealed }) {
                            Icon(
                                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (revealed) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                        IconButton(onClick = { copySensitive("Senha", secret.password) }) {
                            Icon(Icons.Outlined.ContentCopy, "Copiar senha")
                        }
                        IconButton(
                            enabled = secret.password.isNotBlank(),
                            onClick = { pendingSend = "senha" to listOf(secret.password) }
                        ) {
                            Icon(Icons.Outlined.Computer, "Enviar senha ao PC")
                        }
                    }
                    Spacer(Modifier.height(Spacing.xl))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        OutlinedButton(
                            onClick = { copySensitive("Senha", secret.password) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnForeground),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(
                                "Copiar senha",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Button(
                            onClick = {
                                pendingSend = "todos os campos" to listOf(
                                    secret.login,
                                    secret.password,
                                    secret.notes,
                                    secret.customValue
                                ).filter { it.isNotBlank() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(
                                "Enviar tudo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            if (secret.notes.isNotBlank() || secret.customValue.isNotBlank()) {
                Spacer(Modifier.height(Spacing.base))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { extrasOpen = !extrasOpen }
                ) {
                    Column(Modifier.padding(Spacing.base)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mais informações", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Icon(if (extrasOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                        }
                        AnimatedVisibility(extrasOpen) {
                            Column {
                                if (secret.notes.isNotBlank()) {
                                    Spacer(Modifier.height(Spacing.base))
                                    Text("OBSERVAÇÕES", style = MaterialTheme.typography.labelSmall, color = MutedForeground)
                                    DetailActionRow(
                                        value = secret.notes,
                                        onCopy = { copySensitive("Observações", secret.notes) },
                                        onSend = { pendingSend = "observações" to listOf(secret.notes) }
                                    )
                                }
                                if (secret.customValue.isNotBlank()) {
                                    Spacer(Modifier.height(Spacing.base))
                                    Text(
                                        secret.customLabel.ifBlank { "CAMPO ADICIONAL" }.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedForeground
                                    )
                                    DetailActionRow(
                                        value = secret.customValue,
                                        onCopy = {
                                            copySensitive(
                                                secret.customLabel.ifBlank { "Campo adicional" },
                                                secret.customValue
                                            )
                                        },
                                        onSend = {
                                            pendingSend = "campo adicional" to listOf(secret.customValue)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        pendingSend?.let { (fieldName, values) ->
            AlertDialog(
                onDismissRequest = { pendingSend = null },
                title = { Text("Enviar $fieldName ao PC?") },
                text = { Text("O conteúdo será digitado no campo ativo do computador.") },
                confirmButton = {
                    Button(onClick = {
                        pendingSend = null
                        usbTransmitter.sendValues(values) { result ->
                            runOnUiThread {
                                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("Enviar") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingSend = null }) { Text("Cancelar") }
                },
                shape = MaterialTheme.shapes.large
            )
        }
    }

    @Composable
    private fun SearchScreen(onOpen: (Pair<String, String>) -> Unit) {
        var query by remember { mutableStateOf("") }
        val results = if (query.isBlank()) emptyList() else vaultService.vaults().flatMap { vault ->
            vaultService.searchSecrets(vault.id, query).map { vault to it }
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            PageHeader("Buscar") { screen = AppScreen.Home }
            Spacer(Modifier.height(Spacing.base))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar segredos...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xl))
            if (query.isBlank()) {
                Text(
                    "Digite para encontrar um segredo.",
                    color = MutedForeground,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 48.dp)
                )
            } else if (results.isEmpty()) {
                Text(
                    "Nenhum segredo encontrado.",
                    color = MutedForeground,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 48.dp)
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Column {
                        results.forEachIndexed { index, (vault, secret) ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpen(vault.id to secret.id) }
                                    .padding(Spacing.base)
                            ) {
                                Text(secret.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${vault.name} · ${secret.login}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                            if (index != results.lastIndex) Divider(color = Border)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TrashScreen() {
        val trashedVaults = vaultService.trashedVaults()
        val trashedSecrets = vaultService.trashedSecrets()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            PageHeader("Lixeira") { screen = AppScreen.Home }
            Spacer(Modifier.height(Spacing.md))
            Text("Itens excluídos ficam aqui por 30 dias.", color = MutedForeground)
            Spacer(Modifier.height(Spacing.xl))
            if (trashedVaults.isEmpty() && trashedSecrets.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Shield,
                    title = "Nada por aqui",
                    description = "Cofres e segredos excluídos\nvão aparecer aqui.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(bottom = Spacing.xl)
                ) {
                    if (trashedVaults.isNotEmpty()) {
                        item { FieldLabel("COFRES") }
                        items(trashedVaults, key = { it.vault.id }) { item ->
                            TrashItem(
                                title = item.vault.name,
                                subtitle = "${item.vault.secrets.size} ${if (item.vault.secrets.size == 1) "segredo" else "segredos"}",
                                onRestore = {
                                    runAction {
                                        vaultService.restoreVault(item.vault.id)
                                        refresh()
                                    }
                                },
                                onDelete = {
                                    runAction {
                                        vaultService.permanentlyDeleteVault(item.vault.id)
                                        refresh()
                                    }
                                }
                            )
                        }
                    }
                    if (trashedSecrets.isNotEmpty()) {
                        item {
                            if (trashedVaults.isNotEmpty()) Spacer(Modifier.height(Spacing.sm))
                            FieldLabel("SEGREDOS")
                        }
                        items(trashedSecrets, key = { it.secret.id }) { item ->
                            TrashItem(
                                title = item.secret.title,
                                subtitle = item.vaultName,
                                onRestore = {
                                    runAction {
                                        vaultService.restoreSecret(item.secret.id)
                                        refresh()
                                    }
                                },
                                onDelete = {
                                    runAction {
                                        vaultService.permanentlyDeleteSecret(item.secret.id)
                                        refresh()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TrashItem(
        title: String,
        subtitle: String,
        onRestore: () -> Unit,
        onDelete: () -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(Spacing.base)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Restaurar", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Destructive),
                        border = BorderStroke(1.dp, Destructive.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Apagar", maxLines = 1)
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null) {
                runAction {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Não foi possível abrir o local escolhido.")
                    output.use {
                        OutputStreamWriter(output, Charsets.UTF_8).use { it.write(pendingBackup.orEmpty()) }
                    }
                    Toast.makeText(context, "Backup exportado.", Toast.LENGTH_SHORT).show()
                }
            }
            pendingBackup = null
        }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runAction {
                    val content = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Não foi possível ler o arquivo.")
                    vaultService.importBackup(content)
                    biometricStore.disable()
                    Toast.makeText(context, "Backup importado. Desbloqueie novamente.", Toast.LENGTH_LONG).show()
                    lockApp()
                }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            PageHeader("Configurações") { screen = AppScreen.Home }
            Spacer(Modifier.height(Spacing.xxl))
            SettingsSection("SEGURANÇA") {
                SettingsRow(
                    Icons.Outlined.Fingerprint,
                    "Desbloqueio por biometria",
                    trailing = {
                        Switch(
                            checked = biometricStore.isEnabled(),
                            onCheckedChange = {
                                if (it) enableBiometrics() else {
                                    biometricStore.disable()
                                    refresh()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                SettingsRow(
                    Icons.Outlined.LockClock,
                    "Bloqueio automático",
                    subtitle = timeoutLabel(vaultService.lockTimeoutMs()),
                    trailing = { Icon(Icons.Outlined.ExpandMore, null) }
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            SettingsSection("BACKUP") {
                SettingsRow(
                    Icons.Outlined.Backup,
                    "Exportar backup",
                    "Salva um arquivo criptografado no aparelho",
                    onClick = {
                        runAction {
                            this@MainActivity.pendingBackup = vaultService.exportBackup()
                            exportLauncher.launch("cofre-${System.currentTimeMillis()}.cofrebackup")
                        }
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                SettingsRow(
                    Icons.Outlined.Download,
                    "Importar backup",
                    "Restaura um arquivo criptografado",
                    onClick = {
                        importLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            SettingsSection("SINCRONIZAÇÃO") {
                SettingsRow(
                    Icons.Outlined.Bluetooth,
                    "Sincronizar com PC",
                    "Conecta ao aplicativo Windows por Bluetooth",
                    onClick = { screen = AppScreen.Sync }
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            SettingsSection("SOBRE") {
                SettingsRow(
                    Icons.Outlined.Shield,
                    "Cifra",
                    "Seus dados ficam guardados apenas neste aparelho."
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            OutlinedButton(
                onClick = { lockApp() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.Lock, null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Bloquear agora")
            }
        }
    }

    @Composable
    private fun SyncScreen() {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val granted = bluetoothPermissionsGranted()
            bluetoothSyncServer.refreshDiagnostics(granted)
            if (granted) {
                startBluetoothSync()
            } else {
                syncStatus = "Permissões Bluetooth negadas. O app continuará aberto."
            }
        }
        LaunchedEffect(Unit) {
            runCatching {
                bluetoothSyncServer.refreshDiagnostics(bluetoothPermissionsGranted())
            }.onFailure {
                syncStatus = "Erro ao abrir sincronização: ${it.message ?: it.javaClass.simpleName}"
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl)
        ) {
            PageHeader("Sincronizar com PC") {
                bluetoothSyncServer.stop()
                syncModeActive = false
                syncCode = ""
                resetAutoLock()
                screen = AppScreen.Settings
            }
            Spacer(Modifier.height(Spacing.xxl))
            CircleIcon(Icons.Outlined.Bluetooth, Gold, GoldSoft, 64.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                "Conexão segura por Bluetooth",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Abra o Cifra no Windows, procure o celular e informe o código temporário.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedForeground
            )
            if (syncCode.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xxl))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(Spacing.xl)
                    ) {
                        FieldLabel("CÓDIGO TEMPORÁRIO")
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            syncCode,
                            style = MaterialTheme.typography.displayLarge,
                            letterSpacing = 5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(syncStatus, modifier = Modifier.padding(Spacing.base))
            }
            Spacer(Modifier.height(Spacing.base))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(Spacing.base)) {
                    FieldLabel("DIAGNÓSTICO BLE")
                    Spacer(Modifier.height(Spacing.sm))
                    DiagnosticRow("Bluetooth ligado", syncDiagnostics.bluetoothEnabled)
                    DiagnosticRow("Permissões concedidas", syncDiagnostics.permissionsGranted)
                    DiagnosticRow("BLE advertising suportado", syncDiagnostics.advertisingSupported)
                    DiagnosticRow("GATT server iniciado", syncDiagnostics.gattServerStarted)
                    DiagnosticRow("Advertising iniciado", syncDiagnostics.advertisingStarted)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Último erro técnico",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedForeground
                    )
                    Text(
                        syncDiagnostics.lastError.ifBlank { "Nenhum" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(if (syncModeActive) "Parar sincronização" else "Ativar sincronização") {
                if (syncModeActive) {
                    bluetoothSyncServer.stop()
                    syncModeActive = false
                    syncCode = ""
                    resetAutoLock()
                } else if (!bluetoothPermissionsGranted()) {
                    permissionLauncher.launch(requiredBluetoothPermissions())
                } else {
                    bluetoothSyncServer.refreshDiagnostics(true)
                    startBluetoothSync()
                }
            }
            Spacer(Modifier.height(Spacing.base))
            Text(
                "A senha mestre nunca é enviada. O Bluetooth transporta apenas o backup criptografado.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
            )
        }
    }

    @Composable
    private fun DiagnosticRow(label: String, value: Boolean) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                if (value) "sim" else "não",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (value) Success else Destructive
            )
        }
    }

    @Composable
    private fun VaultEditorDialog(
        vault: Vault?,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String) -> Unit
    ) {
        var name by remember(vault) { mutableStateOf(vault?.name.orEmpty()) }
        var description by remember(vault) { mutableStateOf(vault?.description.orEmpty()) }
        val iconKeys = listOf("bank", "social", "work", "games", "shopping", "email", "keys", "personal")
        val colorKeys = listOf("blue", "purple", "amber", "emerald", "rose", "teal")
        var selectedIcon by remember(vault) {
            mutableIntStateOf(iconKeys.indexOf(vault?.iconKey ?: "keys").coerceAtLeast(0))
        }
        var selectedTone by remember(vault) {
            mutableIntStateOf(colorKeys.indexOf(vault?.colorKey ?: "blue").coerceAtLeast(0))
        }
        val icons = listOf(
            Icons.Outlined.Lock to "Bancos",
            Icons.Outlined.Person to "Social",
            Icons.Outlined.Work to "Trabalho",
            Icons.Outlined.Games to "Jogos",
            Icons.Outlined.ShoppingBag to "Compras",
            Icons.Outlined.Email to "E-mail",
            Icons.Outlined.Key to "Chaves",
            Icons.Outlined.Shield to "Pessoal"
        )
        BottomFormDialog(
            onDismiss = onDismiss,
            actionText = if (vault == null) "Criar cofre" else "Salvar alterações",
            actionEnabled = name.isNotBlank(),
            onAction = {
                onSave(name, description, iconKeys[selectedIcon], colorKeys[selectedTone])
            },
            footerContent = {
                FieldLabel("COR")
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(VaultBlue, VaultPurple, VaultAmber, VaultEmerald, VaultRose, VaultTeal)
                        .forEachIndexed { index, tone ->
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedTone == index) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(Background)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(tone.fg)
                                    .clickable { selectedTone = index }
                            )
                        }
                }
            }
        ) {
            DialogHeader(if (vault == null) "Novo cofre" else "Editar cofre", onDismiss)
            Spacer(Modifier.height(Spacing.xl))
            AppTextField(name, { name = it }, "NOME", "Ex.: Bancos")
            Spacer(Modifier.height(Spacing.base))
            AppTextField(
                description,
                { description = it },
                "DESCRIÇÃO (opcional)",
                "Para que serve este cofre?",
                minLines = 2
            )
            Spacer(Modifier.height(Spacing.xl))
            FieldLabel("ÍCONE")
            Spacer(Modifier.height(Spacing.md))
            icons.chunked(4).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEachIndexed { columnIndex, (icon, label) ->
                        val index = rowIndex * 4 + columnIndex
                        IconChoice(
                            icon = icon,
                            label = label,
                            selected = selectedIcon == index,
                            tone = vaultToneByKey(colorKeys[selectedTone])
                        ) { selectedIcon = index }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    @Composable
    private fun SecretEditorDialog(
        secret: Secret?,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String, String, String) -> Unit
    ) {
        var title by remember(secret) { mutableStateOf(secret?.title.orEmpty()) }
        var login by remember(secret) { mutableStateOf(secret?.login.orEmpty()) }
        var password by remember(secret) { mutableStateOf(secret?.password.orEmpty()) }
        var notes by remember(secret) { mutableStateOf(secret?.notes.orEmpty()) }
        var customLabel by remember(secret) { mutableStateOf(secret?.customLabel.orEmpty()) }
        var customValue by remember(secret) { mutableStateOf(secret?.customValue.orEmpty()) }
        var expanded by remember { mutableStateOf(secret != null && (notes.isNotBlank() || customValue.isNotBlank())) }
        BottomFormDialog(
            onDismiss = onDismiss,
            actionText = if (secret == null) "Salvar segredo" else "Salvar alterações",
            actionEnabled = title.isNotBlank(),
            onAction = {
                onSave(title, login, password, notes, customLabel, customValue)
            }
        ) {
            DialogHeader(if (secret == null) "Novo segredo" else "Editar segredo", onDismiss)
            Spacer(Modifier.height(Spacing.xl))
            AppTextField(title, { title = it }, "TÍTULO", "Ex.: Nubank")
            Spacer(Modifier.height(Spacing.base))
            AppTextField(login, { login = it }, "LOGIN", "email ou usuário")
            Spacer(Modifier.height(Spacing.base))
            AppTextField(password, { password = it }, "SENHA", "••••••", password = true)
            Spacer(Modifier.height(Spacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text("Mais opções", style = MaterialTheme.typography.bodySmall)
            }
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(Spacing.xl))
                    AppTextField(
                        notes, { notes = it }, "OBSERVAÇÕES", "Anotações privadas", minLines = 3
                    )
                    Spacer(Modifier.height(Spacing.base))
                    AppTextField(
                        customLabel, { customLabel = it }, "CAMPO ADICIONAL", "Nome do campo"
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    AppTextField(
                        customValue, { customValue = it }, "VALOR", "Valor do campo"
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }

    @Composable
    private fun BottomFormDialog(
        onDismiss: () -> Unit,
        actionText: String,
        actionEnabled: Boolean,
        onAction: () -> Unit,
        footerContent: @Composable ColumnScope.() -> Unit = {},
        content: @Composable ColumnScope.() -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Background,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.84f)
                        .imePadding()
                ) {
                    Column {
                        Column(
                            Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = Spacing.xl,
                                    top = Spacing.xl,
                                    end = Spacing.xl,
                                    bottom = Spacing.md
                                ),
                            content = content
                        )
                        Surface(color = Background, shadowElevation = 8.dp) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = Spacing.xl,
                                        top = Spacing.md,
                                        end = Spacing.xl,
                                        bottom = Spacing.xl
                                    )
                            ) {
                                footerContent()
                                Spacer(Modifier.height(Spacing.md))
                                PrimaryButton(actionText, enabled = actionEnabled, onClick = onAction)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FloatingBottomNav(
        current: AppScreen,
        onNavigate: (AppScreen) -> Unit,
        onAdd: () -> Unit
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.94f),
                shape = CircleShape,
                border = BorderStroke(1.dp, Border),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.padding(horizontal = Spacing.sm)
                ) {
                    NavIcon(Icons.Outlined.Key, "Cofres", current is AppScreen.Home || current is AppScreen.VaultDetail) {
                        onNavigate(AppScreen.Home)
                    }
                    NavIcon(Icons.Outlined.Search, "Buscar", current is AppScreen.Search) {
                        onNavigate(AppScreen.Search)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(Sizes.fabPrimary)
                            .clickable(onClick = onAdd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, "Adicionar")
                        }
                    }
                    NavIcon(Icons.Outlined.Settings, "Configurações", current is AppScreen.Settings) {
                        onNavigate(AppScreen.Settings)
                    }
                    NavIcon(Icons.Outlined.Delete, "Lixeira", current is AppScreen.Trash) {
                        onNavigate(AppScreen.Trash)
                    }
                }
            }
        }
    }

    @Composable
    private fun NavIcon(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
        Surface(
            color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MutedForeground,
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, description, modifier = Modifier.size(22.dp))
            }
        }
    }

    @Composable
    private fun VaultCard(vault: Vault, tone: VaultTone, icon: ImageVector, onClick: () -> Unit) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Border),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(162.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg)
            ) {
                CircleIcon(icon, tone.fg, tone.bg, Sizes.avatarMd)
                Column {
                    Text(vault.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${vault.secrets.size} ${if (vault.secrets.size == 1) "segredo" else "segredos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                }
            }
        }
    }

    @Composable
    private fun NewVaultCard(onClick: () -> Unit) {
        Surface(
            color = Color.Transparent,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(2.dp, Border),
            modifier = Modifier
                .fillMaxWidth()
                .height(162.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircleIcon(Icons.Outlined.Add, MutedForeground, Muted, 40.dp)
                Spacer(Modifier.height(Spacing.sm))
                Text("Novo Cofre", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
            }
        }
    }

    @Composable
    private fun SecretListItem(secret: Secret, onClick: () -> Unit) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(Spacing.base)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Muted),
                    contentAlignment = Alignment.Center
                ) {
                    Text(secret.title.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MutedForeground)
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(secret.title, style = MaterialTheme.typography.bodyLarge)
                    Text(secret.login, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MutedForeground, modifier = Modifier.size(18.dp))
            }
        }
    }

    @Composable
    private fun EmptyState(
        icon: ImageVector,
        title: String,
        description: String,
        modifier: Modifier = Modifier,
        action: String? = null,
        onAction: () -> Unit = {}
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxWidth()
        ) {
            CircleIcon(icon, Gold, GoldSoft, 64.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MutedForeground,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (action != null) {
                Spacer(Modifier.height(Spacing.xl))
                CompactPrimaryButton(action, null, onAction)
            }
        }
    }

    @Composable
    private fun SearchField(placeholder: String, onClick: () -> Unit) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Border),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.base)
            ) {
                Icon(Icons.Outlined.Search, null, tint = MutedForeground, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.md))
                Text(placeholder, color = MutedForeground)
            }
        }
    }

    @Composable
    private fun AppTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        placeholder: String,
        password: Boolean = false,
        minLines: Int = 1
    ) {
        var revealed by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth()) {
            FieldLabel(label)
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = MutedForeground) },
                visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (password) {
                    {
                        IconButton(onClick = { revealed = !revealed }) {
                            Icon(if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                        }
                    }
                } else null,
                singleLine = minLines == 1,
                minLines = minLines,
                keyboardOptions = KeyboardOptions.Default,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun SettingsSection(label: String, content: @Composable ColumnScope.() -> Unit) {
        FieldLabel(label)
        Spacer(Modifier.height(Spacing.sm))
        Column(content = content)
    }

    @Composable
    private fun SettingsRow(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        trailing: @Composable (() -> Unit)? = null,
        onClick: (() -> Unit)? = null
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(Spacing.base)
            ) {
                CircleIcon(icon, Gold, GoldSoft, 40.dp)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                    }
                }
                trailing?.invoke()
            }
        }
    }

    @Composable
    private fun PageHeader(title: String, onBack: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Outlined.ArrowBack, "Voltar", onBack)
            Spacer(Modifier.width(Spacing.md))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }

    @Composable
    private fun DialogHeader(title: String, onClose: () -> Unit) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Close, "Fechar")
            }
        }
    }

    @Composable
    private fun DarkField(
        label: String,
        value: String,
        enabled: Boolean,
        onCopy: () -> Unit,
        onSend: () -> Unit
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Gold, letterSpacing = 2.2.sp)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, "Copiar $label")
            }
            IconButton(enabled = enabled, onClick = onSend) {
                Icon(Icons.Outlined.Computer, "Enviar $label ao PC")
            }
        }
    }

    @Composable
    private fun DetailActionRow(value: String, onCopy: () -> Unit, onSend: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, "Copiar", tint = MutedForeground)
            }
            IconButton(onClick = onSend) {
                Icon(Icons.Outlined.Computer, "Enviar ao PC", tint = Gold)
            }
        }
    }

    @Composable
    private fun Eyebrow(value: String) {
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
            letterSpacing = 2.2.sp
        )
    }

    @Composable
    private fun FieldLabel(value: String) {
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = MutedForeground,
            letterSpacing = 1.2.sp
        )
    }

    @Composable
    private fun CircleIcon(
        icon: ImageVector,
        foreground: Color,
        background: Color,
        size: androidx.compose.ui.unit.Dp
    ) {
        Box(
            Modifier
                .size(size)
                .clip(if (size >= 56.dp) CircleShape else RoundedCornerShape(16.dp))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = foreground, modifier = Modifier.size(size * 0.48f))
        }
    }

    @Composable
    private fun CircleIconButton(
        icon: ImageVector,
        description: String,
        onClick: () -> Unit = {}
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            border = BorderStroke(1.dp, Border),
            shadowElevation = 1.dp,
            modifier = Modifier.size(Sizes.iconBtn)
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, description, modifier = Modifier.size(19.dp))
            }
        }
    }

    @Composable
    private fun IconChoice(
        icon: ImageVector,
        label: String,
        selected: Boolean,
        tone: VaultTone,
        onClick: () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
            Surface(
                color = if (selected) Color.Transparent else tone.bg,
                shape = RoundedCornerShape(18.dp),
                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .size(if (selected) 72.dp else 48.dp)
                    .clickable(onClick = onClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tone.fg, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MutedForeground, maxLines = 1)
        }
    }

    @Composable
    private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = Color(0xFF969795),
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    private fun CompactPrimaryButton(
        text: String,
        icon: ImageVector? = null,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 22.dp),
            modifier = Modifier.height(44.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(text)
        }
    }

    @Composable
    private fun ConfirmDialog(
        title: String,
        message: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Destructive)
                ) { Text("Excluir") }
            },
            dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } },
            shape = MaterialTheme.shapes.large
        )
    }

    @Composable
    private fun fieldColors() = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = Gold,
        unfocusedBorderColor = Border,
        cursorColor = Gold
    )

    private fun toneFor(index: Int): VaultTone =
        listOf(VaultBlue, VaultPurple, VaultAmber, VaultEmerald, VaultRose, VaultTeal)
            .getOrElse(index.coerceAtLeast(0) % 6) { VaultBlue }

    private fun vaultIcon(key: String): ImageVector = when (key) {
        "bank" -> Icons.Outlined.Lock
        "social" -> Icons.Outlined.Person
        "work" -> Icons.Outlined.Work
        "games" -> Icons.Outlined.Games
        "shopping" -> Icons.Outlined.ShoppingBag
        "email" -> Icons.Outlined.Email
        "personal" -> Icons.Outlined.Shield
        else -> Icons.Outlined.Key
    }

    private fun timeoutLabel(milliseconds: Long): String = when (milliseconds) {
        30_000L -> "30 segundos"
        60_000L -> "1 minuto"
        300_000L -> "5 minutos"
        Long.MAX_VALUE -> "Nunca"
        else -> "${milliseconds / 60_000L} minutos"
    }

    private fun authenticateWithBiometrics() {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometria indisponível. Use a senha mestre.", Toast.LENGTH_LONG).show()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runAction {
                        vaultService.unlock(biometricStore.decryptMasterPassword())
                        screen = AppScreen.Home
                        refresh()
                        resetAutoLock()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        Toast.makeText(this@MainActivity, "Biometria: $errString", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear Cofre")
                .setSubtitle("Confirme sua identidade")
                .setNegativeButtonText("Usar senha mestre")
                .build()
        )
    }

    private fun enableBiometrics() {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Cadastre uma biometria nas configurações do aparelho.", Toast.LENGTH_LONG).show()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runAction {
                        biometricStore.enable(vaultService.currentMasterPasswordForBiometricSetup())
                        refresh()
                        Toast.makeText(this@MainActivity, "Biometria ativada.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Ativar biometria")
                .setSubtitle("Confirme sua identidade")
                .setNegativeButtonText("Cancelar")
                .build()
        )
    }

    private fun copySensitive(label: String, value: String) {
        if (value.isBlank()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copiado.", Toast.LENGTH_SHORT).show()
        handler.postDelayed({
            val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (current == value) clipboard.clearPrimaryClip()
        }, 30_000L)
    }

    private fun lockApp() {
        handler.removeCallbacks(lockRunnable)
        vaultService.lock()
        screen = AppScreen.Auth
        refresh()
    }

    private fun resetAutoLock() {
        if (vaultService.currentData() == null) return
        handler.removeCallbacks(lockRunnable)
        handler.postDelayed(lockRunnable, vaultService.lockTimeoutMs())
    }

    private fun refresh() {
        revision++
    }

    private fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun bluetoothPermissionsGranted(): Boolean {
        return requiredBluetoothPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBluetoothSync() {
        try {
            val code = bluetoothSyncServer.start()
            if (code.isNotBlank()) {
                syncCode = code
                syncModeActive = true
                handler.removeCallbacks(lockRunnable)
            } else {
                syncCode = ""
                syncModeActive = false
            }
        } catch (error: Throwable) {
            syncCode = ""
            syncModeActive = false
            syncStatus = "Erro BLE: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun runAction(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "Não foi possível concluir a ação.", Toast.LENGTH_LONG).show()
        }
    }
}
