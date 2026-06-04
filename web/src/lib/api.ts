import { Category, InventoryBatch, Order, Product, Report, Stats, Transaction } from "./types";

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) throw new Error(`API ${path} → ${res.status}`);
  return res.json() as Promise<T>;
}

// Stats
export const getStats = () => req<Stats>("/stats");

// Categories
export const getCategories = () => req<Category[]>("/categories");
export const createCategory = (data: Omit<Category, "id">) =>
  req<Category>("/categories", { method: "POST", body: JSON.stringify(data) });
export const updateCategory = (id: string, data: Partial<Category>) =>
  req<Category>(`/categories/${id}`, { method: "PUT", body: JSON.stringify(data) });
export const deleteCategory = (id: string) =>
  req<void>(`/categories/${id}`, { method: "DELETE" });

// Products
export const getProducts = () => req<Product[]>("/products");
export const getProduct  = (id: string) => req<Product>(`/products/${id}`);
export const createProduct = (data: Omit<Product, "id" | "createdAt" | "updatedAt" | "category" | "isLowStock" | "isOutOfStock">) =>
  req<Product>("/products", { method: "POST", body: JSON.stringify(data) });
export const updateProduct = (id: string, data: Partial<Product>) =>
  req<Product>(`/products/${id}`, { method: "PUT", body: JSON.stringify(data) });
export const deleteProduct = (id: string) =>
  req<void>(`/products/${id}`, { method: "DELETE" });
export const adjustStock = (
  id: string,
  quantity: number,
  type: Transaction["type"],
  note?: string,
  supplier?: string,
  costPerUnit?: number,
) =>
  req<{ product: Product; transaction: Transaction; batch: InventoryBatch | null }>(
    `/products/${id}/adjust`,
    { method: "POST", body: JSON.stringify({ quantity, type, note, supplier, costPerUnit }) },
  );

// Reports
export const getReport = () => req<Report>("/reports");

// Inventory batches
export const getInventoryBatches = (productId?: string, limit = 100) =>
  req<InventoryBatch[]>(
    `/inventory-batches?${productId ? `productId=${productId}&` : ""}limit=${limit}`,
  );

// Transactions
export const getTransactions = (productId?: string, limit = 100) =>
  req<Transaction[]>(`/transactions?${productId ? `productId=${productId}&` : ""}limit=${limit}`);

// Orders
export const getOrders = (limit = 50) => req<Order[]>(`/orders?limit=${limit}`);
export const createOrder = (items: { productId: string; quantity: number }[]) =>
  req<Order>("/orders", { method: "POST", body: JSON.stringify({ items }) });
