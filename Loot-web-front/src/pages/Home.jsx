import { Link } from "react-router-dom";
import {
  ArrowRight,
  Leaf,
  ChefHat,
  ScanLine,
  Sparkles,
  ShoppingBasket,
  History,
  Check,
  ArrowUpRight,
} from "lucide-react";
import { usePreferences } from "../context/Preferences";
export default function Home() {
  const { t } = usePreferences();
  return (
    <div className="landing">
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow pill">
            <Leaf size={14} />
            {t("GOOD FOOD. LESS WASTE.", "طعام لذيذ. هدر أقل.")}
          </span>
          <h1>
            {t("Your pantry.", "مؤنك.")}
            <br />
            {t("Endless", "إمكانيات")}
            <br />
            <em>{t("possibilities.", "بلا حدود.")}</em>
          </h1>
          <p>
            {t(
              "Turn what you already have into what you can cook. Meet your thoughtful kitchen companion, from the first ingredient to the last delicious bite.",
              "حوّل ما لديك إلى وجبات تحبها. رفيقك في المطبخ، من أول مكوّن إلى آخر لقمة لذيذة.",
            )}
          </p>
          <div className="actions">
            <Link className="button orange" to="/signup">
              {t("Let’s get cooking", "لنبدأ الطبخ")}
              <ArrowRight size={19} />
            </Link>
            <a className="text-link" href="#how">
              {t("See how it works", "كيف يعمل لوت")}
              <ArrowUpRight size={18} />
            </a>
          </div>
          <div className="hero-note">
            <span>
              <Check size={15} />
              {t("Know what you have", "اعرف ما لديك")}
            </span>
            <span>
              <Check size={15} />
              {t("Love what you make", "أحب ما تطبخ")}
            </span>
          </div>
        </div>
        <div className="hero-visual">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="hero-brand">
            <img src="/brand.png" alt="Loot | لوت" />
          </div>
          <div className="floating-label float-top">
            <span className="icon-tile">
              <ShoppingBasket size={21} />
            </span>
            <div>
              <b>{t("A pantry with a plan", "مؤن بخطة واضحة")}</b>
              <small>{t("Every ingredient counts", "لكل مكوّن قيمة")}</small>
            </div>
          </div>
          <div className="floating-label float-bottom">
            <span className="icon-tile orange-tile">
              <Sparkles size={21} />
            </span>
            <div>
              <b>{t("A little kitchen magic", "إلهام في مطبخك")}</b>
              <small>
                {t("Thoughtfully powered by AI", "بمساعدة الذكاء الاصطناعي")}
              </small>
            </div>
          </div>
          <span className="visual-caption">{t("THE LOOT WAY OF COOKING", "الطبخ على طريقة لوت")}</span>
        </div>
      </section>
      <div className="value-strip">
        <span>
          <Leaf />
          {t("Waste less", "هدر أقل")}
        </span>
        <span>
          <ShoppingBasket />
          {t("Shop thoughtfully", "تسوّق بوعي")}
        </span>
        <span>
          <ChefHat />
          {t("Cook with confidence", "اطبخ بثقة")}
        </span>
      </div>
      <section id="about" className="about-section">
        <span className="eyebrow">
          {t("A LITTLE MORE THOUGHTFUL", "قليل من الوعي يصنع الفرق")}
        </span>
        <div className="two-column">
          <h2>
            {t("Good meals start with", "وجبات رائعة تبدأ بما")}
            <br />
            <em>{t("what’s already there.", "لديك بالفعل.")}</em>
          </h2>
          <div>
            <p>
              {t(
                "There’s a meal hiding in your kitchen. Loot helps you find it. Keep your ingredients in view, discover recipes that fit your pantry, and give forgotten food a delicious second chance.",
                "في مطبخك وجبة تنتظر اكتشافها. لوت يساعدك على رؤية مؤنك، واكتشاف وصفات تناسبها، ومنح المكونات المنسية فرصة لذيذة جديدة.",
              )}
            </p>
            <p>
              {t(
                "Less wondering what’s for dinner. More enjoying the food you make.",
                "حيرة أقل بشأن العشاء. ومتعة أكثر بما تطبخ.",
              )}
            </p>
          </div>
        </div>
      </section>
      <section id="how">
        <div className="section-heading">
          <div>
            <span className="eyebrow">
              {t("FROM PANTRY TO PLATE", "من المؤن إلى المائدة")}
            </span>
            <h2>
              {t("Simple steps. Better meals.", "خطوات بسيطة. وجبات أفضل.")}
            </h2>
          </div>
          <span className="muted">
            {t("Your everyday cooking rhythm", "روتينك اليومي للطبخ")}
          </span>
        </div>
        <div className="steps">
          {[
            [
              ShoppingBasket,
              "Add your pantry",
              "أضف مؤنك",
              "Keep ingredients, quantities and units in one place.",
              "احتفظ بالمكونات والكميات والوحدات في مكان واحد.",
            ],
            [
              Leaf,
              "Find your match",
              "اكتشف وصفتك",
              "See what’s ready to cook and what’s almost there.",
              "اعرف ما يمكنك طبخه وما ينقصك.",
            ],
            [
              ChefHat,
              "Make something good",
              "اطبخ شيئاً لذيذاً",
              "Follow the recipe and enjoy the process.",
              "اتبع الوصفة واستمتع بالتجربة.",
            ],
            [
              History,
              "Leave the rest to Loot",
              "دع الباقي للوت",
              "Stock updates automatically, and your meal is saved.",
              "تُحدّث الكميات تلقائياً وتُحفظ الوجبة في سجلك.",
            ],
          ].map(([Icon, en, ar, desc, descAr], i) => (
            <article className="step" key={en}>
              <span className="step-number">0{i + 1}</span>
              <Icon />
              <h3>{t(en, ar)}</h3>
              <p>{t(desc, descAr)}</p>
            </article>
          ))}
        </div>
      </section>
      <section id="features">
        <div className="section-heading">
          <div>
            <span className="eyebrow">
              {t("MADE FOR YOUR REAL KITCHEN", "مصمم لمطبخك الحقيقي")}
            </span>
            <h2>
              {t("Small details. A big difference.", "تفاصيل صغيرة. فرق كبير.")}
            </h2>
          </div>
        </div>
        <div className="feature-grid">
          <article className="feature-card forest">
            <ShoppingBasket size={32} />
            <h3>{t("Everything in its place.", "كل شيء في مكانه.")}</h3>
            <p>
              {t(
                "A clear pantry, low-stock reminders, and quantities that stay in sync when you cook.",
                "مؤن واضحة، وتنبيهات للمخزون المنخفض، وكميات تتحدث مع كل وجبة.",
              )}
            </p>
            <div className="decor-jars" aria-hidden="true">
              <i />
              <i />
              <i />
            </div>
          </article>
          <article className="feature-card cream">
            <ChefHat size={32} />
            <h3>{t("Your next favourite meal.", "وجبتك المفضلة القادمة.")}</h3>
            <p>
              {t(
                "Explore the Loot kitchen, create your own recipes, and revisit meals you loved.",
                "استكشف مطبخ لوت، وأضف وصفاتك، وكرر الوجبات التي أحببتها.",
              )}
            </p>
            <Link to="/signup" className="text-link">
              {t("Find your inspiration", "اكتشف إلهامك")}
              <ArrowRight size={18} />
            </Link>
          </article>
          <article className="feature-card ai-feature">
            <Sparkles size={32} />
            <h3>
              {t("Meet your kitchen sidekick.", "تعرّف على مساعد مطبخك.")}
            </h3>
            <p>
              {t(
                "Scan ingredients, find substitutes, rescue leftovers, and generate a recipe from your pantry. All in Loot AI.",
                "تعرّف على المكونات بالصور، واكتشف البدائل، واستفد من البقايا، وأنشئ وصفة من مؤنك. كل ذلك في ذكاء لوت.",
              )}
            </p>
            <span className="badge">
              <ScanLine size={15} />
              {t("Five thoughtful AI tools", "خمس أدوات ذكية")}
            </span>
          </article>
        </div>
      </section>
      <section className="final-cta">
        <Leaf size={35} />
        <h2>
          {t("A better meal is already", "وجبة أفضل تنتظرك")}
          <br />
          {t("in your kitchen.", "في مطبخك.")}
        </h2>
        <p>{t("Let’s find it together.", "لنكتشفها معاً.")}</p>
        <Link to="/signup" className="button orange">
          {t("Start your Loot kitchen", "ابدأ مطبخك مع لوت")}
          <ArrowRight size={18} />
        </Link>
      </section>
    </div>
  );
}
