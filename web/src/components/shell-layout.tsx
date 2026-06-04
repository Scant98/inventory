"use client";

import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { InventoryProvider, useInventoryCtx } from "@/components/inventory-provider";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";
import { SiteHeader } from "@/components/site-header";

function Shell({ children }: { children: React.ReactNode }) {
  const { stats, connected, alerts } = useInventoryCtx();
  return (
    <SidebarProvider
      style={{ "--sidebar-width": "calc(var(--spacing) * 68)", "--header-height": "calc(var(--spacing) * 12)" } as React.CSSProperties}
    >
      <AppSidebar
        variant="inset"
        lowStockCount={(stats?.lowStockCount ?? 0) + (stats?.outOfStockCount ?? 0)}
        connected={connected}
      />
      <SidebarInset>
        <SiteHeader alerts={alerts} />
        <div className="flex flex-1 flex-col @container/main">
          {children}
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}

export function ShellLayout({ children }: { children: React.ReactNode }) {
  return (
    <TooltipProvider>
      <InventoryProvider>
        <Shell>{children}</Shell>
        <Toaster richColors position="top-right" />
      </InventoryProvider>
    </TooltipProvider>
  );
}
