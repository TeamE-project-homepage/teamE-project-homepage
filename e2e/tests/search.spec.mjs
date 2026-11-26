import { test, expect } from "@playwright/test";
import { collectErrors, findOverflow } from "./helpers.mjs";

test("search from home, open an item and see its name on the result page", async ({ page }) => {
  const errors = collectErrors(page);

  await page.goto("index.html", { waitUntil: "networkidle" });
  await page.getByRole("searchbox", { name: "Search items in English" }).fill("bottle");
  await page.keyboard.press("Enter");

  await expect(page).toHaveURL(/items\.html\?q=bottle/);
  const items = page.locator("#item-grid > *");
  await expect(items.first()).toBeVisible();
  expect(await findOverflow(page), "overflow on the search results").toEqual([]);

  await items.first().click();

  await expect(page).toHaveURL(/result\.html\?item=\d+/);
  const itemName = page.locator("#result-item");
  await expect(itemName).not.toHaveText(/^(Item name|Loading\.\.\.|Item not found|Could not load the item)$/);
  expect(await findOverflow(page), "overflow on the result page").toEqual([]);

  expect(errors, "errors during the flow").toEqual([]);
});

test("searching for an unknown item shows the empty state", async ({ page }) => {
  const errors = collectErrors(page);

  await page.goto("items.html?q=zzzzqqqq", { waitUntil: "networkidle" });

  await expect(page.locator("#items-empty")).toBeVisible();
  expect(await findOverflow(page), "overflow on the empty state").toEqual([]);
  expect(errors, "errors on the empty state").toEqual([]);
});
