const MOCK_CATEGORIES = [
  { id: "plastic", name: "Plastic" },
  { id: "paper", name: "Paper" },
  { id: "glass", name: "Glass" },
  { id: "can", name: "Cans & Metal" },
  { id: "food", name: "Food Waste" },
  { id: "clothing", name: "Clothing" },
];

const MOCK_ITEMS = [
  { id: 1, name: "PET Bottle", aliases: ["plastic bottle", "water bottle"], category: "plastic" },
  { id: 2, name: "Plastic Food Container", aliases: ["takeout container", "delivery container"], category: "plastic" },
  { id: 3, name: "Styrofoam", aliases: ["foam box", "polystyrene"], category: "plastic" },
  { id: 4, name: "Cardboard Box", aliases: ["box", "carton"], category: "paper" },
  { id: 5, name: "Newspaper", aliases: ["newsprint"], category: "paper" },
  { id: 6, name: "Paper Cup", aliases: ["coffee cup"], category: "paper" },
  { id: 7, name: "Milk Carton", aliases: ["paper pack", "juice carton"], category: "paper" },
  { id: 8, name: "Glass Bottle", aliases: ["beer bottle", "soju bottle"], category: "glass" },
  { id: 9, name: "Glass Jar", aliases: ["jam jar"], category: "glass" },
  { id: 10, name: "Aluminum Can", aliases: ["soda can", "beer can", "drink can"], category: "can" },
  { id: 11, name: "Tuna Can", aliases: ["canned food", "tin can"], category: "can" },
  { id: 12, name: "Fruit Peel", aliases: ["orange peel", "banana peel"], category: "food" },
  { id: 13, name: "Leftover Rice", aliases: ["cooked rice"], category: "food" },
  { id: 14, name: "Vegetable Scraps", aliases: ["vegetable waste"], category: "food" },
  { id: 15, name: "T-shirt", aliases: ["shirt", "clothes"], category: "clothing" },
  { id: 16, name: "Jeans", aliases: ["pants", "trousers"], category: "clothing" },
];