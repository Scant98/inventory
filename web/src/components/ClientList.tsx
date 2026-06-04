import { ConnectedClient } from "../types";

interface Props {
  clients: ConnectedClient[];
  myClientId: string;
}

export function ClientList({ clients, myClientId }: Props) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-4 h-full overflow-y-auto">
      <h2 className="text-xs font-semibold uppercase tracking-widest text-gray-500 mb-3">
        Online ({clients.length})
      </h2>
      {clients.length === 0 ? (
        <p className="text-gray-400 text-sm">No clients yet</p>
      ) : (
        <ul className="space-y-2">
          {clients.map((c) => (
            <li key={c.clientId} className="flex items-center gap-2 min-w-0">
              <span
                className={`shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                  c.clientType === "android"
                    ? "bg-green-100 text-green-700"
                    : "bg-blue-100 text-blue-700"
                }`}
              >
                {c.clientType === "android" ? "Android" : "Web"}
              </span>
              <span className="text-xs text-gray-500 truncate">
                {c.clientId === myClientId ? "You" : c.clientId.slice(0, 8) + "…"}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
