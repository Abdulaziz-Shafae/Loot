const names = ["Chicken Rice", "Cheese Toast", "Tuna Sandwich", "Chicken Pasta", "Spaghetti Bolognese", "Beef Stir Fry", "Grilled Chicken Salad", "Cheese Omelette", "Egg Toast", "Banana Oat Bowl", "Tuna Toast", "Apple Yogurt Bowl"];
const images = Object.fromEntries(names.map((name) => [name.toLowerCase(), `/recipes/${name.toLowerCase().replaceAll(" ", "-")}.jpg`]));
export const recipeFallback = (recipe) => images[recipe.name?.trim().toLowerCase()] || "/recipes/fallback.svg";
