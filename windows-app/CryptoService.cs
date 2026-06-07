using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace CofreDeSegredos.Windows;

public sealed record CredentialItem(string Title, string Subtitle, string Password);

public static class CryptoService
{
    public static IReadOnlyList<CredentialItem> Unlock(string backupJson, string password)
    {
        using var backup = JsonDocument.Parse(backupJson);
        var root = backup.RootElement;
        if (root.GetProperty("app").GetString() != "cofre-de-segredos")
            throw new InvalidDataException("Arquivo de backup inválido.");

        var salt = Convert.FromBase64String(root.GetProperty("salt").GetString()!);
        var key = Rfc2898DeriveBytes.Pbkdf2(
            password, salt, 210_000, HashAlgorithmName.SHA256, 32);

        DecryptEnvelope(root.GetProperty("verifier"), key);
        var rawVault = DecryptEnvelope(root.GetProperty("vaultData"), key);
        using var data = JsonDocument.Parse(rawVault);
        var credentials = new List<CredentialItem>();
        foreach (var vault in data.RootElement.GetProperty("vaults").EnumerateArray())
        {
            var vaultName = vault.GetProperty("name").GetString() ?? "";
            foreach (var secret in vault.GetProperty("secrets").EnumerateArray())
            {
                credentials.Add(new CredentialItem(
                    secret.GetProperty("title").GetString() ?? "Sem título",
                    $"{vaultName} · {secret.GetProperty("login").GetString()}",
                    secret.GetProperty("password").GetString() ?? ""));
            }
        }
        CryptographicOperations.ZeroMemory(key);
        return credentials;
    }

    private static string DecryptEnvelope(JsonElement envelope, byte[] key)
    {
        var nonce = Convert.FromBase64String(envelope.GetProperty("iv").GetString()!);
        var encryptedWithTag = Convert.FromBase64String(envelope.GetProperty("ciphertext").GetString()!);
        var cipherLength = encryptedWithTag.Length - 16;
        var cipher = encryptedWithTag.AsSpan(0, cipherLength);
        var tag = encryptedWithTag.AsSpan(cipherLength, 16);
        var plain = new byte[cipherLength];
        using var aes = new AesGcm(key, 16);
        aes.Decrypt(nonce, cipher, tag, plain);
        return Encoding.UTF8.GetString(plain);
    }
}
