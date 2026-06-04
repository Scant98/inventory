"use client";

import { PackageIcon, AlertTriangleIcon, DollarSignIcon, ShoppingCartIcon, TrendingUpIcon, XCircleIcon } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { useInventoryCtx } from "@/components/inventory-provider";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const COLORS: Record<string, string> = {
  blue: "#3b82f6", purple: "#a855f7", green: "#22c55e",
  orange: "#f97316", red: "#ef4444", pink: "#ec4899", gray: "#6b7280",
};

export default function DashboardPage() {
  const { stats, products, loading } = useInventoryCtx();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96 text-muted-foreground animate-pulse">
        Loading inventory…
      </div>
    );
  }

  const weekData = (stats?.weeklyRevenue ?? Array(7).fill(0)).map((v, i) => ({
    day: DAYS[(new Date().getDay() + i) % 7],
    revenue: v,
  }));

  const pieData = (stats?.stockByCategory ?? []).filter(c => c.value > 0);

  const lowStockProducts = products
    .filter(p => p.stock <= p.minStock && p.stock > 0)
    .slice(0, 8);
  const outOfStock = products.filter(p => p.stock === 0).slice(0, 5);

  return (
    <div className="flex flex-col gap-6 p-4 md:p-6">
      {/* KPI Cards */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-4">
        <StatCard
          title="Total Products" icon={<PackageIcon className="size-4" />}
          value={stats?.totalProducts ?? 0} sub="across all categories" color="blue"
        />
        <StatCard
          title="Inventory Value" icon={<DollarSignIcon className="size-4" />}
          value={`$${(stats?.totalValue ?? 0).toFixed(0)}`} sub="at cost price" color="green"
        />
        <StatCard
          title="Low / Out of Stock" icon={<AlertTriangleIcon className="size-4" />}
          value={`${stats?.lowStockCount ?? 0} / ${stats?.outOfStockCount ?? 0}`}
          sub="needs restocking" color="orange"
        />
        <StatCard
          title="Today's Revenue" icon={<ShoppingCartIcon className="size-4" />}
          value={`$${(stats?.todayRevenue ?? 0).toFixed(2)}`}
          sub={`${stats?.todayOrders ?? 0} orders today`} color="purple"
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {/* Weekly revenue bar chart */}
        <Card className="md:col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Weekly Revenue</CardTitle>
            <CardDescription>Last 7 days</CardDescription>
          </CardHeader>
          <CardContent>
            <ChartContainer config={{ revenue: { label: "Revenue", color: "#6366f1" } }} className="h-48">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={weekData} margin={{ left: -16 }}>
                  <XAxis dataKey="day" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
                  <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} tickFormatter={v => `$${v}`} />
                  <ChartTooltip content={<ChartTooltipContent />} />
                  <Bar dataKey="revenue" fill="#6366f1" radius={4} />
                </BarChart>
              </ResponsiveContainer>
            </ChartContainer>
          </CardContent>
        </Card>

        {/* Stock by category pie */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Stock by Category</CardTitle>
            <CardDescription>Units in stock</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col items-center gap-3">
            <ChartContainer config={{}} className="h-40 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={40} outerRadius={65} paddingAngle={2}>
                    {pieData.map((entry, i) => (
                      <Cell key={i} fill={COLORS[entry.color] ?? "#6b7280"} />
                    ))}
                  </Pie>
                  <ChartTooltip content={<ChartTooltipContent />} />
                </PieChart>
              </ResponsiveContainer>
            </ChartContainer>
            <div className="flex flex-wrap gap-x-3 gap-y-1 justify-center">
              {pieData.slice(0, 5).map(c => (
                <div key={c.name} className="flex items-center gap-1 text-xs">
                  <span className="size-2 rounded-full" style={{ background: COLORS[c.color] ?? "#6b7280" }} />
                  {c.name}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Low stock + out of stock */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <AlertTriangleIcon className="size-4 text-orange-500" />
              Low Stock
            </CardTitle>
            <CardDescription>Items below minimum threshold</CardDescription>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-52">
              {lowStockProducts.length === 0 ? (
                <p className="text-sm text-muted-foreground">All stock levels OK ✓</p>
              ) : (
                <div className="space-y-3">
                  {lowStockProducts.map(p => (
                    <div key={p.id} className="space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium truncate max-w-[180px]">{p.name}</span>
                        <span className="text-xs text-muted-foreground">{p.stock}/{p.minStock}</span>
                      </div>
                      <Progress value={(p.stock / p.minStock) * 100} className="h-1.5" />
                    </div>
                  ))}
                </div>
              )}
            </ScrollArea>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <XCircleIcon className="size-4 text-destructive" />
              Out of Stock
            </CardTitle>
            <CardDescription>Items that need immediate restocking</CardDescription>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-52">
              {outOfStock.length === 0 ? (
                <p className="text-sm text-muted-foreground">No items out of stock ✓</p>
              ) : (
                <div className="space-y-2">
                  {outOfStock.map(p => (
                    <div key={p.id} className="flex items-center justify-between rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2">
                      <div>
                        <p className="text-sm font-medium">{p.name}</p>
                        <p className="text-xs text-muted-foreground">{p.sku}</p>
                      </div>
                      <Badge variant="destructive" className="text-xs">Out</Badge>
                    </div>
                  ))}
                </div>
              )}
            </ScrollArea>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatCard({ title, icon, value, sub, color }: {
  title: string; icon: React.ReactNode; value: string | number; sub: string; color: string;
}) {
  const bg: Record<string, string> = {
    blue: "bg-blue-50 text-blue-600 dark:bg-blue-950/40",
    green: "bg-green-50 text-green-600 dark:bg-green-950/40",
    orange: "bg-orange-50 text-orange-600 dark:bg-orange-950/40",
    purple: "bg-purple-50 text-purple-600 dark:bg-purple-950/40",
  };
  return (
    <Card className="bg-gradient-to-br from-primary/5 to-card shadow-xs">
      <CardHeader className="pb-2 pt-4 px-4">
        <div className="flex items-center justify-between">
          <CardDescription className="text-xs font-medium">{title}</CardDescription>
          <div className={`rounded-md p-1.5 ${bg[color] ?? ""}`}>{icon}</div>
        </div>
        <CardTitle className="text-2xl font-bold tabular-nums">{value}</CardTitle>
      </CardHeader>
      <CardContent className="px-4 pb-4">
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <TrendingUpIcon className="size-3" />
          {sub}
        </div>
      </CardContent>
    </Card>
  );
}
