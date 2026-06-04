export type MessageType =
  | "HANDSHAKE"
  | "MESSAGE"
  | "BROADCAST"
  | "PING"
  | "PONG"
  | "CLIENT_LIST"
  | "ERROR";

export type ClientType = "android" | "web" | "server";

export interface WsMessage {
  type: MessageType;
  clientId: string;
  clientType: ClientType;
  payload?: string;
  timestamp: number;
}

export interface ConnectedClient {
  clientId: string;
  clientType: "android" | "web";
  connectedAt: number;
}

export type ConnectionState = "CONNECTING" | "CONNECTED" | "DISCONNECTED" | "ERROR";
