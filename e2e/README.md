# 반응형 E2E 테스트

[Playwright Test](https://playwright.dev/docs/intro)로 375px · 768px · 1280px 화면에서 아래 항목을 자동으로 확인합니다.

| 파일 | 확인하는 것 |
| --- | --- |
| `tests/pages.spec.mjs` | 11개 페이지가 HTTP 4xx/5xx, JS 에러, 콘솔 에러 없이 열리는지, 가로 스크롤이 생기지 않는지 |
| `tests/search.spec.mjs` | 홈 검색 → 결과 목록 → 품목 클릭 → 결과 페이지에 품목명 표시, 없는 품목을 검색하면 빈 결과 안내가 보이는지 |

화면 크기는 `playwright.config.mjs`의 `projects`에서 정합니다. 테스트를 실행하면 `serve.mjs`가 `frontend/`를 `localhost:5500`에 띄우고, 화면은 배포된 백엔드 API를 호출합니다. 백엔드 CORS가 5500 포트를 허용하므로 포트를 바꾸지 마세요.

## 실행

```bash
cd e2e
npm install
npx playwright install chromium
npm test
```

설치된 Edge나 Chrome을 쓰려면 `npx playwright install` 없이 `PW_CHANNEL=msedge npm test`(PowerShell: `$env:PW_CHANNEL="msedge"; npm test`)로 실행합니다.

자주 쓰는 옵션:

- 모바일만: `npm test -- --project=mobile-375`
- 검색 테스트만: `npm test -- tests/search.spec.mjs`
- 브라우저 창을 띄워서 보기: `npm test -- --headed`

## 결과

- 터미널에 테스트별 결과가 나오고, `npm run report`로 HTML 리포트를 열 수 있습니다.
- 가로 스크롤이 생기면 화면 밖으로 넘친 요소(태그, id, class, 위치)가 실패 메시지에 나옵니다.
- `shots/`에 화면별 전체 페이지 스크린샷이 저장됩니다. Before/After 비교 자료로 쓸 수 있습니다.

Render 무료 서버가 잠들어 있으면 첫 요청이 느릴 수 있어서 테스트 제한 시간을 90초로 두었습니다.
