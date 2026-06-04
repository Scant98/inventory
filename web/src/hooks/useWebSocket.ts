"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { ConnectionState, ConnectedClient, WsMessage } from "../types";

const CLIENT_ID = uuidv4();
const MAX_RETRIES = 5;
const BASE_DELAY_MS = 1_000;

export function useWebSocket(url: string) {
  const [connectionState, setConnectionState] = useState<ConnectionState>("DISCONNECTED");
  const [messages, setMessages] = useState<WsMessage[]>([]);
  const [connectedClients, setConnectedClients] = useState<ConnectedClient[]>([]);

  const wsRef = useRef<WebSocket | null>(null);
  const retryCount = useRef(0);
  const retryTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mounted = useRef(true);

  const connect = useCallback(() => {
    if (!url || !mounted.current) return;
    setConnectionState("CONNECTING");

    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      if (!mounted.current) { ws.close(); return; }
      retryCount.current = 0;
      setConnectionState("CONNECTED");
      ws.send(JSON.stringify({
        type: "HANDSHAKE",
        clientId: CLIENT_ID,
        clientType: "web",
        timestamp: Date.now(),
      }));
    };

    ws.onmessage = (event: MessageEvent<string>) => {
      try {
        const msg = JSON.parse(event.data) as WsMessage;
        if (msg.type === "CLIENT_LIST" && msg.payload) {
          setConnectedClients(JSON.parse(msg.payload) as ConnectedClient[]);
        } else if (msg.type === "MESSAGE" || msg.type === "BROADCAST") {
          setMessages((prev) => [...prev.slice(-199), msg]);
        }
      } catch {/* ignore malformed frames */}
    };

    ws.onclose = () => {
      if (!mounted.current) return;
      setConnectionState("DISCONNECTED");
      if (retryCount.current < MAX_RETRIES) {
        const delay = BASE_DELAY_MS * Math.pow(2, retryCount.current);
        retryCount.current++;
        retryTimer.current = setTimeout(connect, delay);
      }
    };

    ws.onerror = () => {
      if (mounted.current) setConnectionState("ERROR");
    };
  }, [url]);

  useEffect(() => {
    mounted.current = true;
    connect();
    return () => {
      mounted.current = false;
      if (retryTimer.current) clearTimeout(retryTimer.current);
      wsRef.current?.close(1000, "component unmounted");
    };
  }, [connect]);

  const sendMessage = useCallback((text: string) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: "MESSAGE",
        clientId: CLIENT_ID,
        clientType: "web",
        payload: text,
        timestamp: Date.now(),
      }));
      // Optimistically add to local feed
      setMessages((prev) => [
        ...prev.slice(-199),
        {
          type: "MESSAGE",
          clientId: CLIENT_ID,
          clientType: "web",
          payload: text,
          timestamp: Date.now(),
        },
      ]);
    }
  }, []);

  return { connectionState, messages, connectedClients, sendMessage, clientId: CLIENT_ID };
}
