import WebSocket from "ws";

// ─── WebSocket protocol ──────────────────────────────────────────────────────

export type WsClientType = "android" | "web";

export type WsMessageType =
  | "HANDSHAKE" | "PING" | "PONG" | "CLIENT_LIST" | "ERROR"
  | "PRODUCT_CREATED" | "PRODUCT_UPDATED" | "PRODUCT_DELETED"
  | "STOCK_ADJUSTED" | "LOW_STOCK_ALERT"
  | "ORDER_CREATED"
  | "STATS_UPDATED"
  | "BATCH_ADDED";

export interface WsMessage {
  type: WsMessageType;
  clientId: string;
  clientType: WsClientType | "server";
  payload?: string; // JSON string
  timestamp: number;
}

export interface ExtWebSocket extends WebSocket {
  isAlive: boolean;
  clientId?: string;
}

export type ClientType = WsClientType;

export interface ClientInfo {
  clientId: string;
  clientType: ClientType;
  connectedAt: number;
  ws: WebSocket;
}

export interface ClientSummary {
  clientId: string;
  clientType: ClientType;
  connectedAt: number;
}

// ─── Inventory domain ────────────────────────────────────────────────────────

export interface Category {
  id: string;
  name: string;
  color: string; // tailwind color class, e.g. "blue"
}

export type Gender = "men" | "women" | "kids" | "unisex";

export interface Product {
  id: string;
  name: string;          // model name, e.g. "Air Max 270"
  sku: string;
  categoryId: string;
  brand: string;         // e.g. "Nike"
  productType: string;   // e.g. "Sneakers", "T-Shirt", "Jeans"
  size: string;          // e.g. "42", "M", "XL"
  gender: Gender;
  color: string;         // physical color, e.g. "Black/White"
  price: number;         // selling price
  cost: number;          // purchase cost
  stock: number;
  minStock: number;
  unit: string;          // "pcs", "pair"
  description: string;
  createdAt: number;
  updatedAt: number;
}

export interface InventoryBatch {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  costPerUnit: number;
  totalCost: number;
  supplier: string;
  note: string;
  addedAt: number;
}

export interface Transaction {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  type: "in" | "out" | "adjustment" | "sale";
  quantity: number;
  note: string;
  timestamp: number;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  items: OrderItem[];
  subtotal: number;
  tax: number;
  total: number;
  status: "completed" | "cancelled";
  createdAt: number;
}

export interface Stats {
  totalProducts: number;
  totalValue: number;       // sum of (stock * cost)
  totalRetailValue: number; // sum of (stock * price)
  lowStockCount: number;
  outOfStockCount: number;
  totalCategories: number;
  todayOrders: number;
  todayRevenue: number;
  weeklyRevenue: number[];  // last 7 days
  stockByCategory: { name: string; value: number; color: string }[];
}

// ─── Report types ─────────────────────────────────────────────────────────────

export interface PeriodSummary {
  revenue: number;
  orders: number;
  unitsSold: number;
  avgOrderValue: number;
}

export interface DailyRevenue {
  date: string;     // "YYYY-MM-DD"
  revenue: number;
  orders: number;
}

export interface TopProduct {
  productId: string;
  productName: string;
  brand: string;
  productType: string;
  sku: string;
  unitsSold: number;
  revenue: number;
}

export interface CategoryRevenue {
  name: string;
  color: string;
  revenue: number;
  orders: number;
  percentage: number;
}

export interface SalesReport {
  today: PeriodSummary;
  thisWeek: PeriodSummary;
  thisMonth: PeriodSummary;
  allTime: PeriodSummary;
  last30Days: DailyRevenue[];
  topProducts: TopProduct[];
  byCategory: CategoryRevenue[];
  revenueGrowth: number;   // % week-over-week
}

export interface BrandInventory {
  brand: string;
  products: number;
  totalStock: number;
  costValue: number;
  retailValue: number;
}

export interface InventoryReport {
  summary: {
    totalProducts: number;
    totalStock: number;
    costValue: number;
    retailValue: number;
    potentialProfit: number;
    profitMarginPct: number;
  };
  byCategory: { name: string; color: string; products: number; stock: number; costValue: number; retailValue: number }[];
  byBrand: BrandInventory[];
  topValueProducts: { id: string; name: string; brand: string; size: string; color: string; sku: string; stock: number; unit: string; costValue: number; retailValue: number }[];
  lowStock: { id: string; name: string; brand: string; size: string; sku: string; stock: number; minStock: number; unit: string }[];
  stockMovement: { totalReceived: number; totalSold: number; totalAdjusted: number };
}

export interface Report {
  sales: SalesReport;
  inventory: InventoryReport;
  generatedAt: number;
}
