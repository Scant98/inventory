# Inventory App — Project Memory

## What This Project Is
A shoe & clothing shop inventory management system with three modules:
- **Android app** — Compose + Material3, connects to server via WebSocket
- **Web app** — Next.js 15 + Tailwind + shadcn/ui, full inventory dashboard
- **Server** — Node.js + Express + WebSocket + Prisma + PostgreSQL

One product = one SKU = one specific variant (brand + model + size + color).

---

## Repository
- GitHub: https://github.com/Scant98/inventory.git
- Branch: `master` (default — main branch is empty, ignore it)

---

## Project Structure
```
Inventory/
├── app/                  Android module (Compose, Hilt, OkHttp WS)
├── web/                  Next.js 15 web dashboard
├── server/               Node.js + Express + Prisma server
├── infra/                EC2 deploy scripts (setup.sh, deploy.sh, nginx.conf)
├── CLAUDE.md             This file
├── vercel.json           Vercel deployment config
└── package.json          Root package.json (for Vercel Next.js detection only)
```

---

## Server (`server/`)
- **Entry:** `src/index.ts` — port 8080
- **API:** `src/api.ts` — REST endpoints under `/api/`
- **DB:** `src/db.ts` — Prisma client singleton
- **Store:** `src/store.ts` — async helper functions (allProducts, computeStats, etc.)
- **Seed:** `src/seed.ts` — run once to populate demo data
- **Schema:** `prisma/schema.prisma` — 7 tables: Category, Product, InventoryBatch, Transaction, Order, OrderItem, OrderCounter
- **Key commands:**
  - `npm run dev` — start dev server
  - `npm run build` — compile TypeScript
  - `npm run db:seed` — seed demo data
  - `npm run db:setup` — migrate + seed

### API Endpoints
- `GET/POST /api/products` — list / create products
- `PUT/DELETE /api/products/:id` — update / delete
- `POST /api/products/:id/adjust` — stock adjustment (in/out/sale)
- `GET /api/inventory-batches?productId=X` — batch history
- `GET /api/transactions` — transaction log
- `GET/POST /api/orders` — orders
- `GET /api/stats` — dashboard stats
- `GET /api/reports` — full sales + inventory report
- `GET /api/categories` — categories
- `GET /health` — health check

### Product Model Fields
`id, name, sku, categoryId, brand, productType, size, gender, color, price, cost, stock, minStock, unit, description, createdAt, updatedAt`

### Important Notes
- All API handlers are async (PostgreSQL via Prisma — no in-memory Maps)
- Orders use `db.$transaction()` for atomic stock deduction
- Duplicate SKU returns `400 SKU already exists` (Prisma error code P2002)
- Deleting a product cascades to its transactions and inventory batches
- Cannot delete a category that has products (returns 400)

---

## Web App (`web/`)
- **Framework:** Next.js 15 App Router
- **Pages:** `/` (dashboard), `/products`, `/categories`, `/orders`, `/transactions`, `/reports`
- **WS hook:** `src/hooks/useInventory.ts` — WebSocket + REST, 5-retry backoff
- **API client:** `src/lib/api.ts`
- **Env vars:** `NEXT_PUBLIC_WS_URL`, `NEXT_PUBLIC_API_URL`
- **Local env:** `web/.env.local` (gitignored)
- Start locally: `cd web && npm run dev` → http://localhost:3000

---

## Android App (`app/`)
- **Package:** `com.sombetech.inventory`
- **minSdk:** 26, **targetSdk:** 36
- **Stack:** Compose + Material3, Hilt, OkHttp WebSocket, Coroutines, ViewModel
- **Server URL:** read from `local.properties` → injected into `BuildConfig.WS_URL` / `BuildConfig.API_URL`
- **Build APK:** `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Deployment — LIVE

### EC2 Server (AWS)
- **IP:** `51.20.5.138`
- **Region:** eu-north-1 (Stockholm)
- **Instance:** t2.micro, Ubuntu 22.04 LTS
- **PEM key:** `C:\Users\user\Downloads\inventory-key.pem`
- **SSH:** `ssh -i "C:\Users\user\Downloads\inventory-key.pem" ubuntu@51.20.5.138`
- **App dir:** `/opt/inventory/server`
- **Process manager:** PM2 (process name: `inventory-ws`)
- **Reverse proxy:** Nginx (port 80 → 8080)
- **Health check:** http://51.20.5.138/health ✅ LIVE

### Database (PostgreSQL on EC2)
- **Host:** localhost (on EC2)
- **Database:** `inventory`
- **User:** `inventory`
- **Password:** `fb9858e4e65544e06c80fde43e90c62b04226981`
- **Connection string:** `postgresql://inventory:fb9858e4e65544e06c80fde43e90c62b04226981@localhost:5432/inventory`
- **Status:** Empty (demo data was cleared — ready for real data)

### EC2 Useful Commands (run after SSH)
```bash
pm2 status                          # check server status
pm2 logs inventory-ws               # view live logs
pm2 restart inventory-ws            # restart server

# Redeploy after code changes:
cd /opt/inventory && git pull origin master && cd server && npm ci && npm run build && pm2 restart inventory-ws
```

### Web App (Vercel) — NOT YET DEPLOYED
- Vercel deployment was attempted but kept failing (root directory detection issue)
- Currently running locally only: `cd web && npm run dev`
- Env vars needed when deploying: `NEXT_PUBLIC_WS_URL=ws://51.20.5.138` and `NEXT_PUBLIC_API_URL=http://51.20.5.138/api`

---

## Current URLs
| What | URL |
|---|---|
| Health | http://51.20.5.138/health |
| REST API | http://51.20.5.138/api/stats |
| WebSocket | ws://51.20.5.138 |
| Web (local) | http://localhost:3000 |

---

## What Has Been Done
1. ✅ Replaced in-memory store with PostgreSQL + Prisma ORM
2. ✅ All API handlers rewritten as async with real DB queries
3. ✅ Orders use atomic DB transactions (prevents overselling)
4. ✅ Database migrated and seeded on EC2
5. ✅ EC2 instance running on AWS (t2.micro, free tier)
6. ✅ Nginx configured as reverse proxy
7. ✅ PM2 managing the Node.js process
8. ✅ Code pushed to GitHub (public repo, master branch)
9. ✅ Android `local.properties` updated to point to EC2
10. ✅ Android network security config updated to allow HTTP to EC2 IP
11. ✅ Debug APK built and installed on phone
12. ⏳ Vercel deployment for web app — in progress (failing, needs fix)

## What Still Needs To Be Done
- Fix Vercel deployment for web app
- Set up HTTPS/SSL with a domain name (so `wss://` works instead of `ws://`)
- Build a release APK (signed, for sharing)
- (Optional) Move database to AWS RDS for better reliability

---

## Deployment Notes
- `master` branch is the active branch — always push/pull from master
- `main` branch on GitHub is empty (was auto-created by GitHub) — ignore it
- The `vercel.json` at root uses `buildCommand: "cd web && npm install && npm run build"` to work around the monorepo root directory issue
- `package.json` at repo root exists ONLY for Vercel to detect Next.js — do not add real dependencies to it
- `server/.env` is gitignored — never commit it
- `local.properties` is gitignored — contains EC2 server URL
