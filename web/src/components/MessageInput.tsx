"use client";

import { FormEvent, useState } from "react";

interface Props {
  onSend: (text: string) => void;
  disabled: boolean;
}

export function MessageInput({ onSend, disabled }: Props) {
  const [text, setText] = useState("");

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (trimmed) {
      onSend(trimmed);
      setText("");
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex gap-2 p-3 border-t border-gray-200 bg-white"
    >
      <input
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        disabled={disabled}
        placeholder={disabled ? "Not connected…" : "Type a message…"}
        maxLength={500}
        className="flex-1 rounded-full border border-gray-300 px-4 py-2 text-sm outline-none
                   focus:ring-2 focus:ring-blue-400 focus:border-transparent
                   disabled:bg-gray-100 disabled:cursor-not-allowed transition"
      />
      <button
        type="submit"
        disabled={disabled || !text.trim()}
        className="bg-blue-500 text-white rounded-full px-5 py-2 text-sm font-medium
                   hover:bg-blue-600 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        Send
      </button>
    </form>
  );
}
