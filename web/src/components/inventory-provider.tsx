"use client";

import React, { createContext, useContext } from "react";
import { useInventory, LowStockAlert } from "@/hooks/useInventory";
import { Product, Stats } from "@/lib/types";

interface InventoryCtx {
  products: Product[];
  setProducts: React.Dispatch<React.SetStateAction<Product[]>>;
  stats: Stats | null;
  alerts: LowStockAlert[];
  dismissAlert: (id: string) => void;
  connected: boolean;
  loading: boolean;
  refreshProducts: () => void;
  refreshStats: () => void;
}

const Ctx = createContext<InventoryCtx | null>(null);

export function InventoryProvider({ children }: { children: React.ReactNode }) {
  const value = useInventory();
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useInventoryCtx() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useInventoryCtx must be inside InventoryProvider");
  return ctx;
}
