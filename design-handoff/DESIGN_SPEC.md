# Cofre de Segredos — Design Spec (Premium Orgânico)

Documento de referência para o app Android (Kotlin / Jetpack Compose).
A meta é manter o mesmo "feel" do protótipo web: premium, calmo, orgânico,
com tipografia gentil, muito espaço em branco quente e um ouro discreto
como sotaque de marca.

---

## 1. Filosofia

- **Simples mesmo com 100+ segredos.** Lista vertical, hierarquia clara.
- **Tudo natural e óbvio.** Ações primárias sempre visíveis; nunca esconder
  "Novo cofre" ou "Novo segredo" atrás de menus.
- **Premium e orgânico.** Linho quente como fundo (não branco puro), ouro
  como sotaque, sombras suaves, cantos generosos (16–32dp).
- **Senha primeiro.** A tela do segredo é um cartão preto com a senha em
  destaque — gesto de "cofre aberto".

---

## 2. Paleta de cores

Todos os tokens são semânticos. Use o nome, nunca o hex direto.

### Light (default)

| Token                  | Hex       | Uso |
|------------------------|-----------|-----|
| `background`           | `#F8F7F4` | Fundo da tela (linho quente) |
| `surface`              | `#FFFFFF` | Cards, dialogs, inputs |
| `surface_translucent`  | `#FFFFFFD9` | Bottom nav flutuante (85% opacidade + blur) |
| `foreground` / `ink`   | `#1A1C1E` | Texto principal, botão primário, cartão do segredo |
| `foreground_on_dark`   | `#F8F7F4` | Texto sobre `foreground` |
| `muted`                | `#EEEAE3` | Avatares de iniciais, chips neutros |
| `muted_foreground`     | `#76757A` | Texto secundário, legendas |
| `border`               | `#E9E4DE` | Bordas hair-line (1dp) |
| `gold`                 | `#C5A059` | Sotaque de marca (eyebrow, CTA "Enviar PC") |
| `gold_soft`            | `#F1E6CB` | Fundo do ícone "shield" |
| `destructive`          | `#C8392B` | Ações destrutivas |
| `success`              | `#3A7D5C` | Confirmações (copiado!) |

### Acentos por cofre (chips de cor)

| Nome     | bg        | fg        |
|----------|-----------|-----------|
| blue     | `#E8F1FE` | `#2563EB` |
| purple   | `#F3ECFE` | `#7C3AED` |
| amber    | `#FEF3C7` | `#D97706` |
| emerald  | `#E6F8EE` | `#059669` |
| rose     | `#FEE7EC` | `#E11D48` |
| teal     | `#DEF7F4` | `#0D9488` |

> Dark mode: invertemos `background` → `#101013`, `surface` → `#1A1B1F`,
> `foreground` → `#F4F2EE`, `gold` mantém. Bordas viram `#FFFFFF14`.

---

## 3. Tipografia

Duas famílias, ambas do Google Fonts:

- **Outfit** (display) — headings, números do cofre, eyebrow.
- **Inter** (body) — corpo, labels, botões.

| Estilo            | Família | Peso | Tamanho | Line-height | Tracking |
|-------------------|---------|------|---------|-------------|----------|
| Display Large     | Outfit  | 600  | 30sp    | 1.05        | -0.01em  |
| Display Medium    | Outfit  | 600  | 24sp    | 1.1         | -0.01em  |
| Title             | Outfit  | 500  | 18sp    | 1.2         | normal   |
| Body Large        | Inter   | 500  | 16sp    | 1.4         | normal   |
| Body              | Inter   | 400  | 14sp    | 1.5         | normal   |
| Label             | Inter   | 600  | 12sp    | 1.3         | normal   |
| Eyebrow           | Inter   | 600  | 10sp    | 1           | +0.22em ALL CAPS |
| Mono (senha)      | JetBrains Mono | 500 | 16sp | 1.4 | +0.04em |

---

## 4. Espaçamento, raio e elevação

### Espaçamentos (4dp grid)
`4, 8, 12, 16, 20, 24, 32, 40, 56`. Padding horizontal padrão de tela: **24dp**.

### Raio de cantos
| Token         | dp  | Uso |
|---------------|-----|-----|
| `radius_sm`   | 12  | Botões pequenos, chips |
| `radius_md`   | 16  | Inputs, botões primários |
| `radius_lg`   | 20  | Cards de seção |
| `radius_xl`   | 24  | Containers grandes |
| `radius_2xl`  | 32  | Card do cofre, card do segredo |
| `radius_full` | ∞   | Avatares, FAB, bottom nav |

### Sombras
- **Card**: `0 1 2 rgba(0,0,0,0.04)` + `0 4 12 rgba(0,0,0,0.04)`
- **Bottom nav**: `0 8 30 rgba(26,28,30,0.18)` (blur 30, y=8)
- **Cartão de segredo (escuro)**: `0 20 60 rgba(26,28,30,0.45)` (forte, dramático)

---

## 5. Componentes

### 5.1 Card de Cofre (grid 2 colunas, quadrado)
- Fundo `surface`, raio `2xl` (32dp), padding 20dp.
- Top-left: avatar 48dp com cor de acento (`bg`/`fg`), raio 16dp.
- Bottom: nome em Title, contador em Body+muted_foreground.
- Active scale 0.98.

### 5.2 Item de Segredo (lista vertical, divisores hair-line)
- Altura ~64dp, padding 16dp.
- Avatar 40dp com iniciais (bg=`muted`, fg=`muted_foreground`).
- Right: chevron 16dp em `muted_foreground`.

### 5.3 Cartão do Segredo (tela de detalhe)
- Fundo `foreground` (#1A1C1E), texto sobre escuro.
- Raio 32dp, padding 28dp, sombra dramática.
- Seções "Login" / "Senha" com divisor 1dp em `#FFFFFF1A`.
- Senha: mono, com botão olho (mostrar/ocultar) + copiar.
- Footer: 2 botões em grid — "Copiar senha" (translúcido) e
  "Enviar ao PC" (gold, peso 600).

### 5.4 Bottom Nav (flutuante)
- 90% de largura, centralizada, 24dp acima da borda.
- Container: `surface_translucent` + blur, raio `full`, padding 8dp.
- 5 slots: Cofres · Buscar · **+** (FAB preto 48dp) · Configurações · Lixeira.
- Item ativo: fundo `foreground`, ícone em `background`.

### 5.5 Dialog
- Raio 24dp, padding 24dp, fundo `surface`, sombra forte.
- Título em Outfit 20sp/600.
- Botão primário ocupa largura total, altura 48dp.

### 5.6 Input
- Altura 48dp, raio 16dp, fundo `surface`, borda `border`.
- Foco: borda `foreground/30`, sem outline.
- Label acima (Eyebrow style).

### 5.7 Switch
- Material 3 padrão, mas com track on = `foreground`, thumb branco.

---

## 6. Fluxos e telas

1. **Home (Cofres)** — grid 2 colunas + busca falsa + ícone de escudo.
2. **Detalhe do Cofre** — header com ícone grande + nome + descrição + lista.
3. **Detalhe do Segredo** — cartão preto + extras colapsáveis.
4. **Buscar** — input centrado + lista de resultados.
5. **Lixeira** — duas seções (Cofres / Segredos) com botões Restaurar e Apagar.
6. **Configurações** — Segurança (biometria, auto-lock), Backup, Sobre.

### Comportamentos chave
- **Mostrar/ocultar senha** com gesto de toque no olho.
- **Copiar** sempre mostra um check verde por 1.2s.
- **Enviar ao PC** abre dialog com QR code (240×240, fundo branco, raio 16dp)
  + botão "Copiar tudo como texto".
- **Auto-lock**: 30s / 1min / 5min / nunca (default 1min).
- **Biometria**: opcional, off por padrão (não bloquear primeiro uso).
- **Lixeira**: itens expirar em 30 dias.

### Estados vazios
Sempre: ícone circular dourado (`gold_soft` + `gold`) + título Outfit 20sp +
descrição muted curta + CTA primário pílula 44dp.

---

## 7. Animação

- Transições de tela: slide horizontal 240ms, easing `cubic-bezier(0.2,0,0,1)`.
- Botões: scale 0.98 no press, 100ms.
- Reveal de senha: fade 150ms.
- Toast/check após copiar: scale 0.9→1 com fade, 200ms.

---

## 8. Acessibilidade

- Contraste mínimo 4.5:1 em texto sobre `background` e `surface`.
- Touch target mínimo 44×44dp (botões redondos do header são 40dp — aceitar
  com padding interno gerando hit-slop de 48dp).
- `contentDescription` em todos os IconButtons.
- Suporte a tamanho de fonte do sistema (use `sp`, nunca `dp`, para texto).
