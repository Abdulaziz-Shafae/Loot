import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Sparkles,
  ScanLine,
  ArrowLeftRight,
  ChefHat,
  Leaf,
  Lightbulb,
  Upload,
  Plus,
  Trash2,
} from "lucide-react";
import { aiApi } from "../api/aiApi";
import { recipeApi } from "../api/recipeApi";
import { pantryApi } from "../api/pantryApi";
import { usePreferences } from "../context/Preferences";
import { useLoad } from "../hooks/useLoad";
import {
  PageHead,
  Field,
  Submit,
  ErrorState,
  Loading,
  useToast,
} from "../components/UI";
import PantryPicker from "../components/PantryPicker";
const tools = [
  [
    "image",
    ScanLine,
    "Ingredient scanner",
    "ماسح المكونات",
    "A photo. A clearer pantry.",
    "صورة تجعل مؤنك أوضح.",
  ],
  [
    "substitute",
    ArrowLeftRight,
    "Find a substitute",
    "اكتشف بديلاً",
    "A little flexibility goes a long way.",
    "مرونة صغيرة تصنع فرقاً.",
  ],
  [
    "recommend",
    Lightbulb,
    "Meal inspiration",
    "إلهام الوجبات",
    "Your pantry, a fresh perspective.",
    "مؤنك بنظرة جديدة.",
  ],
  [
    "rescue",
    Leaf,
    "Leftover rescue",
    "إنقاذ البقايا",
    "Good food deserves a second chance.",
    "الطعام الجيد يستحق فرصة ثانية.",
  ],
  [
    "generate",
    ChefHat,
    "Recipe creator",
    "ابتكار وصفة",
    "Something delicious, made for you.",
    "شيء لذيذ، من أجلك.",
  ],
];
export default function AI() {
  const { t, unit } = usePreferences(),
    toast = useToast();
  const [tool, setTool] = useState("image"),
    [file, setFile] = useState(null),
    [preview, setPreview] = useState(""),
    [result, setResult] = useState(null),
    [error, setError] = useState(null),
    [busy, setBusy] = useState(false),
    [recipe, setRecipe] = useState(""),
    [leftovers, setLeftovers] = useState([
      { rowId: crypto.randomUUID(), ingredientId: "", quantity: 1, unit: "" },
    ]);
  const pantry = useLoad(async () => {
    const [rows, ingredients] = await Promise.all([pantryApi.list(), pantryApi.ingredients()]);
    return rows.map((p) => ({ ...p, ...ingredients.find((i) => i.id === p.ingredientId) })).filter((p) => p.name && p.quantity > 0);
  });
  const catalog = useLoad(async () => {
    const [system, user, ingredients, si, ui] = await Promise.all([
      recipeApi.list("system"),
      recipeApi.list("user"),
      pantryApi.ingredients(),
      recipeApi.ingredients("system"),
      recipeApi.ingredients("user"),
    ]);
    return {
      recipes: [
        ...system.map((r) => ({ ...r, type: "system" })),
        ...user.map((r) => ({ ...r, type: "user" })),
      ],
      ingredients,
      si,
      ui,
    };
  });
  useEffect(() => {
    if (!file) {
      setPreview("");
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  function chooseFile(file) {
    setError(null);
    if (
      file &&
      (!["image/jpeg", "image/png", "image/webp"].includes(file.type) ||
        file.size > 5 * 1024 * 1024)
    ) {
      setError({
        response: {
          status: 400,
          data: {
            message: t(
              "Choose a JPEG, PNG or WEBP up to 5 MB",
              "اختر صورة JPEG أو PNG أو WEBP بحجم لا يتجاوز ٥ ميجابايت",
            ),
          },
        },
      });
      return;
    }
    setFile(file);
    setResult(null);
  }
  async function run(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    const values = Object.fromEntries(new FormData(e.currentTarget));
    try {
      let value;
      if (tool === "image") value = await aiApi.image(file);
      if (tool === "substitute") {
        const [type, id] = recipe.split(":");
        value = await aiApi.substitute(type, id, values.ingredient);
      }
      if (tool === "recommend") value = await aiApi.recommend(values.request);
      if (tool === "rescue")
        value = await aiApi.rescue(
          leftovers.map((x) => { const item = pantry.data.find((p) => String(p.ingredientId) === x.ingredientId); return { name: item.name, quantity: Number(x.quantity), unit: item.unit }; }),
        );
      if (tool === "generate") value = await aiApi.generate(values.request);
      setResult(value);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function save(action) {
    setBusy(true);
    setError(null);
    try {
      await action();
      pantry.reload();
      catalog.reload();
      toast(t("Saved to your kitchen", "تم الحفظ في مطبخك"));
      setResult(null);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  const [recipeType, recipeId] = recipe.split(":");
  const selectedLinks =
    (recipeType === "system" ? catalog.data?.si : catalog.data?.ui) || [];
  const validIds = selectedLinks
    .filter(
      (x) =>
        x[recipeType === "system" ? "systemRecipeId" : "userRecipeId"] ===
        Number(recipeId),
    )
    .map((x) => x.ingredientId);
  return (
    <div className="page">
      <PageHead
        eyebrow={t("A LITTLE KITCHEN MAGIC", "قليل من إلهام المطبخ")}
        title={t("Meet Loot AI", "تعرّف على ذكاء لوت")}
        description={t(
          "Thoughtful ideas for the ingredients you already have.",
          "أفكار مدروسة للمكونات التي لديك بالفعل.",
        )}
      >
        <span className="badge orange-badge">
          <Sparkles size={15} />
          {t("Your cooking companion", "رفيقك في الطبخ")}
        </span>
      </PageHead>
      <div className="ai-layout">
        <aside className="ai-tools">
          {tools.map(([id, Icon, en, ar, desc, descAr]) => (
            <button
              className={tool === id ? "ai-tool active" : "ai-tool"}
              key={id}
              disabled={busy}
              onClick={() => {
                setTool(id);
                setResult(null);
                setError(null);
              }}
            >
              <Icon />
              <div>
                <b>{t(en, ar)}</b>
                <small>{t(desc, descAr)}</small>
              </div>
            </button>
          ))}
        </aside>
        <section className="panel ai-workspace">
          <span className="eyebrow">{t("LOOT INTELLIGENCE", "ذكاء لوت")}</span>
          <h2>
            {t(
              tools.find((x) => x[0] === tool)[2],
              tools.find((x) => x[0] === tool)[3],
            )}
          </h2>
          <form onSubmit={run}>
            {tool === "image" && (
              <>
                <label
                  className="dropzone"
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    chooseFile(e.dataTransfer.files[0]);
                  }}
                >
                  {preview ? (
                    <img
                      src={preview}
                      alt={t("Selected ingredient", "المكوّن المختار")}
                    />
                  ) : (
                    <Upload size={32} />
                  )}
                  <strong>
                    {t(
                      "Drop an ingredient photo here",
                      "اسحب صورة المكوّن هنا",
                    )}
                  </strong>
                  <span>
                    {t(
                      "or choose a file · JPEG, PNG, WEBP · up to 5 MB",
                      "أو اختر ملفاً · JPEG، PNG، WEBP · حتى ٥ ميجابايت",
                    )}
                  </span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={(e) => chooseFile(e.target.files[0])}
                    aria-label={t(
                      "Choose ingredient photo",
                      "اختر صورة المكوّن",
                    )}
                  />
                </label>
                <p className="small muted">
                  {t(
                    "You can review the detected ingredient and enter the actual quantity before adding it.",
                    "يمكنك مراجعة المكوّن المكتشف وتحديد كميته الفعلية قبل إضافته.",
                  )}
                </p>
              </>
            )}
            {tool === "substitute" &&
              (catalog.loading ? (
                <Loading />
              ) : catalog.error ? (
                <ErrorState error={catalog.error} retry={catalog.reload} />
              ) : (
                <>
                  <Field label={t("Recipe", "الوصفة")}>
                    <select
                      required
                      value={recipe}
                      onChange={(e) => setRecipe(e.target.value)}
                    >
                      <option value="">
                        {t("Choose a recipe", "اختر وصفة")}
                      </option>
                      {catalog.data.recipes.map((r) => (
                        <option key={r.type + r.id} value={`${r.type}:${r.id}`}>
                          {r.name} ·{" "}
                          {r.type === "system"
                            ? t("Loot", "لوت")
                            : t("Mine", "وصفتي")}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field
                    label={t(
                      "Ingredient to replace",
                      "المكوّن المراد استبداله",
                    )}
                  >
                    <select name="ingredient" key={recipe} required>
                      <option value="">
                        {t("Choose ingredient", "اختر مكوّناً")}
                      </option>
                      {catalog.data.ingredients
                        .filter((i) => validIds.includes(i.id))
                        .map((i) => (
                          <option key={i.id} value={i.id}>
                            {i.name}
                          </option>
                        ))}
                    </select>
                  </Field>
                </>
              ))}
            {["recommend", "generate"].includes(tool) && (
              <Field
                label={t("What are you in the mood for?", "ماذا ترغب في طبخه؟")}
              >
                <textarea
                  name="request"
                  rows={5}
                  required
                  maxLength={2000}
                  placeholder={t(
                    "Something quick and comforting for dinner…",
                    "وجبة سريعة ولذيذة للعشاء…",
                  )}
                />
              </Field>
            )}
            {tool === "rescue" && (
              <>
                <p>
                  {t(
                    "Choose ingredients from your pantry and enter how much you want to use.",
                    "اختر المكونات من مؤنك وحدّد الكمية التي ترغب باستخدامها.",
                  )}
                </p>
                {pantry.loading && <Loading />}{pantry.error && <ErrorState error={pantry.error} retry={pantry.reload} />}
                {!pantry.loading && !pantry.error && !pantry.data?.length && <p>{t("Add ingredients to your pantry first.", "أضف مكونات إلى مؤنك أولاً.")} <Link to="/pantry">{t("Open pantry", "افتح المؤن")}</Link></p>}
                {leftovers.map((item, index) => (
                  <div className="leftover-row" key={item.rowId}>
                    <PantryPicker items={pantry.data || []} value={item.ingredientId} excluded={leftovers.map((x) => x.ingredientId)} onChange={(id) => {
                      const selected = pantry.data.find((p) => String(p.ingredientId) === id);
                      setLeftovers((rows) => rows.map((x, i) => i === index ? { ...x, ingredientId: id, unit: selected?.unit || "" } : x));
                    }} />
                    <Field
                      label={t("Quantity", "الكمية")}
                      type="number"
                      max={pantry.data?.find((p) => String(p.ingredientId) === item.ingredientId)?.quantity}
                      min="0.01"
                      step="0.01"
                      required
                      value={item.quantity}
                      onChange={(e) =>
                        setLeftovers((rows) =>
                          rows.map((x, i) =>
                            i === index
                              ? { ...x, quantity: e.target.value }
                              : x,
                          ),
                        )
                      }
                    />
                    <Field label={t("Unit", "الوحدة")} readOnly value={t(item.unit, {g: "غ", ml: "مل", piece: "حبة", "": ""}[item.unit])} />
                    <button
                      type="button"
                      className="icon-button"
                      disabled={leftovers.length === 1}
                      aria-label={t("Remove ingredient", "حذف المكوّن")}
                      onClick={() =>
                        setLeftovers((rows) =>
                          rows.filter((_, i) => i !== index),
                        )
                      }
                    >
                      <Trash2 size={17} />
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  className="button secondary"
                  disabled={leftovers.length >= Math.min(30, pantry.data?.length || 0)}
                  onClick={() =>
                    setLeftovers((rows) => [
                      ...rows,
                      { rowId: crypto.randomUUID(), ingredientId: "", quantity: 1, unit: "" },
                    ])
                  }
                >
                  <Plus size={17} />
                  {t("Add leftover", "أضف مكوّناً متبقياً")}
                </button>
              </>
            )}
            {error && <ErrorState error={error} />}
            <Submit
              busy={busy}
              disabled={
                (tool === "image" && !file) ||
                (tool === "rescue" && (pantry.loading || !!pantry.error || leftovers.some((x) => !x.ingredientId))) ||
                (tool === "substitute" &&
                  (!recipe || catalog.loading || !!catalog.error))
              }
            >
              <Sparkles size={18} />
              {t("Ask Loot AI", "اسأل ذكاء لوت")}
            </Submit>
          </form>
          {result && (
            <div className="ai-result" aria-live="polite">
              <span className="eyebrow">
                {t("A LITTLE INSPIRATION FOR YOU", "إلهام من أجلك")}
              </span>
              {result.message ? (
                <p>{t(result.message, "لم يتم العثور على نتيجة مناسبة. جرّب مكونات أو طلباً آخر.")}</p>
              ) : tool === "image" ? (
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    const d = Object.fromEntries(new FormData(e.currentTarget));
                    save(() =>
                      aiApi.addImage({ ...d, quantity: Number(d.quantity) }),
                    );
                  }}
                >
                  <Field
                    label={t("Detected ingredient", "المكوّن المكتشف")}
                    name="name"
                    defaultValue={result.name}
                    required
                    maxLength={100}
                  />
                  <Field
                    label={t("Actual quantity", "الكمية الفعلية")}
                    name="quantity"
                    type="number"
                    min="0.01"
                    step="0.01"
                    required
                    defaultValue={result.quantity || ""}
                  />
                  <Field label={t("Unit", "الوحدة")}>
                    <select name="unit" defaultValue={unit(result.unit)}>
                      {["g", "ml", "piece"].map((u) => (
                        <option key={u} value={u}>{t(u, {g: "غ", ml: "مل", piece: "حبة"}[u])}</option>
                      ))}
                    </select>
                  </Field>
                  <Submit busy={busy}>
                    {t("Add to pantry", "أضف إلى المؤن")}
                  </Submit>
                </form>
              ) : tool === "substitute" ? (
                <>
                  <h3>{result.substitute}</h3>
                  <p>
                    {result.quantity} {unit(result.unit)}
                  </p>
                  <p>{result.reason}</p>
                </>
              ) : Array.isArray(result) ? (
                result.map((r, i) => (
                  <article className="ai-recipe" key={i}>
                    <span className="badge">{t(r.category, {Breakfast: "فطور", Lunch: "غداء", Dinner: "عشاء", Snack: "وجبة خفيفة"}[r.category] || r.category)}</span>
                    <h3>{r.name}</h3>
                    <p>{r.description}</p>
                    <p className="instructions">{r.instruction}</p>
                    <p>
                      <Sparkles size={15} /> {r.reason}
                    </p>
                    {r.recipeId && (
                      <Link
                        className="button secondary"
                        to={`/recipes/${r.recipeType.toLowerCase()}/${r.recipeId}`}
                      >
                        {t("View recipe", "عرض الوصفة")}
                      </Link>
                    )}
                  </article>
                ))
              ) : (
                <>
                  <h3>{result.name}</h3>
                  <p>{result.description}</p>
                  <ul>
                    {result.ingredients?.map((i) => (
                      <li key={i.ingredientId}>
                        {i.name} · {i.quantity} {unit(i.unit)}
                      </li>
                    ))}
                  </ul>
                  <p className="instructions">{result.instructions}</p>
                  <button
                    className="button"
                    disabled={busy}
                    onClick={() => save(() => aiApi.save(result))}
                  >
                    {t("Save to my recipes", "حفظ في وصفاتي")}
                  </button>
                </>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
