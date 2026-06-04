"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { Product, Stats } from "@/lib/types";
import { getProducts, getStats } from "@/lib/api";

const CLIENT_ID = uuidv4();
const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080";
const MAX_RETRIES = 5;

export interface LowStockAlert {
  productId: string;
  productName: string;
  sku: string;
  stock: number;
  minStock: number;
  timestamp: number;
}

export function useInventory() {
  const [products, setProducts] = useState<Product[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);

  const wsRef  = useRef<WebSocket | null>(null);
  const retry  = useRef(0);
  const timer  = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mounted = useRef(true);

  const refreshStats = useCallback(() =>
    getStats().then(s => { if (mounted.current) setStats(s); }).catch(() => {}), []);

  const refreshProducts = useCallback(() =>
    getProducts().then(p => { if (mounted.current) setProducts(p); }).catch(() => {}), []);

  // Initial load
  useEffect(() => {
    Promise.all([refreshProducts(), refreshStats()]).finally(() => {
      if (mounted.current) setLoading(false);
    });
  }, [refreshProducts, refreshStats]);

  const connectWs = useCallback(() => {
    if (!mounted.current) return;
    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    ws.onopen = () => {
      retry.current = 0;
      setConnected(true);
      ws.send(JSON.stringify({ type: "HANDSHAKE", clientId: CLIENT_ID, clientType: "web", timestamp: Date.now() }));
    };

    ws.onmessage = ({ data }: MessageEvent<string>) => {
      try {
        const msg = JSON.parse(data) as { type: string; payload?: string };
        switch (msg.type) {
          case "PRODUCT_CREATED":
          case "PRODUCT_UPDATED":
          case "STOCK_ADJUSTED": {
            // Refresh products + stats after any inventory change
            refreshProducts();
            refreshStats();
            break;
          }
          case "PRODUCT_DELETED": {
            const { id } = JSON.parse(msg.payload ?? "{}") as { id: string };
            setProducts(prev => prev.filter(p => p.id !== id));
            refreshStats();
            break;
          }
          case "LOW_STOCK_ALERT": {
            const alert = JSON.parse(msg.payload ?? "{}") as LowStockAlert;
            setAlerts(prev => {
              const without = prev.filter(a => a.productId !== alert.productId);
              return [{ ...alert, timestamp: Date.now() }, ...without].slice(0, 20);
            });
            break;
          }
          case "ORDER_CREATED": {
            refreshStats();
            refreshProducts();
            break;
          }
        }
      } catch {/**/}
    };

    ws.onclose = () => {
      setConnected(false);
      if (!mounted.current || retry.current >= MAX_RETRIES) return;
      timer.current = setTimeout(connectWs, 1000 * Math.pow(2, retry.current++));
    };

    ws.onerror = () => setConnected(false);
  }, [refreshProducts, refreshStats]);

  useEffect(() => {
    mounted.current = true;
    connectWs();
    return () => {
      mounted.current = false;
      if (timer.current) clearTimeout(timer.current);
      wsRef.current?.close();
    };
  }, [connectWs]);

  const dismissAlert = useCallback((productId: string) =>
    setAlerts(prev => prev.filter(a => a.productId !== productId)), []);

  return {
    products, setProducts,
    stats, refreshStats, refreshProducts,
    alerts, dismissAlert,
    connected, loading,
  };
}
