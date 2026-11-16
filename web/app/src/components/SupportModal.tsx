import { ArrowUpRight, Check, Copy, ExternalLink, Heart, Sparkles, X } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { SiGithub } from 'react-icons/si';
import { useUi } from '@/state/ui';

export const UPI_ID = 'auramusic@slc';
export const DEVELOPER_NAME = 'Spandan Dhage';
export const GITHUB_USERNAME = 'HyperSpandan';
export const GITHUB_REPO = 'https://github.com/HyperSpandan/AURA';
export const GITHUB_PROFILE = 'https://github.com/HyperSpandan';

export function upiPayLink(amount?: number): string {
  const am = amount ? `&am=${amount.toFixed(2)}` : '';
  return `upi://pay?pa=${UPI_ID}&pn=AURA${am}&cu=INR&tn=Support%20AURA`;
}

export function SupportModal() {
  const open = useUi((s) => s.supportOpen);
  const setOpen = useUi((s) => s.setSupportOpen);
  const [copied, setCopied] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open) {
      if (!dialog.open) dialog.showModal();
    } else {
      if (dialog.open) dialog.close();
    }
  }, [open]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(UPI_ID);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  if (!open) return null;

  return createPortal(
    <dialog
      ref={dialogRef}
      className="fixed inset-x-4 top-1/2 -translate-y-1/2 mx-auto my-0 max-w-lg w-full rounded-3xl bg-[#1c1c1e] text-[#f6ede9] border border-white/10 shadow-2xl p-0 overflow-hidden z-[10005] backdrop:bg-black/60 backdrop:backdrop-blur-md"
      onCancel={(e) => {
        e.preventDefault();
        setOpen(false);
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) setOpen(false);
      }}
    >
      <div className="flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-6 pb-4 border-b border-white/10">
          <div className="flex items-center gap-3">
            <span className="grid size-10 place-items-center rounded-2xl bg-[#ff2d55]/15 text-[#ff2d55] border border-[#ff2d55]/30">
              <Heart size={20} className="fill-current" />
            </span>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white">Support AURA</h2>
              <p className="text-xs text-secondary">Free · Ad-Free · Open Source</p>
            </div>
          </div>
          <button
            onClick={() => setOpen(false)}
            className="grid size-9 place-items-center rounded-full bg-white/10 text-secondary hover:text-white hover:bg-white/15 transition-colors"
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto px-6 py-5 space-y-5 text-sm">
          {/* Creator Credits Badge */}
          <div className="flex items-center justify-between rounded-2xl bg-white/5 border border-white/10 p-4">
            <div className="flex items-center gap-3">
              <img
                src={`https://github.com/${GITHUB_USERNAME}.png?size=80`}
                alt={DEVELOPER_NAME}
                className="size-11 rounded-full border border-white/20 object-cover bg-black/40"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
              <div>
                <div className="flex items-center gap-1.5 font-semibold text-white">
                  <span>{DEVELOPER_NAME}</span>
                  <span className="text-xs font-normal text-secondary">(@{GITHUB_USERNAME})</span>
                </div>
                <p className="text-xs text-secondary mt-0.5">Creator &amp; Lead Developer of AURA</p>
              </div>
            </div>
            <a
              href={GITHUB_PROFILE}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 rounded-pill bg-white/10 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/20 transition-colors"
            >
              <SiGithub size={13} />
              <span>GitHub</span>
              <ArrowUpRight size={12} />
            </a>
          </div>

          <p className="text-secondary leading-relaxed">
            AURA is built with love and offered completely free with zero ads, zero trackers, and no subscription walls.
            Your support helps maintain FLAC lossless stream adapters, lyrics bridges, and live sync relays.
          </p>

          {/* UPI Support Option */}
          <div className="rounded-2xl bg-white/5 border border-white/10 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-semibold text-white flex items-center gap-1.5">
                <Sparkles size={16} className="text-amber-400" />
                Support via UPI (India)
              </span>
              <span className="text-xs font-mono text-secondary">GPay · PhonePe · Paytm</span>
            </div>

            <div className="flex items-center justify-between gap-2 rounded-xl bg-black/40 border border-white/10 p-2.5">
              <code className="font-mono text-sm font-semibold text-amber-400 px-1">{UPI_ID}</code>
              <button
                onClick={handleCopy}
                className="inline-flex items-center gap-1.5 rounded-lg bg-white/10 px-3 py-1.5 text-xs font-medium text-white hover:bg-white/20 transition-colors"
              >
                {copied ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} />}
                <span>{copied ? 'Copied!' : 'Copy ID'}</span>
              </button>
            </div>

            {/* Quick Amount Pills */}
            <div className="space-y-1.5">
              <span className="text-xs text-secondary block">Quick contribution:</span>
              <div className="grid grid-cols-4 gap-2">
                {[49, 99, 199, 499].map((amount) => (
                  <a
                    key={amount}
                    href={upiPayLink(amount)}
                    className="flex flex-col items-center justify-center py-2 rounded-xl bg-white/5 hover:bg-[#ff2d55]/15 border border-white/10 hover:border-[#ff2d55]/30 transition-colors text-center"
                  >
                    <span className="font-semibold text-white text-sm">₹{amount}</span>
                    <span className="text-[10px] text-secondary">Pay</span>
                  </a>
                ))}
              </div>
            </div>

            <a
              href={upiPayLink()}
              className="block w-full py-2.5 text-center rounded-xl bg-[#ff2d55] text-white font-semibold text-xs hover:brightness-110 transition-all shadow-md"
            >
              Open in any UPI app
            </a>
          </div>

          {/* GitHub Star Option */}
          <div className="flex items-center justify-between rounded-2xl bg-white/5 border border-white/10 p-4">
            <div>
              <p className="font-semibold text-white">Star &amp; Share AURA</p>
              <p className="text-xs text-secondary">Starring on GitHub helps more music listeners discover the app.</p>
            </div>
            <a
              href={GITHUB_REPO}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 rounded-pill bg-white/10 px-3.5 py-2 text-xs font-medium text-white hover:bg-white/20 transition-colors"
            >
              <SiGithub size={14} />
              <span>Star</span>
              <ExternalLink size={12} />
            </a>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-white/[0.02] border-t border-white/10 flex items-center justify-end">
          <button
            onClick={() => setOpen(false)}
            className="rounded-pill bg-white/10 hover:bg-white/20 px-5 py-2 text-xs font-semibold text-white transition-colors"
          >
            Done
          </button>
        </div>
      </div>
    </dialog>,
    document.body,
  );
}
