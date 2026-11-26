# Frontend JS 구조

BinGo 프론트엔드는 빌드 도구 없이 **HTML + CSS + JavaScript**만 사용합니다. 각 화면의 HTML이 필요한 JS 파일을 `<script>`로 불러옵니다. 데이터는 백엔드 API에서 가져옵니다.

## 파일 역할

| 파일 | 사용하는 화면 | 역할 |
| --- | --- | --- |
| `api.js` | 서버를 부르는 모든 화면 | 백엔드 요청을 한 곳에서 처리하고, 응답을 화면이 쓰는 모양으로 바꿈 |
| `common.js` | 모든 화면 | 선택한 자치구 저장·불러오기, 화면 상단 `District:` 줄 표시 |
| `district.js` | `district.html` | 서울 25개 자치구 목록 표시, 선택하면 저장 후 Home으로 이동 |
| `home.js` | `index.html` | 카테고리 카드 표시 |
| `items.js` | `items.html` | 영어 검색, 카테고리 필터링, 품목 카드 표시 |
| `result.js` | `result.html` | 품목 이름과 선택한 자치구의 배출 규칙 표시 |
| `photo.js` | `photo.html` | 사진 선택, 형식·용량 검사, 인식 요청, 후보 전달 |
| `confirm.js` | `confirm.html` | 인식된 후보 표시, 하나 선택해 결과 화면으로 이동 |

## API 호출 (`api.js`)

- 서버 주소는 `API_BASE_URL` 한 곳에서만 바꿉니다.
- 백엔드 응답은 `{ success, data, error }` 형식이고, `api.js`가 `data`만 꺼내 줍니다. 실패하면 `ApiError(code, message)`를 던집니다.
- 화면에서 쓰는 모양은 다음과 같습니다.
  - 자치구 `{ id, code, name }`, 카테고리 `{ id, name }`
  - 품목 `{ id, name, category }` (검색 결과는 `matchedKeyword`가 더 붙을 수 있음)
  - 사진 인식 결과 `{ matched, candidates: [{ id, name, category, categoryName }] }`
- 서버는 무료 플랜이라 한동안 쓰지 않으면 잠들고, 첫 요청이 30~60초 걸릴 수 있습니다.

## 데이터와 약속

- 선택한 자치구는 `localStorage`의 `bingo.district` 키에 `{ id, code, name }`으로 저장합니다.
- 화면 사이 이동은 주소 파라미터로 전달합니다.
  - `items.html?q=<검색어>`, `items.html?category=<카테고리 id>`, 둘을 함께 쓸 수도 있습니다. (카테고리 안에서 검색)
  - `result.html?item=<품목 id>`
- 사진은 JPEG/PNG만, 10 MB 이하만 허용합니다. (서버 기준과 같음, `photo.js`의 `MAX_FILE_SIZE_MB`)
- 사진 인식 후보는 `sessionStorage`의 `bingo.candidates` 키로 `photo.js`에서 `confirm.js`로 전달합니다.
- 사용자에게 보이는 문구는 영어로 작성합니다.

## 작성 규칙

- JS 파일 하나는 화면 하나(또는 공통 기능 하나)만 맡습니다.
- 여러 화면이 쓰는 코드는 `common.js`나 `api.js`에 둡니다. 같은 이름의 전역 변수·함수를 두 파일에서 선언하지 않습니다.
- 다른 파일에 의존하는 파일은 그 파일보다 **뒤에** 불러옵니다. (예: `api.js` → `photo.js`)

## 확인 방법

서버가 `localhost:5500`, `127.0.0.1:5500`에서 오는 요청만 허용하므로, 파일을 더블클릭해서 열지 말고 VS Code의 **Open with Live Server**로 `frontend/index.html`을 엽니다.