const BASE = 'http://localhost:8080';

function authHeaders(token?: string): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message ?? res.statusText);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// Auth
export const authApi = {
  register: (email: string, password: string) =>
    request('/api/auth/register', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ email, password }),
    }),

  login: (email: string, password: string) =>
    request('/api/auth/login', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ email, password }),
    }),

  logout: (token: string) =>
    request('/api/auth/logout', {
      method: 'POST',
      headers: authHeaders(token),
    }),

  refresh: (refreshToken: string) =>
    request('/api/auth/refresh', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ refreshToken }),
    }),
};

// Users
export const userApi = {
  getUser: (id: string, token: string) =>
    request(`/api/users/${id}`, { headers: authHeaders(token) }),

  updateUser: (id: string, token: string, data: { email?: string; password?: string }) =>
    request(`/api/users/${id}`, {
      method: 'PUT',
      headers: authHeaders(token),
      body: JSON.stringify(data),
    }),
};

// Products
export const productApi = {
  list: () => request('/api/products'),

  get: (id: number) => request(`/api/products/${id}`),

  create: (token: string, data: object) =>
    request('/api/products', {
      method: 'POST',
      headers: authHeaders(token),
      body: JSON.stringify(data),
    }),

  update: (id: number, token: string, data: object) =>
    request(`/api/products/${id}`, {
      method: 'PUT',
      headers: authHeaders(token),
      body: JSON.stringify(data),
    }),

  remove: (id: number, token: string) =>
    request(`/api/products/${id}`, {
      method: 'DELETE',
      headers: authHeaders(token),
    }),
};

// Orders
export const orderApi = {
  create: (userId: string, items: { productId: number; quantity: number }[]) =>
    request('/api/orders', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ userId, items }),
    }),

  get: (id: string) => request(`/api/orders/${id}`),

  listByUser: (userId: string) => request(`/api/orders?userId=${userId}`),

  cancel: (id: string) =>
    request(`/api/orders/${id}/cancel`, { method: 'PUT', headers: authHeaders() }),
};

// Payments
export const paymentApi = {
  getByOrder: (orderId: string) => request(`/api/payments/order/${orderId}`),
};

// Notifications
export const notificationApi = {
  listByUser: (userId: string) => request(`/api/notifications?userId=${userId}`),
};
