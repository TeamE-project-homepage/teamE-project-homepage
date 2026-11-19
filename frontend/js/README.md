# Frontend JS 구조

BinGo 프론트엔드는 빌드 도구 없이 **HTML + CSS + JavaScript**만 사용합니다. 각 화면의 HTML이 필요한 JS 파일을 `<script>`로 불러옵니다.

## 파일 역할

| 파일 | 사용하는 화면 | 역할 |
| --- | --- | --- |
| `common.js` | 모든 화면 | 선택한 자치구 저장·불러오기, 화면 상단 `District:` 줄 표시 |
| `district.js` | `district.html` | 서울 25개 자치구 목록 표시, 선택하면 저장 후 Home으로 이동 |
| `mock-data.js` | `index.html`, `items.html` | 개발용 가짜 카테고리·품목 데이터 (백엔드 연결 후 제거) |
| `home.js` | `index.html` | 카테고리 카드 표시 |
| `items.js` | `items.html` | 영어 검색, 카테고리 필터링, 품목 카드 표시 |
| `photo.js` | `photo.html` | 사진 선택, 형식·용량 검사, 미리보기 |

## 데이터와 약속

- 선택한 자치구는 `localStorage`의 `bingo.district` 키에 `{ code, name }`으로 저장합니다.
- 카테고리는 `{ id, name }`, 품목은 `{ id, name, aliases, category }` 형태입니다. (`aliases`는 검색에 쓰는 다른 영어 이름)
- 화면 사이 이동은 주소 파라미터로 전달합니다.
  - `items.html?q=<검색어>`, `items.html?category=<카테고리 id>`, 둘을 함께 쓸 수도 있습니다. (카테고리 안에서 검색)
  - `result.html?item=<품목 id>`
- 사진은 이미지 파일만, 5 MB 이하만 허용합니다. (`photo.js`의 `MAX_FILE_SIZE_MB`, 백엔드 기준이 정해지면 변경)
- 사용자에게 보이는 문구는 영어로 작성합니다.

## 작성 규칙

- JS 파일 하나는 화면 하나(또는 공통 기능 하나)만 맡습니다.
- 여러 화면이 쓰는 코드는 `common.js`에 둡니다. 같은 이름의 전역 변수·함수를 두 파일에서 선언하지 않습니다.
- 다른 파일에 의존하는 파일은 그 파일보다 **뒤에** 불러옵니다. (예: `mock-data.js` → `items.js`)

## 2주차 계획

- 백엔드 API를 부르는 `api.js`를 추가하고, `mock-data.js`를 API 호출로 교체합니다. 백엔드 응답은 `{ success, data, error }` 형식입니다.
- 가짜 자치구 `code`를 백엔드의 값과 맞춥니다.
- 사진 분석을 백엔드에 연결하고, 결과 화면(`result.html`)을 구현합니다.

## 확인 방법

빌드가 필요 없습니다. `frontend/index.html`을 브라우저로 열면 됩니다.