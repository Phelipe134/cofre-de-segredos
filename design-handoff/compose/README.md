# Compose Theme — Cifra

Arquivos para colar em `app/src/main/java/com/example/cofredesegredos/ui/theme/`.
Renomeie o package se o seu app usar outro.

## 1. Gradle

```kotlin
dependencies {
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.0")
}
```

## 2. Provedor de fontes (res/values/font_certs.xml)

Crie o arquivo `app/src/main/res/values/font_certs.xml` com o conteúdo
oficial dos certificados do Google Fonts no Android — copie de:
https://developer.android.com/develop/ui/compose/text/fonts#downloadable-fonts

Sem isso, `Font(googleFont=..., fontProvider=provider, ...)` falha em runtime.

## 3. Aplicar

```kotlin
setContent {
    CofreDeSegredosTheme {
        // sua NavHost aqui
    }
}
```

## 4. Como usar os tokens

- **Cor primária** (texto, botão preto): `MaterialTheme.colorScheme.primary`
- **Sotaque dourado**: `Gold` (importe direto de `ui.theme.Color`)
- **Card do segredo**: fundo = `MaterialTheme.colorScheme.primary`, texto = `onPrimary`
- **Cantos do cofre**: `MaterialTheme.shapes.extraLarge` (32dp)
- **Espaçamento horizontal de tela**: `Sizes.screenPadding` (24dp)
- **Cor por cofre**: `vaultToneByKey("amber").bg` / `.fg`

## 5. Eyebrow (texto pequeno com tracking)

```kotlin
Text(
    "MEUS COFRES",
    style = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 2.2.sp,
        color = Gold
    )
)
```

## 6. Card do segredo (referência)

```kotlin
Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.primary,
    shadowElevation = 20.dp,
    modifier = Modifier.fillMaxWidth()
) {
    Column(Modifier.padding(28.dp)) { /* login, divisor, senha, botões */ }
}
```
