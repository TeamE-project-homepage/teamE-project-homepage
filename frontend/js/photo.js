const MAX_FILE_SIZE_MB = 10;
const MAX_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024;
// 서버가 받는 형식. 다른 형식은 서버에서 415로 거절된다
const ALLOWED_TYPES = ["image/jpeg", "image/png"];
// confirm.html이 읽어 갈 후보 목록을 잠시 넣어 두는 키
const CANDIDATES_KEY = "bingo.candidates";

const photoInput = document.getElementById("photo");
const photoPreview = document.getElementById("photo-preview");
const analyseButton = document.getElementById("analyse-button");
const loadingBox = document.getElementById("photo-loading");
const errorBox = document.getElementById("photo-error");
const errorText = document.getElementById("photo-error-text");

let previewUrl = null;
// 사진을 바꾸면 값이 올라가서, 이전 사진의 늦은 응답은 무시한다
let requestId = 0;

function showError(message) {
  errorText.textContent = message;
  errorBox.hidden = false;
}

function clearMessages() {
  errorBox.hidden = true;
  loadingBox.hidden = true;
}

function cancelAnalysis() {
  requestId += 1;
  loadingBox.hidden = true;
}

function resetPreview() {
  if (previewUrl) {
    URL.revokeObjectURL(previewUrl);
    previewUrl = null;
  }
  photoPreview.textContent = "No photo selected";
  analyseButton.disabled = true;
}

function showPreview(file) {
  if (previewUrl) URL.revokeObjectURL(previewUrl);
  previewUrl = URL.createObjectURL(file);

  const image = document.createElement("img");
  image.src = previewUrl;
  image.alt = "Selected item photo";
  image.style.width = "100%";
  image.style.height = "100%";
  image.style.objectFit = "contain";

  photoPreview.replaceChildren(image);
  analyseButton.disabled = false;
}

function validatePhoto(file) {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return "Please choose a JPEG or PNG photo.";
  }
  if (file.size > MAX_FILE_SIZE) {
    return "The photo is too large. Please choose one under " + MAX_FILE_SIZE_MB + " MB.";
  }
  return null;
}

// 서버 에러 코드를 사용자가 이해할 문장으로 바꾼다
function errorMessage(error) {
  switch (error.code) {
    case "COMMON_413":
      return "The photo is too large. Please choose one under " + MAX_FILE_SIZE_MB + " MB.";
    case "COMMON_415":
      return "Please choose a JPEG or PNG photo.";
    case "RECOGNITION_422":
      return "We could not read this photo. Please try another one.";
    case "RECOGNITION_503":
      return "Photo recognition is not available right now. Please try again in a moment.";
    case "RECOGNITION_400":
      return "Please choose a photo first.";
    default:
      return error.message || "Something went wrong. Please try again.";
  }
}

photoInput.addEventListener("change", () => {
  const file = photoInput.files[0];
  cancelAnalysis();
  clearMessages();

  if (!file) {
    resetPreview();
    return;
  }

  const message = validatePhoto(file);
  if (message) {
    resetPreview();
    showError(message);
    photoInput.value = "";
    return;
  }

  showPreview(file);
});

analyseButton.addEventListener("click", async () => {
  // 요청 중에 버튼이 비활성화되므로 같은 사진을 두 번 보내지 않는다
  if (analyseButton.disabled) return;
  const file = photoInput.files[0];
  if (!file) return;

  const currentId = ++requestId;
  analyseButton.disabled = true;
  errorBox.hidden = true;
  loadingBox.hidden = false;

  try {
    const result = await recognizePhoto(file);
    if (currentId !== requestId) return;

    if (result.matched && result.candidates.length > 0) {
      sessionStorage.setItem(CANDIDATES_KEY, JSON.stringify(result.candidates));
      window.location.href = "confirm.html";
      return;
    }

    loadingBox.hidden = true;
    analyseButton.disabled = false;
    showError("We could not recognise this item. Please try another photo or search by name.");
  } catch (e) {
    if (currentId !== requestId) return;
    loadingBox.hidden = true;
    analyseButton.disabled = false;
    showError(errorMessage(e));
  }
});

resetPreview();