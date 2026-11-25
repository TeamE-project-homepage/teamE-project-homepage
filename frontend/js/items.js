function normalize(text) {
  return text.trim().toLowerCase();
}

function getParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    hasQ: params.has("q"),
    q: params.get("q") || "",
    category: params.get("category") || "",
  };
}

// 검색어가 있으면 이름에 포함된 품목만 남긴다(서버 검색으로 바꾸는 건 다음 PR)
function filterByKeyword(items, q) {
  const keyword = normalize(q);
  if (!keyword) return items;
  return items.filter((item) => normalize(item.name).includes(keyword));
}

function createCard(item) {
  const link = document.createElement("a");
  link.className = "card";
  link.href = "result.html?item=" + encodeURIComponent(item.id);

  const image = document.createElement("div");
  image.className = "card-image";
  image.textContent = "image";

  const title = document.createElement("span");
  title.className = "card-title";
  title.textContent = item.name;

  link.append(image, title);
  return link;
}

async function renderItemsPage() {
  const { hasQ, q, category } = getParams();
  const title = document.getElementById("items-title");
  const count = document.getElementById("items-count");
  const grid = document.getElementById("item-grid");
  const empty = document.getElementById("items-empty");
  const emptyText = document.getElementById("items-empty-text");
  const input = document.querySelector('.search-form input[name="q"]');

  input.value = q;
  grid.replaceChildren();
  empty.hidden = true;

  // state: "empty" = 결과 없음, "error" = 서버 연결·응답 문제
  function showEmpty(message, state) {
    empty.dataset.state = state;
    emptyText.textContent = message;
    empty.hidden = false;
  }

  const keyword = normalize(q);

  if (hasQ && !keyword && !category) {
    title.textContent = "Search";
    count.textContent = "";
    showEmpty("Please enter a search keyword.", "empty");
    return;
  }

  title.textContent = category
    ? "Loading..."
    : keyword
      ? 'Results for "' + q.trim() + '"'
      : hasQ
        ? "Search"
        : "All items";
  count.textContent = "";

  let categories = [];
  let items;
  try {
    [categories, items] = await Promise.all([
      category ? fetchCategories() : Promise.resolve([]),
      fetchItems(category),
    ]);
  } catch (e) {
    if (category && (e.code === "CATEGORY_404" || e.code === "COMMON_400")) {
      title.textContent = "Category not found";
      showEmpty("We could not find that category.", "empty");
    } else {
      title.textContent = category ? "Items" : title.textContent;
      showEmpty(e.message, "error");
    }
    return;
  }

  if (category) {
    const categoryInfo = categories.find((c) => String(c.id) === category);
    if (!categoryInfo) {
      title.textContent = "Category not found";
      showEmpty("We could not find that category.", "empty");
      return;
    }
    title.textContent = categoryInfo.name;

    // 카테고리 안에서 검색할 수 있게 카테고리를 검색 폼에 넘긴다
    const hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.name = "category";
    hidden.value = category;
    document.querySelector(".search-form").appendChild(hidden);
  }

  const shown = filterByKeyword(items, q);
  count.textContent = shown.length + (shown.length === 1 ? " item" : " items");

  if (shown.length === 0) {
    showEmpty("No items found. Try another keyword.", "empty");
    return;
  }

  shown.forEach((item) => grid.appendChild(createCard(item)));
}

renderItemsPage();