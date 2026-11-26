# 반응형 E2E 점검

375px · 768px · 1280px 화면에서 아래 항목을 자동으로 확인합니다.

- 모든 페이지가 API 에러(4xx/5xx), JS 에러, 콘솔 에러 없이 열리는지
- 가로 스크롤이 생기지 않는지 (생기면 화면 밖으로 넘친 요소를 함께 출력)
- 홈에서 검색 → 결과 목록 → 품목 클릭 → 결과 페이지에 품목명이 나오는지

`frontend/`를 `localhost:5500`에서 띄우고 배포된 백엔드 API를 호출합니다. 백엔드 CORS가 5500 포트를 허용하므로 포트를 바꾸지 마세요.

## 실행

```bash
cd e2e
npm install
npx playwright install chromium
npm test
```

설치된 Edge나 Chrome을 쓰려면 `npx playwright install` 대신 `PW_CHANNEL=msedge npm test`(PowerShell: `$env:PW_CHANNEL="msedge"; npm test`)로 실행합니다.

## 결과

- 터미널에 페이지별 PASS/FAIL이 출력되고, 하나라도 실패하면 종료 코드가 1입니다.
- `shots/`에 화면별 전체 페이지 스크린샷이 저장됩니다. Before/After 비교 자료로 쓸 수 있습니다.
- `report.json`에 전체 결과가 저장됩니다.

Render 무료 서버가 잠들어 있으면 첫 요청이 느릴 수 있습니다.
