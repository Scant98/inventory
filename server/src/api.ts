import { Router } from "express";
import {
  db,
  allProducts,
  allCategories,
  allInventoryBatches,
  computeStats,
  nextOrderNumber,
} from "./store";
import { broadcastInventoryEvent } from "./messageHandler";
import { Category, Product, Report, Transaction } from "./types";

export const api = Router();

api.use((req, res, next) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") { res.sendStatus(204); return; }
  next();
});

// ─── Stats ───────────────────────────────────────────────────────────────────

api.get("/stats", async (_req, res) => {
  try {
    res.json(await computeStats());
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Categories ──────────────────────────────────────────────────────────────

api.get("/categories", async (_req, res) => {
  try {
    res.json(await allCategories());
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.post("/categories", async (req, res) => {
  try {
    const { name, color } = req.body as Partial<Category>;
    if (!name) { res.status(400).json({ error: "name required" }); return; }
    const cat = await db.category.create({ data: { name, color: color ?? "gray" } });
    res.status(201).json(cat);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.put("/categories/:id", async (req, res) => {
  try {
    const existing = await db.category.findUnique({ where: { id: req.params.id } });
    if (!existing) { res.status(404).json({ error: "not found" }); return; }
    const updated = await db.category.update({
      where: { id: req.params.id },
      data: { name: req.body.name ?? existing.name, color: req.body.color ?? existing.color },
    });
    res.json(updated);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.delete("/categories/:id", async (req, res) => {
  try {
    const existing = await db.category.findUnique({ where: { id: req.params.id } });
    if (!existing) { res.status(404).json({ error: "not found" }); return; }
    const count = await db.product.count({ where: { categoryId: req.params.id } });
    if (count > 0) {
      res.status(400).json({ error: "cannot delete category with products" });
      return;
    }
    await db.category.delete({ where: { id: req.params.id } });
    res.sendStatus(204);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Products ────────────────────────────────────────────────────────────────

api.get("/products", async (_req, res) => {
  try {
    const prods = await db.product.findMany({
      include: { category: true },
      orderBy: { updatedAt: "desc" },
    });
    res.json(
      prods.map((p) => ({
        ...p,
        isLowStock: p.stock > 0 && p.stock <= p.minStock,
        isOutOfStock: p.stock === 0,
      }))
    );
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.get("/products/:id", async (req, res) => {
  try {
    const p = await db.product.findUnique({
      where: { id: req.params.id },
      include: { category: true },
    });
    if (!p) { res.status(404).json({ error: "not found" }); return; }
    res.json(p);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.post("/products", async (req, res) => {
  try {
    const body = req.body as Partial<Product>;
    if (!body.name || !body.sku || !body.categoryId) {
      res.status(400).json({ error: "name, sku, categoryId required" }); return;
    }
    const now = Date.now();

    const product = await db.product.create({
      data: {
        name: body.name,
        sku: body.sku,
        categoryId: body.categoryId,
        brand: body.brand ?? "",
        productType: body.productType ?? "",
        size: body.size ?? "",
        gender: body.gender ?? "unisex",
        color: body.color ?? "",
        price: body.price ?? 0,
        cost: body.cost ?? 0,
        stock: body.stock ?? 0,
        minStock: body.minStock ?? 5,
        unit: body.unit ?? "pcs",
        description: body.description ?? "",
        createdAt: now,
        updatedAt: now,
      },
    });

    if (product.stock > 0) {
      const displayName = `${product.brand} ${product.name}`.trim();
      await db.inventoryBatch.create({
        data: {
          productId: product.id,
          productName: displayName,
          productSku: product.sku,
          quantity: product.stock,
          costPerUnit: product.cost,
          totalCost: product.stock * product.cost,
          supplier: (body as any).supplier ?? "Opening Stock",
          note: "Initial stock on product creation",
          addedAt: now,
        },
      });
      await db.transaction.create({
        data: {
          productId: product.id,
          productName: displayName,
          productSku: product.sku,
          type: "in",
          quantity: product.stock,
          note: "Initial stock on product creation",
          timestamp: now,
        },
      });
    }

    broadcastInventoryEvent("PRODUCT_CREATED", product);
    res.status(201).json(product);
  } catch (e: any) {
    if (e?.code === "P2002") {
      res.status(400).json({ error: "SKU already exists" });
    } else {
      res.status(500).json({ error: "server error" });
    }
  }
});

api.put("/products/:id", async (req, res) => {
  try {
    const existing = await db.product.findUnique({ where: { id: req.params.id } });
    if (!existing) { res.status(404).json({ error: "not found" }); return; }

    const allowed = ["name", "sku", "categoryId", "brand", "productType", "size",
      "gender", "color", "price", "cost", "stock", "minStock", "unit", "description"];
    const data: Record<string, unknown> = { updatedAt: Date.now() };
    for (const key of allowed) {
      if (req.body[key] !== undefined) data[key] = req.body[key];
    }

    const updated = await db.product.update({ where: { id: req.params.id }, data });
    broadcastInventoryEvent("PRODUCT_UPDATED", updated);
    checkAndAlertLowStock(updated as unknown as Product);
    res.json(updated);
  } catch (e: any) {
    if (e?.code === "P2002") {
      res.status(400).json({ error: "SKU already exists" });
    } else {
      res.status(500).json({ error: "server error" });
    }
  }
});

api.delete("/products/:id", async (req, res) => {
  try {
    const existing = await db.product.findUnique({ where: { id: req.params.id } });
    if (!existing) { res.status(404).json({ error: "not found" }); return; }
    await db.product.delete({ where: { id: req.params.id } });
    broadcastInventoryEvent("PRODUCT_DELETED", { id: req.params.id });
    res.sendStatus(204);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Stock adjustment ─────────────────────────────────────────────────────────

api.post("/products/:id/adjust", async (req, res) => {
  try {
    const { quantity, type, note, supplier, costPerUnit } = req.body as {
      quantity: number;
      type: Transaction["type"];
      note?: string;
      supplier?: string;
      costPerUnit?: number;
    };
    if (typeof quantity !== "number") {
      res.status(400).json({ error: "quantity (number) required" }); return;
    }

    const product = await db.product.findUnique({ where: { id: req.params.id } });
    if (!product) { res.status(404).json({ error: "not found" }); return; }

    const delta = type === "out" || type === "sale" ? -Math.abs(quantity) : Math.abs(quantity);
    const newStock = Math.max(0, product.stock + delta);
    const now = Date.now();
    const displayName = `${product.brand} ${product.name}`.trim();

    const updated = await db.product.update({
      where: { id: product.id },
      data: { stock: newStock, updatedAt: now },
    });

    const txRecord = await db.transaction.create({
      data: {
        productId: product.id,
        productName: displayName,
        productSku: product.sku,
        type: type ?? "adjustment",
        quantity: Math.abs(quantity),
        note: note ?? "",
        timestamp: now,
      },
    });

    let batch = null;
    if (type === "in") {
      const unitCost = costPerUnit ?? product.cost;
      batch = await db.inventoryBatch.create({
        data: {
          productId: product.id,
          productName: displayName,
          productSku: product.sku,
          quantity: Math.abs(quantity),
          costPerUnit: unitCost,
          totalCost: Math.abs(quantity) * unitCost,
          supplier: supplier ?? "",
          note: note ?? "",
          addedAt: now,
        },
      });
      broadcastInventoryEvent("BATCH_ADDED", batch);
    }

    broadcastInventoryEvent("STOCK_ADJUSTED", { product: updated, transaction: txRecord });
    checkAndAlertLowStock(updated as unknown as Product);
    res.json({ product: updated, transaction: txRecord, batch: batch ?? null });
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Inventory batches ────────────────────────────────────────────────────────

api.get("/inventory-batches", async (req, res) => {
  try {
    const { productId, limit = "100" } = req.query as { productId?: string; limit?: string };
    const result = await allInventoryBatches(productId);
    res.json(result.slice(0, parseInt(limit)));
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Transactions ────────────────────────────────────────────────────────────

api.get("/transactions", async (req, res) => {
  try {
    const { productId, limit = "50" } = req.query as { productId?: string; limit?: string };
    const rows = await db.transaction.findMany({
      where: productId ? { productId } : undefined,
      orderBy: { timestamp: "desc" },
      take: parseInt(limit),
    });
    res.json(rows);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Orders ──────────────────────────────────────────────────────────────────

api.get("/orders", async (req, res) => {
  try {
    const { limit = "50" } = req.query as { limit?: string };
    const rows = await db.order.findMany({
      include: { items: true },
      orderBy: { createdAt: "desc" },
      take: parseInt(limit),
    });
    res.json(rows);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

api.post("/orders", async (req, res) => {
  try {
    const { items } = req.body as { items: Array<{ productId: string; quantity: number }> };
    if (!items?.length) { res.status(400).json({ error: "items required" }); return; }

    // Validate stock before touching anything
    const resolved: Array<{ product: any; quantity: number }> = [];
    for (const item of items) {
      const p = await db.product.findUnique({ where: { id: item.productId } });
      if (!p) { res.status(400).json({ error: `product ${item.productId} not found` }); return; }
      if (p.stock < item.quantity) {
        res.status(400).json({ error: `insufficient stock for ${p.name}` }); return;
      }
      resolved.push({ product: p, quantity: item.quantity });
    }

    const orderNumber = await nextOrderNumber();
    const now = Date.now();

    // Atomically deduct stock, create transactions, and create the order
    const order = await db.$transaction(async (t) => {
      const orderItemsData = resolved.map(({ product: p, quantity }) => {
        const displayName = `${p.brand} ${p.name}`.trim();
        return {
          productId: p.id,
          productName: displayName,
          sku: p.sku,
          quantity,
          unitPrice: p.price,
          subtotal: p.price * quantity,
        };
      });

      for (const { product: p, quantity } of resolved) {
        await t.product.update({
          where: { id: p.id },
          data: { stock: p.stock - quantity, updatedAt: now },
        });
        await t.transaction.create({
          data: {
            productId: p.id,
            productName: `${p.brand} ${p.name}`.trim(),
            productSku: p.sku,
            type: "sale",
            quantity,
            note: `Order ${orderNumber}`,
            timestamp: now,
          },
        });
      }

      const subtotal = orderItemsData.reduce((s, i) => s + i.subtotal, 0);
      const tax = subtotal * 0.1;
      return t.order.create({
        data: {
          orderNumber,
          subtotal,
          tax,
          total: subtotal + tax,
          status: "completed",
          createdAt: now,
          items: { create: orderItemsData },
        },
        include: { items: true },
      });
    });

    // Broadcast stock updates after transaction committed
    for (const { product: p, quantity } of resolved) {
      const updated = await db.product.findUnique({ where: { id: p.id } });
      if (updated) {
        broadcastInventoryEvent("STOCK_ADJUSTED", { product: updated });
        checkAndAlertLowStock(updated as unknown as Product);
      }
    }

    broadcastInventoryEvent("ORDER_CREATED", order);
    res.status(201).json(order);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Reports ─────────────────────────────────────────────────────────────────

api.get("/reports", async (_req, res) => {
  try {
    const now = Date.now();
    const [products, categories, orders, txns] = await Promise.all([
      db.product.findMany(),
      db.category.findMany(),
      db.order.findMany({ include: { items: true } }),
      db.transaction.findMany(),
    ]);

    const productMap = new Map(products.map((p) => [p.id, p]));

    const todayStart   = new Date(); todayStart.setHours(0, 0, 0, 0);
    const weekStart    = new Date(now - 6 * 86_400_000); weekStart.setHours(0, 0, 0, 0);
    const prevWeekStart = new Date(now - 13 * 86_400_000); prevWeekStart.setHours(0, 0, 0, 0);
    const monthStart   = new Date(); monthStart.setDate(1); monthStart.setHours(0, 0, 0, 0);

    const completedOrders = orders.filter((o) => o.status === "completed");

    function periodSummary(from: number, to: number = now) {
      const os = completedOrders.filter((o) => o.createdAt >= from && o.createdAt <= to);
      const revenue = os.reduce((s, o) => s + o.total, 0);
      const unitsSold = os.reduce((s, o) => s + o.items.reduce((ss, i) => ss + i.quantity, 0), 0);
      return {
        revenue,
        orders: os.length,
        unitsSold,
        avgOrderValue: os.length ? revenue / os.length : 0,
      };
    }

    const thisWeekRevenue = periodSummary(weekStart.getTime()).revenue;
    const prevWeekRevenue = periodSummary(prevWeekStart.getTime(), weekStart.getTime()).revenue;
    const revenueGrowth =
      prevWeekRevenue === 0
        ? thisWeekRevenue > 0 ? 100 : 0
        : Math.round(((thisWeekRevenue - prevWeekRevenue) / prevWeekRevenue) * 100);

    const last30Days = Array.from({ length: 30 }, (_, i) => {
      const d = new Date(now - (29 - i) * 86_400_000);
      d.setHours(0, 0, 0, 0);
      const end = d.getTime() + 86_400_000;
      const dayOrders = completedOrders.filter(
        (o) => o.createdAt >= d.getTime() && o.createdAt < end
      );
      return {
        date: d.toISOString().slice(0, 10),
        revenue: dayOrders.reduce((s, o) => s + o.total, 0),
        orders: dayOrders.length,
      };
    });

    const productRevMap = new Map<
      string,
      { name: string; brand: string; type: string; sku: string; qty: number; rev: number }
    >();
    for (const o of completedOrders) {
      for (const item of o.items) {
        const p = productMap.get(item.productId);
        const existing = productRevMap.get(item.productId) ?? {
          name: item.productName,
          brand: p?.brand ?? "",
          type: p?.productType ?? "",
          sku: item.sku,
          qty: 0,
          rev: 0,
        };
        existing.qty += item.quantity;
        existing.rev += item.subtotal;
        productRevMap.set(item.productId, existing);
      }
    }
    const topProducts = Array.from(productRevMap.entries())
      .map(([id, v]) => ({
        productId: id,
        productName: v.name,
        brand: v.brand,
        productType: v.type,
        sku: v.sku,
        unitsSold: v.qty,
        revenue: v.rev,
      }))
      .sort((a, b) => b.revenue - a.revenue)
      .slice(0, 10);

    const catRevMap = new Map<string, number>();
    for (const o of completedOrders) {
      for (const item of o.items) {
        const p = productMap.get(item.productId);
        if (!p) continue;
        catRevMap.set(p.categoryId, (catRevMap.get(p.categoryId) ?? 0) + item.subtotal);
      }
    }
    const totalRevenue = completedOrders.reduce((s, o) => s + o.total, 0);
    const byCategory = categories
      .map((cat) => ({
        name: cat.name,
        color: cat.color,
        revenue: catRevMap.get(cat.id) ?? 0,
        orders: completedOrders.filter((o) =>
          o.items.some((i) => productMap.get(i.productId)?.categoryId === cat.id)
        ).length,
        percentage:
          totalRevenue > 0
            ? Math.round(((catRevMap.get(cat.id) ?? 0) / totalRevenue) * 100)
            : 0,
      }))
      .sort((a, b) => b.revenue - a.revenue);

    const costValue   = products.reduce((s, p) => s + p.stock * p.cost, 0);
    const retailValue = products.reduce((s, p) => s + p.stock * p.price, 0);
    const totalStock  = products.reduce((s, p) => s + p.stock, 0);

    const invByCategory = categories.map((cat) => {
      const catProds = products.filter((p) => p.categoryId === cat.id);
      return {
        name: cat.name,
        color: cat.color,
        products: catProds.length,
        stock: catProds.reduce((s, p) => s + p.stock, 0),
        costValue: catProds.reduce((s, p) => s + p.stock * p.cost, 0),
        retailValue: catProds.reduce((s, p) => s + p.stock * p.price, 0),
      };
    });

    const brandMap = new Map<
      string,
      { products: number; stock: number; cost: number; retail: number }
    >();
    for (const p of products) {
      const brand = p.brand || "Unknown";
      const e = brandMap.get(brand) ?? { products: 0, stock: 0, cost: 0, retail: 0 };
      e.products += 1;
      e.stock += p.stock;
      e.cost += p.stock * p.cost;
      e.retail += p.stock * p.price;
      brandMap.set(brand, e);
    }
    const byBrand = Array.from(brandMap.entries())
      .map(([brand, v]) => ({
        brand,
        products: v.products,
        totalStock: v.stock,
        costValue: v.cost,
        retailValue: v.retail,
      }))
      .sort((a, b) => b.retailValue - a.retailValue);

    const topValueProducts = [...products]
      .map((p) => ({
        id: p.id,
        name: `${p.brand} ${p.name}`.trim(),
        brand: p.brand,
        size: p.size,
        color: p.color,
        sku: p.sku,
        stock: p.stock,
        unit: p.unit,
        costValue: p.stock * p.cost,
        retailValue: p.stock * p.price,
      }))
      .sort((a, b) => b.retailValue - a.retailValue)
      .slice(0, 10);

    const lowStock = products
      .filter((p) => p.stock > 0 && p.stock <= p.minStock)
      .map((p) => ({
        id: p.id,
        name: `${p.brand} ${p.name}`.trim(),
        brand: p.brand,
        size: p.size,
        sku: p.sku,
        stock: p.stock,
        minStock: p.minStock,
        unit: p.unit,
      }));

    const totalReceived = txns.filter((t) => t.type === "in").reduce((s, t) => s + t.quantity, 0);
    const totalSold     = txns.filter((t) => t.type === "sale").reduce((s, t) => s + t.quantity, 0);
    const totalAdjusted = txns
      .filter((t) => t.type === "adjustment" || t.type === "out")
      .reduce((s, t) => s + t.quantity, 0);

    const report: Report = {
      sales: {
        today:     periodSummary(todayStart.getTime()),
        thisWeek:  periodSummary(weekStart.getTime()),
        thisMonth: periodSummary(monthStart.getTime()),
        allTime:   periodSummary(0),
        last30Days,
        topProducts,
        byCategory,
        revenueGrowth,
      },
      inventory: {
        summary: {
          totalProducts: products.length,
          totalStock,
          costValue,
          retailValue,
          potentialProfit: retailValue - costValue,
          profitMarginPct:
            retailValue > 0
              ? Math.round(((retailValue - costValue) / retailValue) * 100)
              : 0,
        },
        byCategory: invByCategory,
        byBrand,
        topValueProducts,
        lowStock,
        stockMovement: { totalReceived, totalSold, totalAdjusted },
      },
      generatedAt: now,
    };

    res.json(report);
  } catch {
    res.status(500).json({ error: "server error" });
  }
});

// ─── Low-stock check ──────────────────────────────────────────────────────────

function checkAndAlertLowStock(product: Product) {
  if (product.stock <= product.minStock) {
    broadcastInventoryEvent("LOW_STOCK_ALERT", {
      productId: product.id,
      productName: `${product.brand} ${product.name}`.trim(),
      sku: product.sku,
      stock: product.stock,
      minStock: product.minStock,
    });
  }
}
