using System.IO;
using System.Windows;
using System.Windows.Controls;

namespace CofreDeSegredos.Windows;

public partial class MainWindow : Window
{
    private readonly BluetoothSyncClient _bluetooth = new();
    private readonly string _backupPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "CofreDeSegredos", "cofre.cofrebackup");

    public MainWindow()
    {
        InitializeComponent();
        _bluetooth.StatusChanged += status => Dispatcher.Invoke(() => StatusText.Text = status);
        RefreshBackupInfo();
        Closed += (_, _) => _bluetooth.Dispose();
    }

    private async void Scan_Click(object sender, RoutedEventArgs e) =>
        await RunAsync(async () =>
        {
            StatusText.Text = "Procurando celular...";
            await _bluetooth.ScanAsync(TimeSpan.FromSeconds(20));
        });

    private async void Connect_Click(object sender, RoutedEventArgs e) =>
        await RunAsync(async () =>
        {
            if (PairingCodeBox.Text.Length != 6) throw new InvalidOperationException("Digite o código de seis dígitos.");
            await _bluetooth.ConnectAndAuthenticateAsync(PairingCodeBox.Text);
        });

    private async void Pull_Click(object sender, RoutedEventArgs e) =>
        await RunAsync(async () =>
        {
            var backup = await _bluetooth.PullBackupAsync();
            Directory.CreateDirectory(Path.GetDirectoryName(_backupPath)!);
            await File.WriteAllTextAsync(_backupPath, backup);
            RefreshBackupInfo();
            StatusText.Text = "Sincronização concluída. Backup recebido do celular.";
        });

    private async void Push_Click(object sender, RoutedEventArgs e) =>
        await RunAsync(async () =>
        {
            if (!File.Exists(_backupPath)) throw new FileNotFoundException("Nenhuma cópia local para enviar.");
            await _bluetooth.PushBackupAsync(await File.ReadAllTextAsync(_backupPath));
        });

    private void Unlock_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            if (!File.Exists(_backupPath)) throw new FileNotFoundException("Sincronize o celular primeiro.");
            CredentialList.ItemsSource = CryptoService.Unlock(
                File.ReadAllText(_backupPath), MasterPasswordBox.Password);
            MasterPasswordBox.Clear();
            StatusText.Text = "Cópia local desbloqueada.";
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "Não foi possível desbloquear", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    private void CopyPassword_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button { Tag: string password })
        {
            Clipboard.SetText(password);
            StatusText.Text = "Senha copiada. Limpe o clipboard após o uso.";
        }
    }

    private async Task RunAsync(Func<Task> action)
    {
        try
        {
            IsEnabled = false;
            await action();
        }
        catch (Exception error)
        {
            StatusText.Text = error.Message;
            MessageBox.Show(error.Message, "Cifra", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
        finally
        {
            IsEnabled = true;
        }
    }

    private void RefreshBackupInfo()
    {
        BackupInfoText.Text = File.Exists(_backupPath)
            ? $"Sincronizado em {File.GetLastWriteTime(_backupPath):dd/MM/yyyy HH:mm}"
            : "Nenhum backup sincronizado";
    }
}
