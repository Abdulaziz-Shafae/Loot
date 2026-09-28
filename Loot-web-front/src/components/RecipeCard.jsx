import { Link } from "react-router-dom";
import { ArrowUpRight, ChefHat } from "lucide-react";
import { usePreferences } from "../context/Preferences";
import { recipeFallback } from "./recipeImages";
export function RecipeImage({ recipe }) {
  return (
    <div
      className={"recipe-image " + (recipe.category || "Dinner").toLowerCase()}
    >
        <img
          key={recipe.imageUrl || recipe.name}
          src={recipe.imageUrl || recipeFallback(recipe)}
          alt={recipe.name}
          loading="lazy"
          referrerPolicy="no-referrer"
          onError={(e) => {
            const img = e.currentTarget;
            const fallback = recipeFallback(recipe);
            if (!img.dataset.fallback) { img.dataset.fallback = "true"; img.src = fallback; }
            else if (!img.src.endsWith("/recipes/fallback.svg")) img.src = "/recipes/fallback.svg";
          }}
        />
    </div>
  );
}
export default function RecipeCard({ recipe, type, availability }) {
  const { t } = usePreferences();
  return (
    <Link className="recipe-card" to={`/recipes/${type}/${recipe.id}`}>
      <RecipeImage recipe={recipe} />
      <div className="recipe-body">
        <div className="between">
          <span className="eyebrow">
            {t(
              recipe.category,
              {
                Breakfast: "فطور",
                Lunch: "غداء",
                Dinner: "عشاء",
                Snack: "وجبة خفيفة",
              }[recipe.category],
            )}
          </span>
          <span className="small muted">
            {type === "system"
              ? t("Loot kitchen", "مطبخ لوت")
              : t("My recipe", "وصفتي")}
          </span>
        </div>
        <h3>
          {recipe.name}
          <ArrowUpRight size={19} />
        </h3>
        <p>{recipe.description}</p>
        <span className={"badge " + (availability?.canCook ? "green" : "")}>
          <ChefHat size={14} />
          {availability?.canCook
            ? t("Ready to cook", "جاهزة للطبخ")
            : availability?.almost
              ? t("Almost possible", "قريبة من الجاهزية")
              : t("View ingredients", "عرض المكونات")}
        </span>
      </div>
    </Link>
  );
}
