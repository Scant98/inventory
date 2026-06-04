#!/usr/bin/env bash
# Usage: EC2_HOST=ubuntu@1.2.3.4 ./infra/deploy.sh
set -euo pipefail

EC2_HOST="${EC2_HOST:?Set EC2_HOST, e.g. ubuntu@1.2.3.4}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/inventory-server}"
KEY_FILE="${KEY_FILE:-}"        # optional: path to PEM key

SSH_OPTS=(-o StrictHostKeyChecking=no)
[[ -n "$KEY_FILE" ]] && SSH_OPTS+=(-i "$KEY_FILE")

echo "▶ Deploying to $EC2_HOST:$DEPLOY_PATH"

ssh "${SSH_OPTS[@]}" "$EC2_HOST" DEPLOY_PATH="$DEPLOY_PATH" 'bash -s' <<'REMOTE'
set -euo pipefail
cd "$DEPLOY_PATH"

echo "→ Pulling latest code"
git pull origin master

echo "→ Installing production dependencies"
npm ci --omit=dev

echo "→ Building TypeScript"
npm run build

echo "→ Restarting PM2 process"
pm2 restart inventory-ws 2>/dev/null || pm2 start ecosystem.config.js
pm2 save

echo "✓ Deploy complete"
pm2 list
REMOTE

echo "▶ Done"
