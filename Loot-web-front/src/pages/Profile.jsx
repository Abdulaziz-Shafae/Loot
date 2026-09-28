import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { UserRound, ShieldCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/Preferences";
import { authApi } from "../api/authApi";
import {
  PageHead,
  Field,
  Submit,
  ErrorState,
  useToast,
} from "../components/UI";
export default function Profile() {
  const { user, setUser } = useAuth();
  const { t } = usePreferences();
  const toast = useToast(),
    navigate = useNavigate();
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  async function save(e, password = false) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const data = Object.fromEntries(new FormData(e.currentTarget));
      if (password) {
        await authApi.password(data);
        setUser(null);
        navigate("/login");
        toast(
          t(
            "Password changed. Please sign in again.",
            "تم تغيير كلمة المرور. سجّل الدخول مجدداً.",
          ),
        );
      } else {
        setUser(await authApi.profile(data));
        toast(t("Profile updated", "تم تحديث الحساب"));
      }
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="page narrow">
      <PageHead
        eyebrow={t("YOUR SPACE", "مساحتك")}
        title={t("At home with Loot", "في بيتك مع لوت")}
      />
      <div className="profile-summary panel">
        <div className="avatar">
          <UserRound />
        </div>
        <div>
          <h2>{user.name}</h2>
          <p>{user.email}</p>
          {user.role === "ADMIN" && (
            <span className="badge">
              <ShieldCheck size={15} />
              {t("Administrator", "مسؤول")}
            </span>
          )}
        </div>
      </div>
      {error && <ErrorState error={error} />}
      <div className="two-column">
        <section className="panel">
          <h2>{t("Your details", "بياناتك")}</h2>
          <form onSubmit={save}>
            <Field
              label={t("Name", "الاسم")}
              name="name"
              defaultValue={user.name}
              required
              maxLength={100}
            />
            <Field
              label={t("Phone number", "رقم الجوال")}
              name="phoneNumber"
              defaultValue={user.phoneNumber}
              type="tel"
              pattern="05[0-9]{8}"
              required
            />
            <Submit busy={busy}>{t("Save changes", "حفظ التغييرات")}</Submit>
          </form>
        </section>
        <section className="panel">
          <h2>{t("Change password", "تغيير كلمة المرور")}</h2>
          <form onSubmit={(e) => save(e, true)}>
            <Field
              label={t("Current password", "كلمة المرور الحالية")}
              name="currentPassword"
              type="password"
              autoComplete="current-password"
              required
              maxLength={72}
            />
            <Field
              label={t("New password", "كلمة المرور الجديدة")}
              name="password"
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
            />
            <small className="muted">
              {t(
                "Use uppercase, lowercase, a number and a symbol. You’ll sign in again after changing it.",
                "استخدم حرفاً كبيراً وصغيراً ورقماً ورمزاً. ستحتاج إلى تسجيل الدخول مجدداً.",
              )}
            </small>
            <Submit busy={busy}>
              {t("Update password", "تحديث كلمة المرور")}
            </Submit>
          </form>
        </section>
      </div>
      <button
        className="button secondary"
        disabled={busy}
        onClick={async () => {
          try {
            await logout();
            navigate("/");
          } catch (e) {
            setError(e);
          }
        }}
      >
        {t("Log out", "تسجيل الخروج")}
      </button>
    </div>
  );
}
