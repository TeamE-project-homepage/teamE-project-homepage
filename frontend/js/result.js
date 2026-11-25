// 주소의 ?item= 값이 양의 정수일 때만 품목 번호로 인정한다
function getItemId() {
  const value = new URLSearchParams(window.location.search).get("item") || "";
  return /^[1-9][0-9]*$/.test(value) ? value : "";
}

async function renderResultPage() {
  const itemName = document.getElementById("result-item");
  const type = document.getElementById("result-type");
  const status = document.getElementById("result-status");
  const checkCondition = document.getElementById("check-condition");

  // 체크리스트 API가 아직 없어서, 연결되기 전까지는 진입 버튼을 숨긴다
  checkCondition.hidden = true;

  // 품목을 못 불러왔을 때 배너에 안내를 보여주고, 거짓 상태(Recyclable 등)는 숨긴다
  function showProblem(title, message) {
    itemName.textContent = title;
    type.textContent = message;
    status.hidden = true;
  }

  const itemId = getItemId();
  if (!itemId) {
    showProblem("Item not found", "Please choose an item from the list.");
    return;
  }

  itemName.textContent = "Loading...";
  type.textContent = "";
  status.hidden = true;

  let item;
  try {
    item = await fetchItem(itemId);
  } catch (e) {
    if (e.code === "ITEM_404" || e.code === "COMMON_400") {
      showProblem("Item not found", "We could not find that item.");
    } else {
      showProblem("Could not load the item", e.message);
    }
    return;
  }

  document.title = item.name + " | BinGo";
  itemName.textContent = item.name;

  // 배출 유형 이름과 단계는 아직 서버 API가 없어서 확인 불가로 보여준다
  type.textContent = "Classification unavailable";
}

renderResultPage();