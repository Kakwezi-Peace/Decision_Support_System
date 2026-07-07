import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import { api, setAuthToken, getAuthToken } from "../api/client";
import type { AuthResponse, Role } from "../types";

interface CurrentUser {
  username: string;
  fullName: string;
  role: Role;
}

interface AuthContextValue {
  user: CurrentUser | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: (fullName: string) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const USER_STORAGE_KEY = "dss_user";

function loadStoredUser(): CurrentUser | null {
  if (!getAuthToken()) return null;
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as CurrentUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(loadStoredUser());

  const login = useCallback(async (username: string, password: string) => {
    const response = await api.post<AuthResponse>("/api/auth/login", { username, password });
    setAuthToken(response.token);
    const currentUser: CurrentUser = {
      username: response.username,
      fullName: response.fullName,
      role: response.role,
    };
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(currentUser));
    setUser(currentUser);
  }, []);

  const logout = useCallback(() => {
    setAuthToken(null);
    localStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }, []);

  const refreshUser = useCallback((fullName: string) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, fullName };
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updated));
      return updated;
    });
  }, []);

  return <AuthContext.Provider value={{ user, login, logout, refreshUser }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
