// 화면 크기별 기능 + 가로 스크롤 자동 점검
// 백엔드 CORS가 localhost:5500을 허용하므로 포트는 5500으로 고정한다
import { chromium } from "playwright";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "../frontend");
const BASE = "http://localhost:5500";
const VIEWPORTS = [
  { name: "375", width: 375, height: 812 },
  { name: "768", width: 768, height: 1024 },
  { name: "1280", width: 1280, height: 900 },
];
const PAGES = ["index.html", "items.html", "items.html?q=bottle", "result.html", "district.html",
  "district-guide.html", "dong-rules.html", "photo.html", "confirm.html", "login.html", "mypage.html"];
const TYPES = { ".html": "text/html", ".css": "text/css", ".js": "text/javascript", ".svg": "image/svg+xml", ".png": "image/png" };

const server = http.createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, BASE).pathname);
  if (p === "/") p = "/index.html";
  // 사이트에 favicon이 없어 브라우저 기본 요청이 404로 잡히므로 테스트에서는 빈 응답으로 넘긴다
  if (p === "/favicon.ico") { res.writeHead(204); return res.end(); }
  const f = path.join(ROOT, p);
  if (!f.startsWith(ROOT) || !fs.existsSync(f)) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { "Content-Type": TYPES[path.extname(f)] || "application/octet-stream" });
  fs.createReadStream(f).pipe(res);
}).listen(5500);

const SHOTS = path.join(HERE, "shots");
fs.mkdirSync(SHOTS, { recursive: true });
// PW_CHANNEL=msedge 또는 chrome을 주면 설치된 브라우저를 쓴다. 없으면 Playwright 기본 Chromium
const browser = await chromium.launch(process.env.PW_CHANNEL ? { channel: process.env.PW_CHANNEL } : {});
const report = [];

function watch(page) {
  const issues = [];
  page.on("console", (m) => m.type() === "error" && issues.push("console: " + m.text().slice(0, 160)));
  page.on("pageerror", (e) => issues.push("js error: " + e.message.slice(0, 160)));
  page.on("response", (r) => r.status() >= 400 && issues.push(`http ${r.status()}: ${r.url()}`));
  page.on("requestfailed", (r) => r.url().includes("/api/") && issues.push(`api failed: ${r.url()} ${r.failure()?.errorText}`));
  return issues;
}

async function overflow(page) {
  return page.evaluate(() => {
    const vw = document.documentElement.clientWidth;
    const sw = document.documentElement.scrollWidth;
    const culprits = [];
    if (sw > vw) {
      for (const el of document.querySelectorAll("body *")) {
        const r = el.getBoundingClientRect();
        if (r.right > vw + 1 && r.width > 0) {
          const id = el.tagName.toLowerCase() + (el.id ? "#" + el.id : "") + (el.className && typeof el.className === "string" ? "." + el.className.trim().split(/\s+/).join(".") : "");
          culprits.push(`${id} (right=${Math.round(r.right)})`);
          if (culprits.length >= 5) break;
        }
      }
    }
    return { vw, sw, culprits };
  });
}

for (const vp of VIEWPORTS) {
  const ctx = await browser.newContext({ viewport: { width: vp.width, height: vp.height } });
  for (const p of PAGES) {
    const page = await ctx.newPage();
    const issues = watch(page);
    await page.goto(`${BASE}/${p}`, { waitUntil: "networkidle", timeout: 90000 }).catch((e) => issues.push("load: " + e.message));
    const o = await overflow(page);
    if (o.sw > o.vw) issues.push(`가로 스크롤 ${o.sw}px > ${o.vw}px: ${o.culprits.join(", ")}`);
    await page.screenshot({ path: `${SHOTS}/${vp.name}_${p.replace(/[?=]/g, "_")}.png`, fullPage: true });
    report.push({ vp: vp.name, test: p, issues });
    await page.close();
  }

  // 흐름: 홈 검색 → 결과 목록 → 품목 클릭 → 결과 페이지
  const page = await ctx.newPage();
  const issues = watch(page);
  try {
    await page.goto(`${BASE}/index.html`, { waitUntil: "networkidle", timeout: 90000 });
    await page.fill('input[name="q"]', "bottle");
    await page.press('input[name="q"]', "Enter");
    await page.waitForURL(/items\.html\?q=bottle/);
    await page.waitForSelector("#item-grid > *", { timeout: 30000 });
    const n = await page.locator("#item-grid > *").count();
    await page.locator("#item-grid > *").first().click();
    await page.waitForURL(/result\.html/, { timeout: 15000 });
    await page.waitForLoadState("networkidle");
    const name = (await page.textContent("#result-item"))?.trim();
    if (!name || name === "Item name") issues.push("결과 페이지에 품목명이 안 채워짐");
    issues.push(`(info) 검색 결과 ${n}개, 품목: ${name}`);
  } catch (e) { issues.push("흐름 실패: " + e.message.split("\n")[0]); }
  report.push({ vp: vp.name, test: "FLOW 검색→결과", issues });
  await page.close();
  await ctx.close();
}

await browser.close();
server.close();
fs.writeFileSync(path.join(HERE, "report.json"), JSON.stringify(report, null, 2));
for (const r of report) {
  const bad = r.issues.filter((i) => !i.startsWith("(info)"));
  console.log(`${bad.length ? "FAIL" : "PASS"} [${r.vp}] ${r.test}`);
  for (const i of r.issues) console.log("     - " + i);
}
const failed = report.filter((r) => r.issues.some((i) => !i.startsWith("(info)"))).length;
console.log(`
${report.length - failed}/${report.length} passed`);
process.exitCode = failed ? 1 : 0;
