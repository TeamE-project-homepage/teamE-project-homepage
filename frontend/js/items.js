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

// 카테고리 안에서 검색, 서버 검색은 카테고리를 구분하지 못해 전체에서 찾은 뒤 거른다
// 결과가 최대 개수에 닿았다면 뒤쪽 결과가 잘릴 수 있으니 그 카테고리 품목에서 이름으로도 찾아 합친다
async function searchInCategory(q, category) {
  const found = await searchItems(q);
  const inCategory = found.filter((item) => String(item.category) === category);
  if (found.length < SEARCH_LIMIT) return inCategory;

  const keyword = normalize(q);
  const categoryItems = await fetchItems(category);
  const known = new Set(inCategory.map((item) => item.id));
  const byName = categoryItems.filter(
    (item) => !known.has(item.id) && normalize(item.name).includes(keyword)
  );
  return inCategory.concat(byName);
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
  count.textContent = "Loading...";

  let categories = [];
  let items;
  try {
    [categories, items] = await Promise.all([
      category ? fetchCategories() : Promise.resolve([]),
      keyword
        ? category
          ? searchInCategory(q, category)
          : searchItems(q)
        : fetchItems(category),
    ]);
  } catch (e) {
    count.textContent = "";
    if (category && !keyword && (e.code === "CATEGORY_404" || e.code === "COMMON_400")) {
      title.textContent = "Category not found";
      showEmpty("We could not find that category.", "empty");
    } else if (keyword && e.code === "COMMON_400") {
      showEmpty("Please use a search keyword of 50 characters or fewer.", "empty");
    } else {
      title.textContent = category && !keyword ? "Items" : title.textContent;
      showEmpty(e.message, "error");
    }
    return;
  }

  if (category) {
    const categoryInfo = categories.find((c) => String(c.id) === category);
    if (!categoryInfo) {
      count.textContent = "";
      title.textContent = "Category not found";
      showEmpty("We could not find that category.", "empty");
      return;
    }
    title.textContent = categoryInfo.name;

    // 카테고리 안에서 검색할 수 있게 카테고리를 검색 폼에 함께 넘깁니다.
    const hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.name = "category";
    hidden.value = category;
    document.querySelector(".search-form").appendChild(hidden);
  }

  const shown = items;
  count.textContent = shown.length + (shown.length === 1 ? " item" : " items");

  if (shown.length === 0) {
    showEmpty("No items found. Try another keyword.", "empty");
    return;
  }

  shown.forEach((item) => grid.appendChild(createCard(item)));
}

renderItemsPage();