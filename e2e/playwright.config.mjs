import { defineConfig } from "@playwright/test";

// PW_CHANNEL=msedge 또는 chrome을 주면 설치된 브라우저를 쓴다. 없으면 Playwright 기본 Chromium
const channel = process.env.PW_CHANNEL || undefined;

export default defineConfig({
  testDir: "./tests",
  // Render 무료 서버가 잠들어 있으면 첫 요청이 오래 걸린다
  timeout: 90_000,
  expect: { timeout: 30_000 },
  retries: 1,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "http://localhost:5500",
    channel,
    trace: "retain-on-failure",
  },
  // 과제 기준 화면 크기: 모바일 375px, 태블릿 768px, 데스크톱 1024px 이상
  projects: [
    { name: "mobile-375", use: { viewport: { width: 375, height: 812 } } },
    { name: "tablet-768", use: { viewport: { width: 768, height: 1024 } } },
    { name: "desktop-1280", use: { viewport: { width: 1280, height: 900 } } },
  ],
  webServer: {
    command: "node serve.mjs",
    url: "http://localhost:5500/index.html",
    reuseExistingServer: true,
  },
});
