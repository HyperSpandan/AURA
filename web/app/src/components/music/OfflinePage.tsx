import { ArrowDownToLine, RefreshCw, WifiOff } from 'lucide-react';
import { useState } from 'react';
import { PrefetchLink } from '@/components/PrefetchLink';

export function OfflinePage() {
  const [retrying, setRetrying] = useState(false);

  const handleRetry = () => {
    setRetrying(true);
    setTimeout(() => {
      window.location.reload();
    }, 400);
  };

  return (
    <div className="mx-auto my-16 flex max-w-[500px] flex-col items-center px-(--body-gutter) text-center animate-fade-in">
      {/* Offline emblem with ambient amber glow */}
      <div className="relative mb-6">
        <div className="absolute -inset-4 rounded-full bg-amber-500/15 blur-xl pointer-events-none" aria-hidden="true" />
        <div className="relative grid size-20 place-items-center rounded-3xl border border-white/10 bg-white/[0.05] shadow-[0_8px_32px_rgba(0,0,0,0.4)] backdrop-blur-xl">
          <WifiOff size={36} className="text-amber-400/90" strokeWidth={2.2} />
        </div>
      </div>

      <h1 className="type-title-1-emphasized text-2xl font-bold tracking-tight text-primary">
        You’re offline
      </h1>
      <p className="mt-3 type-body-tall text-secondary leading-relaxed">
        Connect to the internet to stream music, browse charts, and explore artists.
        Your downloaded tracks are ready to listen to offline right now.
      </p>

      {/* Primary and secondary actions */}
      <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
        <PrefetchLink
          to="/downloads"
          className="ot-press inline-flex h-11 items-center justify-center gap-2 rounded-[10px] bg-key-bg px-6 type-body-emphasized text-white shadow-[0_8px_24px_-6px_rgba(0,0,0,0.5)] transition-all hover:brightness-110 active:scale-95"
        >
          <ArrowDownToLine size={18} strokeWidth={2.2} />
          <span>Go to Downloads</span>
        </PrefetchLink>

        <button
          onClick={handleRetry}
          disabled={retrying}
          className="ot-press inline-flex h-11 items-center justify-center gap-2 rounded-[10px] border border-white/10 bg-white/[0.06] px-5 type-body text-primary hover:bg-white/[0.12] active:scale-95 transition-all disabled:opacity-50"
        >
          <RefreshCw size={16} strokeWidth={2} className={retrying ? 'animate-spin' : ''} />
          <span>{retrying ? 'Connecting…' : 'Try Again'}</span>
        </button>
      </div>
    </div>
  );
}
