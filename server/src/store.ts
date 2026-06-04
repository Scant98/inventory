import { db } from "./db";
import { Category, InventoryBatch, Product, Stats } from "./types";

export { db };

export async function allProducts(): Promise<Product[]> {
  const rows = await db.product.findMany({ orderBy: { updatedAt: "desc" } });
  return rows as unknown as Product[];
}

export async function allCategories(): Promise<Category[]> {
  return db.category.findMany();
}

export async function getProduct(id: string): Promise<Product | undefined> {
  const row = await db.product.findUnique({ where: { id } });
  return (row ?? undefined) as unknown as Product | undefined;
}

export async function getCategory(id: string): Promise<Category | undefined> {
  const row = await db.category.findUnique({ where: { id } });
  return row ?? undefined;
}

export async function allInventoryBatches(productId?: string): Promise<InventoryBatch[]> {
  const rows = await db.inventoryBatch.findMany({
    where: productId ? { productId } : undefined,
    orderBy: { addedAt: "desc" },
  });
  return rows as unknown as InventoryBatch[];
}

export async function nextOrderNumber(): Promise<string> {
  const counter = await db.orderCounter.upsert({
    where: { id: 1 },
    update: { current: { increment: 1 } },
    create: { id: 1, current: 1001 },
  });
  return `ORD-${counter.current}`;
}

export async function computeStats(): Promise<Stats> {
  const [products, categories, orders] = await Promise.all([
    db.product.findMany(),
    db.category.findMany(),
    db.order.findMany(),
  ]);

  const now = Date.now();
  const dayStart = new Date();
  dayStart.setHours(0, 0, 0, 0);

  const todayOrders = orders.filter(
    (o) => o.createdAt >= dayStart.getTime() && o.status === "completed"
  );
  const todayRevenue = todayOrders.reduce((s, o) => s + o.total, 0);

  const weeklyRevenue = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(now - (6 - i) * 86_400_000);
    d.setHours(0, 0, 0, 0);
    const end = d.getTime() + 86_400_000;
    return orders
      .filter(
        (o) =>
          o.status === "completed" &&
          o.createdAt >= d.getTime() &&
          o.createdAt < end
      )
      .reduce((s, o) => s + o.total, 0);
  });

  const stockByCategory = categories.map((cat) => ({
    name: cat.name,
    value: products
      .filter((p) => p.categoryId === cat.id)
      .reduce((s, p) => s + p.stock, 0),
    color: cat.color,
  }));

  return {
    totalProducts: products.length,
    totalValue: products.reduce((s, p) => s + p.stock * p.cost, 0),
    totalRetailValue: products.reduce((s, p) => s + p.stock * p.price, 0),
    lowStockCount: products.filter((p) => p.stock > 0 && p.stock <= p.minStock).length,
    outOfStockCount: products.filter((p) => p.stock === 0).length,
    totalCategories: categories.length,
    todayOrders: todayOrders.length,
    todayRevenue,
    weeklyRevenue,
    stockByCategory,
  };
}
