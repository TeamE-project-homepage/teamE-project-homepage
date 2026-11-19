const MAX_FILE_SIZE_MB = 5;
const MAX_FILE_SIZE = MAX_FILE_SIZE_MB * 1024 * 1024;
const FAKE_ANALYSE_DELAY_MS = 1500;

const photoInput = document.getElementById("photo");
const photoPreview = document.getElementById("photo-preview");
const analyseButton = document.getElementById("analyse-button");
const loadingBox = document.getElementById("photo-loading");
const errorBox = document.getElementById("photo-error");
const errorText = document.getElementById("photo-error-text");

let previewUrl = null;
let analyseTimer = null;

function showError(message) {
  errorText.textContent = message;
  errorBox.hidden = false;
}

function clearMessages() {
  errorBox.hidden = true;
  loadingBox.hidden = true;
}

function cancelAnalysis() {
  if (analyseTimer) {
    clearTimeout(analyseTimer);
    analyseTimer = null;
  }
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
  if (!file.type.startsWith("image/")) {
    return "Please choose an image file.";
  }
  if (file.size > MAX_FILE_SIZE) {
    return "The photo is too large. Please choose one under " + MAX_FILE_SIZE_MB + " MB.";
  }
  return null;
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

analyseButton.addEventListener("click", () => {
  if (analyseButton.disabled) return;

  analyseButton.disabled = true;
  loadingBox.hidden = false;

  analyseTimer = setTimeout(() => {
    analyseTimer = null;
    loadingBox.hidden = true;
    analyseButton.disabled = false;
  }, FAKE_ANALYSE_DELAY_MS);
});

resetPreview();