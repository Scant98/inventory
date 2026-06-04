const WINDOW_MS = 1_000;
const MAX_MESSAGES_PER_WINDOW = 10;

interface RateLimitEntry {
  count: number;
  windowStart: number;
}

const limits = new Map<string, RateLimitEntry>();

export function isRateLimited(clientId: string): boolean {
  const now = Date.now();
  const entry = limits.get(clientId);

  if (!entry || now - entry.windowStart > WINDOW_MS) {
    limits.set(clientId, { count: 1, windowStart: now });
    return false;
  }

  if (entry.count >= MAX_MESSAGES_PER_WINDOW) {
    return true;
  }

  entry.count++;
  return false;
}

export function clearRateLimit(clientId: string): void {
  limits.delete(clientId);
}
