import { useState } from "react";
import { usePreferences } from "../context/Preferences";
import { Field } from "./UI";
export default function PantryPicker({ items, value, excluded, onChange }) {
  const { t } = usePreferences();
  const [search, setSearch] = useState("");
  return <div className="leftover-picker">
    <Field label={t("Search pantry", "ابحث في المؤن")} type="search" value={search} onChange={(e) => setSearch(e.target.value)} />
    <Field label={t("Ingredient · available quantity", "المكوّن · الكمية المتوفرة")}>
      <select required value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">{t("Choose from your pantry", "اختر من مؤنك")}</option>
        {items.filter((p) => String(p.ingredientId) === value || (!excluded.includes(String(p.ingredientId)) && p.name.toLocaleLowerCase().includes(search.toLocaleLowerCase()))).map((p) =>
          <option key={p.ingredientId} value={p.ingredientId}>{p.name} · {p.quantity} {t(p.unit, {g: "غ", ml: "مل", piece: "حبة"}[p.unit])}</option>)}
      </select>
    </Field>
  </div>;
}
