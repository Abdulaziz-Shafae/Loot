import { get, post, put, refreshCsrf } from "./client";
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
  forgot: (d) => post("/user/forgot-password", d),
  reset: (d) => post("/user/reset-password", d),
  profile: (d) => put("/user/me", d),
  password: async (d) => {
    const result = await put("/user/me/password", d);
    await refreshCsrf();
    return result;
  },
};
