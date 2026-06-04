"use client";

import { useState, useEffect } from "react";
import { ArrowUpIcon, ArrowDownIcon, RefreshCwIcon, ArrowLeftRightIcon } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Transaction } from "@/lib/types";
import { getTransactions } from "@/lib/api";

const TYPE_CONFIG: Record<Transaction["type"], { label: string; icon: React.ReactNode; cls: string }> = {
  in:         { label: "Stock In",     icon: <ArrowUpIcon   className="size-3" />, cls: "border-green-400 text-green-600" },
  out:        { label: "Stock Out",    icon: <ArrowDownIcon className="size-3" />, cls: "border-red-400 text-red-600"     },
  adjustment: { label: "Adjustment",   icon: <RefreshCwIcon className="size-3" />, cls: "border-blue-400 text-blue-600"   },
  sale:       { label: "Sale",         icon: <ArrowDownIcon className="size-3" />, cls: "border-orange-400 text-orange-600"},
};

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const load = () => {
    setLoading(true);
    getTransactions(undefined, 200)
      .then(setTransactions)
      .catch(() => toast.error("Failed to load transactions"))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const filtered = transactions.filter(t =>
    !search ||
    t.productName.toLowerCase().includes(search.toLowerCase()) ||
    t.productSku.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="flex flex-col gap-4 p-4 md:p-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold">Transactions</h1>
        <div className="flex gap-2">
          <Input
            placeholder="Search product…"
            className="w-48"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <Button variant="outline" size="icon" onClick={load} title="Refresh">
            <RefreshCwIcon className="size-4" />
          </Button>
        </div>
      </div>

      <div className="rounded-xl border bg-card shadow-xs overflow-hidden">
        <ScrollArea className="h-[calc(100vh-210px)]">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Product</TableHead>
                <TableHead>SKU</TableHead>
                <TableHead>Type</TableHead>
                <TableHead className="text-right">Qty</TableHead>
                <TableHead>Note</TableHead>
                <TableHead>Date & Time</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={6} className="h-32 text-center text-muted-foreground animate-pulse">Loading…</TableCell></TableRow>
              ) : filtered.length === 0 ? (
                <TableRow><TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                  <ArrowLeftRightIcon className="size-8 mx-auto mb-2 opacity-30" />
                  No transactions found
                </TableCell></TableRow>
              ) : filtered.map(t => {
                const cfg = TYPE_CONFIG[t.type];
                const isIn = t.type === "in";
                return (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium text-sm">{t.productName}</TableCell>
                    <TableCell className="text-xs text-muted-foreground font-mono">{t.productSku}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className={`gap-1 text-xs ${cfg.cls}`}>
                        {cfg.icon} {cfg.label}
                      </Badge>
                    </TableCell>
                    <TableCell className={`text-right font-mono font-semibold ${isIn ? "text-green-600" : "text-red-600"}`}>
                      {isIn ? "+" : "−"}{t.quantity}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground max-w-[160px] truncate">{t.note || "—"}</TableCell>
                    <TableCell className="text-xs text-muted-foreground whitespace-nowrap">
                      {new Date(t.timestamp).toLocaleString()}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </ScrollArea>
        <div className="border-t px-4 py-2 text-xs text-muted-foreground">
          {filtered.length} transactions
        </div>
      </div>
    </div>
  );
}
