// 백엔드 요청을 한 곳에서 처리한다. 서버 주소가 바뀌면 아래 한 줄만 고치면 된다
const API_BASE_URL = "https://teame-project-homepage.onrender.com";

class ApiError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

// 서버가 { success, data, error }로 감싸 보내는 응답에서 data만 꺼낸다
async function apiGet(path) {
  let response;
  try {
    response = await fetch(API_BASE_URL + path, {
      headers: { Accept: "application/json" },
    });
  } catch (e) {
    throw new ApiError("NETWORK", "Could not reach the server. Please try again.");
  }

  let body = null;
  try {
    body = await response.json();
  } catch (e) {
    body = null;
  }

  if (!response.ok || !body || body.success === false) {
    const error = body && body.error;
    throw new ApiError(
      error ? error.code : "HTTP_" + response.status,
      error ? error.message : "Something went wrong. Please try again."
    );
  }
  return body.data;
}

// 서버의 { code, nameEn }을 화면이 쓰는 { code, name } 모양으로 바꾼다
async function fetchDistricts() {
  const districts = await apiGet("/api/districts");
  return districts.map((d) => ({ id: d.districtId, code: d.code, name: d.nameEn }));
}

// 서버의 { categoryId, name }을 화면이 쓰는 { id, name }으로 바꾼다
async function fetchCategories() {
  const categories = await apiGet("/api/categories");
  return categories.map((c) => ({ id: c.categoryId, name: c.name }));
}

// 서버의 품목을 화면이 쓰는 { id, name, category }로 바꾼다
// categoryId를 넘기면 그 카테고리의 품목만, 비우면 전체 품목을 가져온다
async function fetchItems(categoryId) {
  const path = categoryId
    ? "/api/categories/" + encodeURIComponent(categoryId) + "/items"
    : "/api/items";
  const items = await apiGet(path);
  return items.map((i) => ({ id: i.itemId, name: i.name, category: i.categoryId }));
}

// 서버 검색이 한 번에 돌려줄 수 있는 최대 개수
const SEARCH_LIMIT = 50;

// 영어 이름이나 별칭으로 품목을 검색. 서버가 오타와 비슷한 이름도 찾는다.
// matchedKeyword는 어떤 별칭 때문에 검색됐는지 알려준다.(이름으로 찾았으면 빈 문자열)
async function searchItems(q) {
  const result = await apiGet(
    "/api/items/search?q=" + encodeURIComponent(q.trim()) + "&limit=" + SEARCH_LIMIT
  );
  return result.items.map((i) => ({
    id: i.itemId,
    name: i.name,
    category: i.categoryId,
    matchedKeyword: i.matchedKeyword || "",
  }));
}

// 서버의 품목 1개를 화면이 쓰는 모양으로 바꾼다. disposalTypeId는 배출 유형 번호
async function fetchItem(itemId) {
  const item = await apiGet("/api/items/" + encodeURIComponent(itemId));
  return {
    id: item.itemId,
    name: item.name,
    category: item.categoryId,
    categoryName: item.categoryName,
    disposalTypeId: item.disposalTypeId,
  };
}

// 자치구와 배출 유형에 맞는 배출 규칙을 가져온다
// status는 AVAILABLE, PARTIAL, NO_INFO, UNSUPPORTED 중 하나이고, areaScope는 DISTRICT, DONG, ADDRESS 중 하나
async function fetchDistrictRule(districtId, disposalTypeId) {
  return apiGet(
    "/api/districts/" +
      encodeURIComponent(districtId) +
      "/rules?disposalTypeId=" +
      encodeURIComponent(disposalTypeId)
  );
}

// 사진 1장을 서버에 보내 품목 후보를 받는다. 서버는 JPEG/PNG만, 10 MiB까지 받는다
// matched가 false이면 에러가 아니라 "후보 없음"이고 이때 candidates는 빈 배열이다
async function recognizePhoto(file) {
  const formData = new FormData();
  formData.append("file", file);

  let response;
  try {
    // Content-Type은 직접 쓰지 않는다. 브라우저가 multipart 경계값까지 알아서 붙여 준다
    response = await fetch(API_BASE_URL + "/api/recognitions", {
      method: "POST",
      headers: { Accept: "application/json" },
      body: formData,
    });
  } catch (e) {
    throw new ApiError("NETWORK", "Could not reach the server. Please try again.");
  }

  let body = null;
  try {
    body = await response.json();
  } catch (e) {
    body = null;
  }

  if (!response.ok || !body || body.success === false) {
    const error = body && body.error;
    throw new ApiError(
      error ? error.code : "HTTP_" + response.status,
      error ? error.message : "Something went wrong. Please try again."
    );
  }

  const data = body.data;
  return {
    matched: data.matched === true,
    candidates: (data.candidates || []).map((item) => ({
      id: item.itemId,
      name: item.name,
      category: item.categoryId,
      categoryName: item.categoryName,
    })),
  };
}