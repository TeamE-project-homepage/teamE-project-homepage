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

async function renderCategories() {
  const grid = document.getElementById("category-grid");

  const note = document.createElement("p");
  note.className = "muted";
  note.setAttribute("role", "status");
  note.textContent = "Loading categories... The server may take a moment to wake up.";
  grid.after(note);

  let categories;
  try {
    categories = await fetchCategories();
  } catch (e) {
    note.textContent = e.message;
    return;
  }

  note.remove();
  categories.forEach((category) => {
    grid.appendChild(createCategoryCard(category));
  });
}

renderCategories();