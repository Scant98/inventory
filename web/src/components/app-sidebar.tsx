"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboardIcon, PackageIcon, TagIcon,
  ShoppingCartIcon, ArrowLeftRightIcon, Settings2Icon,
  StoreIcon, WifiIcon, WifiOffIcon, BarChart3Icon,
} from "lucide-react";
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarHeader,
  SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarGroup, SidebarGroupLabel,
} from "@/components/ui/sidebar";
import { Badge } from "@/components/ui/badge";

const navItems = [
  { title: "Dashboard",    href: "/",             icon: LayoutDashboardIcon },
  { title: "Products",     href: "/products",     icon: PackageIcon         },
  { title: "Categories",   href: "/categories",   icon: TagIcon             },
  { title: "Orders",       href: "/orders",       icon: ShoppingCartIcon    },
  { title: "Transactions", href: "/transactions", icon: ArrowLeftRightIcon  },
  { title: "Reports",      href: "/reports",      icon: BarChart3Icon       },
];

interface Props extends React.ComponentProps<typeof Sidebar> {
  lowStockCount?: number;
  connected?: boolean;
}

export function AppSidebar({ lowStockCount = 0, connected = false, ...props }: Props) {
  const pathname = usePathname();

  return (
    <Sidebar collapsible="offcanvas" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              className="data-[slot=sidebar-menu-button]:p-1.5!"
              render={<Link href="/" />}
            >
              <StoreIcon className="size-5 text-primary" />
              <span className="text-base font-bold">Inventory Pro</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Menu</SidebarGroupLabel>
          <SidebarMenu>
            {navItems.map(({ title, href, icon: Icon }) => (
              <SidebarMenuItem key={href}>
                <SidebarMenuButton
                  render={<Link href={href} />}
                  isActive={pathname === href}
                >
                  <Icon className="size-4" />
                  <span className="flex-1">{title}</span>
                  {title === "Products" && lowStockCount > 0 && (
                    <Badge variant="destructive" className="text-[10px] px-1.5 py-0 ml-auto">
                      {lowStockCount}
                    </Badge>
                  )}
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}
          </SidebarMenu>
        </SidebarGroup>

        <SidebarGroup className="mt-auto">
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton render={<Link href="/settings" />}>
                <Settings2Icon className="size-4" />
                Settings
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter>
        <div className="flex items-center gap-2 px-3 py-2 text-xs text-muted-foreground">
          {connected
            ? <><WifiIcon className="size-3 text-green-500" /> Live</>
            : <><WifiOffIcon className="size-3 text-red-400" /> Reconnecting…</>}
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}
