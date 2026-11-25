import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api, { clearAuthStorage } from '../services/api';

const AuthContext = createContext(null);

export function getDefaultRouteForRoles(roles = []) {
  if (roles.includes('SUPER_ADMIN') || roles.includes('PRINCIPAL') || roles.includes('ADMIN')) {
    return '/portal/admin';
  }
  if (roles.includes('TEACHER')) return '/portal/teacher';
  if (roles.includes('ACCOUNTANT')) return '/portal/accountant';
  if (roles.includes('LIBRARIAN')) return '/portal/librarian';
  if (roles.includes('PARENT')) return '/portal/parent';
  if (roles.includes('STUDENT')) return '/portal/student';
  return '/portal/profile';
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('tgs_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('tgs_refresh_token');
    try {
      if (localStorage.getItem('tgs_access_token')) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch {
      // Ignore logout network failures
    } finally {
      clearAuthStorage();
      setUser(null);
    }
  }, []);

  const refreshProfile = useCallback(async () => {
    const token = localStorage.getItem('tgs_access_token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const res = await api.get('/auth/me');
      const profile = res.data?.data;
      if (profile) {
        localStorage.setItem('tgs_user', JSON.stringify(profile));
        setUser(profile);
      }
    } catch {
      clearAuthStorage();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshProfile();
    const handleUnauth = () => {
      clearAuthStorage();
      setUser(null);
    };
    window.addEventListener('tgs:unauthorized', handleUnauth);
    return () => window.removeEventListener('tgs:unauthorized', handleUnauth);
  }, [refreshProfile]);

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password });
    const data = res.data?.data;
    localStorage.setItem('tgs_access_token', data.accessToken);
    localStorage.setItem('tgs_refresh_token', data.refreshToken);
    localStorage.setItem('tgs_user', JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  };

  const hasAnyRole = (...allowed) => {
    if (!user?.roles) return false;
    return allowed.some((r) => user.roles.includes(r));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: !!user,
        login,
        logout,
        refreshProfile,
        hasAnyRole,
        defaultRoute: getDefaultRouteForRoles(user?.roles || []),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
