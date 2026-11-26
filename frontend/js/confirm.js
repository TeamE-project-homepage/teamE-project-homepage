// photo.js가 sessionStorage에 넣어 둔 후보 목록을 읽어 화면에 보여 준다
const CANDIDATES_KEY = "bingo.candidates";

const candidateGrid = document.getElementById("candidate-grid");
const candidateNote = document.getElementById("candidate-note");
const confirmLink = document.getElementById("confirm-item");

// 저장된 값이 깨졌거나 비어 있어도 화면이 죽지 않게, 쓸 수 있는 후보만 골라 돌려준다
function loadCandidates() {
  let list = [];
  try {
    list = JSON.parse(sessionStorage.getItem(CANDIDATES_KEY)) || [];
  } catch (e) {
    list = [];
  }
  if (!Array.isArray(list)) return [];
  return list.filter((item) => item && Number.isInteger(item.id) && item.id > 0 && item.name);
}

function createCandidateCard(item) {
  const card = document.createElement("button");
  card.type = "button";
  card.className = "card";
  card.dataset.itemId = String(item.id);
  card.setAttribute("aria-pressed", "false");

  const image = document.createElement("div");
  image.className = "card-image";
  image.textContent = "image";

  const title = document.createElement("span");
  title.className = "card-title";
  title.textContent = item.name;

  card.append(image, title);

  if (item.categoryName) {
    const sub = document.createElement("span");
    sub.className = "card-sub";
    sub.textContent = item.categoryName;
    card.append(sub);
  }
  return card;
}

function selectCandidate(id) {
  candidateGrid.querySelectorAll(".card").forEach((card) => {
    card.setAttribute("aria-pressed", String(card.dataset.itemId === String(id)));
  });
  confirmLink.href = "result.html?item=" + encodeURIComponent(id);
  confirmLink.removeAttribute("aria-disabled");
}

function renderConfirmPage() {
  const candidates = loadCandidates();

  if (candidates.length === 0) {
    candidateNote.textContent = "There are no photo results to show. Please upload a photo again.";
    candidateNote.hidden = false;
    confirmLink.hidden = true;
    return;
  }

  candidateGrid.replaceChildren(...candidates.map(createCandidateCard));
  candidateGrid.addEventListener("click", (event) => {
    const card = event.target.closest(".card");
    if (card) selectCandidate(card.dataset.itemId);
  });

  // 가장 가능성이 높은 첫 번째 후보를 미리 골라 둔다
  selectCandidate(candidates[0].id);
}

renderConfirmPage();