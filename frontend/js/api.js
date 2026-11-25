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
  return districts.map((d) => ({ code: d.code, name: d.nameEn }));
}