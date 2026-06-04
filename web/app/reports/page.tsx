"use client";

import { useState, useEffect, useCallback } from "react";
import {
  TrendingUpIcon, TrendingDownIcon, PackageIcon, DollarSignIcon,
  ShoppingCartIcon, BarChart3Icon, RefreshCwIcon, AlertTriangleIcon,
  ArrowUpIcon, ArrowDownIcon, MinusIcon,
} from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import {
  BarChart, Bar, XAxis, YAxis, ResponsiveContainer,
  AreaChart, Area, PieChart, Pie, Cell,
} from "recharts";
import { Report } from "@/lib/types";
import { getReport } from "@/lib/api";

const COLOR_MAP: Record<string, string> = {
  blue: "#3b82f6", purple: "#a855f7", green: "#22c55e",
  orange: "#f97316", red: "#ef4444", pink: "#ec4899", gray: "#6b7280",
};
const CHART_COLORS = ["#6366f1", "#22c55e", "#f97316", "#ec4899", "#3b82f6", "#a855f7", "#14b8a6"];

function fmt(n: number) { return `$${n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
function fmtK(n: number) { return n >= 1000 ? `$${(n / 1000).toFixed(1)}k` : fmt(n); }

export default function ReportsPage() {
  const [report, setReport] = useState<Report | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState("sales");

  const load = useCallback(() => {
    setLoading(true);
    getReport()
      .then(setReport)
      .catch(() => toast.error("Failed to load report"))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-muted-foreground animate-pulse">
        <BarChart3Icon className="size-8 mr-3 opacity-40" /> Generating report…
      </div>
    );
  }

  if (!report) return null;

  const { sales, inventory } = report;

  return (
    <div className="flex flex-col gap-5 p-4 md:p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Reports</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Generated {new Date(report.generatedAt).toLocaleString()}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={load} disabled={loading}>
          <RefreshCwIcon className="size-3.5 mr-1.5" /> Refresh
        </Button>
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="grid grid-cols-2 w-full max-w-sm">
          <TabsTrigger value="sales">Sales</TabsTrigger>
          <TabsTrigger value="inventory">Inventory</TabsTrigger>
        </TabsList>

        {/* ── SALES TAB ─────────────────────────────────────────────────────── */}
        <TabsContent value="sales" className="mt-5 space-y-5">

          {/* KPI cards */}
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <KpiCard
              title="Today" value={fmtK(sales.today.revenue)}
              sub={`${sales.today.orders} orders`} icon={<ShoppingCartIcon className="size-4" />}
              color="blue"
            />
            <KpiCard
              title="This Week" value={fmtK(sales.thisWeek.revenue)}
              sub={`${sales.thisWeek.orders} orders`} icon={<TrendingUpIcon className="size-4" />}
              color="green"
              badge={sales.revenueGrowth}
            />
            <KpiCard
              title="This Month" value={fmtK(sales.thisMonth.revenue)}
              sub={`${sales.thisMonth.orders} orders`} icon={<BarChart3Icon className="size-4" />}
              color="purple"
            />
            <KpiCard
              title="All Time" value={fmtK(sales.allTime.revenue)}
              sub={`${sales.allTime.orders} total orders`} icon={<DollarSignIcon className="size-4" />}
              color="orange"
            />
          </div>

          {/* Revenue trend — last 30 days */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Revenue — Last 30 Days</CardTitle>
              <CardDescription>
                Avg. order value: {fmt(sales.thisMonth.avgOrderValue)} · Units sold this month: {sales.thisMonth.unitsSold}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ChartContainer config={{ revenue: { label: "Revenue", color: "#6366f1" } }} className="h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={sales.last30Days} margin={{ left: -20, right: 4 }}>
                    <defs>
                      <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="date" tick={{ fontSize: 10 }} tickLine={false} axisLine={false}
                      tickFormatter={d => { const dt = new Date(d); return `${dt.getMonth()+1}/${dt.getDate()}`; }}
                      interval={4}
                    />
                    <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false}
                      tickFormatter={v => `$${v >= 1000 ? `${(v/1000).toFixed(0)}k` : v}`}
                    />
                    <ChartTooltip
                      content={<ChartTooltipContent />}
                      formatter={(v) => [`${fmt(Number(v ?? 0))}`, "Revenue"]}
                    />
                    <Area dataKey="revenue" type="monotone" stroke="#6366f1" strokeWidth={2} fill="url(#revenueGrad)" />
                  </AreaChart>
                </ResponsiveContainer>
              </ChartContainer>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {/* Top selling products */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">Top Products by Revenue</CardTitle>
                <CardDescription>All time</CardDescription>
              </CardHeader>
              <CardContent className="p-0">
                <ScrollArea className="h-72">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="pl-4">#</TableHead>
                        <TableHead>Product</TableHead>
                        <TableHead className="text-right">Units</TableHead>
                        <TableHead className="text-right pr-4">Revenue</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {sales.topProducts.length === 0 ? (
                        <TableRow><TableCell colSpan={4} className="text-center py-8 text-muted-foreground">No sales data yet</TableCell></TableRow>
                      ) : sales.topProducts.map((p, i) => (
                        <TableRow key={p.productId}>
                          <TableCell className="pl-4 text-muted-foreground text-xs w-6">{i + 1}</TableCell>
                          <TableCell>
                            <p className="font-medium text-sm leading-tight">{p.productName}</p>
                            <p className="text-xs text-muted-foreground">{p.productType} · {p.sku}</p>
                          </TableCell>
                          <TableCell className="text-right text-sm font-mono">{p.unitsSold}</TableCell>
                          <TableCell className="text-right font-bold text-sm pr-4">{fmt(p.revenue)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </ScrollArea>
              </CardContent>
            </Card>

            {/* Revenue by category */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">Revenue by Category</CardTitle>
                <CardDescription>All time share</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-4">
                <div className="flex justify-center">
                  <ChartContainer config={{}} className="h-44 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie data={sales.byCategory.filter(c => c.revenue > 0)} dataKey="revenue" nameKey="name"
                          innerRadius={45} outerRadius={72} paddingAngle={2}>
                          {sales.byCategory.map((c, i) => (
                            <Cell key={c.name} fill={COLOR_MAP[c.color] ?? CHART_COLORS[i % CHART_COLORS.length]} />
                          ))}
                        </Pie>
                        <ChartTooltip formatter={(v) => [fmt(Number(v ?? 0)), "Revenue"]} />
                      </PieChart>
                    </ResponsiveContainer>
                  </ChartContainer>
                </div>
                <div className="space-y-2">
                  {sales.byCategory.filter(c => c.revenue > 0).map((c, i) => (
                    <div key={c.name} className="flex items-center gap-2">
                      <span className="size-2.5 rounded-full shrink-0" style={{ background: COLOR_MAP[c.color] ?? CHART_COLORS[i % CHART_COLORS.length] }} />
                      <span className="text-sm flex-1">{c.name}</span>
                      <span className="text-xs text-muted-foreground">{c.orders} orders</span>
                      <span className="text-sm font-bold">{fmt(c.revenue)}</span>
                      <Badge variant="secondary" className="text-xs">{c.percentage}%</Badge>
                    </div>
                  ))}
                  {sales.byCategory.every(c => c.revenue === 0) && (
                    <p className="text-sm text-muted-foreground text-center py-4">No sales yet</p>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Period comparison table */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Period Comparison</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Period</TableHead>
                    <TableHead className="text-right">Revenue</TableHead>
                    <TableHead className="text-right">Orders</TableHead>
                    <TableHead className="text-right">Units Sold</TableHead>
                    <TableHead className="text-right">Avg Order</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {([
                    ["Today",      sales.today],
                    ["This Week",  sales.thisWeek],
                    ["This Month", sales.thisMonth],
                    ["All Time",   sales.allTime],
                  ] as [string, typeof sales.today][]).map(([label, p]) => (
                    <TableRow key={label}>
                      <TableCell className="font-medium">{label}</TableCell>
                      <TableCell className="text-right font-mono font-bold">{fmt(p.revenue)}</TableCell>
                      <TableCell className="text-right font-mono">{p.orders}</TableCell>
                      <TableCell className="text-right font-mono">{p.unitsSold}</TableCell>
                      <TableCell className="text-right font-mono text-muted-foreground">{p.orders ? fmt(p.avgOrderValue) : "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── INVENTORY TAB ──────────────────────────────────────────────────── */}
        <TabsContent value="inventory" className="mt-5 space-y-5">

          {/* KPI cards */}
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <KpiCard
              title="Total Products" value={inventory.summary.totalProducts.toString()}
              sub={`${inventory.summary.totalStock} units in stock`}
              icon={<PackageIcon className="size-4" />} color="blue"
            />
            <KpiCard
              title="Stock Cost Value" value={fmtK(inventory.summary.costValue)}
              sub="at purchase cost" icon={<DollarSignIcon className="size-4" />} color="orange"
            />
            <KpiCard
              title="Retail Value" value={fmtK(inventory.summary.retailValue)}
              sub="at selling price" icon={<TrendingUpIcon className="size-4" />} color="green"
            />
            <KpiCard
              title="Potential Profit" value={fmtK(inventory.summary.potentialProfit)}
              sub={`${inventory.summary.profitMarginPct}% margin`}
              icon={<BarChart3Icon className="size-4" />} color="purple"
            />
          </div>

          {/* Stock movement */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {[
              { label: "Total Received", value: inventory.stockMovement.totalReceived, icon: <ArrowUpIcon className="size-4" />, color: "text-green-600 bg-green-50" },
              { label: "Total Sold", value: inventory.stockMovement.totalSold, icon: <ShoppingCartIcon className="size-4" />, color: "text-blue-600 bg-blue-50" },
              { label: "Adjustments", value: inventory.stockMovement.totalAdjusted, icon: <MinusIcon className="size-4" />, color: "text-orange-600 bg-orange-50" },
            ].map(({ label, value, icon, color }) => (
              <Card key={label}>
                <CardContent className="flex items-center gap-4 p-4">
                  <div className={`p-2.5 rounded-lg ${color}`}>{icon}</div>
                  <div>
                    <p className="text-xs text-muted-foreground">{label}</p>
                    <p className="text-2xl font-bold">{value.toLocaleString()}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {/* By category */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">Inventory by Category</CardTitle>
                <CardDescription>Retail value breakdown</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                {inventory.byCategory.map((c, i) => (
                  <div key={c.name} className="space-y-1.5">
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <span className="size-2.5 rounded-full" style={{ background: COLOR_MAP[c.color] ?? CHART_COLORS[i % CHART_COLORS.length] }} />
                        <span className="font-medium">{c.name}</span>
                        <span className="text-muted-foreground text-xs">{c.products} products · {c.stock} units</span>
                      </div>
                      <span className="font-bold">{fmtK(c.retailValue)}</span>
                    </div>
                    <Progress
                      value={inventory.summary.retailValue > 0 ? (c.retailValue / inventory.summary.retailValue) * 100 : 0}
                      className="h-1.5"
                      style={{ "--progress-background": COLOR_MAP[c.color] ?? CHART_COLORS[i % CHART_COLORS.length] } as React.CSSProperties}
                    />
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>Cost: {fmtK(c.costValue)}</span>
                      <span>Margin: {c.costValue > 0 ? Math.round(((c.retailValue - c.costValue) / c.retailValue) * 100) : 0}%</span>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            {/* By brand */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-base">Inventory by Brand</CardTitle>
                <CardDescription>Sorted by retail value</CardDescription>
              </CardHeader>
              <CardContent className="p-0">
                <ScrollArea className="h-[280px]">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="pl-4">Brand</TableHead>
                        <TableHead className="text-right">SKUs</TableHead>
                        <TableHead className="text-right">Stock</TableHead>
                        <TableHead className="text-right pr-4">Retail Val.</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {inventory.byBrand.map((b, i) => (
                        <TableRow key={b.brand}>
                          <TableCell className="pl-4">
                            <div className="flex items-center gap-2">
                              <span className="size-2 rounded-full" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                              <span className="font-medium text-sm">{b.brand}</span>
                            </div>
                          </TableCell>
                          <TableCell className="text-right text-sm text-muted-foreground">{b.products}</TableCell>
                          <TableCell className="text-right text-sm font-mono">{b.totalStock}</TableCell>
                          <TableCell className="text-right font-bold text-sm pr-4">{fmtK(b.retailValue)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </ScrollArea>
              </CardContent>
            </Card>
          </div>

          {/* Brand bar chart */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Brand — Retail vs Cost Value</CardTitle>
            </CardHeader>
            <CardContent>
              <ChartContainer
                config={{
                  retailValue: { label: "Retail Value", color: "#6366f1" },
                  costValue:   { label: "Cost Value",   color: "#94a3b8" },
                }}
                className="h-52"
              >
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={inventory.byBrand.slice(0, 8)} margin={{ left: -16, right: 4 }}>
                    <XAxis dataKey="brand" tick={{ fontSize: 10 }} tickLine={false} axisLine={false} />
                    <YAxis tick={{ fontSize: 10 }} tickLine={false} axisLine={false}
                      tickFormatter={v => `$${v >= 1000 ? `${(v/1000).toFixed(0)}k` : v}`}
                    />
                    <ChartTooltip content={<ChartTooltipContent />} formatter={(v) => fmt(Number(v ?? 0))} />
                    <Bar dataKey="retailValue" fill="#6366f1" radius={[3, 3, 0, 0]} />
                    <Bar dataKey="costValue" fill="#94a3b8" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </ChartContainer>
            </CardContent>
          </Card>

          {/* Top value products */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Top 10 Products by Stock Value</CardTitle>
              <CardDescription>Highest retail value currently in stock</CardDescription>
            </CardHeader>
            <CardContent className="p-0">
              <ScrollArea className="h-72">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="pl-4">#</TableHead>
                      <TableHead>Product</TableHead>
                      <TableHead className="text-center">Size</TableHead>
                      <TableHead className="text-right">Stock</TableHead>
                      <TableHead className="text-right">Cost Val.</TableHead>
                      <TableHead className="text-right pr-4">Retail Val.</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {inventory.topValueProducts.map((p, i) => (
                      <TableRow key={p.id}>
                        <TableCell className="pl-4 text-muted-foreground text-xs">{i + 1}</TableCell>
                        <TableCell>
                          <p className="font-medium text-sm">{p.name}</p>
                          <p className="text-xs text-muted-foreground">{p.color} · {p.sku}</p>
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge variant="secondary" className="text-xs font-mono">{p.size || "—"}</Badge>
                        </TableCell>
                        <TableCell className="text-right font-mono text-sm">{p.stock} {p.unit}</TableCell>
                        <TableCell className="text-right text-sm text-muted-foreground">{fmtK(p.costValue)}</TableCell>
                        <TableCell className="text-right font-bold text-sm pr-4">{fmtK(p.retailValue)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </ScrollArea>
            </CardContent>
          </Card>

          {/* Low stock items */}
          {inventory.lowStock.length > 0 && (
            <Card className="border-orange-200">
              <CardHeader className="pb-2">
                <CardTitle className="text-base flex items-center gap-2">
                  <AlertTriangleIcon className="size-4 text-orange-500" />
                  Items Needing Restock ({inventory.lowStock.length})
                </CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="pl-4">Product</TableHead>
                      <TableHead className="text-center">Size</TableHead>
                      <TableHead className="text-center">Stock / Min</TableHead>
                      <TableHead className="text-right pr-4">Urgency</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {inventory.lowStock.map(p => {
                      const pct = p.minStock > 0 ? Math.round((p.stock / p.minStock) * 100) : 0;
                      return (
                        <TableRow key={p.id}>
                          <TableCell className="pl-4">
                            <p className="font-medium text-sm">{p.name}</p>
                            <p className="text-xs text-muted-foreground">{p.sku}</p>
                          </TableCell>
                          <TableCell className="text-center">
                            <Badge variant="secondary" className="text-xs font-mono">{p.size || "—"}</Badge>
                          </TableCell>
                          <TableCell className="text-center text-sm font-mono">
                            <span className="text-orange-600 font-bold">{p.stock}</span>
                            <span className="text-muted-foreground"> / {p.minStock} {p.unit}</span>
                          </TableCell>
                          <TableCell className="text-right pr-4">
                            <div className="flex items-center justify-end gap-2">
                              <Progress value={pct} className="w-16 h-1.5" />
                              <span className="text-xs text-orange-600 font-bold w-8">{pct}%</span>
                            </div>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ── KPI card ──────────────────────────────────────────────────────────────────

function KpiCard({
  title, value, sub, icon, color, badge,
}: {
  title: string; value: string; sub: string;
  icon: React.ReactNode; color: "blue" | "green" | "purple" | "orange";
  badge?: number;
}) {
  const bg: Record<string, string> = {
    blue:   "bg-blue-50 text-blue-600",
    green:  "bg-green-50 text-green-600",
    purple: "bg-purple-50 text-purple-600",
    orange: "bg-orange-50 text-orange-600",
  };
  return (
    <Card className="bg-gradient-to-br from-primary/5 to-card shadow-xs">
      <CardHeader className="pb-2 pt-4 px-4">
        <div className="flex items-center justify-between gap-1">
          <CardDescription className="text-xs font-medium">{title}</CardDescription>
          <div className={`rounded-md p-1.5 ${bg[color]}`}>{icon}</div>
        </div>
        <div className="flex items-end gap-2">
          <CardTitle className="text-2xl font-bold tabular-nums">{value}</CardTitle>
          {badge !== undefined && badge !== 0 && (
            <div className={`flex items-center gap-0.5 text-xs font-semibold pb-0.5 ${badge > 0 ? "text-green-600" : "text-red-500"}`}>
              {badge > 0 ? <ArrowUpIcon className="size-3" /> : <ArrowDownIcon className="size-3" />}
              {Math.abs(badge)}%
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent className="px-4 pb-4">
        <p className="text-xs text-muted-foreground">{sub}</p>
      </CardContent>
    </Card>
  );
}
