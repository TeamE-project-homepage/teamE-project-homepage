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

function findItems(q, categoryId) {
  const keyword = normalize(q);
  return MOCK_ITEMS.filter((item) => {
    if (categoryId && item.category !== categoryId) return false;
    if (!keyword) return true;
    const names = [item.name, ...item.aliases].map(normalize);
    return names.some((name) => name.includes(keyword));
  });
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

function renderItemsPage() {
  const { hasQ, q, category } = getParams();
  const title = document.getElementById("items-title");
  const count = document.getElementById("items-count");
  const grid = document.getElementById("item-grid");
  const empty = document.getElementById("items-empty");
  const emptyText = document.getElementById("items-empty-text");
  const input = document.querySelector('.search-form input[name="q"]');

  input.value = q;
  grid.replaceChildren();

  function showEmpty(message) {
    emptyText.textContent = message;
    empty.hidden = false;
  }

  const categoryInfo = MOCK_CATEGORIES.find((c) => c.id === category);
  const keyword = normalize(q);
  if (categoryInfo) {
    title.textContent = categoryInfo.name;
  } else if (keyword) {
    title.textContent = 'Results for "' + q.trim() + '"';
  } else if (hasQ) {
    title.textContent = "Search";
  } else {
    title.textContent = "All items";
  }

  if (hasQ && !keyword && !categoryInfo) {
    count.textContent = "";
    showEmpty("Please enter a search keyword.");
    return;
  }

  const items = findItems(q, category);
  count.textContent = items.length + (items.length === 1 ? " item" : " items");

  if (items.length === 0) {
    showEmpty("No items found. Try another keyword.");
    return;
  }

  empty.hidden = true;
  items.forEach((item) => grid.appendChild(createCard(item)));
}

renderItemsPage();