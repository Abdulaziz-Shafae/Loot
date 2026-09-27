import { Link } from "react-router-dom";
import {
  ShoppingBasket,
  ChefHat,
  ArrowUpRight,
  Sparkles,
  History,
  Plus,
  Leaf,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/Preferences";
import { useLoad } from "../hooks/useLoad";
import { pantryApi } from "../api/pantryApi";
import { recipeApi } from "../api/recipeApi";
import { historyApi } from "../api/historyApi";
import { asList } from "../api/client";
import { PageHead, Loading, ErrorState, Empty } from "../components/UI";
import RecipeCard from "../components/RecipeCard";
export default function Dashboard() {
  const { user } = useAuth();
  const { t, lang } = usePreferences();
  const state = useLoad(async () => {
    const [pantry, low, system, mine, sa, ua, history] = await Promise.all([
      pantryApi.list(),
      pantryApi.low(),
      recipeApi.list("system"),
      recipeApi.list("user"),
      recipeApi.availability("system"),
      recipeApi.availability("user"),
      historyApi.list(),
    ]);
    const possible = [
      ...system.map((r) => ({
        ...r,
        type: "system",
        availability: sa.find((a) => a.id === r.id),
      })),
      ...mine.map((r) => ({
        ...r,
        type: "user",
        availability: ua.find((a) => a.id === r.id),
      })),
    ].filter((r) => r.availability?.canCook);
    return {
      pantry,
      low: asList(low),
      possible,
      history: history.sort((a, b) => b.cookedAt.localeCompare(a.cookedAt)),
    };
  });
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  const d = state.data;
  const hour = new Date().getHours();
  return (
    <div className="page">
      <PageHead
        eyebrow={t("YOUR EVERYDAY KITCHEN", "مطبخك اليومي")}
        title={`${hour < 12 ? t("Good morning", "صباح الخير") : hour < 18 ? t("Good afternoon", "مساء الخير") : t("Good evening", "مساء الخير")}, ${user.name.split(" ")[0]}`}
        description={t("What can we cook today?", "ماذا سنطبخ اليوم؟")}
      >
        <Link className="button" to="/pantry">
          <Plus size={18} />
          {t("Add ingredient", "أضف مكوّناً")}
        </Link>
      </PageHead>
      <div className="dashboard-banner">
        <div>
          <span className="eyebrow">
            {t("A FRESH PERSPECTIVE", "نظرة جديدة")}
          </span>
          <h2>
            {t("Good food starts with", "الطعام اللذيذ يبدأ بما")}
            <br />
            <em>{t("what you have.", "لديك.")}</em>
          </h2>
          <Link to="/recipes" className="text-link">
            {t("Explore your possibilities", "استكشف إمكانياتك")}
            <ArrowUpRight size={18} />
          </Link>
        </div>
        <img src="/logo.png" alt="" />
      </div>
      <div className="stats">
        {[
          [ShoppingBasket, d.pantry.length, "Pantry items", "مكونات المؤن"],
          [ChefHat, d.possible.length, "Ready to cook", "جاهزة للطبخ"],
          [Leaf, d.low.length, "Running low", "مخزون منخفض"],
          [History, d.history.length, "Meals cooked", "وجبات مطبوخة"],
        ].map(([Icon, n, en, ar]) => (
          <div className="stat" key={en}>
            <Icon />
            <strong>{n}</strong>
            <span>{t(en, ar)}</span>
          </div>
        ))}
      </div>
      <div className="section-heading">
        <h2>{t("From your pantry, with love.", "من مؤنك، بكل حب.")}</h2>
        <Link className="text-link" to="/recipes">
          {t("All recipes", "كل الوصفات")}
          <ArrowUpRight size={17} />
        </Link>
      </div>
      {d.possible.length ? (
        <div className="recipe-grid">
          {d.possible.slice(0, 3).map((r) => (
            <RecipeCard
              key={r.type + r.id}
              recipe={r}
              type={r.type}
              availability={r.availability}
            />
          ))}
        </div>
      ) : (
        <Empty
          title={t("Your next meal starts here", "وجبتك القادمة تبدأ هنا")}
        >
          <p>
            {t(
              "Add pantry ingredients, then explore recipes that match.",
              "أضف المكونات ثم اكتشف الوصفات المناسبة.",
            )}
          </p>
          <Link to="/pantry" className="button secondary">
            {t("Build my pantry", "أضف مؤنك")}
          </Link>
        </Empty>
      )}
      <div className="dashboard-bottom">
        <section className="panel">
          <div className="between">
            <h3>{t("A little top-up", "حان وقت التزوّد")}</h3>
            <Link to="/pantry">{t("Pantry", "المؤن")}</Link>
          </div>
          {d.low.length ? (
            d.low.slice(0, 5).map((x) => (
              <div className="data-row" key={x.name}>
                <b>{x.name}</b>
                <span>
                  {x.available} / {x.threshold}
                </span>
              </div>
            ))
          ) : (
            <p className="muted">
              {t(
                "No ingredients below your stock threshold.",
                "لا توجد مكونات تحت حد المخزون.",
              )}
            </p>
          )}
        </section>
        <section className="panel">
          <div className="between">
            <h3>{t("Fresh from your kitchen", "آخر ما طبخت")}</h3>
            <Link to="/history">{t("History", "السجل")}</Link>
          </div>
          {d.history.length ? (
            d.history.slice(0, 4).map((x) => (
              <div className="data-row" key={x.id}>
                <b>{x.recipeName}</b>
                <small>{new Date(x.cookedAt).toLocaleDateString(lang)}</small>
              </div>
            ))
          ) : (
            <p className="muted">
              {t(
                "Your cooked meals will appear here.",
                "ستظهر وجباتك المطبوخة هنا.",
              )}
            </p>
          )}
        </section>
        <Link to="/ai" className="panel ai-callout">
          <Sparkles />
          <h3>{t("A spark of inspiration.", "شرارة إلهام.")}</h3>
          <p>
            {t(
              "Let Loot AI help you make the most of your ingredients.",
              "دع ذكاء لوت يساعدك على الاستفادة من مكوناتك.",
            )}
          </p>
          <span className="text-link">
            {t("Ask Loot AI", "اسأل ذكاء لوت")}
            <ArrowUpRight size={18} />
          </span>
        </Link>
      </div>
    </div>
  );
}
