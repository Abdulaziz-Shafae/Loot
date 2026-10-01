import { Routes, Route, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import { Loading, ErrorState, Empty } from "./components/UI";
import Layout from "./layouts/Layout";
import Home from "./pages/Home";
import Auth from "./pages/Auth";
import Dashboard from "./pages/Dashboard";
import Pantry from "./pages/Pantry";
import Recipes from "./pages/Recipes";
import RecipeDetail from "./pages/RecipeDetail";
import History from "./pages/History";
import AI from "./pages/AI";
import Profile from "./pages/Profile";
import Admin from "./pages/Admin";
function Guard({ admin = false }) {
  const { user, loading, error, refresh } = useAuth();
  if (loading) return <Loading />;
  if (error) return <ErrorState error={error} retry={refresh} />;
  if (!user) return <Navigate to="/login" replace />;
  if (admin && user.role !== "ADMIN")
    return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="login" element={<Auth mode="login" />} />
        <Route path="signup" element={<Auth mode="signup" />} />
        {/* Email reset is paused; retain the Auth modes for future restoration. */}
        <Route path="forgot-password" element={<Navigate to="/login" replace />} />
        <Route path="reset-password" element={<Navigate to="/login" replace />} />
        <Route element={<Guard />}>
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="pantry" element={<Pantry />} />
          <Route path="recipes" element={<Recipes />} />
          <Route path="recipes/:type/:id" element={<RecipeDetail />} />
          <Route path="history" element={<History />} />
          <Route path="ai" element={<AI />} />
          <Route path="profile" element={<Profile />} />
          <Route element={<Guard admin />}>
            <Route path="admin/*" element={<Admin />} />
          </Route>
        </Route>
        <Route path="*" element={<Empty title="404 — Loot" />} />
      </Route>
    </Routes>
  );
}
