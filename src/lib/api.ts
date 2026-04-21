// ─── Base configuration ────────────────────────────────────────────────────

const BASE_URL = "https://backendfullfmcg-production.up.railway.app/api";
const TOKEN_KEY = "fmcg_token";

// ─── TypeScript types ──────────────────────────────────────────────────────

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

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
  role?: Role;
  creditPeriodDays?: number;
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

export interface UploadImageResponse {
  image_url: string;
  path: string;
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
  paymentMethod?: PaymentMethod;
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
  type: string;
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

// ─── Token helpers ─────────────────────────────────────────────────────────

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// ─── Image URL helper ──────────────────────────────────────────────────────

/**
 * Resolves a relative image path returned by the backend into a full URL.
 * If the value is already an absolute URL it is returned unchanged.
 */
export function getImageUrl(path: string | null | undefined): string {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  // Strip a leading slash so we don't double-up
  const clean = path.startsWith("/") ? path.slice(1) : path;
  return `${BASE_URL}/files/${clean}`;
}

// ─── Core request helper ───────────────────────────────────────────────────

/**
 * Makes an authenticated HTTP request to the backend.
 *
 * - Automatically attaches `Authorization: Bearer <token>` when a token is
 *   present in localStorage.
 * - Sets `Content-Type: application/json` for plain-object bodies.
 * - Passes `FormData` bodies through without a Content-Type header so the
 *   browser can set the correct multipart boundary automatically.
 * - Throws a descriptive `Error` for non-2xx responses.
 */
export async function apiRequest<T = unknown>(
  method: string,
  path: string,
  body?: unknown,
  extraHeaders?: Record<string, string>
): Promise<T> {
  const token = getToken();

  const headers: Record<string, string> = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extraHeaders,
  };

  let requestBody: BodyInit | undefined;

  if (body instanceof FormData) {
    // Let the browser set Content-Type with the correct multipart boundary
    requestBody = body;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    requestBody = JSON.stringify(body);
  }

  const url = path.startsWith("http") ? path : `${BASE_URL}${path}`;

  const response = await fetch(url, {
    method,
    headers,
    body: requestBody,
  });

  if (!response.ok) {
    let message = `Request failed: ${response.status} ${response.statusText}`;
    try {
      const errorBody = await response.json();
      if (errorBody?.detail) message = errorBody.detail;
      else if (errorBody?.message) message = errorBody.message;
    } catch {
      // ignore JSON parse errors on error responses
    }
    throw new Error(message);
  }

  // Some endpoints return 200 with an empty body (e.g. delete)
  const contentType = response.headers.get("Content-Type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }

  return response.text() as unknown as T;
}

// ─── Auth API ──────────────────────────────────────────────────────────────

export const authApi = {
  /** Authenticate with email + password. Returns a JWT token and user info. */
  login(data: LoginRequest): Promise<AuthResponse> {
    return apiRequest<AuthResponse>("POST", "/auth/login", data);
  },

  /** Register a new account. Returns a JWT token and user info. */
  register(data: RegisterRequest): Promise<AuthResponse> {
    return apiRequest<AuthResponse>("POST", "/auth/register", data);
  },

  /** Fetch the currently authenticated user's profile. */
  me(): Promise<User> {
    return apiRequest<User>("GET", "/auth/me");
  },
};

// ─── Products API ──────────────────────────────────────────────────────────

export const productsApi = {
  /** Retrieve all products. */
  getAll(): Promise<Product[]> {
    return apiRequest<Product[]>("GET", "/products");
  },

  /** Create a new product. Requires ADMIN role. */
  create(data: ProductRequest): Promise<Product> {
    return apiRequest<Product>("POST", "/products", data);
  },

  /** Update an existing product by ID. Requires ADMIN role. */
  update(id: number, data: ProductRequest): Promise<Product> {
    return apiRequest<Product>("PUT", `/products/${id}`, data);
  },

  /** Delete a product by ID. Requires ADMIN role. */
  delete(id: number): Promise<{ message: string }> {
    return apiRequest<{ message: string }>("DELETE", `/products/${id}`);
  },

  /**
   * Upload a product image.
   * Accepts a `File` object and sends it as multipart/form-data.
   */
  uploadImage(id: number, file: File): Promise<UploadImageResponse> {
    const formData = new FormData();
    formData.append("file", file);
    return apiRequest<UploadImageResponse>(
      "POST",
      `/products/${id}/upload-image`,
      formData
    );
  },
};

// ─── Orders API ────────────────────────────────────────────────────────────

export const ordersApi = {
  /** Create a new order for the authenticated shop owner. */
  create(data: OrderRequest): Promise<Order> {
    return apiRequest<Order>("POST", "/orders", data);
  },

  /** Retrieve all orders visible to the authenticated user. */
  getAll(): Promise<Order[]> {
    return apiRequest<Order[]>("GET", "/orders");
  },

  /** Retrieve a single order by ID. */
  getById(id: number): Promise<Order> {
    return apiRequest<Order>("GET", `/orders/${id}`);
  },

  /**
   * Update the status of an order.
   * `status` must be one of: PENDING | CONFIRMED | DELIVERED | CANCELLED
   */
  updateStatus(id: number, status: OrderStatus): Promise<Order> {
    return apiRequest<Order>(
      "PUT",
      `/orders/${id}/status?status=${encodeURIComponent(status)}`
    );
  },
};

// ─── Shop Owners API ───────────────────────────────────────────────────────

export const shopOwnersApi = {
  /** Retrieve all shop owner accounts. Requires ADMIN role. */
  getAll(): Promise<User[]> {
    return apiRequest<User[]>("GET", "/shop-owners");
  },

  /**
   * Update the credit period (in days) for a shop owner.
   * Requires ADMIN role.
   */
  updateCreditPeriod(
    id: number,
    creditPeriodDays: number
  ): Promise<{ message: string }> {
    return apiRequest<{ message: string }>(
      "PUT",
      `/shop-owners/${id}/credit-period?creditPeriodDays=${encodeURIComponent(
        creditPeriodDays
      )}`
    );
  },
};

// ─── Notifications API ─────────────────────────────────────────────────────

export const notificationsApi = {
  /** Retrieve all notifications for the authenticated user. */
  getAll(): Promise<Notification[]> {
    return apiRequest<Notification[]>("GET", "/notifications");
  },

  /** Mark a specific notification as read. */
  markAsRead(id: number): Promise<{ message: string }> {
    return apiRequest<{ message: string }>("PUT", `/notifications/${id}/read`);
  },

  /** Get the count of unread notifications for the authenticated user. */
  getUnreadCount(): Promise<{ count: number }> {
    return apiRequest<{ count: number }>("GET", "/notifications/unread-count");
  },
};

// ─── Analytics API ─────────────────────────────────────────────────────────

export const analyticsApi = {
  /** Retrieve aggregated sales analytics. Requires ADMIN role. */
  getSales(): Promise<SalesAnalytics> {
    return apiRequest<SalesAnalytics>("GET", "/analytics/sales");
  },
};
