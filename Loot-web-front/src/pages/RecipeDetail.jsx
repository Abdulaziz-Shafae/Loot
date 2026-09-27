import { useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { ChefHat, Plus, Pencil, Trash2, Copy, ArrowLeft } from "lucide-react";
import { recipeApi } from "../api/recipeApi";
import { pantryApi } from "../api/pantryApi";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/Preferences";
import { useLoad } from "../hooks/useLoad";
import {
  Loading,
  ErrorState,
  Field,
  Modal,
  Submit,
  Confirm,
  useToast,
} from "../components/UI";
import { RecipeImage } from "../components/RecipeCard";
import RecipeEditor, { categoryArabic } from "../components/RecipeEditor";
export default function RecipeDetail() {
  const { type, id } = useParams();
  const { user } = useAuth();
  const { t } = usePreferences();
  const navigate = useNavigate(),
    toast = useToast();
  const [edit, setEdit] = useState(false),
    [ingredient, setIngredient] = useState(null),
    [confirm, setConfirm] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  const canEdit = type === "user" || user.role === "ADMIN";
  const state = useLoad(async () => {
    if (!["system", "user"].includes(type)) throw new Error("Invalid type");
    const [all, detail, links, catalog] = await Promise.all([
      recipeApi.list(type),
      recipeApi.detail(type, id),
      recipeApi.ingredients(type),
      pantryApi.ingredients(),
    ]);
    const recipe = all.find((r) => r.id === Number(id));
    if (!recipe) throw new Error("Recipe not found");
    return {
      recipe,
      detail,
      links: links.filter(
        (x) =>
          x[type === "system" ? "systemRecipeId" : "userRecipeId"] ===
          Number(id),
      ),
      catalog,
    };
  }, [type, id]);
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  const { recipe, detail, links, catalog } = state.data;
  async function saveIngredient(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const values = Object.fromEntries(new FormData(e.currentTarget));
    const body = {
      ingredientId: Number(values.ingredientId),
      requiredQuantity: Number(values.requiredQuantity),
      [type === "system" ? "systemRecipeId" : "userRecipeId"]: Number(id),
    };
    try {
      ingredient.id
        ? await recipeApi.editIngredient(type, ingredient.id, body)
        : await recipeApi.addIngredient(type, body);
      setIngredient(null);
      state.reload();
      toast(t("Ingredients updated", "تم تحديث المكونات"));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="page">
      <Link to="/recipes" className="text-link">
        <ArrowLeft size={17} />
        {t("Back to recipes", "العودة للوصفات")}
      </Link>
      <div className="recipe-detail">
        <RecipeImage recipe={recipe} />
        <div>
          <span className="eyebrow">
            {t(recipe.category, categoryArabic[recipe.category])} /{" "}
            {type === "system"
              ? t("Loot kitchen", "مطبخ لوت")
              : t("My recipe", "وصفتي")}
          </span>
          <h1>{recipe.name}</h1>
          <p>{recipe.description}</p>
          <span
            className={
              "badge " + (!detail.missing.length ? "green" : "warning")
            }
          >
            {!detail.missing.length
              ? t("Ready to cook", "جاهزة للطبخ")
              : t("A few ingredients to pick up", "بعض المكونات تنقصك")}
          </span>
          <div className="actions detail-actions">
            <button
              className="button orange"
              disabled={detail.missing.length > 0 || !links.length}
              onClick={() =>
                setConfirm({
                  title: t(
                    "Cook this recipe and update your pantry?",
                    "طبخ هذه الوصفة وتحديث المؤن؟",
                  ),
                  run: async () => {
                    await recipeApi.cook(type, id);
                    state.reload();
                    toast(
                      t(
                        "Meal cooked. Your pantry and history are updated.",
                        "تم الطبخ وتحديث المؤن والسجل.",
                      ),
                    );
                  },
                })
              }
            >
              <ChefHat size={18} />
              {t("Let’s cook", "لنطبخ")}
            </button>
            {type === "system" && (
              <button
                className="button secondary"
                onClick={() =>
                  setConfirm({
                    title: t(
                      "Save a copy to my recipes?",
                      "حفظ نسخة في وصفاتي؟",
                    ),
                    run: async () => {
                      await recipeApi.convert(id);
                      toast(t("Saved to my recipes", "تم الحفظ في وصفاتي"));
                    },
                  })
                }
              >
                <Copy size={17} />
                {t("Make it mine", "اجعلها وصفتي")}
              </button>
            )}
            {canEdit && (
              <>
                <button
                  className="icon-button"
                  aria-label={t("Edit recipe", "تعديل الوصفة")}
                  onClick={() => setEdit(true)}
                >
                  <Pencil size={19} />
                </button>
                <button
                  className="icon-button"
                  aria-label={t("Delete recipe", "حذف الوصفة")}
                  onClick={() =>
                    setConfirm({
                      title: t("Delete this recipe?", "حذف هذه الوصفة؟"),
                      run: async () => {
                        await recipeApi.remove(type, id);
                        navigate("/recipes");
                      },
                    })
                  }
                >
                  <Trash2 size={19} />
                </button>
              </>
            )}
          </div>
          {!links.length && (
            <p className="small muted">
              {t(
                "Add ingredients before cooking this recipe.",
                "أضف المكونات قبل طبخ هذه الوصفة.",
              )}
            </p>
          )}
        </div>
      </div>
      <div className="recipe-columns">
        <section className="panel">
          <div className="between">
            <h2>{t("The ingredients", "المكونات")}</h2>
            {canEdit && (
              <button
                className="icon-button"
                aria-label={t("Add ingredient", "أضف مكوّناً")}
                onClick={() => {
                  setError(null);
                  setIngredient({});
                }}
              >
                <Plus />
              </button>
            )}
          </div>
          {links.map((row) => {
            const item = catalog.find((c) => c.id === row.ingredientId);
            const missing = detail.missing.find((m) => m.name === item?.name);
            return (
              <div className="ingredient-row" key={row.id}>
                <div>
                  <b>{item?.name}</b>
                  <span>
                    {row.requiredQuantity} {item?.unit}
                  </span>
                  {missing && (
                    <small className="warning-text">
                      {t("Available", "المتوفر")}: {missing.available} ·{" "}
                      {t("Missing", "الناقص")}: {missing.missing} {item?.unit}
                    </small>
                  )}
                </div>
                {canEdit && (
                  <div className="actions">
                    <button
                      className="icon-button"
                      aria-label={t("Edit ingredient", "تعديل المكوّن")}
                      onClick={() => {
                        setError(null);
                        setIngredient(row);
                      }}
                    >
                      <Pencil size={16} />
                    </button>
                    <button
                      className="icon-button"
                      aria-label={t("Remove ingredient", "حذف المكوّن")}
                      onClick={() =>
                        setConfirm({
                          title: t(
                            "Remove ingredient from recipe?",
                            "حذف المكوّن من الوصفة؟",
                          ),
                          run: async () => {
                            await recipeApi.removeIngredient(type, row.id);
                            state.reload();
                          },
                        })
                      }
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </section>
        <section className="panel instructions">
          <span className="eyebrow">
            {t("LET’S MAKE SOMETHING GOOD", "لنصنع شيئاً لذيذاً")}
          </span>
          <h2>{t("In your kitchen", "في مطبخك")}</h2>
          <p>{recipe.instructions}</p>
        </section>
      </div>
      {edit && (
        <RecipeEditor
          recipe={recipe}
          type={type}
          onClose={() => setEdit(false)}
          onSaved={() => {
            setEdit(false);
            state.reload();
          }}
        />
      )}
      {ingredient && (
        <Modal
          title={t("Recipe ingredient", "مكوّن الوصفة")}
          onClose={() => !busy && setIngredient(null)}
        >
          <form onSubmit={saveIngredient}>
            <Field label={t("Ingredient", "المكوّن")}>
              <select
                name="ingredientId"
                defaultValue={ingredient.ingredientId || ""}
                required
                disabled={!!ingredient.id}
              >
                <option value="">
                  {t("Choose ingredient", "اختر مكوّناً")}
                </option>
                {catalog.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.unit})
                  </option>
                ))}
              </select>
            </Field>
            {ingredient.id && (
              <input
                type="hidden"
                name="ingredientId"
                value={ingredient.ingredientId}
              />
            )}
            <Field
              label={t("Required quantity", "الكمية المطلوبة")}
              name="requiredQuantity"
              type="number"
              min="0.01"
              step="0.01"
              max="100000000"
              defaultValue={ingredient.requiredQuantity || 1}
              required
            />
            {error && <ErrorState error={error} />}
            <Submit busy={busy}>{t("Save ingredient", "حفظ المكوّن")}</Submit>
          </form>
        </Modal>
      )}
      {confirm && (
        <Confirm
          title={confirm.title}
          onConfirm={confirm.run}
          onClose={() => setConfirm(null)}
        />
      )}
    </div>
  );
}
