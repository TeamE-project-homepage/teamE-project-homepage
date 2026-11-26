// 페이지에서 생긴 에러(HTTP 4xx/5xx, 요청 실패, JS 에러, 콘솔 에러)를 모은다
export function collectErrors(page) {
  const errors = [];
  page.on("response", (r) => {
    if (r.status() >= 400) errors.push(`HTTP ${r.status()} ${r.url()}`);
  });
  page.on("requestfailed", (r) => errors.push(`Request failed ${r.url()} (${r.failure()?.errorText})`));
  page.on("pageerror", (e) => errors.push(`JS error: ${e.message}`));
  page.on("console", (m) => {
    if (m.type() === "error") errors.push(`Console error: ${m.text()}`);
  });
  return errors;
}

// 화면 오른쪽 밖으로 넘친 요소를 찾는다. 없으면 빈 배열
export function findOverflow(page) {
  return page.evaluate(() => {
    const vw = document.documentElement.clientWidth;
    if (document.documentElement.scrollWidth <= vw) return [];
    const found = [];
    for (const el of document.querySelectorAll("body *")) {
      const r = el.getBoundingClientRect();
      if (r.width > 0 && r.right > vw + 1) {
        const cls = typeof el.className === "string" && el.className.trim() ? "." + el.className.trim().split(/\s+/).join(".") : "";
        found.push(`${el.tagName.toLowerCase()}${el.id ? "#" + el.id : ""}${cls} (right ${Math.round(r.right)}px > ${vw}px)`);
        if (found.length >= 5) break;
      }
    }
    return found;
  });
}
