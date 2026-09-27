import { useState } from "react";
import { History as HistoryIcon, RotateCcw } from "lucide-react";
import { historyApi } from "../api/historyApi";
import { useLoad } from "../hooks/useLoad";
import { usePreferences } from "../context/Preferences";
import {
  PageHead,
  Loading,
  ErrorState,
  Empty,
  Modal,
  Submit,
  useToast,
} from "../components/UI";
export default function History() {
  const { t, lang } = usePreferences();
  const toast = useToast();
  const [preview, setPreview] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  const state = useLoad(async () => {
    const [history, ingredients] = await Promise.all([
      historyApi.list(),
      historyApi.ingredients(),
    ]);
    return {
      history: history.sort((a, b) => b.cookedAt.localeCompare(a.cookedAt)),
      ingredients,
    };
  });
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  return (
    <div className="page">
      <PageHead
        eyebrow={t("MADE BY YOU", "من صنع يديك")}
        title={t("Your kitchen stories", "حكايات مطبخك")}
        description={t(
          "Good meals are worth remembering. And making again.",
          "وجبات تستحق التذكر والتكرار.",
        )}
      />
      {error && !preview && <ErrorState error={error} />}{" "}
      {state.data.history.length ? (
        <div className="history-list">
          {state.data.history.map((h) => (
            <article className="panel history-card" key={h.id}>
              <div className="history-symbol">
                <HistoryIcon />
              </div>
              <div>
                <span className="eyebrow">
                  {new Date(h.cookedAt).toLocaleString(lang)} ·{" "}
                  {h.recipeType === "System"
                    ? t("Loot recipe", "وصفة لوت")
                    : t("My recipe", "وصفتي")}
                </span>
                <h3>{h.recipeName}</h3>
                <p>{h.description}</p>
                <div className="ingredient-chips">
                  {state.data.ingredients
                    .filter((i) => i.cookingHistoryId === h.id)
                    .map((i) => (
                      <span className="badge" key={i.id}>
                        {i.ingredientName} · {i.usedQuantity} {i.unit}
                      </span>
                    ))}
                </div>
              </div>
              <button
                className="button secondary"
                disabled={busy}
                onClick={async () => {
                  setBusy(true);
                  setError(null);
                  try {
                    setPreview({
                      id: h.id,
                      ...(await historyApi.preview(h.id)),
                    });
                  } catch (e) {
                    setError(e);
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                <RotateCcw size={17} />
                {t("Cook again", "اطبخها مجدداً")}
              </button>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title={t("Your first kitchen story awaits", "حكايتك الأولى تنتظر")}
        >
          <p>
            {t(
              "Cook a recipe and we’ll save the memory here.",
              "اطبخ وصفة وسنحفظ ذكراها هنا.",
            )}
          </p>
        </Empty>
      )}
      {preview && (
        <Modal title={preview.name} onClose={() => !busy && setPreview(null)}>
          <p>
            {t(
              "Repeating this meal will deduct ingredients and add a new history entry.",
              "سيتم خصم المكونات وإضافة الوجبة إلى السجل.",
            )}
          </p>
          {preview.missing.length ? (
            <div className="error">
              <h3>{t("You’ll need a little more", "تحتاج إلى المزيد")}</h3>
              {preview.missing.map((m) => (
                <p key={m.name}>
                  {m.name}: {t("Missing", "الناقص")} {m.missing} ·{" "}
                  {t("Available", "المتوفر")} {m.available} / {m.required}
                </p>
              ))}
            </div>
          ) : (
            <div className="success">
              {t("All ingredients are ready.", "جميع المكونات جاهزة.")}
            </div>
          )}
          {error && <ErrorState error={error} />}
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              try {
                await historyApi.repeat(preview.id);
                setPreview(null);
                state.reload();
                toast(t("Cooked again. Enjoy!", "تم الطبخ مجدداً. بالعافية!"));
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Submit busy={busy || preview.missing.length > 0}>
              {t("Confirm and cook", "تأكيد الطبخ")}
            </Submit>
          </form>
        </Modal>
      )}
    </div>
  );
}
