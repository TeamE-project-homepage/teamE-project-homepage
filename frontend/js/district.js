const DISTRICTS = [
  { code: "jongno", name: "Jongno-gu" },
  { code: "jung", name: "Jung-gu" },
  { code: "yongsan", name: "Yongsan-gu" },
  { code: "seongdong", name: "Seongdong-gu" },
  { code: "gwangjin", name: "Gwangjin-gu" },
  { code: "dongdaemun", name: "Dongdaemun-gu" },
  { code: "jungnang", name: "Jungnang-gu" },
  { code: "seongbuk", name: "Seongbuk-gu" },
  { code: "gangbuk", name: "Gangbuk-gu" },
  { code: "dobong", name: "Dobong-gu" },
  { code: "nowon", name: "Nowon-gu" },
  { code: "eunpyeong", name: "Eunpyeong-gu" },
  { code: "seodaemun", name: "Seodaemun-gu" },
  { code: "mapo", name: "Mapo-gu" },
  { code: "yangcheon", name: "Yangcheon-gu" },
  { code: "gangseo", name: "Gangseo-gu" },
  { code: "guro", name: "Guro-gu" },
  { code: "geumcheon", name: "Geumcheon-gu" },
  { code: "yeongdeungpo", name: "Yeongdeungpo-gu" },
  { code: "dongjak", name: "Dongjak-gu" },
  { code: "gwanak", name: "Gwanak-gu" },
  { code: "seocho", name: "Seocho-gu" },
  { code: "gangnam", name: "Gangnam-gu" },
  { code: "songpa", name: "Songpa-gu" },
  { code: "gangdong", name: "Gangdong-gu" },
];

function chooseDistrict(code) {
  const district = DISTRICTS.find((d) => d.code === code);
  if (!district) return;

  if (!saveDistrict(district)) {
    document.getElementById("district-status").textContent =
      "Could not save your district. Please check your browser settings and try again.";
    return;
  }

  window.location.href = "index.html";
}

function renderDistrictPage() {
  const select = document.getElementById("district");
  const list = document.getElementById("district-list");
  const status = document.getElementById("district-status");
  const current = loadDistrict();

  DISTRICTS.forEach((d) => {
    const option = document.createElement("option");
    option.value = d.code;
    option.textContent = d.name;
    select.appendChild(option);
  });
  select.addEventListener("change", () => chooseDistrict(select.value));

  DISTRICTS.forEach((d) => {
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

  if (current) {
    select.value = current.code;
    status.textContent = "Current district: " + current.name;
  } else {
    status.textContent = "Please select your district.";
  }
}

renderDistrictPage();