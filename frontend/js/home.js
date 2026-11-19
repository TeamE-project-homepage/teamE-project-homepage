function createCategoryCard(category) {
  const link = document.createElement("a");
  link.className = "card";
  link.href = "items.html?category=" + encodeURIComponent(category.id);

  const image = document.createElement("div");
  image.className = "card-image";
  image.textContent = "image";

  const title = document.createElement("span");
  title.className = "card-title";
  title.textContent = category.name;

  link.append(image, title);
  return link;
}

function renderCategories() {
  const grid = document.getElementById("category-grid");
  MOCK_CATEGORIES.forEach((category) => {
    grid.appendChild(createCategoryCard(category));
  });
}

renderCategories();