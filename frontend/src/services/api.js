import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

let isRefreshing = false;
let pendingSubscribers = [];

function notifySubscribers(newToken) {
  pendingSubscribers.forEach((cb) => cb(newToken));
  pendingSubscribers = [];
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('tgs_access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      const refreshToken = localStorage.getItem('tgs_refresh_token');
      if (!refreshToken) {
        clearAuthStorage();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingSubscribers.push((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            resolve(api(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
        const data = res.data?.data;
        if (data?.accessToken) {
          localStorage.setItem('tgs_access_token', data.accessToken);
          if (data.refreshToken) {
            localStorage.setItem('tgs_refresh_token', data.refreshToken);
          }
          if (data.user) {
            localStorage.setItem('tgs_user', JSON.stringify(data.user));
          }
          notifySubscribers(data.accessToken);
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        clearAuthStorage();
        window.dispatchEvent(new Event('tgs:unauthorized'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

export function clearAuthStorage() {
  localStorage.removeItem('tgs_access_token');
  localStorage.removeItem('tgs_refresh_token');
  localStorage.removeItem('tgs_user');
}

export function extractErrorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (!err) return fallback;
  const data = err.response?.data;
  if (data?.errors && typeof data.errors === 'object') {
    const first = Object.entries(data.errors)[0];
    if (first) return `${first[0]}: ${first[1]}`;
  }
  return data?.message || err.message || fallback;
}

export async function downloadFile(url, filename) {
  const response = await api.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export default api;
