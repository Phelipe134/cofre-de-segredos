using System.Collections.Concurrent;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Advertisement;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace CofreDeSegredos.Windows;

public sealed class BluetoothSyncClient : IDisposable
{
    public static readonly Guid ServiceUuid = Guid.Parse("9d8f2b20-8a9a-4f31-a832-79b79d53a901");
    private static readonly Guid ControlUuid = Guid.Parse("9d8f2b21-8a9a-4f31-a832-79b79d53a901");
    private static readonly Guid DataUuid = Guid.Parse("9d8f2b22-8a9a-4f31-a832-79b79d53a901");

    private BluetoothLEAdvertisementWatcher? _watcher;
    private ulong? _address;
    private BluetoothLEDevice? _device;
    private GattCharacteristic? _control;
    private GattCharacteristic? _data;
    private readonly ConcurrentDictionary<int, byte[]> _chunks = new();
    private int _expectedChunks;
    private TaskCompletionSource<string>? _pullCompletion;

    public event Action<string>? StatusChanged;

    public Task ScanAsync(TimeSpan timeout)
    {
        var completion = new TaskCompletionSource();
        _watcher = new BluetoothLEAdvertisementWatcher
        {
            ScanningMode = BluetoothLEScanningMode.Active
        };
        _watcher.Received += (_, args) =>
        {
            if (args.Advertisement.ServiceUuids.Contains(ServiceUuid))
            {
                _address = args.BluetoothAddress;
                StatusChanged?.Invoke($"Celular encontrado: {args.BluetoothAddress:X}");
                completion.TrySetResult();
                _watcher.Stop();
            }
        };
        _watcher.Start();
        _ = Task.Delay(timeout).ContinueWith(_ =>
        {
            _watcher?.Stop();
            completion.TrySetException(new TimeoutException("Celular não encontrado. Ative a sincronização no Android."));
        });
        return completion.Task;
    }

    public async Task ConnectAndAuthenticateAsync(string code)
    {
        if (_address is null) throw new InvalidOperationException("Procure o celular primeiro.");
        _device = await BluetoothLEDevice.FromBluetoothAddressAsync(_address.Value)
            ?? throw new InvalidOperationException("Não foi possível conectar ao celular.");
        var services = await _device.GetGattServicesForUuidAsync(ServiceUuid, BluetoothCacheMode.Uncached);
        var service = services.Services.FirstOrDefault()
            ?? throw new InvalidOperationException("Serviço do Cofre não encontrado.");
        _control = (await service.GetCharacteristicsForUuidAsync(ControlUuid, BluetoothCacheMode.Uncached))
            .Characteristics.FirstOrDefault() ?? throw new InvalidOperationException("Canal de controle ausente.");
        _data = (await service.GetCharacteristicsForUuidAsync(DataUuid, BluetoothCacheMode.Uncached))
            .Characteristics.FirstOrDefault() ?? throw new InvalidOperationException("Canal de dados ausente.");

        var helloResult = await _control.ReadValueAsync(BluetoothCacheMode.Uncached);
        var hello = ReadString(helloResult.Value);
        var parts = hello.Split(':');
        if (parts.Length != 3 || parts[0] != "HELLO") throw new InvalidDataException("Resposta Bluetooth inválida.");
        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(code));
        var proof = Convert.ToHexString(hmac.ComputeHash(Encoding.UTF8.GetBytes(parts[2]))).ToLowerInvariant();
        await WriteStringAsync(_control, $"AUTH:{proof}");
        var authResult = ReadString((await _control.ReadValueAsync(BluetoothCacheMode.Uncached)).Value);
        if (authResult != "AUTH_OK")
            throw new UnauthorizedAccessException("Código de pareamento inválido.");
        await _data.WriteClientCharacteristicConfigurationDescriptorAsync(
            GattClientCharacteristicConfigurationDescriptorValue.Notify);
        _data.ValueChanged += Data_ValueChanged;
        StatusChanged?.Invoke("Conectado e autenticado.");
    }

    public async Task<string> PullBackupAsync()
    {
        EnsureConnected();
        _chunks.Clear();
        _expectedChunks = 0;
        _pullCompletion = new TaskCompletionSource<string>();
        await WriteStringAsync(_control!, "PULL");
        return await _pullCompletion.Task.WaitAsync(TimeSpan.FromSeconds(60));
    }

    public async Task PushBackupAsync(string backup)
    {
        EnsureConnected();
        var bytes = Encoding.UTF8.GetBytes(backup);
        await WriteStringAsync(_control!, $"PUSH:{bytes.Length}");
        for (var offset = 0; offset < bytes.Length; offset += 16)
        {
            var length = Math.Min(16, bytes.Length - offset);
            using var writer = new DataWriter();
            writer.WriteBytes(bytes.AsSpan(offset, length).ToArray());
            var result = await _data!.WriteValueWithResultAsync(writer.DetachBuffer(), GattWriteOption.WriteWithResponse);
            if (result.Status != GattCommunicationStatus.Success)
                throw new IOException($"Falha ao enviar bloco: {result.Status}");
        }
        StatusChanged?.Invoke("Backup enviado ao celular.");
    }

    private void Data_ValueChanged(GattCharacteristic sender, GattValueChangedEventArgs args)
    {
        using var reader = DataReader.FromBuffer(args.CharacteristicValue);
        var bytes = new byte[reader.UnconsumedBufferLength];
        reader.ReadBytes(bytes);
        if (bytes.Length == 1 && bytes[0] == 0x7f)
        {
            if (_expectedChunks == 0 || _chunks.Count != _expectedChunks)
            {
                _pullCompletion?.TrySetException(new IOException("Transferência Bluetooth incompleta."));
                return;
            }
            var combined = Enumerable.Range(0, _expectedChunks).SelectMany(i => _chunks[i]).ToArray();
            _pullCompletion?.TrySetResult(Encoding.UTF8.GetString(combined));
            return;
        }
        if (bytes.Length < 4) return;
        var sequence = (bytes[0] << 8) | bytes[1];
        _expectedChunks = (bytes[2] << 8) | bytes[3];
        _chunks[sequence] = bytes[4..];
    }

    private static async Task WriteStringAsync(GattCharacteristic characteristic, string value)
    {
        using var writer = new DataWriter();
        writer.WriteBytes(Encoding.UTF8.GetBytes(value));
        var result = await characteristic.WriteValueWithResultAsync(
            writer.DetachBuffer(), GattWriteOption.WriteWithResponse);
        if (result.Status != GattCommunicationStatus.Success)
            throw new IOException($"Falha Bluetooth: {result.Status}");
    }

    private static string ReadString(IBuffer buffer)
    {
        using var reader = DataReader.FromBuffer(buffer);
        var bytes = new byte[reader.UnconsumedBufferLength];
        reader.ReadBytes(bytes);
        return Encoding.UTF8.GetString(bytes);
    }

    private void EnsureConnected()
    {
        if (_control is null || _data is null) throw new InvalidOperationException("Conecte ao celular primeiro.");
    }

    public void Dispose()
    {
        _watcher?.Stop();
        if (_data is not null) _data.ValueChanged -= Data_ValueChanged;
        _device?.Dispose();
    }
}
