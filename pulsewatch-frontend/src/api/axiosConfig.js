import axios from 'axios';

const getAuthToken = () => localStorage.getItem('token');

export const api = axios.create({
    // Use relative path so this works with Vite dev proxy AND Nginx reverse proxy in Docker.
    // Fallback to the env var for non-proxied setups.
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

// Request interceptor: attach Bearer token to every request
api.interceptors.request.use((config) => {
    const token = getAuthToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor: redirect to login on 401 (expired/missing token)
// This ensures silent 401 failures are caught and the user is redirected.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            // Clear ALL stale auth data (must match AuthContext.logout())
            localStorage.removeItem('token');
            localStorage.removeItem('userEmail');
            localStorage.removeItem('userRole');
            localStorage.removeItem('displayName');
            // Only redirect if not already on login/landing page
            const currentPath = window.location.pathname;
            if (currentPath !== '/' && currentPath !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);
