"use client";

import { BellIcon, AlertTriangleIcon } from "lucide-react";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { LowStockAlert } from "@/hooks/useInventory";

interface Props {
  alerts?: LowStockAlert[];
}

export function SiteHeader({ alerts = [] }: Props) {
  return (
    <header className="sticky top-0 z-50 flex h-(--header-height) shrink-0 items-center gap-2 border-b bg-background/95 backdrop-blur px-4 lg:px-6">
      <SidebarTrigger className="-ml-1" />
      <Separator orientation="vertical" className="mx-1 h-4 data-vertical:self-auto" />
      <span className="font-semibold text-sm hidden sm:block">Inventory Pro</span>

      <div className="ml-auto flex items-center gap-2">
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <Button variant="ghost" size="icon" className="relative" />
            }
          >
            <BellIcon className="size-4" />
            {alerts.length > 0 && (
              <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-destructive text-[10px] text-destructive-foreground font-bold">
                {alerts.length > 9 ? "9+" : alerts.length}
              </span>
            )}
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <DropdownMenuLabel className="flex items-center gap-2">
              <AlertTriangleIcon className="size-4 text-orange-500" />
              Stock Alerts
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            {alerts.length === 0 ? (
              <DropdownMenuItem disabled>All stock levels normal</DropdownMenuItem>
            ) : (
              alerts.slice(0, 8).map(a => (
                <DropdownMenuItem key={a.productId} className="flex flex-col items-start gap-0.5 py-2">
                  <span className="font-medium text-sm">{a.productName}</span>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    {a.stock === 0 ? "Out of stock" : `${a.stock} left (min ${a.minStock})`}
                    <Badge variant={a.stock === 0 ? "destructive" : "outline"} className="text-[10px] px-1">
                      {a.sku}
                    </Badge>
                  </div>
                </DropdownMenuItem>
              ))
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
