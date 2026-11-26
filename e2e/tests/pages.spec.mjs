import { test, expect } from "@playwright/test";
import { fileURLToPath } from "node:url";
import { collectErrors, findOverflow } from "./helpers.mjs";

const SHOTS = fileURLToPath(new URL("../shots/", import.meta.url));

const PAGES = [
  "index.html",
  "items.html",
  "items.html?q=bottle",
  "result.html",
  "district.html",
  "district-guide.html",
  "dong-rules.html",
  "photo.html",
  "confirm.html",
  "login.html",
  "mypage.html",
];

for (const path of PAGES) {
  test(`${path} loads without errors or horizontal scroll`, async ({ page }, testInfo) => {
    const errors = collectErrors(page);

    await page.goto(path, { waitUntil: "networkidle" });

    // Before/After 제출 자료용 전체 페이지 스크린샷
    const name = `${testInfo.project.name}_${path.replace(/[?=&]/g, "_")}.png`;
    await page.screenshot({ path: SHOTS + name, fullPage: true });

    expect(await findOverflow(page), "elements overflowing the screen").toEqual([]);
    expect(errors, "errors while loading the page").toEqual([]);
  });
}
