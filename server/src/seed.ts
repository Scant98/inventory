import { db } from "./db";

async function main() {
  const existing = await db.category.count();
  if (existing > 0) {
    console.log("Database already has data — skipping seed.");
    return;
  }

  console.log("Seeding database...");

  const shoes    = await db.category.create({ data: { name: "Shoes",    color: "blue"   } });
  const clothing = await db.category.create({ data: { name: "Clothing", color: "purple" } });

  const now = Date.now();

  type Seed = {
    name: string; sku: string; categoryId: string; brand: string; productType: string;
    size: string; gender: string; color: string; price: number; cost: number;
    stock: number; minStock: number; unit: string; description: string;
  };

  const shoeSeeds: Seed[] = [
    { name: "Air Max 270",     sku: "SH-NK-001", categoryId: shoes.id,    brand: "Nike",    productType: "Sneakers", size: "40", gender: "men",    color: "Black/White", price: 150, cost: 68, stock: 8,  minStock: 5, unit: "pair", description: "Nike Air Max 270 men's running-inspired sneaker" },
    { name: "Air Max 270",     sku: "SH-NK-002", categoryId: shoes.id,    brand: "Nike",    productType: "Sneakers", size: "42", gender: "men",    color: "Black/White", price: 150, cost: 68, stock: 12, minStock: 5, unit: "pair", description: "Nike Air Max 270 men's running-inspired sneaker" },
    { name: "Air Max 270",     sku: "SH-NK-003", categoryId: shoes.id,    brand: "Nike",    productType: "Sneakers", size: "44", gender: "men",    color: "White",       price: 150, cost: 68, stock: 4,  minStock: 5, unit: "pair", description: "Nike Air Max 270 men's sneaker in white" },
    { name: "Air Force 1 '07", sku: "SH-NK-004", categoryId: shoes.id,    brand: "Nike",    productType: "Sneakers", size: "37", gender: "women",  color: "White",       price: 120, cost: 52, stock: 10, minStock: 4, unit: "pair", description: "Classic Nike Air Force 1 women's low-top" },
    { name: "Air Force 1 '07", sku: "SH-NK-005", categoryId: shoes.id,    brand: "Nike",    productType: "Sneakers", size: "39", gender: "women",  color: "White",       price: 120, cost: 52, stock: 6,  minStock: 4, unit: "pair", description: "Classic Nike Air Force 1 women's low-top" },
    { name: "Revolution 6",    sku: "SH-NK-006", categoryId: shoes.id,    brand: "Nike",    productType: "Running",  size: "41", gender: "men",    color: "Black",       price: 95,  cost: 40, stock: 15, minStock: 6, unit: "pair", description: "Lightweight Nike Revolution 6 running shoe" },
    { name: "Stan Smith",      sku: "SH-AD-001", categoryId: shoes.id,    brand: "Adidas",  productType: "Sneakers", size: "40", gender: "unisex", color: "White/Green", price: 100, cost: 42, stock: 9,  minStock: 4, unit: "pair", description: "Iconic Adidas Stan Smith tennis sneaker" },
    { name: "Stan Smith",      sku: "SH-AD-002", categoryId: shoes.id,    brand: "Adidas",  productType: "Sneakers", size: "42", gender: "unisex", color: "White/Green", price: 100, cost: 42, stock: 7,  minStock: 4, unit: "pair", description: "Iconic Adidas Stan Smith tennis sneaker" },
    { name: "Ultraboost 22",   sku: "SH-AD-003", categoryId: shoes.id,    brand: "Adidas",  productType: "Running",  size: "41", gender: "men",    color: "Black",       price: 180, cost: 85, stock: 5,  minStock: 4, unit: "pair", description: "Adidas Ultraboost 22 premium running shoe" },
    { name: "Ultraboost 22",   sku: "SH-AD-004", categoryId: shoes.id,    brand: "Adidas",  productType: "Running",  size: "43", gender: "men",    color: "Black",       price: 180, cost: 85, stock: 3,  minStock: 4, unit: "pair", description: "Adidas Ultraboost 22 premium running shoe" },
    { name: "RS-X",            sku: "SH-PU-001", categoryId: shoes.id,    brand: "Puma",    productType: "Sneakers", size: "39", gender: "unisex", color: "White/Red",   price: 110, cost: 48, stock: 8,  minStock: 4, unit: "pair", description: "Puma RS-X chunky retro sneaker" },
    { name: "RS-X",            sku: "SH-PU-002", categoryId: shoes.id,    brand: "Puma",    productType: "Sneakers", size: "42", gender: "unisex", color: "White/Red",   price: 110, cost: 48, stock: 6,  minStock: 4, unit: "pair", description: "Puma RS-X chunky retro sneaker" },
    { name: "Old Skool",       sku: "SH-VN-001", categoryId: shoes.id,    brand: "Vans",    productType: "Sneakers", size: "34", gender: "kids",   color: "Black/White", price: 75,  cost: 30, stock: 10, minStock: 5, unit: "pair", description: "Vans Old Skool kids' classic skate shoe" },
    { name: "Old Skool",       sku: "SH-VN-002", categoryId: shoes.id,    brand: "Vans",    productType: "Sneakers", size: "36", gender: "kids",   color: "Black/White", price: 75,  cost: 30, stock: 7,  minStock: 5, unit: "pair", description: "Vans Old Skool kids' classic skate shoe" },
  ];

  const clothingSeeds: Seed[] = [
    { name: "Dri-FIT T-Shirt",   sku: "CL-NK-001", categoryId: clothing.id, brand: "Nike",          productType: "T-Shirt", size: "M",  gender: "men",    color: "Black",      price: 35, cost: 13, stock: 20, minStock: 8, unit: "pcs", description: "Nike Dri-FIT moisture-wicking training tee" },
    { name: "Dri-FIT T-Shirt",   sku: "CL-NK-002", categoryId: clothing.id, brand: "Nike",          productType: "T-Shirt", size: "L",  gender: "men",    color: "Black",      price: 35, cost: 13, stock: 15, minStock: 8, unit: "pcs", description: "Nike Dri-FIT moisture-wicking training tee" },
    { name: "Dri-FIT T-Shirt",   sku: "CL-NK-003", categoryId: clothing.id, brand: "Nike",          productType: "T-Shirt", size: "XL", gender: "men",    color: "Black",      price: 35, cost: 13, stock: 2,  minStock: 8, unit: "pcs", description: "Nike Dri-FIT moisture-wicking training tee" },
    { name: "Sportswear Hoodie", sku: "CL-NK-004", categoryId: clothing.id, brand: "Nike",          productType: "Hoodie",  size: "L",  gender: "men",    color: "Gray",       price: 75, cost: 30, stock: 8,  minStock: 5, unit: "pcs", description: "Nike Sportswear fleece pullover hoodie" },
    { name: "Tiro Track Pants",  sku: "CL-AD-001", categoryId: clothing.id, brand: "Adidas",        productType: "Pants",   size: "M",  gender: "men",    color: "Black",      price: 60, cost: 24, stock: 12, minStock: 5, unit: "pcs", description: "Adidas Tiro 23 football track pants" },
    { name: "Tiro Track Pants",  sku: "CL-AD-002", categoryId: clothing.id, brand: "Adidas",        productType: "Pants",   size: "L",  gender: "men",    color: "Black",      price: 60, cost: 24, stock: 9,  minStock: 5, unit: "pcs", description: "Adidas Tiro 23 football track pants" },
    { name: "Essentials Hoodie", sku: "CL-AD-003", categoryId: clothing.id, brand: "Adidas",        productType: "Hoodie",  size: "L",  gender: "unisex", color: "Gray",       price: 65, cost: 27, stock: 6,  minStock: 4, unit: "pcs", description: "Adidas Essentials fleece hoodie" },
    { name: "Slim Fit Jeans",    sku: "CL-ZR-001", categoryId: clothing.id, brand: "Zara",          productType: "Jeans",   size: "32", gender: "men",    color: "Blue",       price: 70, cost: 28, stock: 10, minStock: 5, unit: "pcs", description: "Zara slim fit stretch denim jeans" },
    { name: "Slim Fit Jeans",    sku: "CL-ZR-002", categoryId: clothing.id, brand: "Zara",          productType: "Jeans",   size: "34", gender: "men",    color: "Blue",       price: 70, cost: 28, stock: 8,  minStock: 5, unit: "pcs", description: "Zara slim fit stretch denim jeans" },
    { name: "Linen Blazer",      sku: "CL-ZR-003", categoryId: clothing.id, brand: "Zara",          productType: "Jacket",  size: "S",  gender: "women",  color: "Beige",      price: 90, cost: 36, stock: 4,  minStock: 3, unit: "pcs", description: "Zara linen blend tailored blazer" },
    { name: "Floral Wrap Dress", sku: "CL-HM-001", categoryId: clothing.id, brand: "H&M",           productType: "Dress",   size: "S",  gender: "women",  color: "White/Pink", price: 50, cost: 18, stock: 7,  minStock: 4, unit: "pcs", description: "H&M floral print wrap dress" },
    { name: "Floral Wrap Dress", sku: "CL-HM-002", categoryId: clothing.id, brand: "H&M",           productType: "Dress",   size: "M",  gender: "women",  color: "White/Pink", price: 50, cost: 18, stock: 5,  minStock: 4, unit: "pcs", description: "H&M floral print wrap dress" },
    { name: "501 Original Jeans",sku: "CL-LV-001", categoryId: clothing.id, brand: "Levi's",        productType: "Jeans",   size: "28", gender: "women",  color: "Blue",       price: 90, cost: 38, stock: 9,  minStock: 4, unit: "pcs", description: "Levi's 501 original straight-leg jeans" },
    { name: "501 Original Jeans",sku: "CL-LV-002", categoryId: clothing.id, brand: "Levi's",        productType: "Jeans",   size: "30", gender: "women",  color: "Blue",       price: 90, cost: 38, stock: 6,  minStock: 4, unit: "pcs", description: "Levi's 501 original straight-leg jeans" },
    { name: "Tech 2.0 T-Shirt",  sku: "CL-UA-001", categoryId: clothing.id, brand: "Under Armour",  productType: "T-Shirt", size: "M",  gender: "men",    color: "Navy",       price: 30, cost: 11, stock: 18, minStock: 8, unit: "pcs", description: "Under Armour Tech 2.0 loose-fit tee" },
    { name: "Running Shorts",    sku: "CL-UA-002", categoryId: clothing.id, brand: "Under Armour",  productType: "Shorts",  size: "M",  gender: "men",    color: "Black",      price: 40, cost: 15, stock: 14, minStock: 6, unit: "pcs", description: "Under Armour Launch running shorts 7\"" },
  ];

  const allSeeds = [...shoeSeeds, ...clothingSeeds];
  const seededProducts = await Promise.all(
    allSeeds.map(async (s) => {
      const createdAt = now - Math.random() * 60 * 86_400_000;
      const p = await db.product.create({
        data: { ...s, createdAt, updatedAt: createdAt },
      });
      if (p.stock > 0) {
        const displayName = `${p.brand} ${p.name}`.trim();
        await db.inventoryBatch.create({
          data: {
            productId: p.id,
            productName: displayName,
            productSku: p.sku,
            quantity: p.stock,
            costPerUnit: p.cost,
            totalCost: p.stock * p.cost,
            supplier: "Opening Stock",
            note: "Initial inventory",
            addedAt: createdAt,
          },
        });
      }
      return p;
    })
  );

  // Seed 7 days of orders
  let orderNum = 1000;
  for (let day = 6; day >= 0; day--) {
    const dayBase = now - day * 86_400_000;
    const numOrders = 2 + Math.floor(Math.random() * 6);
    for (let o = 0; o < numOrders; o++) {
      const numItems = 1 + Math.floor(Math.random() * 3);
      const itemsData = [];
      for (let i = 0; i < numItems; i++) {
        const prod = seededProducts[Math.floor(Math.random() * seededProducts.length)];
        const qty  = 1 + Math.floor(Math.random() * 2);
        itemsData.push({
          productId:   prod.id,
          productName: `${prod.brand} ${prod.name}`.trim(),
          sku:         prod.sku,
          quantity:    qty,
          unitPrice:   prod.price,
          subtotal:    prod.price * qty,
        });
      }
      const subtotal = itemsData.reduce((s, i) => s + i.subtotal, 0);
      const tax = subtotal * 0.1;
      await db.order.create({
        data: {
          orderNumber: `ORD-${++orderNum}`,
          subtotal,
          tax,
          total: subtotal + tax,
          status: "completed",
          createdAt: dayBase + Math.random() * 36_000_000,
          items: { create: itemsData },
        },
      });
    }
  }

  // Seed a few stock-in transactions
  for (const p of seededProducts.slice(0, 8)) {
    await db.transaction.create({
      data: {
        productId:   p.id,
        productName: `${p.brand} ${p.name}`.trim(),
        productSku:  p.sku,
        type:        "in",
        quantity:    p.stock,
        note:        "Initial stock",
        timestamp:   p.createdAt,
      },
    });
  }

  // Set order counter to the last seeded order number
  await db.orderCounter.upsert({
    where:  { id: 1 },
    update: { current: orderNum },
    create: { id: 1, current: orderNum },
  });

  console.log(`Seeded: ${allSeeds.length} products, ${orderNum - 1000} orders.`);
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(() => db.$disconnect());
