# 시드 파일 컬럼 정의·작성 규칙

현재 `docs/data`의 CSV 10개를 기준으로 수집 항목과 자치구 배출 정보의 형식을 정의한다. CSV 하나를 스프레드시트 시트 하나로 사용한다. 아래 키와 필수 여부는 수집 데이터의 작성·검토 기준이며, DB 제약이나 자동 가져오기 기능이 구현되어 있다는 뜻은 아니다.

## 공통 작성 규칙

| 항목 | 규칙 |
| --- | --- |
| 파일·시트 이름 | 기존 CSV 파일명을 유지한다. 스프레드시트의 시트 이름은 확장자를 제외한 파일명으로 맞춘다. |
| 헤더 | 첫 행에 아래 컬럼명을 순서대로 작성한다. 번역·공백 추가·임의의 컬럼 변경을 하지 않는다. |
| 인코딩·줄바꿈 | UTF-8 BOM, LF 줄바꿈을 유지한다. |
| 한 행의 단위 | 품목·키워드·단계·배출 규칙 등 시트별 단위 하나를 한 행에 작성한다. 셀 병합이나 중간 제목 행을 넣지 않는다. |
| CSV 이스케이프 | 쉼표·큰따옴표·줄바꿈이 있는 셀은 큰따옴표로 감싼다. 셀 내부 큰따옴표는 두 번 쓴다. CSV 내보내기 기능이나 CSV 라이브러리를 사용한다. |
| 빈칸 | 값이 없거나 확인되지 않았으면 빈 셀로 둔다. `null`, `N/A`, `-`, 임의의 기본값을 넣지 않는다. |
| 코드 | 대소문자를 포함해 기준 시트의 값을 그대로 사용한다. 자치구 코드는 계산용 숫자가 아닌 5자리 문자열로 취급한다. |
| 순서 | 순서 컬럼은 1부터 시작하는 정수다. 품목 또는 배출 규칙별로 독립적으로 부여한다. |
| 언어 | 화면용 이름·설명·주의사항은 영어로 작성한다. `name_ko`, `_notes_ko`는 한국어, 출처명은 원문을 사용한다. 검색 키워드는 영어 동의어와 한국어를 함께 사용할 수 있다. |
| 날짜 | `YYYY-MM-DD` 형식의 텍스트로 작성한다. 확인일을 자료의 발행일로 대체하지 않는다. |
| URL | 실제 자료 페이지나 파일의 URL을 기록한다. 확인하지 않은 이미지 URL을 만들어 넣지 않는다. |
| 검토용 컬럼 | `_`로 시작하는 컬럼은 수집·검토 메타데이터다. 사용자 화면에 그대로 표시하지 않는다. |

필수 여부의 `조건부`는 해당 행의 검증 상태나 데이터 내용에 따라 값이 필요하다는 뜻이다. `선택` 항목도 확인한 정보가 있으면 작성한다.

## 코드값

### 카테고리

| 코드 | 영문 이름 | 수집 범위 |
| --- | --- | --- |
| `PL` | Plastic | 플라스틱 |
| `VN` | Vinyl | 비닐 |
| `PA` | Paper | 종이 |
| `GL` | Glass | 유리 |
| `ME` | Metal | 금속 |
| `AP` | Appliances | 가전 |
| `FU` | Furniture | 가구 |
| `CL` | Clothing | 의류·섬유 |
| `FO` | Food Waste | 음식물과 음식물로 혼동하기 쉬운 품목 |
| `BK` | Bulky Waste | 기타 대형폐기물 |

### 배출 유형

| 코드 | 영문 이름 | 의미 |
| --- | --- | --- |
| `RECYCLABLE` | Recyclable Waste | 재활용 대상. 품목에 따라 일반 재활용품·폐가전·의류 전용 수거 경로를 구분한다. |
| `GENERAL` | General Waste | 일반쓰레기 종량제봉투 배출 |
| `FOOD` | Food Waste | 음식물 전용 배출 |
| `BULKY` | Bulky Waste | 자치구 대형폐기물 신고·수거 |
| `NON_COMBUSTIBLE` | Non-combustible Waste (Special Sack) | 불연성 폐기물의 특수규격마대 등 별도 배출 |

카테고리와 배출 유형은 독립적이다. `FO` 품목도 `GENERAL`로 배출할 수 있다. 기존에 분리된 `BULKY`와 `NON_COMBUSTIBLE`을 하나의 `SPECIAL` 값으로 되돌려 쓰지 않는다.

### 상태·검토 코드

| 컬럼 | 허용값 | 의미 |
| --- | --- | --- |
| `recyclable` | `RECYCLABLE` | 해당 수거 경로에서 재활용 가능 |
| `recyclable` | `CONDITIONAL` | 세척·상태·전용 수거 등 조건을 충족해야 가능 |
| `recyclable` | `NOT_RECYCLABLE` | 일반 재활용품 수거 대상이 아님. 재사용·별도 자원회수 가능성은 별도 확인 |
| `_priority` | `P1`, `P2` | 수집 우선순위. P1은 우선 정리 대상, P2는 후속·보완 대상 |
| `_verification` | `VERIFIED`, `UNVERIFIED` | 공식 출처에서 근거를 확인했는지 여부 |
| `area_scope` | `DISTRICT`, `DONG`, `ADDRESS` | 자치구 공통·동별 구역·주소별 조회로 나뉘는 적용 범위 |

`VERIFIED`는 기록된 근거를 확인했다는 의미다. 지역별 예외까지 전부 확인했다거나 해당 품목의 최신 정책이 영구히 보장된다는 의미는 아니다. `UNVERIFIED`는 확인한 내용만 입력하고, 미확인 사유를 `_notes_ko`에 남긴다. `_priority`는 사용자용 위험도나 배출 유형이 아니다.

## 기준 정보 시트

### categories.csv

한 행은 카테고리 하나이며, 고유키는 `code`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `code` | string | 필수 | 카테고리 코드. 예: `AP` |
| `name` | string | 필수 | 화면용 영문 이름. 예: `Appliances` |
| `sort_order` | integer | 필수 | 카테고리 표시 순서. 예: `6` |

### disposal_types.csv

한 행은 배출 유형 하나이며, 고유키는 `code`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `code` | string | 필수 | 배출 유형 코드. 예: `BULKY` |
| `name` | string | 필수 | 화면용 영문 이름. 예: `Bulky Waste` |

### districts.csv

한 행은 자치구 하나이며, 고유키는 `code`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `code` | string | 필수 | 5자리 자치구 코드. 예: `11110` |
| `name_en` | string | 필수 | 자치구 영문 이름. 예: `Jongno-gu` |
| `name_ko` | string | 필수 | 자치구 한국어 이름. 예: `종로구` |
| `city` | string | 필수 | 도시 이름. 현재 데이터는 `Seoul` |

## 수집 품목 시트

### items.csv

한 행은 품목 하나이며, 고유키는 `item_key`다. 동일한 물건도 상태에 따라 안내가 달라지면 구분된 키로 작성할 수 있다. 예: `reusable_clothes`, `damaged_clothes`.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `item_key` | string | 필수 | 소문자 영문·숫자·밑줄로 구성한 안정적인 품목 식별자. 예: `rice_cooker` |
| `category_code` | string | 필수 | `categories.code` 참조. 예: `AP` |
| `disposal_type_code` | string | 필수 | `disposal_types.code` 참조. 예: `RECYCLABLE` |
| `name` | string | 필수 | 화면용 영문 품목명. 예: `Electric Rice Cooker` |
| `name_ko` | string | 필수 | 수집·검토용 한국어 품목명. 예: `전기밥솥` |
| `recyclable` | string / enum | 필수 | 위 상태 코드 중 하나. 예: `CONDITIONAL` |
| `status_note` | string | 선택 | 상태를 보완하는 짧은 영문 문구. 예: `if clean` |
| `caution` | string | 조건부 | 지역·크기·상태에 따른 예외나 전용 수거 안내가 있으면 작성 |
| `image_url` | string / URL | 선택 | 확보한 실제 품목 이미지 URL. 현재 미확보 값은 빈칸 |
| `clip_prompt` | string | 필수 | 이미지 인식용 영문 품목 묘사. 예: `a photo of an electric rice cooker` |
| `source_name` | string | 조건부 | `VERIFIED`인 경우 필수. 근거 자료의 제목·기관 |
| `source_url` | string / URL | 조건부 | `VERIFIED`인 경우 필수. 대표 근거 URL |
| `verified_at` | string / date | 필수 | 최종 검토일. `UNVERIFIED`라면 확인을 시도한 날짜 |
| `_priority` | string / enum | 필수 | `P1` 또는 `P2` |
| `_verification` | string / enum | 필수 | `VERIFIED` 또는 `UNVERIFIED` |
| `_notes_ko` | string | 조건부 | 미확인 사유·지역 차이·자료 불일치·일반 규칙을 적용한 판단 근거가 있으면 작성. 보조 출처도 여기에 기록 |

품목 이름을 바꾸더라도 같은 품목이면 `item_key`를 유지한다. 키를 변경할 때는 아래 세 시트의 참조도 함께 변경한다. 여러 품목이 공유하는 단어는 각 품목의 키워드에 등록할 수 있다.

### item_keywords.csv

한 행은 검색 키워드 하나이며, 고유키는 `(item_key, keyword)`다. 여러 검색어를 하나의 셀에 쉼표로 나열하지 않는다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `item_key` | string | 필수 | `items.item_key` 참조. 예: `refrigerator` |
| `keyword` | string | 필수 | 검색어·동의어·한국어 이름. 예: `fridge`, `냉장고` |

앞뒤 공백과 불필요한 중복을 제거한다. 같은 품목에서 영문 대소문자만 다른 키워드를 중복 등록하지 않는다.

### guide_steps.csv

한 행은 품목의 배출 단계 하나이며, 고유키는 `(item_key, step_order)`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `item_key` | string | 필수 | `items.item_key` 참조 |
| `step_order` | integer | 필수 | 품목별 단계 순서. `1`, `2`, `3` 등 |
| `description` | string | 필수 | 화면용 영문 행동 안내. 예: `Empty & rinse` |
| `image_url` | string / URL | 선택 | 해당 단계를 설명하는 실제 이미지 URL |

### condition_checks.csv

한 행은 품목의 확인 조건 하나이며, 고유키는 `(item_key, sort_order)`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `item_key` | string | 필수 | `items.item_key` 참조 |
| `sort_order` | integer | 필수 | 품목별 체크 항목 표시 순서 |
| `label` | string | 필수 | 화면용 영문 조건 문구. 예: `Clean and dry` |

조건이 없는 품목에는 빈 행이나 가짜 체크 항목을 만들지 않는다. 조건부 수거와 대형폐기물 신고처럼 확인이 필요한 품목은 실제 안내에 맞는 체크 항목을 작성한다.

## 자치구 배출 정보 시트

### district_rules.csv

한 행은 자치구 하나의 배출 유형별 기본 규칙이며, 고유키는 `(district_code, disposal_type_code)`다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `district_code` | string | 필수 | `districts.code` 참조. 예: `11140` |
| `disposal_type_code` | string | 필수 | `disposal_types.code` 참조. 예: `FOOD` |
| `area_scope` | string / enum | 필수 | `DISTRICT`, `DONG`, `ADDRESS`. 미확인 행의 임시 값은 확정된 범위로 해석하지 않는다. |
| `method` | string | 조건부 | `VERIFIED`인 경우 필수. 봉투·용기·신고·배출 장소 등을 설명하는 영문 기본 안내 |
| `day` | string | 선택 | 공통 배출요일 또는 예약 문구. 예: `Sun~Fri`, `By appointment`. 확인된 공통값이 없으면 빈칸 |
| `time` | string | 선택 | 공통 배출시간. 예: `19:00~24:00`. 익일 종료·계절별 차이는 설명에 함께 기록 |
| `local_notes` | string | 조건부 | 공동주택·도로변·계절·특정 재질 등 적용 예외가 있으면 영문으로 작성 |
| `source_name` | string | 조건부 | `VERIFIED`인 경우 필수. 공식 근거 자료 제목·기관 |
| `source_url` | string / URL | 조건부 | `VERIFIED`인 경우 필수. 공식 근거 URL |
| `verified_at` | string / date | 필수 | 최종 검토일. `UNVERIFIED`라면 확인을 시도한 날짜 |
| `_applies_to` | string | 조건부 | 적용 대상·범위를 검토용 영어로 기록. 동별·주소별·건물 유형별 차이가 있으면 작성 |
| `_verification` | string / enum | 필수 | `VERIFIED` 또는 `UNVERIFIED` |
| `_notes_ko` | string | 조건부 | 미확인 내용·출처 불일치·지역별 예외·동 목록 누락 사유 등을 한국어로 기록 |

`day`와 `time`은 현재 자유 텍스트이며 기계적인 요일·시간 파서의 입력 계약이 아니다. 원문이 일몰·예약·계절별 시간으로 안내하면 정확한 조건을 설명에 남기고, 확인되지 않은 단일 숫자 시간으로 바꾸지 않는다. 기존 요일 기호나 문구를 변경하려면 값을 사용하는 코드와 데이터도 함께 검토한다.

### district_rule_areas.csv

한 행은 같은 자치구·배출 유형 안에서 요일·시간을 공유하는 구역 하나다. 고유키는 `(district_code, disposal_type_code, sort_order)`이며, 앞의 두 컬럼으로 `district_rules`를 참조한다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `district_code` | string | 필수 | 부모 규칙의 자치구 코드. 예: `11140` |
| `disposal_type_code` | string | 필수 | 부모 규칙의 배출 유형. 예: `FOOD` |
| `sort_order` | integer | 필수 | 부모 규칙 안에서의 구역 식별 순서. 예: `1` |
| `day` | string | 필수 | 구역별로 확인한 배출요일. 예: `Mon–Sat (no Sun)` |
| `time` | string | 선택 | 구역별 시간. 빈칸이면 확인된 부모 규칙의 공통 시간을 참고하며, 부모도 비어 있으면 미확인으로 유지 |

`sort_order`는 표시 순서이면서 하위 동 목록의 참조값이다. 구역 순서를 바꾸면 하위 시트의 `area_sort_order`도 함께 변경한다. 이 시트에 별도 출처 컬럼은 없으므로 부모 규칙의 출처·메모에 구역 분할 근거를 남긴다.

### district_rule_area_dongs.csv

한 행은 배출 구역에 포함되는 동 하나다. 고유키는 `(district_code, disposal_type_code, area_sort_order, name_ko)`이며, 앞의 세 컬럼으로 `district_rule_areas`의 구역을 참조한다.

| 컬럼 | 타입 | 필수 | 설명·예시 |
| --- | --- | --- | --- |
| `district_code` | string | 필수 | 부모 구역의 자치구 코드. 예: `11140` |
| `disposal_type_code` | string | 필수 | 부모 구역의 배출 유형. 예: `FOOD` |
| `area_sort_order` | integer | 필수 | 부모 구역의 `sort_order`. 예: `1` |
| `name_ko` | string | 필수 | 한국어 동 이름. 예: `을지로동` |
| `name_en` | string | 필수 | 영어 동 이름. 예: `Euljiro-dong` |

동 목록을 쉼표로 한 셀에 합치지 않고 동마다 행을 나눈다. 한영 동 이름은 서로 다른 컬럼에 작성한다. 동 일부·옛 행정동 기준 분할이 있으면 부모 규칙의 메모에 정확한 범위를 남긴다. 동 이름만으로 상세 주소의 구역을 확정할 수 없는 경우 추가 확인을 안내한다.

## 연결 관계와 작성 순서

| 먼저 작성할 기준 | 연결되는 시트·컬럼 |
| --- | --- |
| `categories.code` | `items.category_code` |
| `disposal_types.code` | `items.disposal_type_code`, 자치구 배출 정보 3개 시트의 `disposal_type_code` |
| `districts.code` | 자치구 배출 정보 3개 시트의 `district_code` |
| `items.item_key` | `item_keywords.item_key`, `guide_steps.item_key`, `condition_checks.item_key` |
| `district_rules.(district_code, disposal_type_code)` | `district_rule_areas.(district_code, disposal_type_code)` |
| `district_rule_areas.(district_code, disposal_type_code, sort_order)` | `district_rule_area_dongs.(district_code, disposal_type_code, area_sort_order)` |

기준 코드 → 품목·자치구 기본 규칙 → 키워드·단계·조건·배출 구역 → 구역별 동 목록 순서로 작성한다. 참조하는 부모 행이 없는 상태로 하위 행만 추가하지 않는다.

## 자치구 규칙의 적용 기준

다음은 향후 화면·가져오기 로직에서 적용해야 할 기준이다.

1. 자치구와 배출 유형이 일치하는 기본 규칙을 찾는다. 조합이 없거나 `UNVERIFIED`면 확인되지 않은 배출요일·시간을 확정해서 안내하지 않는다.
2. `DISTRICT`는 확인된 공통값을 사용하되 공동주택·도로변 등 `local_notes`의 적용 조건을 함께 안내한다.
3. `DONG`은 동 목록에서 해당 동의 구역을 찾는다. 구역의 요일·시간을 우선하고, 구역 시간이 비어 있을 때만 적용 가능한 부모 공통 시간을 참고한다. 예외 조건도 함께 확인한다.
4. 동별 구분은 확인했지만 구역·동 목록이 없거나 매칭이 모호하면 자치구 확인을 안내한다. 다른 동의 구역을 기본값으로 선택하지 않는다.
5. `ADDRESS`는 동 이름만으로 요일을 확정하지 않는다. 주소별 공식 조회 링크와 확인 안내를 제공한다. 현재 CSV에는 상세 주소별 규칙 시트가 없다.
6. 폐가전·의류 전용 수거는 일반 재활용품의 일정과 구분한다. 품목의 전용 수거 안내를 먼저 확인한다.

### 기존 데이터 연결 예시

중구(`11140`) 음식물(`FOOD`) 규칙의 연결 형식을 발췌했다. 최신 정책을 새로 확인한 예시가 아니라 현재 CSV의 관계를 설명하는 예시다.

`district_rule_areas.csv`

```csv
district_code,disposal_type_code,sort_order,day,time
11140,FOOD,1,Mon–Sat (no Sun),
```

`district_rule_area_dongs.csv`

```csv
district_code,disposal_type_code,area_sort_order,name_ko,name_en
11140,FOOD,1,을지로동,Euljiro-dong
```

두 행은 `11140 + FOOD + 1`로 연결된다. 구역 시간은 빈칸이며 부모 `district_rules`의 공통 시간은 `19:00~24:00`다. 도로변·관광특구 등 별도 적용 조건은 부모의 `local_notes`, `_applies_to`, `_notes_ko`도 함께 확인해야 한다.

## 현재 데이터에서 남은 확인 사항

2026-11-19의 저장소 데이터 기준이다. 아래 내용은 이번 형식 정리에서 새로운 배출 사실로 확정하지 않았다.

- `district_rules`는 25개 자치구 × 4개 배출 유형(`GENERAL`, `FOOD`, `RECYCLABLE`, `BULKY`)의 100행이다. `NON_COMBUSTIBLE`의 자치구별 규칙은 아직 등록되어 있지 않다.
- 양천구(`11470`)의 일반·음식물·재활용 규칙 3행은 `UNVERIFIED`다. 날짜가 있어도 배출 정보 확인이 완료된 것으로 취급하지 않는다.
- 구로구(`11530`)의 일반·음식물 규칙은 `DONG`이지만 구역·동 목록을 확보하지 못했다. 공통 규칙이 검증됐어도 동별 요일을 확정할 수 없다.
- 구역 시트의 `time`은 현재 56행 모두 빈칸이다. 부모의 공통 시간과 예외 조건을 확인해야 하며, 부모 값도 없는 경우 임의로 채우지 않는다.
- 이미지 URL은 미확보 상태다. 이미지 프롬프트가 있다는 이유로 이미지가 준비됐거나 인식 성능이 검증됐다고 보지 않는다.

## 변경 전 검토 항목

- 헤더·열 순서와 CSV 이스케이프, UTF-8 BOM·LF 유지
- 필수값, 허용 코드, 검토일 형식 확인
- 고유키 중복 및 부모 없는 참조 확인
- 품목별 단계·조건 순서와 구역 식별 순서 확인
- 출처 확인 여부와 `_verification` 일치 확인
- 미확인 요일·시간을 추측해서 채우지 않았는지 확인
- 동 목록·주소 조회·공동주택 등 적용 범위와 예외 확인
- 기존 품목과 자치구 데이터가 의도치 않게 변경되지 않았는지 확인
