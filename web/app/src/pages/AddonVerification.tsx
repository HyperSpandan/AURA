import { useEffect, useState } from 'react';
import { disconnectVerification, pollPairing, startPairing, verifiedUntil, type Pairing } from '@/addons/verification';
import { AlertCircle, CheckCircle2, Copy, ExternalLink, Loader2, ShieldCheck, X } from 'lucide-react';

export function AddonVerification() {
  const [pair, setPair] = useState<Pairing | null>(null);
  const [until, setUntil] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void verifiedUntil()
      .then(setUntil)
      .catch(() => setError('Browser storage is unavailable. Enable local storage to verify this browser.'));
  }, []);

  useEffect(() => {
    if (!pair) return;
    let active = true;
    let timer: ReturnType<typeof setTimeout>;

    const poll = async () => {
      try {
        const ready = await pollPairing(pair);
        if (!active) return;
        if (ready) {
          setUntil(await verifiedUntil());
          setPair(null);
          setStatus('Browser successfully verified. Protected sources are ready.');
          setError(null);
        } else {
          timer = setTimeout(() => void poll(), 3000);
        }
      } catch (e) {
        if (active) {
          setPair(null);
          setError(e instanceof Error ? e.message : 'Verification failed');
        }
      }
    };

    timer = setTimeout(() => void poll(), 3000);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [pair]);

  const handleStart = async () => {
    setBusy(true);
    setError(null);
    setStatus(null);
    try {
      const pairing = await startPairing();
      setPair(pairing);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not start verification.');
    } finally {
      setBusy(false);
    }
  };

  const handleDisconnect = async () => {
    setBusy(true);
    try {
      await disconnectVerification();
      setUntil(0);
      setStatus('Browser session cleared. This browser was disconnected.');
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not disconnect session.');
    } finally {
      setBusy(false);
    }
  };

  const isVerified = until > Date.now();
  const buttonStyle = 'inline-flex items-center gap-1.5 rounded-pill bg-quinary px-4 py-2 font-medium text-primary hover:bg-quinary/80 transition-colors disabled:opacity-45';

  return (
    <section className="mx-4 mb-4 rounded-2xl bg-secondary/10 p-5 border border-white/5" aria-label="AURA source verification">
      <div className="flex items-center gap-2">
        <ShieldCheck className="h-5 w-5 text-key" />
        <h2 className="font-semibold text-primary">AURA protected source verification</h2>
      </div>

      <p className="mt-2 text-sm text-secondary leading-relaxed">
        Only official AURA-hosted add-ons use this cryptographic device check to prevent third-party automated abuse.
        Public community add-ons work independently. Sources are currently accessible in staged rollout mode;
        verifying pairs this browser with your verified Android or TV app for up to 12 hours.
      </p>

      {isVerified && (
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 px-3 py-2 text-sm text-emerald-400">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>Verified until {new Date(until).toLocaleString()}</span>
        </div>
      )}

      {error && (
        <div className="mt-3 flex items-start gap-2.5 rounded-xl bg-destructive/15 border border-destructive/30 p-3 text-sm text-destructive" role="alert">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold">Verification Notice</p>
            <p className="mt-0.5 opacity-90">{error}</p>
          </div>
          <button onClick={() => setError(null)} className="opacity-70 hover:opacity-100 p-0.5" aria-label="Dismiss error">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {status && (
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 p-3 text-sm text-emerald-400" role="status">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{status}</span>
        </div>
      )}

      {!pair && (
        <div className="mt-4 flex flex-wrap gap-2.5">
          <button className={buttonStyle} disabled={busy} onClick={() => void handleStart()}>
            {busy ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Starting verification…</span>
              </>
            ) : isVerified ? (
              'Verify again'
            ) : (
              'Verify browser'
            )}
          </button>

          {until > 0 && (
            <button className={buttonStyle} disabled={busy} onClick={() => void handleDisconnect()}>
              Disconnect browser
            </button>
          )}
        </div>
      )}

      {pair && (
        <div className="mt-4 space-y-3 rounded-xl bg-black/20 p-4 border border-white/5">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-secondary">Confirmation code:</span>
            <strong className="font-mono text-lg tracking-widest text-key bg-quinary/80 px-2.5 py-0.5 rounded-md">{pair.code}</strong>
          </div>
          <p className="text-xs text-secondary leading-normal">
            Open the link on your verified Android or TV AURA app. Only approve when the app displays this exact same 8-character confirmation code.
          </p>

          <div className="flex flex-wrap gap-2 pt-1">
            <a className={buttonStyle} href={pair.link} target="_blank" rel="noreferrer">
              <ExternalLink className="h-4 w-4" />
              <span>Open in AURA app</span>
            </a>
            <button
              className={buttonStyle}
              onClick={() => {
                void navigator.clipboard.writeText(pair.link)
                  .then(() => setStatus('Approval link copied to clipboard. Open it on your verified Android or TV device.'))
                  .catch(() => setStatus('Select and copy the approval link manually below.'));
              }}
            >
              <Copy className="h-4 w-4" />
              <span>Copy approval link</span>
            </button>
            <button
              className={buttonStyle}
              onClick={() => {
                setPair(null);
                setStatus('Verification request cancelled. It will expire automatically.');
              }}
            >
              Cancel
            </button>
          </div>

          <div className="pt-1">
            <label className="text-xs text-secondary mb-1 block">Approval URL (for manual sharing):</label>
            <input
              aria-label="Browser approval link"
              readOnly
              value={pair.link}
              className="block w-full bg-quinary/50 p-2 text-xs rounded-lg font-mono text-secondary select-all border border-white/5"
              onFocus={e => e.currentTarget.select()}
            />
          </div>

          <div className="flex items-center gap-2 pt-1 text-xs text-secondary">
            <Loader2 className="h-3.5 w-3.5 animate-spin text-key" />
            <span>Waiting for approval on your mobile device…</span>
          </div>
        </div>
      )}
    </section>
  );
}
