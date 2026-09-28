import { useState } from "react";
import { NavLink, Routes, Route } from "react-router-dom";
import {
  Users,
  Leaf,
  ChefHat,
  History,
  Plus,
  Pencil,
  Trash2,
} from "lucide-react";
import { adminApi } from "../api/adminApi";
import { pantryApi } from "../api/pantryApi";
import { useLoad } from "../hooks/useLoad";
import { usePreferences } from "../context/Preferences";
import { useAuth } from "../context/AuthContext";
import {
  PageHead,
  Loading,
  ErrorState,
  Empty,
  Confirm,
  Modal,
  Field,
  Submit,
  useToast,
} from "../components/UI";
import Recipes from "./Recipes";
function Overview() {
  const { t } = usePreferences();
  const state = useLoad(adminApi.overview);
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  return (
    <>
      <h2>{t("A view of the whole kitchen", "نظرة على المطبخ بأكمله")}</h2>
      <div className="stats">
        {[
          [Users, "users", "Users", "المستخدمون"],
          [Leaf, "ingredients", "Ingredients", "المكونات"],
          [ChefHat, "recipes", "System recipes", "وصفات النظام"],
          [History, "cooks", "Meals cooked", "الوجبات المطبوخة"],
        ].map(([Icon, key, en, ar]) => (
          <div className="stat" key={key}>
            <Icon />
            <strong>{state.data[key]}</strong>
            <span>{t(en, ar)}</span>
          </div>
        ))}
      </div>
    </>
  );
}
function UserList() {
  const { t, unit } = usePreferences(),
    { user } = useAuth();
  const [remove, setRemove] = useState(null);
  const state = useLoad(adminApi.users);
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  return (
    <section className="panel">
      <h2>{t("Our kitchen community", "مجتمع مطبخنا")}</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              {["Name", "Email", "Phone", "Role", ""].map((x, i) => (
                <th key={i}>
                  {t(x, ["الاسم", "البريد", "الجوال", "الدور", ""][i])}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {state.data.map((u) => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td>{u.email}</td>
                <td dir="ltr">{u.phoneNumber}</td>
                <td>{u.role === "ADMIN" ? t("Administrator", "مسؤول") : t("User", "مستخدم")}</td>
                <td>
                  <button
                    className="icon-button"
                    disabled={u.id === user.id}
                    onClick={() => setRemove(u)}
                    aria-label={t("Delete user", "حذف المستخدم")}
                  >
                    <Trash2 size={17} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {remove && (
        <Confirm
          title={t(
            "Delete this user and their personal data?",
            "حذف هذا المستخدم وبياناته الشخصية؟",
          )}
          onClose={() => setRemove(null)}
          onConfirm={async () => {
            await adminApi.removeUser(remove.id);
            state.reload();
          }}
        />
      )}
    </section>
  );
}
function Ingredients() {
  const { t, unit } = usePreferences(),
    toast = useToast();
  const state = useLoad(pantryApi.ingredients);
  const [edit, setEdit] = useState(null),
    [remove, setRemove] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  if (state.loading) return <Loading />;
  if (state.error)
    return <ErrorState error={state.error} retry={state.reload} />;
  return (
    <section className="panel">
      <div className="between">
        <h2>{t("Ingredient catalog", "قائمة المكونات")}</h2>
        <button
          className="button"
          onClick={() => {
            setError(null);
            setEdit({});
          }}
        >
          <Plus size={17} />
          {t("Add ingredient", "أضف مكوّناً")}
        </button>
      </div>
      {state.data.length ? (
        state.data.map((i) => (
          <div className="data-row" key={i.id}>
            <div>
              <b>{i.name}</b> <span className="muted">{unit(i.unit)}</span>
            </div>
            <div className="actions">
              <button
                className="icon-button"
                aria-label={t("Edit ingredient", "تعديل المكوّن")}
                onClick={() => {
                  setError(null);
                  setEdit(i);
                }}
              >
                <Pencil size={17} />
              </button>
              <button
                className="icon-button"
                aria-label={t("Delete ingredient", "حذف المكوّن")}
                onClick={() => setRemove(i)}
              >
                <Trash2 size={17} />
              </button>
            </div>
          </div>
        ))
      ) : (
        <Empty />
      )}
      {edit && (
        <Modal
          title={t("Ingredient details", "تفاصيل المكوّن")}
          onClose={() => !busy && setEdit(null)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError(null);
              try {
                const data = Object.fromEntries(new FormData(e.currentTarget));
                edit.id
                  ? await adminApi.editIngredient(edit.id, data)
                  : await adminApi.addIngredient(data);
                setEdit(null);
                state.reload();
                toast(t("Catalog updated", "تم تحديث القائمة"));
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Field
              label={t("Name", "الاسم")}
              name="name"
              required
              maxLength={100}
              defaultValue={edit.name}
            />
            <Field label={t("Unit", "الوحدة")}>
              <select name="unit" defaultValue={edit.unit || "g"}>
                {["g", "ml", "piece"].map((u) => (
                  <option key={u} value={u}>{t(u, {g: "غ", ml: "مل", piece: "حبة"}[u])}</option>
                ))}
              </select>
            </Field>
            <p className="small muted">
              {t(
                "Units cannot change while an ingredient is used in a pantry, recipe or history.",
                "لا يمكن تغيير الوحدة أثناء استخدام المكوّن في مؤن أو وصفة أو سجل.",
              )}
            </p>
            {error && <ErrorState error={error} />}
            <Submit busy={busy}>{t("Save ingredient", "حفظ المكوّن")}</Submit>
          </form>
        </Modal>
      )}
      {remove && (
        <Confirm
          title={t("Delete this ingredient?", "حذف هذا المكوّن؟")}
          onClose={() => setRemove(null)}
          onConfirm={async () => {
            await adminApi.removeIngredient(remove.id);
            state.reload();
          }}
        />
      )}
    </section>
  );
}
export default function Admin() {
  const { t } = usePreferences();
  return (
    <div className="page">
      <PageHead
        eyebrow={t("LOOT ADMINISTRATION", "إدارة لوت")}
        title={t("Behind the kitchen", "خلف المطبخ")}
        description={t(
          "Care for the catalog and the community.",
          "اعتنِ بالمكونات والوصفات والمجتمع.",
        )}
      />
      <nav className="admin-tabs">
        {[
          ["", "Overview", "نظرة عامة"],
          ["users", "Users", "المستخدمون"],
          ["ingredients", "Ingredients", "المكونات"],
          ["recipes", "System recipes", "وصفات النظام"],
        ].map(([path, en, ar]) => (
          <NavLink end key={path} to={"/admin" + (path ? "/" + path : "")}>
            {t(en, ar)}
          </NavLink>
        ))}
      </nav>
      <Routes>
        <Route index element={<Overview />} />
        <Route path="users" element={<UserList />} />
        <Route path="ingredients" element={<Ingredients />} />
        <Route path="recipes" element={<Recipes admin />} />
      </Routes>
    </div>
  );
}
