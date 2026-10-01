import { get, post, put, refreshCsrf } from "./client";
import axios from "axios";
// These public token flows intentionally bypass the session/CSRF interceptor.
const resetClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  withCredentials: false,
  timeout: 75000,
});
export const authApi = {
  me: () => get("/user/me"),
  signup: async (d) => {
    const result = await post("/user/add", d);
    try { await refreshCsrf(); } catch { return { ...result, authenticated: false }; }
    return result;
  },
  login: async (d) => {
    const user = await post("/user/login", d);
    await refreshCsrf();
    return user;
  },
  logout: async () => {
    await post("/user/logout");
    await refreshCsrf();
  },
  forgot: (d) => resetClient.post("/user/forgot-password", d).then((r) => r.data),
  reset: (d) => resetClient.post("/user/reset-password", d).then((r) => r.data),
  profile: (d) => put("/user/me", d),
  password: async (d) => {
    const result = await put("/user/me/password", d);
    await refreshCsrf();
    return result;
  },
};
