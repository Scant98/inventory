"use client";

import { useState, useEffect, useMemo } from "react";
import {
  SearchIcon, ShoppingCartIcon, CheckCircleIcon, Trash2Icon,
  PlusIcon, MinusIcon, ReceiptIcon, XIcon,
} from "lucide-react";
import { toast } from "sonner";
import { useInventoryCtx } from "@/components/inventory-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Order, Product } from "@/lib/types";
import { getOrders, createOrder } from "@/lib/api";

interface CartItem { product: Product; quantity: number; }

export default function POSPage() {
  const { products, refreshProducts, refreshStats } = useInventoryCtx();
  const [orders, setOrders] = useState<Order[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [search, setSearch] = useState("");
  const [catFilter, setCatFilter] = useState("all");
  const [genderFilter, setGenderFilter] = useState("all");
  const [placing, setPlacing] = useState(false);
  const [tab, setTab] = useState("products");

  useEffect(() => {
    getOrders(100).then(setOrders).catch(() => toast.error("Failed to load orders"));
  }, []);

  const available = useMemo(() => products.filter(p => p.stock > 0), [products]);

  const categories = useMemo(() => {
    const map = new Map<string, string>();
    products.forEach(p => { if (p.category) map.set(p.categoryId, p.category.name); });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [products]);

  const filtered = useMemo(() => {
    let list = available;
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        p.size.toLowerCase().includes(q) ||
        p.color.toLowerCase().includes(q) ||
        p.productType.toLowerCase().includes(q)
      );
    }
    if (catFilter !== "all") list = list.filter(p => p.categoryId === catFilter);
    if (genderFilter !== "all") list = list.filter(p => p.gender === genderFilter);
    return list;
  }, [available, search, catFilter, genderFilter]);

  const cartTotals = useMemo(() => {
    const sub = cart.reduce((s, i) => s + i.product.price * i.quantity, 0);
    return { sub, tax: sub * 0.1, total: sub * 1.1 };
  }, [cart]);

  const cartCount = cart.reduce((s, i) => s + i.quantity, 0);

  const addToCart = (p: Product) => {
    setCart(prev => {
      const existing = prev.find(i => i.product.id === p.id);
      const currentQty = existing?.quantity ?? 0;
      if (currentQty >= p.stock) { toast.error(`Only ${p.stock} ${p.unit} in stock`); return prev; }
      return existing
        ? prev.map(i => i.product.id === p.id ? { ...i, quantity: i.quantity + 1 } : i)
        : [...prev, { product: p, quantity: 1 }];
    });
  };

  const setQty = (productId: string, delta: number) => {
    setCart(prev => prev
      .map(i => i.product.id === productId ? { ...i, quantity: Math.max(0, i.quantity + delta) } : i)
      .filter(i => i.quantity > 0)
    );
  };

  const removeFromCart = (productId: string) =>
    setCart(prev => prev.filter(i => i.product.id !== productId));

  const completeSale = async () => {
    if (cart.length === 0) { toast.error("Cart is empty"); return; }
    setPlacing(true);
    try {
      const order = await createOrder(cart.map(i => ({ productId: i.product.id, quantity: i.quantity })));
      setOrders(prev => [order, ...prev]);
      setCart([]);
      refreshProducts(); refreshStats();
      toast.success(`Sale complete — ${order.orderNumber}  $${order.total.toFixed(2)}`);
      setTab("products");
    } catch (e: unknown) {
      toast.error((e as Error).message ?? "Order failed");
    }
    setPlacing(false);
  };

  const ProductGrid = (
    <div className="flex flex-col h-full gap-3">
      {/* Search + filters */}
      <div className="flex gap-2 flex-wrap">
        <div className="relative flex-1 min-w-[180px]">
          <SearchIcon className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
          <Input
            placeholder="Brand, name, size, color…"
            className="pl-8"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <Select value={catFilter} onValueChange={v => setCatFilter(v ?? "all")}>
          <SelectTrigger className="w-32"><SelectValue placeholder="Category" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All</SelectItem>
            {categories.map(c => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={genderFilter} onValueChange={v => setGenderFilter(v ?? "all")}>
          <SelectTrigger className="w-28"><SelectValue placeholder="Gender" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All</SelectItem>
            <SelectItem value="men">Men</SelectItem>
            <SelectItem value="women">Women</SelectItem>
            <SelectItem value="kids">Kids</SelectItem>
            <SelectItem value="unisex">Unisex</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Product cards */}
      <ScrollArea className="flex-1">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-40 text-muted-foreground gap-2">
            <ShoppingCartIcon className="size-8 opacity-30" />
            <p className="text-sm">No products match your filter</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3 pr-2">
            {filtered.map(p => {
              const inCart = cart.find(i => i.product.id === p.id);
              const almostOut = p.stock <= p.minStock;
              return (
                <button
                  key={p.id}
                  onClick={() => addToCart(p)}
                  className={`
                    text-left rounded-xl border p-3 transition-all hover:shadow-md active:scale-95
                    ${inCart
                      ? "border-primary bg-primary/5 ring-1 ring-primary/20"
                      : "border-border bg-card hover:border-primary/40"
                    }
                  `}
                >
                  <div className="flex items-start justify-between gap-1 mb-1">
                    <span className="text-xs text-muted-foreground font-medium">{p.brand}</span>
                    {inCart && (
                      <span className="text-xs font-bold text-primary bg-primary/10 rounded-full px-1.5 py-0.5 leading-none">
                        ×{inCart.quantity}
                      </span>
                    )}
                  </div>
                  <p className="font-semibold text-sm leading-tight mb-1">{p.name}</p>
                  <div className="flex flex-wrap gap-1 mb-2">
                    <span className="text-xs bg-secondary text-secondary-foreground rounded px-1.5 py-0.5 font-mono">Sz {p.size}</span>
                    <span className="text-xs text-muted-foreground truncate">{p.color}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-base">${p.price.toFixed(2)}</span>
                    <span className={`text-xs ${almostOut ? "text-orange-500" : "text-green-600"}`}>
                      {p.stock} {p.unit}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </ScrollArea>
    </div>
  );

  const CartPanel = (
    <div className="flex flex-col h-full gap-3">
      {/* Cart header */}
      <div className="flex items-center justify-between">
        <h2 className="font-bold text-base flex items-center gap-2">
          <ShoppingCartIcon className="size-4" />
          Current Sale
          {cartCount > 0 && (
            <Badge className="rounded-full text-xs">{cartCount}</Badge>
          )}
        </h2>
        {cart.length > 0 && (
          <Button size="sm" variant="ghost" className="text-destructive hover:text-destructive h-7 px-2" onClick={() => setCart([])}>
            <Trash2Icon className="size-3.5 mr-1" /> Clear
          </Button>
        )}
      </div>

      {/* Cart items */}
      <ScrollArea className="flex-1 min-h-0">
        {cart.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-32 text-muted-foreground gap-2">
            <ShoppingCartIcon className="size-8 opacity-20" />
            <p className="text-sm">Tap a product to add it</p>
          </div>
        ) : (
          <div className="space-y-2 pr-1">
            {cart.map(item => (
              <div key={item.product.id} className="rounded-lg border bg-card p-3">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="min-w-0">
                    <p className="font-semibold text-sm leading-tight truncate">
                      {item.product.brand} {item.product.name}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Sz {item.product.size} · {item.product.color}
                    </p>
                  </div>
                  <button onClick={() => removeFromCart(item.product.id)} className="text-muted-foreground hover:text-destructive shrink-0 p-0.5">
                    <XIcon className="size-3.5" />
                  </button>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setQty(item.product.id, -1)}
                      className="size-6 rounded-md border flex items-center justify-center hover:bg-muted transition-colors"
                    >
                      <MinusIcon className="size-3" />
                    </button>
                    <span className="w-8 text-center font-bold text-sm">{item.quantity}</span>
                    <button
                      onClick={() => setQty(item.product.id, +1)}
                      disabled={item.quantity >= item.product.stock}
                      className="size-6 rounded-md border flex items-center justify-center hover:bg-muted transition-colors disabled:opacity-40"
                    >
                      <PlusIcon className="size-3" />
                    </button>
                    <span className="text-xs text-muted-foreground ml-1">× ${item.product.price.toFixed(2)}</span>
                  </div>
                  <span className="font-bold text-sm">${(item.product.price * item.quantity).toFixed(2)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </ScrollArea>

      {/* Totals + checkout */}
      {cart.length > 0 && (
        <div className="space-y-3 border-t pt-3">
          <div className="space-y-1 text-sm">
            <div className="flex justify-between text-muted-foreground">
              <span>Subtotal</span><span>${cartTotals.sub.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Tax (10%)</span><span>${cartTotals.tax.toFixed(2)}</span>
            </div>
            <Separator />
            <div className="flex justify-between font-bold text-lg pt-1">
              <span>Total</span><span>${cartTotals.total.toFixed(2)}</span>
            </div>
          </div>
          <Button
            className="w-full h-12 text-base font-bold bg-green-600 hover:bg-green-700 text-white"
            disabled={placing}
            onClick={completeSale}
          >
            {placing ? (
              <span className="flex items-center gap-2">Processing…</span>
            ) : (
              <span className="flex items-center gap-2">
                <CheckCircleIcon className="size-5" /> Complete Sale
              </span>
            )}
          </Button>
        </div>
      )}
    </div>
  );

  return (
    <div className="flex flex-col h-[calc(100vh-56px)] p-4 gap-4 md:p-5">
      {/* ── Desktop: side-by-side layout ── */}
      <div className="hidden lg:grid lg:grid-cols-[1fr_360px] gap-4 flex-1 min-h-0">
        <div className="flex flex-col min-h-0 gap-1">
          <h1 className="text-xl font-bold shrink-0">Point of Sale</h1>
          <div className="flex-1 min-h-0">
            {ProductGrid}
          </div>
        </div>
        <div className="rounded-xl border bg-card shadow-sm p-4 flex flex-col min-h-0">
          {CartPanel}
        </div>
      </div>

      {/* ── Mobile: tabbed layout ── */}
      <div className="lg:hidden flex-1 min-h-0">
        <Tabs value={tab} onValueChange={setTab} className="flex flex-col h-full">
          <TabsList className="grid grid-cols-2 w-full shrink-0">
            <TabsTrigger value="products">Products</TabsTrigger>
            <TabsTrigger value="cart" className="relative">
              Cart
              {cartCount > 0 && (
                <span className="ml-1.5 bg-primary text-primary-foreground text-xs rounded-full px-1.5 py-0.5 leading-none">
                  {cartCount}
                </span>
              )}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="products" className="flex-1 min-h-0 mt-3">
            {ProductGrid}
          </TabsContent>
          <TabsContent value="cart" className="flex-1 min-h-0 mt-3">
            <div className="rounded-xl border bg-card p-4 flex flex-col h-full">
              {CartPanel}
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* ── Order history ── */}
      <div className="shrink-0 rounded-xl border bg-card shadow-sm overflow-hidden hidden lg:block">
        <div className="flex items-center gap-2 px-4 py-2.5 border-b">
          <ReceiptIcon className="size-4 text-muted-foreground" />
          <span className="font-semibold text-sm">Recent Orders</span>
          <span className="text-xs text-muted-foreground ml-auto">{orders.length} total</span>
        </div>
        <ScrollArea className="h-40">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="py-2">Order #</TableHead>
                <TableHead className="py-2">Items</TableHead>
                <TableHead className="py-2 text-right">Total</TableHead>
                <TableHead className="py-2">Date</TableHead>
                <TableHead className="py-2">Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-6">No orders yet</TableCell>
                </TableRow>
              ) : orders.slice(0, 20).map(o => (
                <TableRow key={o.id}>
                  <TableCell className="font-mono text-xs font-medium py-2">{o.orderNumber}</TableCell>
                  <TableCell className="text-xs py-2">{o.items.length} item{o.items.length !== 1 ? "s" : ""}</TableCell>
                  <TableCell className="text-right font-mono text-xs font-bold py-2">${o.total.toFixed(2)}</TableCell>
                  <TableCell className="text-xs text-muted-foreground py-2">
                    {new Date(o.createdAt).toLocaleDateString()} {new Date(o.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </TableCell>
                  <TableCell className="py-2">
                    <Badge variant="outline" className="text-xs border-green-400 text-green-600">{o.status}</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </ScrollArea>
      </div>
    </div>
  );
}
