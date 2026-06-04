#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# One-time setup script for the Inventory server on a fresh Ubuntu 22.04 EC2.
# Run as the ubuntu user:  bash setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

REPO="https://github.com/Scant98/inventory.git"
APP_DIR="/opt/inventory"
SERVER_DIR="$APP_DIR/server"

echo ""
echo "================================================="
echo "  Inventory Server — EC2 First-Time Setup"
echo "================================================="
echo ""

# ─── 1. System update ────────────────────────────────────────────────────────
echo "▶ [1/9] Updating system packages..."
sudo apt-get update -q
sudo apt-get upgrade -y -q
sudo apt-get install -y -q git curl openssl

# ─── 2. Node.js 20 ───────────────────────────────────────────────────────────
echo "▶ [2/9] Installing Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - >/dev/null 2>&1
sudo apt-get install -y nodejs
echo "   Node $(node -v) · npm $(npm -v)"

# ─── 3. PM2 ──────────────────────────────────────────────────────────────────
echo "▶ [3/9] Installing PM2..."
sudo npm install -g pm2 >/dev/null 2>&1

# ─── 4. Nginx ────────────────────────────────────────────────────────────────
echo "▶ [4/9] Installing Nginx..."
sudo apt-get install -y -q nginx
sudo systemctl enable nginx

# ─── 5. PostgreSQL ───────────────────────────────────────────────────────────
echo "▶ [5/9] Installing PostgreSQL..."
sudo apt-get install -y -q postgresql postgresql-contrib
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Create database user and database with a random password
DB_PASS=$(openssl rand -hex 20)
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='inventory'" \
  | grep -q 1 || sudo -u postgres psql -c "CREATE USER inventory WITH PASSWORD '$DB_PASS';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='inventory'" \
  | grep -q 1 || sudo -u postgres psql -c "CREATE DATABASE inventory OWNER inventory;"
DATABASE_URL="postgresql://inventory:$DB_PASS@localhost:5432/inventory"
echo "   PostgreSQL ready. Database: inventory"

# ─── 6. Clone repository ─────────────────────────────────────────────────────
echo "▶ [6/9] Cloning repository..."
sudo mkdir -p "$APP_DIR"
sudo chown ubuntu:ubuntu "$APP_DIR"
if [ -d "$APP_DIR/.git" ]; then
  echo "   Repo already exists — pulling latest..."
  cd "$APP_DIR" && git pull origin master
else
  git clone "$REPO" "$APP_DIR"
fi

# ─── 7. Environment file ─────────────────────────────────────────────────────
echo "▶ [7/9] Creating .env file..."
mkdir -p "$SERVER_DIR/logs"
cat > "$SERVER_DIR/.env" <<EOF
DATABASE_URL="$DATABASE_URL"
PORT=8080
NODE_ENV=production
ALLOWED_ORIGINS=*
EOF
echo "   .env written to $SERVER_DIR/.env"
echo "   DATABASE_URL saved. Write this down for reference:"
echo "   $DATABASE_URL"

# ─── 8. Install deps, migrate, seed, build ───────────────────────────────────
echo "▶ [8/9] Installing dependencies and building..."
cd "$SERVER_DIR"
npm ci --omit=dev
# Reinstall prisma devDep so generate works
npm install --save-dev prisma
npx prisma generate
npx prisma migrate deploy
npm run db:seed || echo "   (seed skipped — data already exists)"
# Install ts-node-dev for the build step, then build
npm install --save-dev ts-node-dev typescript
npm run build

# ─── 9. PM2 — start and enable on boot ───────────────────────────────────────
echo "▶ [9/9] Starting server with PM2..."
pm2 delete inventory-ws 2>/dev/null || true
cd "$SERVER_DIR"
pm2 start ecosystem.config.js
pm2 save

# Enable PM2 to start on system reboot
PM2_STARTUP_CMD=$(pm2 startup systemd -u ubuntu --hp /home/ubuntu 2>&1 | grep "sudo env")
if [ -n "$PM2_STARTUP_CMD" ]; then
  eval "$PM2_STARTUP_CMD" || true
fi
pm2 save

# ─── Nginx — HTTP proxy (no domain needed) ───────────────────────────────────
echo "▶ Configuring Nginx..."
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "YOUR_EC2_IP")

sudo tee /etc/nginx/sites-available/inventory > /dev/null <<NGINX
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Upgrade    \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host               \$host;
        proxy_set_header X-Real-IP          \$remote_addr;
        proxy_set_header X-Forwarded-For    \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto  \$scheme;

        proxy_read_timeout  86400s;
        proxy_send_timeout  86400s;
        proxy_connect_timeout 10s;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/inventory /etc/nginx/sites-enabled/inventory
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# ─── UFW firewall ─────────────────────────────────────────────────────────────
echo "▶ Configuring firewall..."
sudo ufw allow OpenSSH   >/dev/null 2>&1 || true
sudo ufw allow 'Nginx Full' >/dev/null 2>&1 || true
sudo ufw allow 8080/tcp  >/dev/null 2>&1 || true
echo "y" | sudo ufw enable >/dev/null 2>&1 || true

# ─── Done ─────────────────────────────────────────────────────────────────────
echo ""
echo "================================================="
echo "  SETUP COMPLETE"
echo "================================================="
echo ""
echo "  Server health:  http://$PUBLIC_IP/health"
echo "  REST API:       http://$PUBLIC_IP/api/stats"
echo "  WebSocket:      ws://$PUBLIC_IP"
echo ""
echo "  PM2 status:"
pm2 list
echo ""
echo "  To view live logs:  pm2 logs inventory-ws"
echo "  To redeploy:        cd $APP_DIR && git pull && cd server && npm ci && npm run build && pm2 restart inventory-ws"
echo ""
echo "  IMPORTANT: Update your Android app local.properties:"
echo "  ws.url=ws://$PUBLIC_IP"
echo ""
echo "  IMPORTANT: Update your web app NEXT_PUBLIC_WS_URL:"
echo "  NEXT_PUBLIC_WS_URL=ws://$PUBLIC_IP"
echo ""
