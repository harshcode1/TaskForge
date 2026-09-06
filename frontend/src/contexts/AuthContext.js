'use client';

import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

// Same fallback as src/services/api.js — both hardcoded 'localhost:6060'
// independently before this, which only ever worked by coincidence in local
// dev (the deployed frontend has no way to reach the visitor's own machine).
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:6060/api';

const AuthContext = createContext();

// Plain module-level flag, deliberately outside React state. ProtectedRoute
// needs to distinguish "isAuthenticated just went false because the user
// explicitly logged out" (its own redirect should stand down — the logout
// caller already owns navigation) from "isAuthenticated is false because
// this is someone hitting a protected URL cold" (ProtectedRoute should
// redirect to /login). Timing-based approaches (delay the redirect, cancel
// on unmount, check window.location.pathname at fire-time) all lost this
// race under real navigation timing — Next keeps the old route mounted
// during a client-side transition, and dev-mode transitions are slower
// than any short delay. This encodes intent directly instead of guessing
// at it from timing, so it isn't timing-sensitive at all.
export let isLoggingOut = false;

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(null);

  useEffect(() => {
    // Check for stored token on mount
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser && storedUser !== 'undefined') {
      try {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        // Set default authorization header
        axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
      } catch (error) {
        // Clear invalid data
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/auth/login`, {
        email,
        password,
      });

      const { token: newToken, ...userData } = response.data;

      // Store in localStorage
      localStorage.setItem('token', newToken);
      localStorage.setItem('user', JSON.stringify(userData));

      // Update state
      setToken(newToken);
      setUser(userData);

      // Set default authorization header
      axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;

      return { success: true };
    } catch (error) {
      return { 
        success: false, 
        error: error.response?.data?.message || 'Login failed' 
      };
    }
  };

  const register = async (name, email, password) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/auth/register`, {
        name,
        email,
        password,
      });

      const { token: newToken, ...userData } = response.data;

      // Store in localStorage
      localStorage.setItem('token', newToken);
      localStorage.setItem('user', JSON.stringify(userData));
      
      // Update state
      setToken(newToken);
      setUser(userData);
      
      // Set default authorization header
      axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
      
      return { success: true };
    } catch (error) {
      console.error('Registration error:', error);
      return { 
        success: false, 
        error: error.response?.data?.message || 'Registration failed' 
      };
    }
  };

  // Merges a partial update (e.g. a new name from the Profile page) into both
  // the localStorage copy and the live React state. A page that only wrote to
  // localStorage directly — which the Profile page used to do — updates what
  // survives a reload but not what Navbar is actually reading right now
  // (it's rendering the `user` object from this context's state, not from
  // localStorage), so the name changes never showed up until the user
  // manually refreshed.
  const updateUser = (patch) => {
    setUser((prev) => {
      const next = { ...prev, ...patch };
      localStorage.setItem('user', JSON.stringify(next));
      return next;
    });
  };

  const logout = () => {
    isLoggingOut = true;
    // Cleared after the caller's own navigation (e.g. router.push('/')) has
    // had time to land — see the comment on isLoggingOut above.
    setTimeout(() => { isLoggingOut = false; }, 1500);

    // Clear localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    // Clear state
    setToken(null);
    setUser(null);

    // Remove authorization header
    delete axios.defaults.headers.common['Authorization'];
  };

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    updateUser,
    isAuthenticated: !!user,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

