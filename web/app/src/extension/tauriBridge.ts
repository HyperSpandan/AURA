import { BridgeCallError, type Bridge, type DetectedExtension } from '@aura/shared/bridge';
import type { BridgeEvent, BridgeMethod, ParamsOf, ResultOf } from '@aura/shared/bridge';

interface TauriInternals {
  invoke: <T>(cmd: string, args?: Record<string, unknown>) => Promise<T>;
}

interface RustBridgeResult {
  ok: boolean;
  result?: unknown;
  error?: string;
}

function tauriInternals(): TauriInternals | null {
  if (typeof window === 'undefined') return null;
  const w = window as unknown as { __TAURI_INTERNALS__?: TauriInternals };
  return w.__TAURI_INTERNALS__ ?? null;
}

export function isTauriDesktop(): boolean {
  return tauriInternals() !== null;
}

export async function detectTauriBridge(): Promise<DetectedExtension | null> {
  const internals = tauriInternals();
  if (!internals) return null;

  const listeners = new Set<(event: BridgeEvent) => void>();
  const bridge: Bridge = {
    transport: 'external',
    async call<M extends BridgeMethod>(method: M, params: ParamsOf<M>): Promise<ResultOf<M>> {
      const res = await internals.invoke<RustBridgeResult>('bridge_call', {
        call: { method, params },
      });
      if (!res.ok) {
        throw new BridgeCallError('unavailable', res.error ?? `Desktop bridge error for ${method}`);
      }
      return res.result as ResultOf<M>;
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };

  try {
    const info = await bridge.call('ping', {});
    return { bridge, info };
  } catch {
    return null;
  }
}
