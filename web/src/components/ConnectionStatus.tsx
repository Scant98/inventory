import { ConnectionState } from "../types";

const config: Record<ConnectionState, { label: string; textCls: string; dotCls: string }> = {
  CONNECTING: {
    label: "Connecting…",
    textCls: "text-yellow-600",
    dotCls: "bg-yellow-400 animate-pulse",
  },
  CONNECTED: {
    label: "Connected",
    textCls: "text-green-600",
    dotCls: "bg-green-500",
  },
  DISCONNECTED: {
    label: "Disconnected",
    textCls: "text-gray-500",
    dotCls: "bg-gray-400",
  },
  ERROR: {
    label: "Connection Error",
    textCls: "text-red-600",
    dotCls: "bg-red-500",
  },
};

export function ConnectionStatus({ state }: { state: ConnectionState }) {
  const { label, textCls, dotCls } = config[state];
  return (
    <div className="flex items-center gap-2">
      <span className={`inline-block w-2.5 h-2.5 rounded-full ${dotCls}`} />
      <span className={`text-sm font-medium ${textCls}`}>{label}</span>
    </div>
  );
}
