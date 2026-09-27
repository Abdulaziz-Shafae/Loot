import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { Menu, X, SunMoon, LogOut, Leaf } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/Preferences";
import { ErrorState } from "../components/UI";
export default function Layout() {
  const { user, logout } = useAuth();
  const { lang, setLang, theme, setTheme, t } = usePreferences();
  const [open, setOpen] = useState(false),
    [error, setError] = useState(null);
  const navigate = useNavigate();
  const links = user
    ? [
        ["/dashboard", "Dashboard", "الرئيسية"],
        ["/recipes", "Recipes", "الوصفات"],
        ["/pantry", "Pantry", "المؤن"],
        ["/ai", "Loot AI", "ذكاء لوت"],
        ["/history", "History", "السجل"],
        ["/profile", "Profile", "حسابي"],
        ...(user.role === "ADMIN" ? [["/admin", "Admin", "الإدارة"]] : []),
      ]
    : [
        ["/", "Home", "الرئيسية"],
        ["/#about", "About", "عن لوت"],
        ["/#features", "Features", "المزايا"],
      ];
  return (
    <>
      <a href="#main" className="skip">
        {t("Skip to content", "تخطّ إلى المحتوى")}
      </a>
      <header className="site-header">
        <Link to={user ? "/dashboard" : "/"} className="brand">
          <img src="/logo.png" alt="" />
          <span>
            Loot <i>|</i> لوت
          </span>
        </Link>
        <button
          className="icon-button menu-button"
          aria-expanded={open}
          aria-label={t("Menu", "القائمة")}
          onClick={() => setOpen(!open)}
        >
          {open ? <X /> : <Menu />}
        </button>
        <nav
          className={open ? "navigation open" : "navigation"}
          aria-label={t("Main navigation", "التنقل الرئيسي")}
        >
          {links.map(([url, en, ar]) =>
            url.includes("#") ? (
              <a key={url} href={url} onClick={() => setOpen(false)}>
                {t(en, ar)}
              </a>
            ) : (
              <NavLink key={url} to={url} end onClick={() => setOpen(false)}>
                {t(en, ar)}
              </NavLink>
            ),
          )}
        </nav>
        <div className={"header-tools " + (open ? "open" : "")}>
          <button
            className="language"
            onClick={() => setLang(lang === "en" ? "ar" : "en")}
            aria-label={t("Switch to Arabic", "Switch to English")}
          >
            {lang === "en" ? "العربية" : "EN"}
          </button>
          <label className="theme">
            <SunMoon size={18} />
            <span className="sr-only">{t("Theme", "المظهر")}</span>
            <select value={theme} onChange={(e) => setTheme(e.target.value)}>
              <option value="system">{t("System", "النظام")}</option>
              <option value="light">{t("Light", "فاتح")}</option>
              <option value="dark">{t("Dark", "داكن")}</option>
            </select>
          </label>
          {user ? (
            <button
              className="icon-button"
              aria-label={t("Log out", "تسجيل الخروج")}
              onClick={async () => {
                try {
                  await logout();
                  navigate("/");
                } catch (e) {
                  setError(e);
                }
              }}
            >
              <LogOut size={19} />
            </button>
          ) : (
            <>
              <Link to="/login" onClick={() => setOpen(false)}>
                {t("Log in", "دخول")}
              </Link>
              <Link
                className="button small"
                to="/signup"
                onClick={() => setOpen(false)}
              >
                {t("Get started", "ابدأ الآن")}
              </Link>
            </>
          )}
        </div>
      </header>
      {error && <ErrorState error={error} />}
      <main id="main">
        <Outlet />
      </main>
      <footer>
        <Link className="brand" to="/">
          <Leaf size={23} />
          Loot | لوت
        </Link>
        <p>
          {t(
            "A thoughtful kitchen. A little less waste.",
            "مطبخ واعٍ. هدر أقل.",
          )}
        </p>
        <span>© {new Date().getFullYear()} Loot</span>
      </footer>
    </>
  );
}
