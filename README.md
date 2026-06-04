# Inventory — Real-Time WebSocket App

Three-module monorepo: **Android** (Kotlin + Compose) · **Web** (React + Vite) · **Server** (Node.js + TypeScript).

```
Inventory/
├── android/        ← this IS the Android project root (app/ is the module)
├── app/            ← Android app module
├── server/         ← WebSocket backend
├── web/            ← React frontend
├── infra/          ← nginx config + deploy script
└── gradle/         ← shared Gradle version catalog
```

---

## Message Protocol

All frames are JSON:

```jsonc
{
  "type": "HANDSHAKE | MESSAGE | BROADCAST | PING | PONG | CLIENT_LIST",
  "clientId": "<uuid>",
  "clientType": "android | web | server",
  "payload": "string (optional)",
  "timestamp": 1718000000000
}
```

---

## Server

### Local dev

```bash
cd server
npm install
cp .env.example .env          # edit ALLOWED_ORIGINS if needed
npm run dev                   # ts-node-dev, hot-reload
```

Server listens on `http://localhost:8080`.  
Health check: `GET /health`

### Build for production

```bash
npm run build          # compiles TypeScript → dist/
npm start              # node dist/index.js
```

### Docker

```bash
docker build -t inventory-ws ./server
docker run -p 8080:8080 --env-file server/.env inventory-ws
```

---

## Web App

### Local dev

```bash
cd web
npm install
cp .env.example .env.local
# Set VITE_WS_URL=ws://localhost:8080
npm run dev            # Vite HMR at http://localhost:5173
```

### Build for S3 / CloudFront

```bash
npm run build          # output in web/dist/
```

Upload `web/dist/` to your S3 bucket, set the bucket policy for public read, and configure CloudFront to serve it.

---

## Android App

### Prerequisites

- Android Studio Meerkat or newer
- JDK 17+
- Android SDK 26+

### Configuration

Open `local.properties` (project root) and set the WebSocket URL:

```properties
# Emulator → host machine
ws.url=ws://10.0.2.2:8080

# Real device on same WiFi
ws.url=ws://192.168.x.x:8080

# Production
ws.url=wss://yourserver.example.com
```

> **Note:** `local.properties` is git-ignored. Never hardcode the URL.

### Build & run

1. Open the project root (`Inventory/`) in Android Studio.
2. Let Gradle sync finish.
3. Run `app` on an emulator or device.

### Architecture

```
app/
└── src/main/java/com/sombetech/inventory/
    ├── data/
    │   ├── model/WsMessage.kt          ← JSON DTO + serialization
    │   └── websocket/WebSocketClientImpl.kt  ← OkHttp + reconnect logic
    ├── domain/
    │   ├── model/                      ← ChatMessage, ConnectedClient, ConnectionState
    │   └── repository/WebSocketRepository.kt  ← interface
    ├── presentation/
    │   ├── viewmodel/ChatViewModel.kt  ← StateFlow, optimistic send
    │   └── ui/
    │       ├── screen/ChatScreen.kt    ← main screen
    │       ├── components/             ← ConnectionStatusBar, MessageBubble
    │       └── theme/                  ← Material3 dynamic color
    ├── di/AppModule.kt                 ← Hilt: OkHttpClient, repository binding
    ├── InventoryApplication.kt         ← @HiltAndroidApp
    └── MainActivity.kt                 ← @AndroidEntryPoint, edge-to-edge Compose
```

**Reconnection:** exponential backoff (1 s, 2 s, 4 s, 8 s, 16 s), max 5 retries.

---

## AWS Deployment

### EC2 — WebSocket server (PM2)

```bash
# On the EC2 instance (Amazon Linux 2 / Ubuntu)
sudo apt install -y nodejs npm nginx certbot python3-certbot-nginx
npm install -g pm2

git clone <your-repo> /opt/inventory-server
cd /opt/inventory-server/server
cp .env.example .env           # set ALLOWED_ORIGINS

npm ci && npm run build
pm2 start ecosystem.config.js
pm2 save && pm2 startup
```

Copy `infra/nginx.conf` to `/etc/nginx/sites-available/inventory`, symlink to `sites-enabled/`, then:

```bash
sudo certbot --nginx -d yourdomain.com
sudo systemctl reload nginx
```

### Deploy script

```bash
# From your dev machine:
EC2_HOST=ubuntu@1.2.3.4  KEY_FILE=~/.ssh/key.pem  ./infra/deploy.sh
```

### S3 + CloudFront — Web frontend

1. Create an S3 bucket (e.g. `inventory-web-prod`), disable "Block all public access".
2. Add a bucket policy allowing `s3:GetObject` to `*`.
3. Enable "Static website hosting" → index document `index.html`.
4. Create a CloudFront distribution pointing at the S3 bucket.
5. Set a custom error page for 404 → 200 `/index.html` (SPA fallback).
6. Build and upload:

```bash
cd web
VITE_WS_URL=wss://yourserver.example.com npm run build
aws s3 sync dist/ s3://inventory-web-prod --delete
aws cloudfront create-invalidation --distribution-id EXXXX --paths "/*"
```

---

## Security notes

- **WSS only** in production — nginx terminates TLS.
- **Origin allowlist** via `ALLOWED_ORIGINS` env var.
- **Rate limiting**: 10 messages / second / client (server-side).
- **Input sanitization**: HTML entities escaped, payload capped at 4 096 chars.
- `local.properties` and `.env` files are git-ignored — never commit secrets.
