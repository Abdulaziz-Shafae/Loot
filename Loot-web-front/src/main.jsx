import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Preferences } from "./context/Preferences";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./components/UI";
import App from "./App";
import "./styles/main.css";
createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <Preferences>
        <AuthProvider>
          <ToastProvider>
            <App />
          </ToastProvider>
        </AuthProvider>
      </Preferences>
    </BrowserRouter>
  </React.StrictMode>,
);
