// ─── Base URL ────────────────────────────────────────────────────────────────
// Reads from VITE_API_URL at build time; falls back to the Railway backend.
const BASE_URL =
  (typeof import.meta !== "undefined" &&
    (import.meta as { env?: { VITE_API_URL?: string } }).env?.VITE_API_URL) ||
  "http://localhost:8080/api";

// ─── Token helper ────────────────────────────────────────────────────────────
/**
 * Reads the JWT that AuthContext stores under the key "fmcg_token".
 * Returns null when the user is not logged in.
 */
function getToken(): string | null {
  return localStorage.getItem("fmcg_token");
}

// ─── Generic authenticated fetch ─────────────────────────────────────────────
/**
 * Wraps the native fetch API.
 * - Always adds `Content-Type: application/json` for non-FormData bodies.
 * - Always adds `Authorization: Bearer <token>` when a token is present.
 * - Throws an Error with the server's `detail` message (or a generic one) on
 *   non-2xx responses so callers can surface meaningful messages to the UI.
 */
async function apiRequest<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();

  const headers = new Headers(options.headers);

  // Attach auth header whenever a token is available
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  // Only set Content-Type for JSON bodies (FormData sets its own boundary)
  if (!(options.body instanceof FormData)) {
    if (!headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const errorBody = await response.json();
      if (errorBody?.detail) {
        message = errorBody.detail;
      } else if (errorBody?.message) {
        message = errorBody.message;
      }
    } catch {
      // Response body was not JSON — keep the generic message
    }
    throw new Error(message);
  }

  // 204 No Content — return undefined cast to T
  if (response.status === 204) {
    return undefined as unknown as T;
  }

  return response.json() as Promise<T>;
}

// ─── TypeScript types ─────────────────────────────────────────────────────────

export type Role = "ADMIN" | "SHOP_OWNER";
export type OrderStatus = "PENDING" | "CONFIRMED" | "DELIVERED" | "CANCELLED";
export type PaymentMethod = "ONLINE" | "COD";

export interface User {
  id: number;
  email: string;
  name: string;
  phone: string;
  role: Role;
  creditPeriodDays: number;
  isActive: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string | null;
  stock: number;
  category: string;
  isActive: boolean;
  createdAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stock: number;
  category: string;
}

export interface OrderItemRequest {
  productId: number;
  productName?: string;
  quantity: number;
  price?: number;
  total?: number;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  total: number;
}

export interface OrderRequest {
  shopOwnerId?: number;
  items: OrderItemRequest[];
  paymentMethod: PaymentMethod;
}

export interface Order {
  id: number;
  shopOwnerId: number;
  shopOwnerName: string;
  items: OrderItemResponse[];
  totalAmount: number;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  deliveryDate: string | null;
  paymentDueDate: string | null;
  createdBy: number;
  createdAt: string;
}

export interface Notification {
  id: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface SalesAnalytics {
  total_sales: number;
  total_orders: number;
  pending_orders: number;
  delivered_orders: number;
  paid_amount: number;
  pending_amount: number;
  top_products: Array<{
    product_name: string;
    quantity: number;
    revenue: number;
  }>;
  top_shops: Array<{
    shop_name: string;
    total_orders: number;
    total_amount: number;
  }>;
}

// ─── Auth API ─────────────────────────────────────────────────────────────────

export const authApi = {
  /**
   * Register a new user. Public endpoint — no token required.
   */
  register: (data: {
    email: string;
    password: string;
    name: string;
    phone: string;
    role?: Role;
    creditPeriodDays?: number;
  }): Promise<AuthResponse> =>
    apiRequest<AuthResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  /**
   * Log in with email + password. Public endpoint — no token required.
   */
  login: (data: { email: string; password: string }): Promise<AuthResponse> =>
    apiRequest<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  /**
   * Fetch the currently authenticated user's profile.
   * Requires a valid JWT in localStorage.
   */
  me: (): Promise<User> => apiRequest<User>("/auth/me"),

  /**
   * Request a password-reset code for the given email. Public endpoint.
   */
  forgotPassword: (data: { email: string }): Promise<{ message: string; reset_code: string }> =>
    apiRequest("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  /**
   * Reset the password using the code received from forgotPassword.
   */
  resetPassword: (data: {
    email: string;
    resetCode: string;
    newPassword: string;
  }): Promise<{ message: string }> =>
    apiRequest("/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),
};

// ─── Products API ─────────────────────────────────────────────────────────────

export const productsApi = {
  /**
   * Fetch all products. Requires authentication.
   */
  getAll: (): Promise<Product[]> => apiRequest<Product[]>("/products"),

  /**
   * Fetch a single product by ID. Requires authentication.
   */
  getById: (id: number): Promise<Product> =>
    apiRequest<Product>(`/products/${id}`),

  /**
   * Create a new product. Requires authentication.
   */
  create: (data: ProductRequest): Promise<Product> =>
    apiRequest<Product>("/products", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  /**
   * Update an existing product. Requires authentication.
   */
  update: (id: number, data: ProductRequest): Promise<Product> =>
    apiRequest<Product>(`/products/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),

  /**
   * Delete a product by ID. Requires authentication.
   */
  delete: (id: number): Promise<{ message: string }> =>
    apiRequest<{ message: string }>(`/products/${id}`, {
      method: "DELETE",
    }),

  /**
   * Upload a product image. Uses FormData so the Authorization header is
   * added manually — fetch would otherwise override the Content-Type boundary.
   */
  uploadImage: async (
    id: number,
    file: File
  ): Promise<{ image_url: string; path: string }> => {
    const token = getToken();
    const formData = new FormData();
    formData.append("file", file);

    const headers: HeadersInit = {};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${BASE_URL}/products/${id}/upload-image`, {
      method: "POST",
      headers,
      body: formData,
    });

    if (!response.ok) {
      let message = `Image upload failed with status ${response.status}`;
      try {
        const errorBody = await response.json();
        if (errorBody?.detail) message = errorBody.detail;
      } catch {
        // Non-JSON error body
      }
      throw new Error(message);
    }

    return response.json();
  },
};

// ─── Orders API ───────────────────────────────────────────────────────────────

export const ordersApi = {
  /**
   * Create a new order. Requires authentication.
   */
  create: (data: OrderRequest): Promise<Order> =>
    apiRequest<Order>("/orders", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  /**
   * Fetch all orders visible to the authenticated user.
   */
  getAll: (): Promise<Order[]> => apiRequest<Order[]>("/orders"),

  /**
   * Fetch a single order by ID. Requires authentication.
   */
  getById: (id: number): Promise<Order> =>
    apiRequest<Order>(`/orders/${id}`),

  /**
   * Update the status of an order. Requires authentication.
   */
  updateStatus: (id: number, status: OrderStatus): Promise<Order> =>
    apiRequest<Order>(`/orders/${id}/status?status=${status}`, {
      method: "PUT",
    }),
};

// ─── Shop Owners API ──────────────────────────────────────────────────────────

export const shopOwnersApi = {
  /**
   * Fetch all shop owners. Requires authentication (ADMIN role).
   */
  getAll: (): Promise<User[]> => apiRequest<User[]>("/shop-owners"),

  /**
   * Update the credit period (in days) for a shop owner.
   * Requires authentication (ADMIN role).
   */
  updateCreditPeriod: (
    id: number,
    creditPeriodDays: number
  ): Promise<{ message: string }> =>
    apiRequest<{ message: string }>(
      `/shop-owners/${id}/credit-period?creditPeriodDays=${creditPeriodDays}`,
      { method: "PUT" }
    ),
};

// ─── Notifications API ────────────────────────────────────────────────────────

export const notificationsApi = {
  /**
   * Fetch all notifications for the authenticated user.
   */
  getAll: (): Promise<Notification[]> =>
    apiRequest<Notification[]>("/notifications"),

  /**
   * Mark a notification as read.
   */
  markAsRead: (id: number): Promise<{ message: string }> =>
    apiRequest<{ message: string }>(`/notifications/${id}/read`, {
      method: "PUT",
    }),

  /**
   * Get the count of unread notifications for the authenticated user.
   */
  getUnreadCount: (): Promise<{ count: number }> =>
    apiRequest<{ count: number }>("/notifications/unread-count"),
};

// ─── Analytics API ────────────────────────────────────────────────────────────

export const analyticsApi = {
  /**
   * Fetch aggregated sales analytics. Requires authentication.
   */
  getSales: (): Promise<SalesAnalytics> =>
    apiRequest<SalesAnalytics>("/analytics/sales"),
};
