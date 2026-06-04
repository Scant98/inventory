"use client";

import { useState, useMemo, useEffect } from "react";
import {
  PlusIcon, SearchIcon, PencilIcon, Trash2Icon, PackageIcon,
  ArrowUpIcon, ArrowDownIcon, HistoryIcon,
} from "lucide-react";
import { toast } from "sonner";
import { useInventoryCtx } from "@/components/inventory-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Product, InventoryBatch, Gender } from "@/lib/types";
import { createProduct, updateProduct, deleteProduct, adjustStock, getInventoryBatches } from "@/lib/api";

const SHOE_TYPES    = ["Sneakers", "Running", "Boots", "Sandals", "Slip-ons", "High-tops", "Loafers"];
const CLOTHING_TYPES = ["T-Shirt", "Jeans", "Hoodie", "Jacket", "Dress", "Pants", "Shorts", "Skirt", "Shirt", "Coat"];
const GENDERS: { value: Gender; label: string }[] = [
  { value: "men",    label: "Men"    },
  { value: "women",  label: "Women"  },
  { value: "kids",   label: "Kids"   },
  { value: "unisex", label: "Unisex" },
];

const GENDER_BADGE: Record<Gender, string> = {
  men:    "border-blue-400 text-blue-600",
  women:  "border-pink-400 text-pink-600",
  kids:   "border-yellow-400 text-yellow-600",
  unisex: "border-gray-400 text-gray-600",
};

export default function ProductsPage() {
  const { products, loading, refreshProducts, refreshStats } = useInventoryCtx();
  const [search, setSearch] = useState("");
  const [catFilter, setCatFilter] = useState("all");
  const [genderFilter, setGenderFilter] = useState("all");
  const [stockFilter, setStockFilter] = useState("all");
  const [editProduct, setEditProduct] = useState<Product | null>(null);
  const [showAdd, setShowAdd] = useState(false);
  const [adjustProd, setAdjustProd] = useState<Product | null>(null);
  const [batchProd, setBatchProd] = useState<Product | null>(null);

  const categories = useMemo(() => {
    const map = new Map<string, string>();
    products.forEach(p => { if (p.category) map.set(p.categoryId, p.category.name); });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [products]);

  const filtered = useMemo(() => {
    let list = products;
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        p.sku.toLowerCase().includes(q) ||
        p.productType.toLowerCase().includes(q) ||
        p.size.toLowerCase().includes(q)
      );
    }
    if (catFilter !== "all") list = list.filter(p => p.categoryId === catFilter);
    if (genderFilter !== "all") list = list.filter(p => p.gender === genderFilter);
    if (stockFilter === "low") list = list.filter(p => p.isLowStock);
    if (stockFilter === "out") list = list.filter(p => p.isOutOfStock);
    return list;
  }, [products, search, catFilter, genderFilter, stockFilter]);

  const handleDelete = async (p: Product) => {
    if (!confirm(`Delete "${p.brand} ${p.name}" (${p.size})?`)) return;
    try { await deleteProduct(p.id); refreshProducts(); refreshStats(); toast.success("Product deleted"); }
    catch { toast.error("Failed to delete"); }
  };

  return (
    <div className="flex flex-col gap-4 p-4 md:p-6">
      {/* Toolbar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-xl font-bold">Products</h1>
        <div className="flex gap-2 flex-wrap">
          <div className="relative flex-1 min-w-[180px]">
            <SearchIcon className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
            <Input placeholder="Brand, name, size, SKU…" className="pl-8" value={search} onChange={e => setSearch(e.target.value)} />
          </div>
          <Select value={catFilter} onValueChange={v => setCatFilter(v ?? "all")}>
            <SelectTrigger className="w-32"><SelectValue placeholder="Category" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All categories</SelectItem>
              {categories.map(c => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={genderFilter} onValueChange={v => setGenderFilter(v ?? "all")}>
            <SelectTrigger className="w-28"><SelectValue placeholder="Gender" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All genders</SelectItem>
              {GENDERS.map(g => <SelectItem key={g.value} value={g.value}>{g.label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={stockFilter} onValueChange={v => setStockFilter(v ?? "all")}>
            <SelectTrigger className="w-32"><SelectValue placeholder="Stock" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All stock</SelectItem>
              <SelectItem value="low">Low stock</SelectItem>
              <SelectItem value="out">Out of stock</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={() => setShowAdd(true)}>
            <PlusIcon className="size-4 mr-1" /> Add Product
          </Button>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-xl border bg-card shadow-xs overflow-hidden">
        <ScrollArea className="h-[calc(100vh-270px)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Size</TableHead>
                <TableHead>Gender</TableHead>
                <TableHead>Color</TableHead>
                <TableHead className="text-right">Price</TableHead>
                <TableHead className="text-center">Stock</TableHead>
                <TableHead className="text-center">Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={9} className="h-32 text-center text-muted-foreground">Loading…</TableCell></TableRow>
              ) : filtered.length === 0 ? (
                <TableRow><TableCell colSpan={9} className="h-32 text-center text-muted-foreground">
                  <PackageIcon className="size-8 mx-auto mb-2 opacity-30" />
                  No products found
                </TableCell></TableRow>
              ) : filtered.map(p => (
                <TableRow key={p.id} className="group">
                  <TableCell>
                    <div className="font-semibold text-sm">{p.brand} {p.name}</div>
                    <div className="text-xs text-muted-foreground font-mono">{p.sku}</div>
                  </TableCell>
                  <TableCell className="text-sm">{p.productType || "—"}</TableCell>
                  <TableCell>
                    <Badge variant="secondary" className="text-xs font-mono">{p.size || "—"}</Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className={`text-xs capitalize ${GENDER_BADGE[p.gender] ?? ""}`}>
                      {p.gender}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{p.color || "—"}</TableCell>
                  <TableCell className="text-right font-mono text-sm">${p.price.toFixed(2)}</TableCell>
                  <TableCell className="text-center font-mono font-medium">{p.stock} <span className="text-xs text-muted-foreground">{p.unit}</span></TableCell>
                  <TableCell className="text-center">
                    {p.isOutOfStock
                      ? <Badge variant="destructive" className="text-xs">Out of stock</Badge>
                      : p.isLowStock
                      ? <Badge variant="outline" className="text-xs border-orange-400 text-orange-600">Low stock</Badge>
                      : <Badge variant="outline" className="text-xs border-green-400 text-green-600">In stock</Badge>}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <Button size="icon" variant="ghost" className="size-7" onClick={() => setBatchProd(p)} title="Batch history">
                        <HistoryIcon className="size-3.5" />
                      </Button>
                      <Button size="icon" variant="ghost" className="size-7" onClick={() => setAdjustProd(p)} title="Add / remove stock">
                        <ArrowUpIcon className="size-3.5" />
                      </Button>
                      <Button size="icon" variant="ghost" className="size-7" onClick={() => setEditProduct(p)} title="Edit">
                        <PencilIcon className="size-3.5" />
                      </Button>
                      <Button size="icon" variant="ghost" className="size-7 text-destructive hover:text-destructive" onClick={() => handleDelete(p)} title="Delete">
                        <Trash2Icon className="size-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </ScrollArea>
        <div className="border-t px-4 py-2 text-xs text-muted-foreground">
          {filtered.length} of {products.length} products
        </div>
      </div>

      {/* Dialogs */}
      <ProductFormDialog
        open={showAdd} onClose={() => setShowAdd(false)}
        categories={categories}
        onSave={async data => {
          await createProduct(data as Parameters<typeof createProduct>[0]);
          refreshProducts(); refreshStats();
          toast.success("Product created");
          setShowAdd(false);
        }}
      />
      {editProduct && (
        <ProductFormDialog
          open onClose={() => setEditProduct(null)}
          initial={editProduct} categories={categories}
          onSave={async data => {
            await updateProduct(editProduct.id, data);
            refreshProducts(); refreshStats();
            toast.success("Product updated");
            setEditProduct(null);
          }}
        />
      )}
      {adjustProd && (
        <AdjustStockDialog
          product={adjustProd} open
          onClose={() => setAdjustProd(null)}
          onSave={async (qty, type, note, supplier, costPerUnit) => {
            await adjustStock(adjustProd.id, qty, type, note, supplier, costPerUnit);
            refreshProducts(); refreshStats();
            toast.success(type === "in" ? "Stock added and batch recorded" : "Stock adjusted");
            setAdjustProd(null);
          }}
        />
      )}
      {batchProd && (
        <BatchHistoryDialog
          product={batchProd} open
          onClose={() => setBatchProd(null)}
        />
      )}
    </div>
  );
}

// ─── Product form dialog ─────────────────────────────────────────────────────

function ProductFormDialog({ open, onClose, initial, categories, onSave }: {
  open: boolean; onClose: () => void;
  initial?: Product;
  categories: { id: string; name: string }[];
  onSave: (data: Partial<Product>) => Promise<void>;
}) {
  const isShoeCategory = (catId: string) => {
    const cat = categories.find(c => c.id === catId);
    return cat?.name.toLowerCase().includes("shoe") ?? false;
  };

  const [form, setForm] = useState({
    brand:       initial?.brand        ?? "",
    name:        initial?.name         ?? "",
    sku:         initial?.sku          ?? "",
    categoryId:  initial?.categoryId   ?? "",
    productType: initial?.productType  ?? "",
    size:        initial?.size         ?? "",
    gender:      (initial?.gender      ?? "unisex") as Gender,
    color:       initial?.color        ?? "",
    price:       initial?.price        ?? 0,
    cost:        initial?.cost         ?? 0,
    stock:       initial?.stock        ?? 0,
    minStock:    initial?.minStock     ?? 5,
    unit:        initial?.unit         ?? "pcs",
    description: initial?.description  ?? "",
  });
  const [saving, setSaving] = useState(false);

  const set = (k: string, v: string | number) => setForm(f => ({ ...f, [k]: v }));
  const typeOptions = isShoeCategory(form.categoryId) ? SHOE_TYPES : CLOTHING_TYPES;

  return (
    <Dialog open={open} onOpenChange={v => !v && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{initial ? "Edit Product" : "Add Product"}</DialogTitle>
        </DialogHeader>
        <ScrollArea className="max-h-[70vh]">
          <div className="grid gap-3 py-2 px-1">
            {/* Row 1: Brand + Name */}
            <div className="grid grid-cols-2 gap-3">
              <Field label="Brand"><Input placeholder="Nike, Adidas, Zara…" value={form.brand} onChange={e => set("brand", e.target.value)} /></Field>
              <Field label="Model Name"><Input placeholder="Air Max 270, Slim Fit Jeans…" value={form.name} onChange={e => set("name", e.target.value)} /></Field>
            </div>
            {/* Row 2: Category + Type */}
            <div className="grid grid-cols-2 gap-3">
              <Field label="Category">
                <Select value={form.categoryId} onValueChange={v => set("categoryId", v ?? "")}>
                  <SelectTrigger><SelectValue placeholder="Select category" /></SelectTrigger>
                  <SelectContent>{categories.map(c => <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>)}</SelectContent>
                </Select>
              </Field>
              <Field label="Product Type">
                <Select value={form.productType} onValueChange={v => set("productType", v ?? "")}>
                  <SelectTrigger><SelectValue placeholder="Select type" /></SelectTrigger>
                  <SelectContent>
                    {typeOptions.map(t => <SelectItem key={t} value={t}>{t}</SelectItem>)}
                  </SelectContent>
                </Select>
              </Field>
            </div>
            {/* Row 3: Size + Gender + Color */}
            <div className="grid grid-cols-3 gap-3">
              <Field label="Size"><Input placeholder="42, M, XL, 28…" value={form.size} onChange={e => set("size", e.target.value)} /></Field>
              <Field label="Gender">
                <Select value={form.gender} onValueChange={v => set("gender", v ?? "unisex")}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>{GENDERS.map(g => <SelectItem key={g.value} value={g.value}>{g.label}</SelectItem>)}</SelectContent>
                </Select>
              </Field>
              <Field label="Color"><Input placeholder="Black, White/Red…" value={form.color} onChange={e => set("color", e.target.value)} /></Field>
            </div>
            {/* Row 4: SKU + Unit */}
            <div className="grid grid-cols-2 gap-3">
              <Field label="SKU"><Input placeholder="SH-NK-001" value={form.sku} onChange={e => set("sku", e.target.value)} /></Field>
              <Field label="Unit">
                <Select value={form.unit} onValueChange={v => set("unit", v ?? "pcs")}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="pcs">pcs</SelectItem>
                    <SelectItem value="pair">pair</SelectItem>
                  </SelectContent>
                </Select>
              </Field>
            </div>
            {/* Row 5: Prices */}
            <div className="grid grid-cols-2 gap-3">
              <Field label="Selling Price ($)"><Input type="number" min={0} step={0.01} value={form.price} onChange={e => set("price", parseFloat(e.target.value) || 0)} /></Field>
              <Field label="Cost Price ($)"><Input type="number" min={0} step={0.01} value={form.cost} onChange={e => set("cost", parseFloat(e.target.value) || 0)} /></Field>
            </div>
            {/* Row 6: Stock */}
            <div className="grid grid-cols-2 gap-3">
              <Field label={initial ? "Current Stock" : "Initial Stock"}>
                <Input type="number" min={0} value={form.stock} onChange={e => set("stock", parseInt(e.target.value) || 0)} />
              </Field>
              <Field label="Low-stock Alert Below">
                <Input type="number" min={0} value={form.minStock} onChange={e => set("minStock", parseInt(e.target.value) || 0)} />
              </Field>
            </div>
            <Field label="Description">
              <Textarea value={form.description} onChange={e => set("description", e.target.value)} rows={2} />
            </Field>
          </div>
        </ScrollArea>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button
            disabled={saving || !form.name || !form.sku || !form.brand}
            onClick={async () => {
              setSaving(true);
              try { await onSave(form); } catch { toast.error("Save failed"); }
              setSaving(false);
            }}
          >
            {saving ? "Saving…" : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─── Stock adjust dialog ─────────────────────────────────────────────────────

function AdjustStockDialog({ product, open, onClose, onSave }: {
  product: Product; open: boolean; onClose: () => void;
  onSave: (qty: number, type: "in" | "out" | "adjustment", note: string, supplier: string, costPerUnit: number) => Promise<void>;
}) {
  const [qty, setQty] = useState(1);
  const [type, setType] = useState<"in" | "out" | "adjustment">("in");
  const [note, setNote] = useState("");
  const [supplier, setSupplier] = useState("");
  const [costPerUnit, setCostPerUnit] = useState(product.cost);
  const [saving, setSaving] = useState(false);

  const isStockIn = type === "in";

  return (
    <Dialog open={open} onOpenChange={v => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Stock Adjustment</DialogTitle>
        </DialogHeader>
        <div className="space-y-3 py-2">
          <div className="rounded-lg border bg-muted/40 p-3 text-sm">
            <p className="font-semibold">{product.brand} {product.name}</p>
            <p className="text-muted-foreground text-xs">Size {product.size} · {product.color}</p>
            <p className="mt-1">Current stock: <strong>{product.stock} {product.unit}</strong></p>
          </div>
          <Field label="Type">
            <Select value={type} onValueChange={v => { if (v) setType(v as typeof type); }}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="in"><span className="flex items-center gap-2"><ArrowUpIcon className="size-3 text-green-500" />Stock In — add inventory</span></SelectItem>
                <SelectItem value="out"><span className="flex items-center gap-2"><ArrowDownIcon className="size-3 text-red-500" />Stock Out — remove inventory</span></SelectItem>
                <SelectItem value="adjustment">Adjustment — set correction</SelectItem>
              </SelectContent>
            </Select>
          </Field>
          <Field label="Quantity">
            <Input type="number" min={1} value={qty} onChange={e => setQty(parseInt(e.target.value) || 1)} />
          </Field>
          {isStockIn && (
            <>
              <Field label="Cost Per Unit ($) at This Batch">
                <Input type="number" min={0} step={0.01} value={costPerUnit} onChange={e => setCostPerUnit(parseFloat(e.target.value) || 0)} />
              </Field>
              <Field label="Supplier (optional)">
                <Input value={supplier} onChange={e => setSupplier(e.target.value)} placeholder="Supplier name or reference…" />
              </Field>
            </>
          )}
          <Field label="Note (optional)">
            <Input value={note} onChange={e => setNote(e.target.value)} placeholder="Reason or reference…" />
          </Field>
          {isStockIn && (
            <div className="rounded-md bg-muted/60 px-3 py-2 text-xs text-muted-foreground">
              This will create a batch record: <strong>{qty} {product.unit}</strong> @ <strong>${costPerUnit.toFixed(2)}</strong> each = <strong>${(qty * costPerUnit).toFixed(2)}</strong> total cost
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || qty < 1} onClick={async () => {
            setSaving(true);
            try { await onSave(qty, type, note, supplier, costPerUnit); } catch { toast.error("Failed"); }
            setSaving(false);
          }}>
            {saving ? "Saving…" : isStockIn ? "Add Stock" : "Apply"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─── Batch history dialog ─────────────────────────────────────────────────────

function BatchHistoryDialog({ product, open, onClose }: {
  product: Product; open: boolean; onClose: () => void;
}) {
  const [batches, setBatches] = useState<InventoryBatch[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    getInventoryBatches(product.id)
      .then(setBatches)
      .catch(() => toast.error("Failed to load batch history"))
      .finally(() => setLoading(false));
  }, [open, product.id]);

  const totalReceived = batches.reduce((s, b) => s + b.quantity, 0);
  const totalCost     = batches.reduce((s, b) => s + b.totalCost, 0);

  return (
    <Dialog open={open} onOpenChange={v => !v && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <HistoryIcon className="size-4" />
            Inventory Batch History
          </DialogTitle>
        </DialogHeader>
        <div className="rounded-lg border bg-muted/40 p-3 text-sm mb-2">
          <p className="font-semibold">{product.brand} {product.name}</p>
          <p className="text-xs text-muted-foreground">Size {product.size} · {product.color} · SKU {product.sku}</p>
          <div className="flex gap-6 mt-2 text-xs">
            <span>Current stock: <strong>{product.stock} {product.unit}</strong></span>
            <span>Total ever received: <strong>{totalReceived} {product.unit}</strong></span>
            <span>Total cost received: <strong>${totalCost.toFixed(2)}</strong></span>
          </div>
        </div>
        <ScrollArea className="h-64">
          {loading ? (
            <div className="h-32 flex items-center justify-center text-muted-foreground text-sm animate-pulse">Loading…</div>
          ) : batches.length === 0 ? (
            <div className="h-32 flex items-center justify-center text-muted-foreground text-sm">No batch records found</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date Received</TableHead>
                  <TableHead className="text-right">Qty</TableHead>
                  <TableHead className="text-right">Cost/Unit</TableHead>
                  <TableHead className="text-right">Total Cost</TableHead>
                  <TableHead>Supplier</TableHead>
                  <TableHead>Note</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {batches.map(b => (
                  <TableRow key={b.id}>
                    <TableCell className="text-xs whitespace-nowrap">
                      {new Date(b.addedAt).toLocaleDateString()} {new Date(b.addedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    </TableCell>
                    <TableCell className="text-right font-mono font-semibold text-green-600">+{b.quantity}</TableCell>
                    <TableCell className="text-right font-mono text-sm">${b.costPerUnit.toFixed(2)}</TableCell>
                    <TableCell className="text-right font-mono text-sm">${b.totalCost.toFixed(2)}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">{b.supplier || "—"}</TableCell>
                    <TableCell className="text-xs text-muted-foreground max-w-[140px] truncate">{b.note || "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </ScrollArea>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Close</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <Label className="text-xs font-medium">{label}</Label>
      {children}
    </div>
  );
}
