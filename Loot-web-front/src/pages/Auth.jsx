import { useEffect, useState } from "react";
import { Link, useNavigate, Navigate } from "react-router-dom";
import { Leaf } from "lucide-react";
import { authApi } from "../api/authApi";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/Preferences";
import { Field, ErrorState, Submit, useToast } from "../components/UI";
export default function Auth({ mode }) {
  const { t } = usePreferences();
  const { user, login, setUser } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [token] = useState(() => new URLSearchParams(window.location.search).get("token") || "");
  useEffect(() => {
    if (mode === "reset") window.history.replaceState(window.history.state, "", window.location.pathname);
  }, [mode]);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(null),
    [message, setMessage] = useState("");
  const title =
    mode === "signup"
      ? t("Make yourself at home.", "أهلاً بك في مطبخك.")
      : mode === "forgot"
        ? t("Let’s get you back in.", "لنساعدك على العودة.")
        : mode === "reset"
          ? t("A fresh start.", "بداية جديدة.")
          : t("Welcome back to your kitchen.", "أهلاً بعودتك إلى مطبخك.");
  if (user && (mode === "login" || mode === "signup"))
    return <Navigate to="/dashboard" replace />;
  async function submit(e) {
    e.preventDefault();
    const values = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setError(null);
    setMessage("");
    try {
      if (mode === "login") {
        await login(values);
        navigate("/dashboard");
      } else if (mode === "signup") {
        const registration = await authApi.signup(values);
        try {
          if (!registration.authenticated) throw new Error("Sign in required");
          setUser(await authApi.me());
          navigate("/dashboard", { replace: true });
        } catch {
          toast(t("Account created. You can now sign in.", "تم إنشاء حسابك. يمكنك تسجيل الدخول الآن."));
          navigate("/login", { replace: true });
        }
      } else if (mode === "forgot") {
        await authApi.forgot(values);
        setMessage(
          t(
            "If an account exists for this email, a password reset link has been sent. Email delivery may take some time.",
            "إذا كان هناك حساب مرتبط بهذا البريد، فقد تم إرسال رابط إعادة تعيين كلمة المرور. قد يستغرق وصول البريد بعض الوقت.",
          ),
        );
      } else {
        if (values.password !== values.confirmPassword) {
          setMessage(t("Passwords do not match.", "كلمتا المرور غير متطابقتين."));
          return;
        }
        await authApi.reset({ token, newPassword: values.password });
        setUser(null);
        setMessage(
          t(
            "Password updated. Please sign in.",
            "تم تحديث كلمة المرور. سجّل الدخول.",
          ),
        );
        e.target.reset();
      }
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-layout" key={mode}>
      <aside className="auth-story">
        <Leaf size={34} />
        <span className="eyebrow">LOOT | لوت</span>
        <h2>
          {t("A little less waste.", "هدر أقل.")}
          <br />
          <em>{t("A lot more possibility.", "وفرص أكثر.")}</em>
        </h2>
        <p>
          {t(
            "Your ingredients have a story. Let’s make it a delicious one.",
            "لمكوناتك حكاية. لنصنع منها حكاية لذيذة.",
          )}
        </p>
        <img src="/logo.png" alt="" />
      </aside>
      <div className="auth-form">
        <span className="eyebrow">
          {t("YOUR LOOT KITCHEN", "مطبخك مع لوت")}
        </span>
        <h1>{title}</h1>
        <p>
          {mode === "login"
            ? t(
                "Good things are waiting in your pantry.",
                "أشياء لذيذة تنتظرك في مؤنك.",
              )
            : t(
                "A few details, and you’re on your way.",
                "تفاصيل بسيطة لتبدأ رحلتك.",
              )}
        </p>
        <form onSubmit={submit} key={mode}>
          {mode === "signup" && (
            <Field
              label={t("Full name", "الاسم الكامل")}
              name="name"
              autoComplete="name"
              required
              maxLength={100}
            />
          )}
          {mode !== "reset" && <Field
            label={t("Email address", "البريد الإلكتروني")}
            name="email"
            type="email"
            autoComplete="email"
            required
            maxLength={150}
          />}
          {mode === "signup" && (
            <Field
              label={t("Phone number", "رقم الجوال")}
              name="phoneNumber"
              type="tel"
              placeholder="05xxxxxxxx"
              pattern="05[0-9]{8}"
              autoComplete="tel"
              required
            />
          )}
          {mode !== "forgot" && (
            <>
              <Field
                label={
                  mode === "reset"
                    ? t("New password", "كلمة المرور الجديدة")
                    : t("Password", "كلمة المرور")
                }
                name="password"
                type="password"
                minLength={mode === "login" ? undefined : 8}
                maxLength={72}
                autoComplete={
                  mode === "login" ? "current-password" : "new-password"
                }
                required
              />
              {mode !== "login" && (
                <small className="muted">
                  {t(
                    "8–72 characters. Include uppercase, lowercase, a number and a symbol.",
                    "٨–٧٢ حرفاً، مع حرف إنجليزي كبير وصغير ورقم ورمز.",
                  )}
                </small>
              )}
            </>
          )}
          {mode === "reset" && <Field label={t("Confirm password", "تأكيد كلمة المرور")} name="confirmPassword" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />}
          {mode === "reset" && !token && <p role="alert">{t("Open the reset link from your email to continue.", "افتح رابط إعادة التعيين من بريدك الإلكتروني للمتابعة.")}</p>}
          {error && <ErrorState error={error} />}{" "}
          {message && (
            <div className="success" role="status">
              {message}
            </div>
          )}
          <Submit busy={busy} disabled={mode === "reset" && !token}>
            {mode === "login"
              ? t("Log in", "تسجيل الدخول")
              : mode === "signup"
                ? t("Create account", "إنشاء حساب")
                : mode === "forgot"
                  ? t("Send reset link", "إرسال رابط إعادة التعيين")
                  : t("Reset password", "تعيين كلمة المرور")}
          </Submit>
        </form>
        <div className="auth-links">
          {mode === "login" ? (
            <>
              <Link to="/forgot-password">{t("Forgot password?", "نسيت كلمة المرور؟")}</Link>
              <p>
                {t("New to Loot?", "جديد في لوت؟")}{" "}
                <Link to="/signup">
                  {t("Create an account", "أنشئ حساباً")}
                </Link>
              </p>
            </>
          ) : (
            <Link to="/login">
              {t("Back to login", "العودة لتسجيل الدخول")}
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
