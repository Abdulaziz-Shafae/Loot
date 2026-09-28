import { useEffect, useId, useRef, useState } from "react";
import { Check, ChevronDown, SunMoon } from "lucide-react";
import { usePreferences } from "../context/Preferences";

export default function ThemeMenu() {
  const { theme, setTheme, t } = usePreferences();
  const [open, setOpen] = useState(false);
  const root = useRef(), trigger = useRef(), items = useRef([]);
  const id = useId();
  const options = [["system", t("System", "النظام")], ["light", t("Light", "فاتح")], ["dark", t("Dark", "داكن")]];
  useEffect(() => {
    if (!open) return;
    items.current[options.findIndex(([value]) => value === theme)]?.focus();
    const close = (e) => { if (!root.current?.contains(e.target)) setOpen(false); };
    document.addEventListener("pointerdown", close);
    return () => document.removeEventListener("pointerdown", close);
  }, [open]);
  return <div className="theme-menu" ref={root} onBlur={(e) => { if (!e.currentTarget.contains(e.relatedTarget)) setOpen(false); }}>
    <button ref={trigger} type="button" className="theme-trigger" aria-label={t("Theme", "المظهر")} aria-haspopup="menu" aria-expanded={open} aria-controls={id}
      onClick={() => setOpen(!open)} onKeyDown={(e) => { if (["ArrowDown", "ArrowUp"].includes(e.key)) { e.preventDefault(); setOpen(true); } }}>
      <SunMoon size={18} /><span>{options.find(([value]) => value === theme)[1]}</span><ChevronDown size={14} />
    </button>
    {open && <div id={id} role="menu" aria-label={t("Theme", "المظهر")} className="theme-options" onKeyDown={(e) => {
      const index = items.current.indexOf(document.activeElement);
      if (["ArrowDown", "ArrowUp", "Home", "End"].includes(e.key)) {
        e.preventDefault();
        items.current[e.key === "Home" ? 0 : e.key === "End" ? 2 : (index + (e.key === "ArrowDown" ? 1 : 2)) % 3]?.focus();
      } else if (e.key === "Escape") { e.preventDefault(); setOpen(false); trigger.current.focus(); }
    }}>
      {options.map(([value, label], index) => <button key={value} ref={(node) => { items.current[index] = node; }} type="button" role="menuitemradio" aria-checked={theme === value}
        onClick={() => { setTheme(value); setOpen(false); trigger.current.focus(); }}><span>{label}</span>{theme === value && <Check size={16} />}</button>)}
    </div>}
  </div>;
}
