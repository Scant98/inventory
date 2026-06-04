import express from "express";
import http from "http";
import WebSocket, { WebSocketServer } from "ws";
import { api } from "./api";
import { handleMessage, validateOrigin, broadcastClientList } from "./messageHandler";
import { removeClient } from "./clientManager";
import { clearRateLimit } from "./rateLimiter";
import { ExtWebSocket } from "./types";
import { db } from "./db";

const PORT = parseInt(process.env.PORT ?? "8080", 10);
const HEARTBEAT_MS = 30_000;

// ─── Express app ─────────────────────────────────────────────────────────────

const app = express();
app.use(express.json());

app.get("/health", (_req, res) =>
  res.json({ status: "ok", timestamp: Date.now() })
);

app.use("/api", api);

// ─── HTTP + WebSocket server ─────────────────────────────────────────────────

const server = http.createServer(app);

const wss = new WebSocketServer({
  server,
  verifyClient: ({ origin }, done) => {
    const ok = validateOrigin(origin);
    done(ok, ok ? undefined : 403, ok ? undefined : "Origin not allowed");
  },
});

wss.on("connection", (rawWs: WebSocket, req) => {
  const ws = rawWs as ExtWebSocket;
  ws.isAlive = true;

  log(`WS connection from ${req.socket.remoteAddress}`);

  ws.on("pong", () => { ws.isAlive = true; });

  ws.on("message", (data) => {
    const text = data.toString();
    if (!ws.clientId) {
      try { const m = JSON.parse(text); if (m.clientId) ws.clientId = m.clientId; } catch {/**/}
    }
    handleMessage(ws, text);
  });

  ws.on("close", () => {
    if (ws.clientId) {
      log(`WS disconnect: ${ws.clientId.slice(0, 8)}`);
      removeClient(ws.clientId);
      clearRateLimit(ws.clientId);
      broadcastClientList();
    }
  });

  ws.on("error", (err) => log(`WS error: ${err.message}`));
});

// ─── Heartbeat ───────────────────────────────────────────────────────────────

const heartbeat = setInterval(() => {
  wss.clients.forEach(rawWs => {
    const ws = rawWs as ExtWebSocket;
    if (!ws.isAlive) {
      if (ws.clientId) { removeClient(ws.clientId); clearRateLimit(ws.clientId); broadcastClientList(); }
      ws.terminate(); return;
    }
    ws.isAlive = false;
    ws.ping();
  });
}, HEARTBEAT_MS);

wss.on("close", () => clearInterval(heartbeat));

// ─── Start ───────────────────────────────────────────────────────────────────

db.$connect()
  .then(() => {
    log("Database connected");
    server.listen(PORT, () => log(`Server on port ${PORT}  REST: http://localhost:${PORT}/api`));
  })
  .catch((err: Error) => {
    log(`Database connection failed: ${err.message}`);
    process.exit(1);
  });

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT",  () => shutdown("SIGINT"));

function shutdown(sig: string) {
  log(`${sig} — shutting down`);
  clearInterval(heartbeat);
  wss.close(() => server.close(() => db.$disconnect().then(() => { log("done"); process.exit(0); })));
}

function log(msg: string) { console.log(`[${new Date().toISOString()}] ${msg}`); }
