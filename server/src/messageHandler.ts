import WebSocket from "ws";
import { WsClientType, WsMessage, WsMessageType, ExtWebSocket } from "./types";
import * as ClientManager from "./clientManager";
import { isRateLimited } from "./rateLimiter";

const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || "")
  .split(",").map(o => o.trim()).filter(Boolean);

export function validateOrigin(origin: string | undefined): boolean {
  if (ALLOWED_ORIGINS.length === 0) return true;
  if (!origin) return false;
  return ALLOWED_ORIGINS.includes(origin) || ALLOWED_ORIGINS.includes("*");
}

function sanitize(input: string): string {
  return input
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#x27;")
    .slice(0, 4_096);
}

export function handleMessage(ws: WebSocket, raw: string): void {
  let msg: WsMessage;
  try { msg = JSON.parse(raw) as WsMessage; }
  catch { ws.send(serverFrame("ERROR", "Invalid JSON")); return; }

  if (!msg.type || !msg.clientId) {
    ws.send(serverFrame("ERROR", "Missing required fields")); return;
  }
  if (msg.type !== "HANDSHAKE" && isRateLimited(msg.clientId)) {
    ws.send(serverFrame("ERROR", "Rate limit exceeded")); return;
  }
  if (msg.payload) msg.payload = sanitize(msg.payload);

  log(`[${msg.type}] ${msg.clientType}:${msg.clientId.slice(0, 8)}`);

  switch (msg.type) {
    case "HANDSHAKE":
      ClientManager.addClient(msg.clientId, msg.clientType as WsClientType, ws);
      ws.send(JSON.stringify({
        type: "HANDSHAKE", clientId: "server", clientType: "server",
        payload: "connected", timestamp: Date.now(),
      }));
      broadcastClientList();
      break;
    case "PING":
      ws.send(JSON.stringify({
        type: "PONG", clientId: "server", clientType: "server", timestamp: Date.now(),
      }));
      break;
  }
}

export function broadcastClientList(): void {
  const clients = ClientManager.getClientSummaries();
  ClientManager.broadcast(JSON.stringify({
    type: "CLIENT_LIST", clientId: "server", clientType: "server",
    payload: JSON.stringify(clients), timestamp: Date.now(),
  }));
}

// Called from REST API routes to push real-time events to all WS clients
export function broadcastInventoryEvent(type: WsMessageType, data: unknown): void {
  const frame = JSON.stringify({
    type, clientId: "server", clientType: "server",
    payload: JSON.stringify(data), timestamp: Date.now(),
  });
  ClientManager.broadcast(frame);
  log(`[BROADCAST] ${type}`);
}

function serverFrame(type: WsMessageType, msg: string): string {
  return JSON.stringify({ type, clientId: "server", clientType: "server", payload: msg, timestamp: Date.now() });
}

function log(msg: string): void {
  console.log(`[${new Date().toISOString()}] ${msg}`);
}
