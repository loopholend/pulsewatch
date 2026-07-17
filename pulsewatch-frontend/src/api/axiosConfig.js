import axios from 'axios';

const getAuthToken = () => localStorage.getItem('token');

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
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
            // Clear stale auth data
            localStorage.removeItem('token');
            localStorage.removeItem('userEmail');
            localStorage.removeItem('userRole');
            // Only redirect if not already on login/landing page
            const currentPath = window.location.pathname;
            if (currentPath !== '/' && currentPath !== '/login') {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);
