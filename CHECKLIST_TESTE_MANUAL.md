# CHECKLIST_TESTE_MANUAL

## Preparacao

- Instalar o APK mais recente da pasta `outputs`.
- Se quiser testar do zero, limpar dados do app no Android antes de abrir.
- Criar uma senha mestre forte e anotar apenas para teste.
- Criar pelo menos um cofre e um segredo antes de testar backup.

## Biometria

- Abrir o app e desbloquear com senha mestre.
- Tocar em `Ativar biometria`.
- Confirmar ativacao.
- Bloquear o app pelo botao `Bloquear`.
- Tocar em `Entrar com biometria`.
- Validar desbloqueio com digital/face.
- Cancelar a biometria e confirmar que ainda e possivel entrar com senha mestre.
- Reiniciar o app e repetir o desbloqueio biometrico.

## Exportacao para Downloads

- Entrar no app.
- Tocar em `Exportar backup`.
- Escolher a pasta Downloads.
- Salvar como `cofre-de-segredos.cofrebackup`.
- Confirmar mensagem de sucesso.
- Abrir o arquivo em um visualizador de texto e confirmar que nao ha senhas em texto puro.

## Exportacao para Google Drive

- Entrar no app.
- Tocar em `Exportar backup`.
- Escolher Google Drive no seletor.
- Salvar o arquivo.
- Confirmar mensagem de sucesso.
- Conferir se o arquivo aparece no Drive.

## Importacao de backup valido

- Exportar um backup valido primeiro.
- Alterar algum dado no app, por exemplo nome de cofre ou segredo.
- Tocar em `Importar backup`.
- Selecionar o arquivo `.cofrebackup`.
- Confirmar importacao.
- Verificar que o app bloqueia e pede senha mestre.
- Entrar com a senha mestre do backup.
- Conferir se os dados restaurados batem com o backup.

## Importacao de arquivo invalido

- Criar ou selecionar um arquivo que nao seja backup do app.
- Tocar em `Importar backup`.
- Selecionar esse arquivo.
- Confirmar que o app mostra erro amigavel.
- Confirmar que os dados atuais nao foram corrompidos.

## App em segundo plano

- Entrar no cofre.
- Enviar o app para segundo plano.
- Aguardar alguns minutos.
- Voltar ao app.
- Confirmar se o bloqueio automatico acontece apos o tempo configurado.

## Multitarefa

- Entrar no cofre e abrir um segredo.
- Abrir a tela de apps recentes.
- Verificar se informacoes sensiveis aparecem no preview.
- Resultado esperado: preview deve ficar bloqueado/oculto pelo Android por causa de `FLAG_SECURE`.
- Registrar se algum dado sensivel ainda aparecer.

## Screenshots / Preview

- Entrar no cofre e abrir um segredo.
- Tentar tirar screenshot.
- Registrar se o Android permitiu.
- Resultado esperado: Android deve bloquear screenshot/gravação de tela por causa de `FLAG_SECURE`.

## Clipboard

- Copiar login.
- Colar em um campo de texto seguro para conferir conteudo.
- Copiar senha.
- Colar em um campo de texto seguro para conferir conteudo.
- Aguardar 30 segundos.
- Tentar colar novamente.
- Resultado esperado: o clipboard deve ter sido limpo se ainda continha o valor copiado pelo app.

## Revelar/Ocultar senha

- Abrir um segredo.
- Confirmar que senha aparece como `********`.
- Tocar em `Mostrar`.
- Confirmar que senha fica visivel.
- Tocar em `Ocultar`.
- Confirmar que senha volta a ficar oculta.
- Fechar e reabrir o segredo/cofre.
- Confirmar que senha inicia oculta.

## Busca

- Criar dois segredos no mesmo cofre.
- Buscar por titulo.
- Buscar por login.
- Buscar por observacao.
- Buscar pelo campo personalizado.
- Confirmar que a lista filtra em tempo real.
