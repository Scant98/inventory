import WebSocket from "ws";
import { ClientInfo, ClientSummary, ClientType } from "./types";

const clients = new Map<string, ClientInfo>();

export function addClient(
  clientId: string,
  clientType: ClientType,
  ws: WebSocket
): void {
  clients.set(clientId, { clientId, clientType, connectedAt: Date.now(), ws });
}

export function removeClient(clientId: string): void {
  clients.delete(clientId);
}

export function getClient(clientId: string): ClientInfo | undefined {
  return clients.get(clientId);
}

export function getClientCount(): number {
  return clients.size;
}

export function broadcast(message: string, excludeClientId?: string): void {
  clients.forEach((client, id) => {
    if (id !== excludeClientId && client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(message);
    }
  });
}

export function getClientSummaries(): ClientSummary[] {
  return Array.from(clients.values()).map(({ clientId, clientType, connectedAt }) => ({
    clientId,
    clientType,
    connectedAt,
  }));
}
