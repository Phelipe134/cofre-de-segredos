# Protocolo de sincronização Bluetooth

## Papéis

- Android: servidor/periférico BLE.
- Windows: cliente/central BLE.
- Transporte: backup criptografado já produzido por `VaultStorage`.

## Serviço GATT

- Serviço: `9d8f2b20-8a9a-4f31-a832-79b79d53a901`
- Controle: `9d8f2b21-8a9a-4f31-a832-79b79d53a901`
- Dados: `9d8f2b22-8a9a-4f31-a832-79b79d53a901`

## Autenticação

1. O Android cria um código decimal temporário de seis dígitos e um nonce aleatório de 16 bytes.
2. O Windows lê `HELLO:1:<nonce-hex>` na característica de controle.
3. O Windows envia `AUTH:<hmac-hex>`.
4. A prova é `HMAC-SHA256(chave = código UTF-8, mensagem = nonce-hex UTF-8)`.
5. O Windows lê `AUTH_OK` ou `AUTH_FAILED`.

O código autentica a sessão próxima, mas não substitui o pareamento do sistema nem a criptografia do backup.

## Android para Windows

1. Windows envia `PULL`.
2. Android divide o backup UTF-8 em blocos de 16 bytes.
3. Cada notificação contém quatro bytes de cabeçalho: sequência de 16 bits, total de blocos de 16 bits e payload.
4. Android envia `0x7F` ao concluir.

## Windows para Android

1. Windows envia `PUSH:<quantidade-de-bytes>`.
2. Windows escreve blocos de até 16 bytes na característica de dados.
3. Android importa somente quando recebe a quantidade declarada.
4. Depois da importação, o app Android bloqueia o cofre e exige a senha mestre.

## Decisões

- Blocos de 16 bytes mantêm cada pacote dentro do payload BLE mínimo de 20 bytes.
- O protocolo nunca transporta a senha mestre nem dados descriptografados.
- A sincronização inicial é manual e substitutiva para evitar merge silencioso e perda não detectada.

