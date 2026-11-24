const STORAGE_KEY = "bingo.district";

function loadDistrict() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : null;
  } catch (e) {
    return null;
  }
}

function saveDistrict(district) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(district));
    return true;
  } catch (e) {
    return false;
  }
}

function clearDistrict() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (e) {
    // 지우지 못해도 화면 동작에는 영향이 없다
  }
}

function renderDistrictBar() {
  const label = document.querySelector(".district-bar span");
  if (!label) return;

  const district = loadDistrict();
  label.textContent = district
    ? "District: " + district.name
    : "District: Not selected";
}

renderDistrictBar();