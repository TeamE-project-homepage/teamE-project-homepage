let districts = [];

function chooseDistrict(code) {
  const district = districts.find((d) => d.code === code);
  if (!district) return;

  if (!saveDistrict(district)) {
    document.getElementById("district-status").textContent =
      "Could not save your district. Please check your browser settings and try again.";
    return;
  }

  window.location.href = "index.html";
}

function renderDistrictList(select, list) {
  districts.forEach((d) => {
    const option = document.createElement("option");
    option.value = d.code;
    option.textContent = d.name;
    select.appendChild(option);
  });
  select.addEventListener("change", () => chooseDistrict(select.value));

  districts.forEach((d) => {
    const item = document.createElement("li");
    item.className = "list-item";

    const name = document.createElement("span");
    name.className = "grow";
    name.textContent = d.name;

    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-secondary";
    button.textContent = "Select";
    button.addEventListener("click", () => chooseDistrict(d.code));

    item.append(name, button);
    list.appendChild(item);
  });
}

async function renderDistrictPage() {
  const select = document.getElementById("district");
  const list = document.getElementById("district-list");
  const status = document.getElementById("district-status");

  status.textContent = "Loading districts... The server may take a moment to wake up.";

  try {
    districts = await fetchDistricts();
  } catch (e) {
    status.textContent = e.message;
    return;
  }

  renderDistrictList(select, list);

  // 예전 가짜 코드(예: "jongno")가 저장돼 있으면 목록에 없으므로 선택 안 됨
  const saved = loadDistrict();
  const current = saved && districts.find((d) => d.code === saved.code);
  if (current) {
    select.value = current.code;
    status.textContent = "Current district: " + current.name;
  } else {
    // 목록에 없는 예전 값(예: "jongno")은 다른 화면에 남지 않게 지운다
    if (saved) clearDistrict();
    status.textContent = "Please select your district.";
  }
}

renderDistrictPage();