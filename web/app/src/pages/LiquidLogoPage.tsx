import { useRef } from 'react';
import { RotateCcw } from 'lucide-react';
import { LiquidGlassLogo, type LiquidGlassLogoHandle } from '@/components/liquid-glass-logo';

/** Showcases AURA's Liquid Glass Logo entrance animation on a pure black canvas. */
export default function LiquidLogoPage() {
  const logoRef = useRef<LiquidGlassLogoHandle>(null);

  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center bg-black px-6 py-12">
      <div className="w-full max-w-[640px] overflow-hidden rounded-2xl bg-black">
        <LiquidGlassLogo
          ref={logoRef}
          logo={`${import.meta.env.BASE_URL}splash-logo.png`}
          logoColor="original"
          ending="logo"
          style={{ height: 480 }}
          aria-label="AURA"
        />
      </div>
      <button
        type="button"
        onClick={() => logoRef.current?.replay()}
        className="mt-6 inline-flex items-center gap-2 rounded-lg border border-white/15 bg-white/10 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/20"
      >
        <RotateCcw className="h-4 w-4" />
        Replay animation
      </button>
    </div>
  );
}
