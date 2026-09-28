import { createContext, useContext, useEffect, useRef, useState, useId } from "react";
import { LoaderCircle, Leaf, X, CheckCircle2 } from "lucide-react";
import { usePreferences } from "../context/Preferences";
import { errorMessage } from "../api/client";
export function Loading() {
  const { t } = usePreferences();
  return (
    <div className="state" role="status">
      <LoaderCircle className="spin" />
      {t("Preparing your kitchen…", "نجهز مطبخك…")}
    </div>
  );
}
export function ErrorState({ error, retry }) {
  const { t, lang } = usePreferences();
  return (
    <div className="error" role="alert">
      <p>{errorMessage(error, lang)}</p>
      {retry && (
        <button className="button secondary" onClick={retry}>
          {t("Try again", "حاول مجدداً")}
        </button>
      )}
    </div>
  );
}
export function Empty({ title, children }) {
  const { t } = usePreferences();
  return (
    <div className="state empty">
      <Leaf size={34} />
      <h3>
        {title || t("A little room for possibility", "مساحة لبدايات لذيذة")}
      </h3>
      {children}
    </div>
  );
}
export function PageHead({ eyebrow, title, description, children }) {
  return (
    <div className="page-head">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      <div className="actions">{children}</div>
    </div>
  );
}
export function Field({ label, children, ...props }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children || <input {...props} />}
    </label>
  );
}
export function Modal({ title, children, onClose }) {
  const ref = useRef();
  const titleId = useId();
  const { t } = usePreferences();
  useEffect(() => {
    const node = ref.current;
    const old = document.activeElement;
    node.showModal();
    return () => {
      node.close();
      old?.focus?.();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); onClose(); }}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
    >
      <div className="modal-head">
        <h2 id={titleId}>{title}</h2>
        <button
          className="icon-button"
          onClick={onClose}
          aria-label={t("Close", "إغلاق")}
        >
          <X />
        </button>
      </div>
      {children}
    </dialog>
  );
}
const ToastContext = createContext();
export function ToastProvider({ children }) {
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => setMessage(""), 4500);
    return () => clearTimeout(timer);
  }, [message]);
  return (
    <ToastContext.Provider value={setMessage}>
      {children}
      {message && (
        <div className="toast" role="status">
          <CheckCircle2 />
          {message}
        </div>
      )}
    </ToastContext.Provider>
  );
}
export const useToast = () => useContext(ToastContext);
export function Confirm({ title, onConfirm, onClose }) {
  const { t } = usePreferences();
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(null);
  return (
    <Modal
      title={title}
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      <p>{t("Please confirm to continue.", "يرجى التأكيد للمتابعة.")}</p>
      {error && <ErrorState error={error} />}
      <div className="actions">
        <button className="button secondary" disabled={busy} onClick={onClose}>
          {t("Cancel", "إلغاء")}
        </button>
        <button
          className="button"
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            try {
              await onConfirm();
              onClose();
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          {busy ? t("Working…", "جارٍ التنفيذ…") : t("Confirm", "تأكيد")}
        </button>
      </div>
    </Modal>
  );
}
export function Submit({ busy, disabled, children }) {
  const { t } = usePreferences();
  return (
    <button className="button" type="submit" disabled={busy || disabled}>
      {busy ? (
        <>
          <LoaderCircle size={17} className="spin" />
          {t("Please wait…", "يرجى الانتظار…")}
        </>
      ) : (
        children
      )}
    </button>
  );
}
