const STORAGE_KEY = "cofre-segredos-v1";
const DEFAULT_LOCK_TIMEOUT_MS = 300000;
const PBKDF2_ITERATIONS = 210000;

const state = {
  mode: "loading",
  key: null,
  data: null,
  activeVaultId: null,
  lockTimer: null,
};

const emptyData = () => ({
  version: 1,
  settings: {
    lockTimeoutMs: DEFAULT_LOCK_TIMEOUT_MS,
  },
  vaults: [],
});

const $ = (selector, root = document) => root.querySelector(selector);

function getStoredEnvelope() {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}

function setStoredEnvelope(envelope) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(envelope));
}

function bytesToBase64(bytes) {
  const view = new Uint8Array(bytes);
  let binary = "";
  for (let index = 0; index < view.length; index += 0x8000) {
    binary += String.fromCharCode(...view.subarray(index, index + 0x8000));
  }
  return btoa(binary);
}

function base64ToBytes(value) {
  return Uint8Array.from(atob(value), (char) => char.charCodeAt(0));
}

async function deriveKey(password, salt) {
  const material = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveKey"]
  );

  return crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations: PBKDF2_ITERATIONS,
      hash: "SHA-256",
    },
    material,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  );
}

async function encryptJson(key, data) {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encoded = new TextEncoder().encode(JSON.stringify(data));
  const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, encoded);
  return {
    iv: bytesToBase64(iv),
    ciphertext: bytesToBase64(ciphertext),
  };
}

async function decryptJson(key, payload) {
  const plaintext = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: base64ToBytes(payload.iv) },
    key,
    base64ToBytes(payload.ciphertext)
  );
  return JSON.parse(new TextDecoder().decode(plaintext));
}

async function createVaultStore(password) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const key = await deriveKey(password, salt);
  const data = emptyData();
  const verifier = await encryptJson(key, { ok: true });
  const vaultData = await encryptJson(key, data);
  setStoredEnvelope({
    version: 1,
    kdf: "PBKDF2-SHA256",
    iterations: PBKDF2_ITERATIONS,
    salt: bytesToBase64(salt),
    verifier,
    vaultData,
  });
  state.key = key;
  state.data = data;
}

async function unlockStore(password) {
  const envelope = getStoredEnvelope();
  const key = await deriveKey(password, base64ToBytes(envelope.salt));
  await decryptJson(key, envelope.verifier);
  state.data = await decryptJson(key, envelope.vaultData);
  state.key = key;
}

async function persistData() {
  const envelope = getStoredEnvelope();
  envelope.vaultData = await encryptJson(state.key, state.data);
  setStoredEnvelope(envelope);
}

function resetLockTimer() {
  clearTimeout(state.lockTimer);
  if (!state.key || !state.data) return;
  state.lockTimer = setTimeout(lockApp, state.data.settings.lockTimeoutMs);
}

function lockApp() {
  clearTimeout(state.lockTimer);
  state.key = null;
  state.data = null;
  state.activeVaultId = null;
  renderAuth();
}

function wireActivityTracking() {
  ["pointerdown", "keydown", "touchstart", "visibilitychange"].forEach((eventName) => {
    document.addEventListener(eventName, () => {
      if (document.visibilityState === "hidden") return;
      resetLockTimer();
    }, { passive: true });
  });
}

function cloneTemplate(id) {
  return document.importNode($(id).content, true);
}

function renderAuth() {
  const envelope = getStoredEnvelope();
  const isSetup = !envelope;
  const view = cloneTemplate("#auth-template");
  const form = $("#auth-form", view);
  const subtitle = $("#auth-subtitle", view);
  const confirmGroup = $("#confirm-group", view);
  const submit = $(".primary-button", view);
  const error = $("#auth-error", view);

  subtitle.textContent = isSetup
    ? "Crie uma senha mestre forte para proteger seus dados locais."
    : "Digite sua senha mestre para abrir o cofre.";
  submit.textContent = isSetup ? "Criar senha e entrar" : "Entrar";
  confirmGroup.hidden = !isSetup;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    error.textContent = "";
    const password = $("#master-password", form).value;
    const confirmation = $("#confirm-password", form)?.value;

    try {
      if (isSetup) {
        if (password.length < 8) throw new Error("Use pelo menos 8 caracteres.");
        if (password !== confirmation) throw new Error("As senhas não conferem.");
        await createVaultStore(password);
      } else {
        await unlockStore(password);
      }
      renderVaultList();
      resetLockTimer();
    } catch (err) {
      error.textContent = isSetup ? err.message : "Senha mestre inválida.";
    }
  });

  $("#app").replaceChildren(view);
}

function renderVaultList() {
  const view = cloneTemplate("#vault-template");
  const list = $("#vault-list", view);
  const settings = $("#settings-panel", view);
  const timeout = $("#lock-timeout", view);

  timeout.value = String(state.data.settings.lockTimeoutMs);
  timeout.addEventListener("change", async () => {
    state.data.settings.lockTimeoutMs = Number(timeout.value);
    await persistData();
    resetLockTimer();
  });

  $("#settings-button", view).addEventListener("click", () => {
    settings.hidden = !settings.hidden;
  });
  $("#lock-button", view).addEventListener("click", lockApp);
  $("#new-vault-button", view).addEventListener("click", () => openVaultModal());

  if (state.data.vaults.length === 0) {
    list.innerHTML = '<p class="empty-state">Nenhum cofre criado ainda.</p>';
  } else {
    state.data.vaults.forEach((vault) => list.appendChild(renderVaultCard(vault)));
  }

  $("#app").replaceChildren(view);
}

function renderVaultCard(vault) {
  const fragment = cloneTemplate("#vault-card-template");
  const card = $(".vault-card", fragment);
  $(".vault-initial", card).textContent = vault.name.trim().charAt(0).toUpperCase() || "C";
  $(".vault-name", card).textContent = vault.name;
  $(".vault-count", card).textContent = `${vault.secrets.length} segredo${vault.secrets.length === 1 ? "" : "s"}`;
  $(".card-main", card).addEventListener("click", () => {
    state.activeVaultId = vault.id;
    renderVaultDetail();
  });
  $(".card-delete", card).addEventListener("click", async () => {
    if (!confirm(`Excluir o cofre "${vault.name}" e todos os seus segredos?`)) return;
    state.data.vaults = state.data.vaults.filter((item) => item.id !== vault.id);
    await persistData();
    renderVaultList();
  });
  return fragment;
}

function renderVaultDetail() {
  const vault = getActiveVault();
  if (!vault) {
    renderVaultList();
    return;
  }

  const view = cloneTemplate("#vault-detail-template");
  $("#detail-vault-name", view).textContent = vault.name;
  $("#back-button", view).addEventListener("click", renderVaultList);
  $("#detail-lock-button", view).addEventListener("click", lockApp);
  $("#new-secret-button", view).addEventListener("click", () => openSecretModal(vault.id));

  const list = $("#secret-list", view);
  if (vault.secrets.length === 0) {
    list.innerHTML = '<p class="empty-state">Nenhum segredo neste cofre.</p>';
  } else {
    vault.secrets.forEach((secret) => list.appendChild(renderSecretCard(vault.id, secret)));
  }

  $("#app").replaceChildren(view);
}

function renderSecretCard(vaultId, secret) {
  const fragment = cloneTemplate("#secret-card-template");
  const card = $(".secret-card", fragment);
  $(".secret-title", card).textContent = secret.title;
  $(".secret-user", card).textContent = secret.login || "Sem login informado";
  $(".secret-delete", card).addEventListener("click", async () => {
    if (!confirm(`Excluir o segredo "${secret.title}"?`)) return;
    const vault = state.data.vaults.find((item) => item.id === vaultId);
    vault.secrets = vault.secrets.filter((item) => item.id !== secret.id);
    await persistData();
    renderVaultDetail();
  });

  const fields = [
    ["Login", secret.login],
    ["Senha", secret.password],
    ["Observação", secret.notes],
    [secret.customLabel || "Campo personalizado", secret.customValue],
  ];

  const list = $(".secret-fields", card);
  fields.forEach(([label, value]) => {
    const row = document.createElement("div");
    row.className = "secret-field";
    row.innerHTML = `
      <div>
        <dt></dt>
        <dd></dd>
      </div>
      <button class="copy-button" type="button">Copiar</button>
    `;
    $("dt", row).textContent = label;
    $("dd", row).textContent = value ? maskIfPassword(label, value) : "Vazio";
    $(".copy-button", row).disabled = !value;
    $(".copy-button", row).addEventListener("click", () => copyValue(value, $(".copy-button", row)));
    list.appendChild(row);
  });

  return fragment;
}

function maskIfPassword(label, value) {
  return label.toLowerCase().includes("senha") ? "••••••••" : value;
}

async function copyValue(value, button) {
  await navigator.clipboard.writeText(value);
  const original = button.textContent;
  button.textContent = "Copiado";
  setTimeout(() => {
    button.textContent = original;
  }, 1200);
}

function getActiveVault() {
  return state.data.vaults.find((vault) => vault.id === state.activeVaultId);
}

function openVaultModal() {
  const modal = buildModal("Novo cofre", `
    <label>
      <span>Nome do cofre</span>
      <input name="name" required maxlength="80" autocomplete="off">
    </label>
  `);

  modal.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(modal.form);
    state.data.vaults.push({
      id: crypto.randomUUID(),
      name: data.get("name").trim(),
      secrets: [],
      createdAt: new Date().toISOString(),
    });
    await persistData();
    modal.close();
    renderVaultList();
  });
}

function openSecretModal(vaultId) {
  const modal = buildModal("Novo segredo", `
    <label>
      <span>Título</span>
      <input name="title" required maxlength="120" autocomplete="off">
    </label>
    <label>
      <span>Usuário/Login</span>
      <input name="login" autocomplete="off">
    </label>
    <label>
      <span>Senha</span>
      <input name="password" type="password" autocomplete="new-password">
    </label>
    <label>
      <span>Observações</span>
      <textarea name="notes"></textarea>
    </label>
    <label>
      <span>Nome do campo adicional</span>
      <input name="customLabel" placeholder="Ex.: PIN, chave, código">
    </label>
    <label>
      <span>Valor do campo adicional</span>
      <input name="customValue" autocomplete="off">
    </label>
  `);

  modal.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = new FormData(modal.form);
    const vault = state.data.vaults.find((item) => item.id === vaultId);
    vault.secrets.push({
      id: crypto.randomUUID(),
      title: data.get("title").trim(),
      login: data.get("login").trim(),
      password: data.get("password"),
      notes: data.get("notes").trim(),
      customLabel: data.get("customLabel").trim(),
      customValue: data.get("customValue").trim(),
      createdAt: new Date().toISOString(),
    });
    await persistData();
    modal.close();
    renderVaultDetail();
  });
}

function buildModal(title, fieldsHtml) {
  const fragment = cloneTemplate("#modal-template");
  const backdrop = $(".modal-backdrop", fragment);
  const form = $(".modal", fragment);
  form.innerHTML = `
    <h2>${title}</h2>
    ${fieldsHtml}
    <div class="modal-actions">
      <button class="secondary-button" type="button" data-cancel>Cancelar</button>
      <button class="primary-button" type="submit">Salvar</button>
    </div>
  `;
  const close = () => backdrop.remove();
  $("[data-cancel]", form).addEventListener("click", close);
  backdrop.addEventListener("click", (event) => {
    if (event.target === backdrop) close();
  });
  document.body.appendChild(fragment);
  $("input, textarea, select", form)?.focus();
  return { form, close };
}

function assertCryptoSupport() {
  if (!window.crypto?.subtle) {
    $("#app").innerHTML = '<p class="empty-state">Este navegador não oferece Web Crypto em contexto seguro. Abra pelo localhost ou HTTPS.</p>';
    return false;
  }
  return true;
}

function boot() {
  if (!assertCryptoSupport()) return;
  wireActivityTracking();
  renderAuth();
}

boot();
