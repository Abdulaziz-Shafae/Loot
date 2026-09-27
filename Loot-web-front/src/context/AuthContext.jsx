import { createContext, useContext, useEffect, useState } from "react";
import { authApi } from "../api/authApi";
const Context = createContext();
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(null);
  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      setUser(await authApi.me());
    } catch (e) {
      setUser(null);
      if (e.response?.status !== 401) setError(e);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    refresh();
    const listener = () => setUser(null);
    window.addEventListener("loot:unauthorized", listener);
    return () => window.removeEventListener("loot:unauthorized", listener);
  }, []);
  const login = async (data) => setUser(await authApi.login(data));
  const logout = async () => {
    await authApi.logout();
    setUser(null);
  };
  return (
    <Context.Provider
      value={{ user, setUser, loading, error, refresh, login, logout }}
    >
      {children}
    </Context.Provider>
  );
}
export const useAuth = () => useContext(Context);
