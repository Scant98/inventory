"use client";

import { useWebSocket } from "./hooks/useWebSocket";
import { ConnectionStatus } from "./components/ConnectionStatus";
import { ClientList } from "./components/ClientList";
import { MessageFeed } from "./components/MessageFeed";
import { MessageInput } from "./components/MessageInput";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080";

export function App() {
  const { connectionState, messages, connectedClients, sendMessage, clientId } =
    useWebSocket(WS_URL);

  const isConnected = connectionState === "CONNECTED";

  return (
    <div className="h-screen flex flex-col bg-gray-50 overflow-hidden">
      {/* Header */}
      <header className="shrink-0 bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between shadow-sm">
        <h1 className="text-base font-bold text-gray-800 tracking-tight">Inventory Chat</h1>
        <ConnectionStatus state={connectionState} />
      </header>

      {/* Body */}
      <div className="flex flex-1 gap-3 p-3 overflow-hidden">
        {/* Sidebar — hidden on mobile */}
        <aside className="hidden md:block w-52 shrink-0">
          <ClientList clients={connectedClients} myClientId={clientId} />
        </aside>

        {/* Chat panel */}
        <main className="flex flex-col flex-1 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <MessageFeed messages={messages} myClientId={clientId} />
          <MessageInput onSend={sendMessage} disabled={!isConnected} />
        </main>
      </div>
    </div>
  );
}
