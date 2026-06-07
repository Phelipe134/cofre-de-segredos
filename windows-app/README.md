# Cofre de Segredos para Windows

Aplicativo Windows 10/11 para receber uma cópia criptografada do Cofre de Segredos por Bluetooth Low Energy.

## Como usar

1. No Android, abra `Configurações > Sincronizar com PC`.
2. Toque em `Ativar sincronização` e mantenha a tela aberta.
3. No Windows, abra `CofreDeSegredos.Windows.exe`.
4. Clique em `Procurar celular`.
5. Digite o código temporário de seis dígitos exibido no Android.
6. Clique em `Conectar e autenticar`.
7. Use `Receber do celular` para guardar no PC a cópia criptografada mais recente.
8. Informe a senha mestre e clique em `Desbloquear no PC` para consultar as credenciais.

`Enviar para o celular` substitui os dados locais do Android pela cópia criptografada existente no PC. O Android é bloqueado imediatamente após a importação para exigir novamente a senha mestre.

## Segurança

- A senha mestre não é enviada por Bluetooth.
- A transferência contém o mesmo backup AES-GCM protegido pela senha mestre.
- O pareamento da sessão exige um código temporário e uma prova HMAC-SHA256.
- A cópia persistida no Windows permanece criptografada.
- Esta compilação é de desenvolvimento e não possui assinatura digital comercial.

## Limitações atuais

- A sincronização é manual e transfere o estado completo, sem merge de alterações.
- Não edite os dois lados antes de sincronizar: a operação `Enviar para o celular` substitui o estado Android.
- O primeiro teste Bluetooth precisa ser realizado em um PC e celular reais.
- O computador precisa oferecer Bluetooth Low Energy compatível com Windows 10 versão 2004 ou superior.

