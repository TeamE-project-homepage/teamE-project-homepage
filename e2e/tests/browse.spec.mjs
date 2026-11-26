import { test, expect } from "@playwright/test";
import { collectErrors, findOverflow } from "./helpers.mjs";

test("home shows categories from the server and a category opens its items", async ({ page }) => {
  const errors = collectErrors(page);

  await page.goto("index.html", { waitUntil: "networkidle" });

  const cards = page.locator("#category-grid > a.card");
  await expect(cards.first()).toBeVisible();
  const categoryName = (await cards.first().locator(".card-title").textContent()).trim();

  await cards.first().click();

  await expect(page).toHaveURL(/items\.html\?category=/);
  await expect(page.locator("#items-title")).toHaveText(categoryName);
  await expect(page.locator("#item-grid > *").first()).toBeVisible();
  expect(await findOverflow(page), "overflow on the category items").toEqual([]);

  expect(errors, "errors during the flow").toEqual([]);
});

test("district page lists all 25 Seoul districts and saves the selected one", async ({ page }) => {
  const errors = collectErrors(page);

  await page.goto("district.html", { waitUntil: "networkidle" });

  const rows = page.locator("#district-list > li");
  await expect(rows).toHaveCount(25);
  expect(await findOverflow(page), "overflow on the district list").toEqual([]);

  const first = rows.first();
  const districtName = (await first.locator(".grow").textContent()).trim();
  await first.getByRole("button", { name: "Select" }).click();

  // 선택하면 홈으로 돌아가고, 상단 바에 고른 자치구가 보여야 한다
  await expect(page).toHaveURL(/index\.html/);
  await expect(page.locator(".district-bar span")).toHaveText(`District: ${districtName}`);
  // 홈의 카테고리 요청이 끝나기 전에 이동하면 요청이 끊겨 에러로 잡히므로 기다린다
  await page.waitForLoadState("networkidle");

  // 다시 들어가면 저장된 자치구가 현재 값으로 보여야 한다
  await page.goto("district.html", { waitUntil: "networkidle" });
  await expect(page.locator("#district-status")).toHaveText(`Current district: ${districtName}`);

  expect(errors, "errors during the flow").toEqual([]);
});
