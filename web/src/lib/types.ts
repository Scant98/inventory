export interface Category {
  id: string;
  name: string;
  color: string;
}

export type Gender = "men" | "women" | "kids" | "unisex";

export interface Product {
  id: string;
  name: string;          // model name e.g. "Air Max 270"
  sku: string;
  categoryId: string;
  category?: Category;
  brand: string;         // e.g. "Nike"
  productType: string;   // e.g. "Sneakers", "T-Shirt"
  size: string;          // e.g. "42", "M"
  gender: Gender;
  color: string;         // e.g. "Black/White"
  price: number;
  cost: number;
  stock: number;
  minStock: number;
  unit: string;
  description: string;
  isLowStock?: boolean;
  isOutOfStock?: boolean;
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

// ─── Report types ─────────────────────────────────────────────────────────────

export interface PeriodSummary {
  revenue: number;
  orders: number;
  unitsSold: number;
  avgOrderValue: number;
}

export interface DailyRevenue {
  date: string;
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
  revenueGrowth: number;
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

export interface Stats {
  totalProducts: number;
  totalValue: number;
  totalRetailValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalCategories: number;
  todayOrders: number;
  todayRevenue: number;
  weeklyRevenue: number[];
  stockByCategory: { name: string; value: number; color: string }[];
}
