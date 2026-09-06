'use client';

import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';

// Same base the REST client uses (see src/services/api.js), just swapped
// from http(s)/api to ws(s)/ws — the STOMP endpoint is registered at /ws,
// not under /api. Plain native WebSocket (no SockJS) since every deploy
// target here supports it directly; one less dependency to ship.
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:6060/api';
const WS_URL = API_BASE_URL.replace(/^http/, 'ws').replace(/\/api\/?$/, '') + '/ws';

/**
 * Subscribes to live task events for one project's board
 * (/topic/project/{projectId}/tasks) for as long as the calling component
 * is mounted. Broadcast-only channel — no auth on the socket itself, see
 * WebSocketConfig's Javadoc for why that's an acceptable trade-off here.
 *
 * `onEvent` is called with the parsed TaskEvent for every message,
 * including ones this same tab caused — the caller decides whether to
 * ignore its own actorEmail (the board page does, since it already has
 * fresh state from the REST response that caused the event).
 *
 * Returns a `connected` boolean for a small "Live" indicator in the UI.
 */
export function useProjectSocket(projectId, onEvent) {
  const [connected, setConnected] = useState(false);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    if (!projectId) return;

    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 4000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/project/${projectId}/tasks`, (message) => {
          try {
            const event = JSON.parse(message.body);
            onEventRef.current?.(event);
          } catch (error) {
            console.error('Failed to parse task event:', error);
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      // Keep this quiet by default — a dropped socket degrades to "board
      // just doesn't live-update until reconnect," not a broken app, so it
      // doesn't deserve console noise on every reconnect attempt.
      onStompError: () => setConnected(false),
    });

    client.activate();

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [projectId]);

  return connected;
}
