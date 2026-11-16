export const OFFICIAL_ADDON = 'https://aura-lossless.aura-music.workers.dev';
type Session = { key: CryptoKey; session: string; expires: number; offset: number };
export type Pairing = { id: string; poll: string; code: string; link: string; expires: number; key: CryptoKey };
const bytes = (s: string) => new TextEncoder().encode(s);
const b64 = (b: ArrayBuffer | Uint8Array<ArrayBuffer>) => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const hash = async (s: string) => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes(s)))].map(x => x.toString(16).padStart(2, '0')).join('');
async function database(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB storage is not supported in this browser environment.'));
      return;
    }
    const request = indexedDB.open('aura-addon-device', 1);
    request.onupgradeneeded = () => { request.result.createObjectStore('session'); };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('Browser storage access denied. Enable storage to verify this browser.'));
  });
}
async function stored(write?: Session | null): Promise<Session | undefined> {
  const db = await database();
  try {
    return await new Promise((resolve, reject) => {
      const tx = db.transaction('session', write === undefined ? 'readonly' : 'readwrite'), object = tx.objectStore('session');
      const r = write === undefined ? object.get('current') : write === null ? object.delete('current') : object.put(write, 'current');
      let value: Session | undefined;
      r.onsuccess = () => { if (write === undefined) value = r.result as Session | undefined; };
      tx.oncomplete = () => resolve(value); tx.onerror = () => reject(tx.error); tx.onabort = () => reject(tx.error);
    });
  } finally { db.close(); }
}
async function call(path: string, data: object): Promise<Record<string, unknown>> {
  let r: Response;
  try {
    r = await fetch(OFFICIAL_ADDON + path, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { 'Content-Type': 'application/json' },
      credentials: 'omit',
      redirect: 'error',
      referrerPolicy: 'no-referrer',
      signal: AbortSignal.timeout(15_000),
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'TimeoutError') {
      throw new Error('Connection to verification server timed out.');
    }
    throw new Error('Could not reach verification server. Network connection error.');
  }

  let result: Record<string, unknown> = {};
  try {
    result = (await r.json()) as Record<string, unknown>;
  } catch {
    throw new Error(`Verification service returned an invalid response (HTTP ${r.status}).`);
  }

  if (!r.ok) {
    const isLocalDev = typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
    if (r.status === 401 && isLocalDev) {
      throw new Error(`Local development (${window.location.origin}) cannot be paired with the mobile app. Browser pairing requires the official hosted web app.`);
    }
    throw new Error(typeof result.error === 'string' ? result.error : `Verification failed (HTTP ${r.status})`);
  }
  return result;
}
export async function startPairing(): Promise<Pairing> {
  const isLocalDev = typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
  if (isLocalDev) {
    throw new Error(`Browser pairing is only supported on the production web app (https://hyperspandan.github.io/AURA/player/). Local development origins (${window.location.origin}) are rejected by the official add-on auth service.`);
  }
  // The browser key cannot be exported. A copied session token cannot sign requests.
  const keys = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign', 'verify']);
  const publicKey = await crypto.subtle.exportKey('jwk', keys.publicKey);
  const result = await call('/auth/web/start', { key: publicKey });
  if (typeof result.id !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(result.id) || typeof result.poll !== 'string' || typeof result.code !== 'string' || typeof result.expires !== 'number') throw new Error('Invalid browser verification response');
  return { id: result.id, poll: result.poll, code: result.code, expires: result.expires, link: 'aura://addon-approve?id=' + result.id, key: keys.privateKey };
}
export async function pollPairing(pair: Pairing): Promise<boolean> {
  if (Date.now() >= pair.expires) throw new Error('Request expired. Start verification again.');
  const result = await call('/auth/web/status', { id: pair.id, poll: pair.poll });
  if (!result.session) return false;
  if (typeof result.session !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(result.session) || typeof result.expires !== 'number' || typeof result.serverTime !== 'number') throw new Error('Invalid verification response');
  await stored({ key: pair.key, session: result.session, expires: result.expires, offset: result.serverTime - Date.now() });
  return true;
}
export async function verifiedUntil(): Promise<number> {
  try {
    const value = await stored(); return value && value.expires > Date.now() + value.offset ? value.expires : 0;
  } catch {
    return 0;
  }
}
export async function disconnectVerification(): Promise<void> { await stored(null); }
export function isOfficialAddon(url: string): boolean {
  try { return new URL(url).origin === OFFICIAL_ADDON; } catch { return false; }
}
export function needsVerification(url: string): boolean {
  const u = new URL(url); return isOfficialAddon(url) && u.pathname !== '/' && !u.pathname.endsWith('/manifest.json') && !u.pathname.endsWith('/health');
}
export async function verificationHeaders(url: string): Promise<Record<string, string>> {
  if (!needsVerification(url)) return {};
  try {
    const value = await stored();
    // In staged mode, an unverified browser can still stream without error.
    if (!value || value.expires <= Date.now() + value.offset) return {};
    const time = String(Date.now() + value.offset), nonce = b64(crypto.getRandomValues(new Uint8Array(24))), u = new URL(url);
    const message = ['AURA-Addon-v1', value.session, time, nonce, 'GET', u.pathname + u.search, await hash('')].join('\n');
    const signature = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, value.key, bytes(message));
    return { Authorization: 'Bearer ' + value.session, 'X-AURA-Time': time, 'X-AURA-Nonce': nonce, 'X-AURA-Signature': b64(signature) };
  } catch {
    return {};
  }
}
