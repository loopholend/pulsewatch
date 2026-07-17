// src/context/AuthContext.jsx
// Single source of truth for authentication state.
// Reads the JWT once and decodes workspace/email from the payload.
// All components should use useAuth() instead of reading localStorage directly.

import React, { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

/** Decode a JWT without verifying signature (signature is verified server-side). */
const decodeToken = (token) => {
    if (!token) return null;
    try {
        const [, payloadB64] = token.split('.');
        if (!payloadB64) return null;
        // atob works with standard base64; JWT uses base64url — fix padding
        const padded = payloadB64.replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(padded));
    } catch (e) {
        console.error('[AuthContext] Failed to decode token:', e);
        return null;
    }
};

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(() => localStorage.getItem('token'));

    const payload = token ? decodeToken(token) : null;
    const email        = payload?.sub         ?? null;
    const workspaceId  = payload?.workspaceId ?? null;
    const workspaceRole = payload?.workspaceRole ?? null;
    const isAuthenticated = !!token;

    const login = useCallback((newToken) => {
        localStorage.setItem('token', newToken);
        setToken(newToken);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRole');
        localStorage.removeItem('displayName');
        setToken(null);
    }, []);

    return (
        <AuthContext.Provider value={{
            token,
            email,
            workspaceId,
            workspaceRole,
            isAuthenticated,
            login,
            logout,
        }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth() must be used within <AuthProvider>');
    return ctx;
};

export default AuthContext;
