import axios from "axios";
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  withCredentials: true,
  timeout: 75000,
});
let csrf;
let pending;
export async function refreshCsrf() {
  if (!pending)
    pending = client
      .get("/user/csrf")
      .then(({ data }) => {
        csrf = data;
        return data;
      })
      .finally(() => {
        pending = null;
      });
  return pending;
}
client.interceptors.request.use(async (config) => {
  if (!["get", "head", "options"].includes(config.method)) {
    const token = csrf || (await refreshCsrf());
    config.headers[token.headerName] = token.token;
  }
  return config;
});
client.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && !error.config.url.endsWith("/login"))
      window.dispatchEvent(new Event("loot:unauthorized"));
    if (error.response?.status === 403) csrf = null;
    return Promise.reject(error);
  },
);
export const get = (path) => client.get(path).then((r) => r.data);
export const post = (path, data) => client.post(path, data).then((r) => r.data);
export const put = (path, data) => client.put(path, data).then((r) => r.data);
export const del = (path) => client.delete(path).then((r) => r.data);
export const asList = (value) => (Array.isArray(value) ? value : []);
export const errorMessage = (error, lang = "en") => {
  const status = error.response?.status;
  if (lang === "ar")
    return (
      {
        401: "يرجى التحقق من بيانات الدخول وتسجيل الدخول.",
        403: "الطلب غير مسموح. حدّث الصفحة وحاول مجدداً.",
        409: "البيانات مستخدمة أو تغيّرت. حدّث الصفحة وحاول مجدداً.",
        429: "طلبات كثيرة. حاول لاحقاً.",
        503: "الخدمة غير متاحة مؤقتاً. حاول لاحقاً.",
      }[status] ||
      (status === 400
        ? "تحقق من البيانات والكميات ثم حاول مجدداً."
        : "تعذّر إكمال الطلب. تحقق من الاتصال وحاول مجدداً.")
    );
  if (!error.response)
    return "Could not reach Loot. Check your connection and try again.";
  const data = error.response.data;
  return typeof data === "string" && data.length < 200
    ? data
    : data?.message || "Could not complete the request. Please try again.";
};
