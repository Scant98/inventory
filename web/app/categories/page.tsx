"use client";

import { useState, useMemo } from "react";
import { PlusIcon, PencilIcon, Trash2Icon, TagIcon } from "lucide-react";
import { toast } from "sonner";
import { useInventoryCtx } from "@/components/inventory-provider";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Category } from "@/lib/types";
import { createCategory, updateCategory, deleteCategory, getCategories } from "@/lib/api";
import { useEffect } from "react";

const COLORS = [
  { value: "blue",   label: "Blue",   cls: "bg-blue-500"   },
  { value: "purple", label: "Purple", cls: "bg-purple-500" },
  { value: "green",  label: "Green",  cls: "bg-green-500"  },
  { value: "orange", label: "Orange", cls: "bg-orange-500" },
  { value: "red",    label: "Red",    cls: "bg-red-500"    },
  { value: "pink",   label: "Pink",   cls: "bg-pink-500"   },
  { value: "gray",   label: "Gray",   cls: "bg-gray-500"   },
];

const COLOR_MAP: Record<string, string> = Object.fromEntries(COLORS.map(c => [c.value, c.cls]));

export default function CategoriesPage() {
  const { products } = useInventoryCtx();
  const [categories, setCategories] = useState<Category[]>([]);
  const [editing, setEditing] = useState<Category | null>(null);
  const [showAdd, setShowAdd] = useState(false);

  useEffect(() => {
    getCategories().then(setCategories).catch(() => toast.error("Failed to load categories"));
  }, []);

  const productCounts = useMemo(() => {
    const map = new Map<string, number>();
    products.forEach(p => map.set(p.categoryId, (map.get(p.categoryId) ?? 0) + 1));
    return map;
  }, [products]);

  const refresh = () => getCategories().then(setCategories);

  const handleDelete = async (c: Category) => {
    const count = productCounts.get(c.id) ?? 0;
    if (count > 0) { toast.error(`Cannot delete — ${count} products use this category`); return; }
    if (!confirm(`Delete "${c.name}"?`)) return;
    try { await deleteCategory(c.id); refresh(); toast.success("Deleted"); }
    catch { toast.error("Failed to delete"); }
  };

  return (
    <div className="flex flex-col gap-4 p-4 md:p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Categories</h1>
        <Button onClick={() => setShowAdd(true)}><PlusIcon className="size-4 mr-1" /> Add Category</Button>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {categories.map(c => {
          const count = productCounts.get(c.id) ?? 0;
          return (
            <Card key={c.id} className="group shadow-xs">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`size-3 rounded-full ${COLOR_MAP[c.color] ?? "bg-gray-400"}`} />
                    <CardTitle className="text-base">{c.name}</CardTitle>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button size="icon" variant="ghost" className="size-7" onClick={() => setEditing(c)}>
                      <PencilIcon className="size-3" />
                    </Button>
                    <Button size="icon" variant="ghost" className="size-7 text-destructive hover:text-destructive" onClick={() => handleDelete(c)}>
                      <Trash2Icon className="size-3" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <Badge variant="outline" className="gap-1">
                  <TagIcon className="size-3" />
                  {count} product{count !== 1 ? "s" : ""}
                </Badge>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <CategoryDialog
        open={showAdd} onClose={() => setShowAdd(false)}
        onSave={async (name, color) => {
          await createCategory({ name, color });
          refresh(); toast.success("Category created"); setShowAdd(false);
        }}
      />
      {editing && (
        <CategoryDialog
          open initial={editing} onClose={() => setEditing(null)}
          onSave={async (name, color) => {
            await updateCategory(editing.id, { name, color });
            refresh(); toast.success("Category updated"); setEditing(null);
          }}
        />
      )}
    </div>
  );
}

function CategoryDialog({ open, onClose, initial, onSave }: {
  open: boolean; onClose: () => void;
  initial?: Category;
  onSave: (name: string, color: string) => Promise<void>;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [color, setColor] = useState(initial?.color ?? "blue");
  const [saving, setSaving] = useState(false);

  return (
    <Dialog open={open} onOpenChange={v => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{initial ? "Edit Category" : "New Category"}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1">
            <Label>Name</Label>
            <Input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Electronics" />
          </div>
          <div className="space-y-2">
            <Label>Colour</Label>
            <div className="flex gap-2 flex-wrap">
              {COLORS.map(c => (
                <button
                  key={c.value}
                  onClick={() => setColor(c.value)}
                  className={`size-7 rounded-full ${c.cls} ring-offset-2 transition ${color === c.value ? "ring-2 ring-primary" : "ring-0"}`}
                  title={c.label}
                />
              ))}
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button disabled={saving || !name} onClick={async () => {
            setSaving(true);
            try { await onSave(name, color); } catch { toast.error("Save failed"); }
            setSaving(false);
          }}>
            {saving ? "Saving…" : "Save"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
