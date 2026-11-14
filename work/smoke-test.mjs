import { chromium } from "file:///C:/Users/pheli/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.60.0/node_modules/playwright/index.mjs";

const url = "http://127.0.0.1:4173/index.html";
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 390, height: 844 } });

await page.goto(url);
await page.evaluate(() => localStorage.clear());
await page.reload();

await page.fill("#master-password", "SenhaMestre123!");
await page.fill("#confirm-password", "SenhaMestre123!");
await page.click("button.primary-button");
await page.click("#new-vault-button");
await page.fill("input[name='name']", "Redes sociais com nome muito longo para testar card fixo");
await page.click(".modal button[type='submit']");

await page.click("#new-vault-button");
await page.fill("input[name='name']", "Banco");
await page.click(".modal button[type='submit']");

const cardHeights = await page.locator(".vault-card").evaluateAll((cards) =>
  cards.map((card) => Math.round(card.getBoundingClientRect().height))
);
if (new Set(cardHeights).size !== 1 || cardHeights[0] !== 148) {
  throw new Error(`Altura inesperada dos cards: ${cardHeights.join(", ")}`);
}

await page.locator(".vault-card").first().locator(".card-main").click();
await page.click("#new-secret-button");
await page.fill("input[name='title']", "Instagram");
await page.fill("input[name='login']", "usuario@example.com");
await page.fill("input[name='password']", "senha-secreta");
await page.fill("textarea[name='notes']", "Observacao privada");
await page.fill("input[name='customLabel']", "2FA");
await page.fill("input[name='customValue']", "123456");
await page.click(".modal button[type='submit']");

const firstVaultSecrets = await page.locator(".secret-card").count();
if (firstVaultSecrets !== 1) {
  throw new Error(`Esperava 1 segredo no primeiro cofre, recebeu ${firstVaultSecrets}`);
}

await page.click("#back-button");
await page.locator(".vault-card").nth(1).locator(".card-main").click();
const secondVaultEmpty = await page.locator(".empty-state").textContent();
if (!secondVaultEmpty.includes("Nenhum segredo")) {
  throw new Error("O segundo cofre recebeu dados do primeiro cofre.");
}

await page.click("#detail-lock-button");
await page.fill("#master-password", "SenhaMestre123!");
await page.click("button.primary-button");
const unlockedTitle = await page.locator("h1").first().textContent();
if (!unlockedTitle.includes("Cifra")) {
  throw new Error("Falha ao desbloquear o cofre apos logout.");
}

await browser.close();
console.log("smoke-test ok");
