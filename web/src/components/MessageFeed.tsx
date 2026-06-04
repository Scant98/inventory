"use client";

import { useEffect, useRef } from "react";
import { WsMessage } from "../types";

interface Props {
  messages: WsMessage[];
  myClientId: string;
}

export function MessageFeed({ messages, myClientId }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-3 min-h-0">
      {messages.length === 0 && (
        <p className="text-center text-gray-400 text-sm mt-12">
          No messages yet — say hello!
        </p>
      )}
      {messages.map((msg, i) => {
        const isMe = msg.clientId === myClientId;
        const isAndroid = msg.clientType === "android";

        return (
          <div key={i} className={`flex ${isMe ? "justify-end" : "justify-start"}`}>
            <div
              className={`max-w-[75%] rounded-2xl px-4 py-2 shadow-sm ${
                isMe
                  ? "bg-blue-500 text-white rounded-br-sm"
                  : isAndroid
                  ? "bg-green-100 text-gray-800 rounded-bl-sm"
                  : "bg-white border border-gray-200 text-gray-800 rounded-bl-sm"
              }`}
            >
              {!isMe && (
                <p
                  className={`text-[11px] font-semibold mb-0.5 ${
                    isAndroid ? "text-green-700" : "text-blue-600"
                  }`}
                >
                  {isAndroid ? "Android" : "Web"} · {msg.clientId.slice(0, 6)}
                </p>
              )}
              <p className="text-sm break-words">{msg.payload}</p>
              <p className={`text-[10px] mt-0.5 ${isMe ? "text-blue-200" : "text-gray-400"}`}>
                {new Date(msg.timestamp).toLocaleTimeString()}
              </p>
            </div>
          </div>
        );
      })}
      <div ref={bottomRef} />
    </div>
  );
}
