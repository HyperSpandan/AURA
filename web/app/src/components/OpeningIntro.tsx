import { useEffect, useState } from 'react';
import { useLocation } from 'react-router';
import { LiquidGlassLogo } from '@/components/liquid-glass-logo';

const SESSION_KEY = 'aura_opening_played';

/** Plays the Liquid Glass Logo entrance once per browser session on launch. */
export function OpeningIntro() {
  const { pathname } = useLocation();
  const [visible, setVisible] = useState(() => {
    if (typeof window === 'undefined') return false;
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return false;
    try {
      if (sessionStorage.getItem(SESSION_KEY) === '1') return false;
      sessionStorage.setItem(SESSION_KEY, '1');
      return true;
    } catch {
      return false;
    }
  });
  const [fading, setFading] = useState(false);

  useEffect(() => {
    if (!visible) return;
    if (pathname === '/logo') {
      setVisible(false);
      return;
    }
    const fadeTimer = setTimeout(() => setFading(true), 3400);
    const hideTimer = setTimeout(() => setVisible(false), 3950);
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setFading(true);
        setTimeout(() => setVisible(false), 250);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(hideTimer);
      window.removeEventListener('keydown', onKey);
    };
  }, [visible, pathname]);

  if (!visible) return null;

  return (
    <div
      role="presentation"
      onClick={() => {
        setFading(true);
        setTimeout(() => setVisible(false), 250);
      }}
      className={`fixed inset-0 z-[9999] flex cursor-pointer items-center justify-center bg-black transition-opacity duration-500 ease-out ${
        fading ? 'pointer-events-none opacity-0' : 'opacity-100'
      }`}
    >
      <div className="w-full max-w-[520px] px-6">
        <LiquidGlassLogo
          logo={`${import.meta.env.BASE_URL}splash-logo.png`}
          logoColor="original"
          ending="logo"
          style={{ height: 420 }}
          aria-label="AURA"
        />
      </div>
    </div>
  );
}
