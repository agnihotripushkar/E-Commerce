'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { AuthResponse, User } from '@/lib/types';
import { authApi } from '@/lib/api';

interface AuthState {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('auth');
    if (stored) {
      try {
        const { user, token } = JSON.parse(stored);
        setUser(user);
        setToken(token);
      } catch {
        localStorage.removeItem('auth');
      }
    }
    setIsLoading(false);
  }, []);

  function persist(user: User, token: string) {
    setUser(user);
    setToken(token);
    localStorage.setItem('auth', JSON.stringify({ user, token }));
  }

  function decodeUser(accessToken: string): User {
    const payload = JSON.parse(atob(accessToken.split('.')[1]));
    return { id: payload.sub, email: payload.email, role: payload.role, createdAt: '' };
  }

  async function login(email: string, password: string) {
    const data = (await authApi.login(email, password)) as AuthResponse;
    persist(decodeUser(data.accessToken), data.accessToken);
  }

  async function register(email: string, password: string) {
    const data = (await authApi.register(email, password)) as AuthResponse;
    persist(decodeUser(data.accessToken), data.accessToken);
  }

  async function logout() {
    if (token) await authApi.logout(token).catch(() => {});
    setUser(null);
    setToken(null);
    localStorage.removeItem('auth');
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
