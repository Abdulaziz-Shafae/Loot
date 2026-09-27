import { createContext, useContext, useEffect, useState } from "react";
const Context = createContext();
function stored(key, fallback, allowed) {
  try {
    const value = localStorage.getItem(key);
    return allowed.includes(value) ? value : fallback;
  } catch {
    return fallback;
  }
}
export function Preferences({ children }) {
  const [lang, setLang] = useState(() =>
    stored("loot-language", "en", ["en", "ar"]),
  );
  const [theme, setTheme] = useState(() =>
    stored("loot-theme", "system", ["system", "light", "dark"]),
  );
  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
    try {
      localStorage.setItem("loot-language", lang);
    } catch {}
  }, [lang]);
  useEffect(() => {
    const media = matchMedia("(prefers-color-scheme: dark)");
    const update = () => {
      document.documentElement.dataset.theme =
        theme === "system" ? (media.matches ? "dark" : "light") : theme;
    };
    update();
    media.addEventListener("change", update);
    try {
      localStorage.setItem("loot-theme", theme);
    } catch {}
    return () => media.removeEventListener("change", update);
  }, [theme]);
  const t = (en, ar) => (lang === "ar" ? ar : en);
  return (
    <Context.Provider value={{ lang, setLang, theme, setTheme, t }}>
      {children}
    </Context.Provider>
  );
}
export const usePreferences = () => useContext(Context);
