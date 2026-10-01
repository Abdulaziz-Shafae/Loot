import { useState } from "react";
import {
  Plus,
  LayoutGrid,
  List,
  Leaf,
  Pencil,
  Trash2,
  Mail,
  Search,
} from "lucide-react";
import { usePreferences } from "../context/Preferences";
import { useLoad } from "../hooks/useLoad";
import { pantryApi } from "../api/pantryApi";
import {
  PageHead,
  Loading,
  ErrorState,
  Empty,
  Modal,
  Field,
  Submit,
  Confirm,
  useToast,
} from "../components/UI";
export default function Pantry() {
  const { t, unit } = usePreferences();
  const toast = useToast();
  const [view, setView] = useState("cards"),
    [search, setSearch] = useState(""),
    [editing, setEditing] = useState(null),
    [remove, setRemove] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  const state = useLoad(async () => {
    const [items, ingredients] = await Promise.all([
      pantryApi.list(),
      pantryApi.ingredients(),
    ]);
    return { items, ingredients };
  });
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  const { items, ingredients } = state.data;
  const rows = items
    .map((p) => ({
      ...p,
      ingredient: ingredients.find((i) => i.id === p.ingredientId),
    }))
    .filter((p) =>
      p.ingredient?.name.toLowerCase().includes(search.toLowerCase()),
    );
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const d = Object.fromEntries(new FormData(e.currentTarget));
    d.ingredientId = Number(d.ingredientId);
    d.quantity = Number(d.quantity);
    d.lowStockThreshold = Number(d.lowStockThreshold);
    try {
      editing.id ? await pantryApi.edit(editing.id, d) : await pantryApi.add(d);
      setEditing(null);
      state.reload();
      toast(t("Pantry updated", "تم تحديث المؤن"));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="page">
      <PageHead
        eyebrow={t("KNOW WHAT YOU HAVE", "اعرف ما لديك")}
        title={t("Your pantry", "مؤنك")}
        description={t(
          "Little ingredients. So many possibilities.",
          "مكونات بسيطة. إمكانيات كثيرة.",
        )}
      >
        <button
          className="button secondary"
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            try {
              const result = await pantryApi.email();
                toast(
                result?.message === "No low stock ingredients"
                  ? t("No low-stock ingredients to send.", "لا توجد مكونات منخفضة المخزون لإرسالها.")
                  : t("Your missing ingredients list has been sent. Email delivery may take a few minutes.", "تم إرسال قائمة المكونات الناقصة. قد يستغرق وصول البريد الإلكتروني بضع دقائق."),
              );
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Mail size={17} />
          {t("Email low stock", "أرسل النواقص")}
        </button>
        <button
          className="button"
          onClick={() => {
            setError(null);
            setEditing({});
          }}
        >
          <Plus size={18} />
          {t("Add ingredient", "أضف مكوّناً")}
        </button>
      </PageHead>
      {error && !editing && <ErrorState error={error} />}
      <div className="toolbar">
        <label className="search">
          <Search size={19} />
          <input
            aria-label={t("Search pantry", "ابحث في المؤن")}
            placeholder={t("Find an ingredient…", "ابحث عن مكوّن…")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        <div className="segmented">
          <button
            aria-label={t("Card view", "عرض البطاقات")}
            aria-pressed={view === "cards"}
            onClick={() => setView("cards")}
          >
            <LayoutGrid size={18} />
          </button>
          <button
            aria-label={t("List view", "عرض القائمة")}
            aria-pressed={view === "list"}
            onClick={() => setView("list")}
          >
            <List size={18} />
          </button>
        </div>
      </div>
      {rows.length ? (
        <div className={"pantry-grid " + (view === "list" ? "list-view" : "")}>
          {rows.map((p) => (
            <article className="pantry-card" key={p.id}>
              <div className="icon-tile">
                <Leaf />
              </div>
              <div className="pantry-name">
                <h3>{p.ingredient?.name}</h3>
                <span
                  className={
                    "badge " +
                    (p.quantity <= p.lowStockThreshold ? "warning" : "green")
                  }
                >
                  {p.quantity <= p.lowStockThreshold
                    ? t("Running low", "مخزون منخفض")
                    : t("In stock", "متوفر")}
                </span>
              </div>
              <div className="quantity">
                <strong>{p.quantity}</strong> <span>{unit(p.ingredient?.unit)}</span>
                <small>
                  {t("Low-stock threshold", "حد المخزون المنخفض")}:{" "}
                  {p.lowStockThreshold}
                </small>
              </div>
              <div className="actions">
                <button
                  className="icon-button"
                  aria-label={t("Edit ", "تعديل ") + p.ingredient?.name}
                  onClick={() => {
                    setError(null);
                    setEditing(p);
                  }}
                >
                  <Pencil size={17} />
                </button>
                <button
                  className="icon-button"
                  aria-label={t("Delete ", "حذف ") + p.ingredient?.name}
                  onClick={() => setRemove(p)}
                >
                  <Trash2 size={17} />
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty title={t("Make room for good food", "مساحة للطعام اللذيذ")}>
          <p>
            {t(
              "Add your first ingredient to get started, or try another search.",
              "أضف أول مكوّن للبدء أو جرّب بحثاً آخر.",
            )}
          </p>
        </Empty>
      )}
      {editing && (
        <Modal
          title={
            editing.id
              ? t("Edit ingredient", "تعديل المكوّن")
              : t("Add to your pantry", "أضف إلى مؤنك")
          }
          onClose={() => !busy && setEditing(null)}
        >
          <form onSubmit={save}>
            <Field label={t("Ingredient", "المكوّن")}>
              <select
                name="ingredientId"
                defaultValue={editing.ingredientId || ""}
                required
                disabled={!!editing.id}
              >
                <option value="">
                  {t("Choose an ingredient", "اختر مكوّناً")}
                </option>
                {ingredients.map((i) => (
                  <option value={i.id} key={i.id}>
                    {i.name} ({unit(i.unit)})
                  </option>
                ))}
              </select>
            </Field>
            {editing.id && (
              <input
                type="hidden"
                name="ingredientId"
                value={editing.ingredientId}
              />
            )}
            <Field
              label={t("Quantity", "الكمية")}
              name="quantity"
              type="number"
              min="0"
              step="0.01"
              max="100000000"
              defaultValue={editing.quantity ?? 1}
              required
            />
            <Field
              label={t("Low-stock threshold", "حد المخزون المنخفض")}
              name="lowStockThreshold"
              type="number"
              min="0"
              step="0.01"
              max="100000000"
              defaultValue={editing.lowStockThreshold ?? 0}
              required
            />
            {!ingredients.length && (
              <p>
                {t(
                  "The ingredient catalog is empty. Ask your admin to add ingredients, or use Loot AI image recognition.",
                  "قائمة المكونات فارغة. اطلب من المسؤول إضافة المكونات أو استخدم التعرّف على الصور في ذكاء لوت.",
                )}
              </p>
            )}
            {error && <ErrorState error={error} />}
            <Submit busy={busy}>{t("Save ingredient", "حفظ المكوّن")}</Submit>
          </form>
        </Modal>
      )}
      {remove && (
        <Confirm
          title={t("Remove this pantry item?", "حذف هذا المكوّن من مؤنك؟")}
          onClose={() => setRemove(null)}
          onConfirm={async () => {
            await pantryApi.remove(remove.id);
            state.reload();
            toast(t("Ingredient removed", "تم حذف المكوّن"));
          }}
        />
      )}
    </div>
  );
}
