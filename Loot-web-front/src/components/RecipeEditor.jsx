import { useState } from "react";
import { recipeApi } from "../api/recipeApi";
import { usePreferences } from "../context/Preferences";
import { Modal, Field, Submit, ErrorState } from "./UI";
export const categories = ["Breakfast", "Lunch", "Dinner", "Snack"];
export const categoryArabic = {
  Breakfast: "فطور",
  Lunch: "غداء",
  Dinner: "عشاء",
  Snack: "وجبة خفيفة",
};
export default function RecipeEditor({ recipe = {}, type, onClose, onSaved }) {
  const { t } = usePreferences();
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const d = Object.fromEntries(new FormData(e.currentTarget));
    try {
      const result = recipe.id
        ? await recipeApi.edit(type, recipe.id, d)
        : await recipeApi.add(type, d);
      onSaved(recipe.id || result.id);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={
        recipe.id
          ? t("Edit recipe", "تعديل الوصفة")
          : t("A recipe of your own", "وصفتك الخاصة")
      }
      onClose={() => !busy && onClose()}
    >
      <form onSubmit={save}>
        <Field
          label={t("Recipe name", "اسم الوصفة")}
          name="name"
          defaultValue={recipe.name}
          required
          maxLength={150}
        />
        <Field label={t("Category", "الفئة")}>
          <select name="category" defaultValue={recipe.category || "Dinner"}>
            {categories.map((c) => (
              <option key={c} value={c}>
                {t(c, categoryArabic[c])}
              </option>
            ))}
          </select>
        </Field>
        <Field label={t("Description", "الوصف")}>
          <textarea
            name="description"
            defaultValue={recipe.description}
            maxLength={500}
            rows={2}
          />
        </Field>
        <Field label={t("Instructions", "الخطوات")}>
          <textarea
            name="instructions"
            defaultValue={recipe.instructions}
            required
            maxLength={20000}
            rows={5}
          />
        </Field>
        <Field
          label={t(
            "Image URL (optional, HTTPS)",
            "رابط الصورة (اختياري، HTTPS)",
          )}
          name="imageUrl"
          type="url"
          pattern="https://.*"
          maxLength={2048}
          defaultValue={recipe.imageUrl || ""}
        />
        <p className="small muted">
          {t(
            "Save your recipe, then add its ingredients on the next screen.",
            "احفظ الوصفة ثم أضف مكوناتها في الصفحة التالية.",
          )}
        </p>
        {error && <ErrorState error={error} />}
        <Submit busy={busy}>{t("Save recipe", "حفظ الوصفة")}</Submit>
      </form>
    </Modal>
  );
}
