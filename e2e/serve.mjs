// frontend/를 정적 파일로 띄운다. 백엔드 CORS가 localhost:5500을 허용하므로 포트는 5500으로 고정한다
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../frontend");
const PORT = 5500;
const TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css",
  ".js": "text/javascript",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
};

http
  .createServer((req, res) => {
    let p = decodeURIComponent(new URL(req.url, `http://localhost:${PORT}`).pathname);
    if (p === "/") p = "/index.html";
    // 사이트에 favicon이 없어 브라우저 기본 요청이 404로 잡히므로 빈 응답으로 넘긴다
    if (p === "/favicon.ico") {
      res.writeHead(204);
      return res.end();
    }
    const file = path.join(ROOT, p);
    if (!file.startsWith(ROOT) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
      res.writeHead(404);
      return res.end();
    }
    res.writeHead(200, { "Content-Type": TYPES[path.extname(file)] || "application/octet-stream" });
    fs.createReadStream(file).pipe(res);
  })
  .listen(PORT, () => console.log(`Serving frontend on http://localhost:${PORT}`));
