const NOT_CONFIRMED = "Not confirmed";

// 주소의 ?item= 값이 양의 정수일 때만 품목 번호로 인정한다
function getItemId() {
  const value = new URLSearchParams(window.location.search).get("item") || "";
  return /^[1-9][0-9]*$/.test(value) ? value : "";
}

// 요소 안을 한 줄씩 나눠서 채운다
function setLines(element, lines) {
  element.replaceChildren();
  lines.forEach((line) => {
    const row = document.createElement("div");
    row.textContent = line;
    element.appendChild(row);
  });
}

// 지역 정보 영역에 안내 문구만 보여준다 (표는 숨긴다)
function showRulesMessage(message) {
  document.getElementById("district-rules").hidden = true;
  const notice = document.getElementById("district-rules-empty");
  notice.querySelector("p").textContent = message;
  notice.hidden = false;
}

function showRulesLoading() {
  document.getElementById("district-rules").hidden = false;
  document.getElementById("district-rules-empty").hidden = true;
  ["rule-day", "rule-time", "rule-method", "rule-source"].forEach((id) => {
    document.getElementById(id).textContent = "Loading...";
  });
}

// 저장된 자치구에 id가 없으면(예전에 저장된 값) 서버 목록에서 코드로 찾아 채운다
async function loadDistrictWithId() {
  const saved = loadDistrict();
  if (!saved) return null;
  if (saved.id) return saved;

  const districts = await fetchDistricts();
  const found = districts.find((d) => d.code === saved.code);
  if (!found) {
    clearDistrict();
    renderDistrictBar();
    return null;
  }
  saveDistrict(found);
  return found;
}

// 동별 규칙은 "요일 (동 이름들)" 형태로 한 줄씩 보여준다
function areaLines(areas, field) {
  return areas.map((area) => {
    const dongs = area.dongs.map((dong) => dong.nameEn).join(", ");
    return (area[field] || NOT_CONFIRMED) + " (" + dongs + ")";
  });
}

function renderSource(element, rule) {
  element.replaceChildren();
  const label = rule.sourceName || rule.sourceUrl || NOT_CONFIRMED;
  // http나 https 주소만 링크로 연다
  if (rule.sourceUrl && /^https?:\/\//i.test(rule.sourceUrl)) {
    const link = document.createElement("a");
    link.href = rule.sourceUrl;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = label;
    element.appendChild(link);
  } else {
    element.textContent = label;
  }
}

function renderRule(rule) {
  const panel = document.getElementById("district-rules");
  const day = document.getElementById("rule-day");
  const time = document.getElementById("rule-time");

  panel.hidden = false;
  document.getElementById("district-rules-empty").hidden = true;

  // 동 단위 정보를 자치구 전체 규칙처럼 보여주지 않는다
  if (rule.areaScope === "DONG") {
    if (rule.areas && rule.areas.length > 0) {
      setLines(day, areaLines(rule.areas, "day"));
      setLines(time, areaLines(rule.areas, "time"));
    } else {
      day.textContent = NOT_CONFIRMED;
      time.textContent = NOT_CONFIRMED;
    }
  } else if (rule.areaScope === "ADDRESS") {
    day.textContent = "Depends on your address";
    time.textContent = rule.time || NOT_CONFIRMED;
  } else {
    day.textContent = rule.day || NOT_CONFIRMED;
    time.textContent = rule.time || NOT_CONFIRMED;
  }

  document.getElementById("rule-method").textContent = rule.method || NOT_CONFIRMED;
  renderSource(document.getElementById("rule-source"), rule);

  // 이전에 붙인 추가 문구를 지우고, 지역별 유의사항과 확인일을 붙인다
  panel.querySelectorAll(".rule-extra").forEach((node) => node.remove());
  const extras = [];
  if (rule.localNotes) extras.push(rule.localNotes);
  if (rule.verifiedAt) extras.push("Last verified: " + rule.verifiedAt);
  extras.forEach((text) => {
    const paragraph = document.createElement("p");
    paragraph.className = "muted rule-extra";
    paragraph.textContent = text;
    panel.appendChild(paragraph);
  });
}

async function renderDistrictRules(item) {
  if (!item.disposalTypeId) {
    showRulesMessage("District information is not available for this item yet.");
    return;
  }

  showRulesLoading();

  let district;
  try {
    district = await loadDistrictWithId();
  } catch (e) {
    showRulesMessage(e.message);
    return;
  }
  if (!district) {
    showRulesMessage("Select your district to see the disposal schedule.");
    return;
  }

  let rule;
  try {
    rule = await fetchDistrictRule(district.id, item.disposalTypeId);
  } catch (e) {
    if (e.code === "DISTRICT_404") {
      clearDistrict();
      renderDistrictBar();
      showRulesMessage("Please select your district again.");
    } else {
      showRulesMessage(e.message);
    }
    return;
  }

  if (rule.status === "NO_INFO") {
    showRulesMessage("No schedule is available for this district yet. Please check with the district office.");
  } else if (rule.status === "UNSUPPORTED") {
    showRulesMessage("District information is not supported for this type of waste yet.");
  } else {
    renderRule(rule);
  }
}

async function renderResultPage() {
  const itemName = document.getElementById("result-item");
  const type = document.getElementById("result-type");
  const status = document.getElementById("result-status");
  const checkCondition = document.getElementById("check-condition");

  // 체크리스트 API가 아직 없어서, 연결되기 전까지는 진입 버튼을 숨긴다
  checkCondition.hidden = true;

  // 품목을 못 불러왔을 때 배너에 안내를 보여주고, 거짓 상태(Recyclable 등)와 지역 정보 표는 숨긴다
  function showProblem(title, message) {
    itemName.textContent = title;
    type.textContent = message;
    status.hidden = true;
    document.getElementById("district-rules").hidden = true;
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

  // 지역 정보는 실패해도 품목 이름과 분류는 계속 보여준다
  renderDistrictRules(item);
}

renderResultPage();