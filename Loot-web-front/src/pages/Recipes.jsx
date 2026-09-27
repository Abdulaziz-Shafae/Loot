import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import { recipeApi } from "../api/recipeApi";
import { useLoad } from "../hooks/useLoad";
import { usePreferences } from "../context/Preferences";
import { PageHead, Loading, ErrorState, Empty } from "../components/UI";
import RecipeCard from "../components/RecipeCard";
import RecipeEditor, {
  categories,
  categoryArabic,
} from "../components/RecipeEditor";
export default function Recipes({ admin = false }) {
  const { t } = usePreferences();
  const navigate = useNavigate();
  const [type, setType] = useState("system"),
    [category, setCategory] = useState(""),
    [availability, setAvailability] = useState(""),
    [search, setSearch] = useState(""),
    [create, setCreate] = useState(false);
  const state = useLoad(async () => {
    const [recipes, status] = await Promise.all([
      recipeApi.list(type),
      recipeApi.availability(type),
    ]);
    return { recipes, status };
  }, [type]);
  const rows = (state.data?.recipes || [])
    .map((r) => ({
      ...r,
      availability: state.data.status.find((s) => s.id === r.id),
    }))
    .filter(
      (r) =>
        (!category || r.category === category) &&
        (!availability || r.availability?.[availability]) &&
        r.name.toLowerCase().includes(search.toLowerCase()),
    );
  return (
    <div className={admin ? "" : "page"}>
      <PageHead
        eyebrow={t("A LITTLE INSPIRATION", "قليل من الإلهام")}
        title={
          admin
            ? t("System recipes", "وصفات النظام")
            : t("What sounds good?", "ماذا تشتهي اليوم؟")
        }
        description={t(
          "Find a new favourite, or make something your own.",
          "اكتشف وصفة جديدة أو ابتكر وصفتك الخاصة.",
        )}
      >
        <button className="button" onClick={() => setCreate(true)}>
          <Plus size={18} />
          {admin
            ? t("Add system recipe", "أضف وصفة للنظام")
            : t("Create my recipe", "أنشئ وصفتي")}
        </button>
      </PageHead>
      <div className="toolbar">
        {!admin && (
          <div className="segmented">
            <button
              aria-pressed={type === "system"}
              onClick={() => setType("system")}
            >
              {t("Loot recipes", "وصفات لوت")}
            </button>
            <button
              aria-pressed={type === "user"}
              onClick={() => setType("user")}
            >
              {t("My recipes", "وصفاتي")}
            </button>
          </div>
        )}
        <label className="search">
          <Search size={18} />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("Search recipes…", "ابحث عن وصفة…")}
            aria-label={t("Search recipes", "بحث الوصفات")}
          />
        </label>
      </div>
      <div className="filters">
        <div className="category-tabs">
          <button
            className={!category ? "active" : ""}
            onClick={() => setCategory("")}
          >
            {t("All meals", "كل الوجبات")}
          </button>
          {categories.map((c) => (
            <button
              key={c}
              className={category === c ? "active" : ""}
              onClick={() => setCategory(c)}
            >
              {t(c, categoryArabic[c])}
            </button>
          ))}
        </div>
        <label className="availability-select">
          <span className="sr-only">{t("Availability", "الجاهزية")}</span>
          <select
            value={availability}
            onChange={(e) => setAvailability(e.target.value)}
          >
            <option value="">{t("All availability", "كل الوصفات")}</option>
            <option value="canCook">{t("Can cook", "يمكن طبخها")}</option>
            <option value="almost">
              {t("Almost possible", "قريبة من الجاهزية")}
            </option>
          </select>
        </label>
      </div>
      {state.loading ? (
        <Loading />
      ) : state.error ? (
        <ErrorState error={state.error} retry={state.reload} />
      ) : rows.length ? (
        <div className="recipe-grid">
          {rows.map((r) => (
            <RecipeCard
              key={r.id}
              recipe={r}
              type={type}
              availability={r.availability}
            />
          ))}
        </div>
      ) : (
        <Empty
          title={t("A new favourite is on its way", "وصفتك المفضلة في انتظارك")}
        >
          <p>
            {t(
              "Try a different filter or create a recipe of your own.",
              "جرّب تصفية أخرى أو أنشئ وصفتك الخاصة.",
            )}
          </p>
        </Empty>
      )}
      {create && (
        <RecipeEditor
          type={admin ? "system" : "user"}
          onClose={() => setCreate(false)}
          onSaved={(id) => {
            setCreate(false);
            navigate(`/recipes/${admin ? "system" : "user"}/${id}`);
          }}
        />
      )}
    </div>
  );
}
