import { lazy, Suspense, useState } from 'react';
import { GlassCard } from '@/components/liquid-glass/glass-card';
import { useUi, type SidePanel as Panel } from '@/state/ui';
import { motionAllowed } from './playerMorph';
import { DevicesPanel } from '@/connect/DevicesPanel';
import { UpNext } from './UpNext';

// am-lyrics is large; it loads the first time the panel opens (prompt §3).
const LyricsPanel = lazy(() => import('./LyricsPanel').then((m) => ({ default: m.LyricsPanel })));

/** Up Next / Lyrics / Devices glass panel (report §7). */
export function SidePanel() {
  const panel = useUi((s) => s.sidePanel);
  // Hidden under the full-screen player, which has its own lyrics: one am-lyrics at a time.
  const fullScreen = useUi((s) => s.fullScreen);
  // The panel last shown stays on screen while it slides back out, rather than vanishing.
  const [shown, setShown] = useState<Panel>(panel);
  if (panel && panel !== shown) setShown(panel);
  // With motion off no animation runs, so nothing would ever end it; under the full player
  // it can't be seen leaving. Either way it goes at once.
  if (!panel && shown && (fullScreen || !motionAllowed())) setShown(null);
  const leaving = !panel && shown !== null;
  if (!shown || fullScreen) return null;

  return (
    <GlassCard
      role="complementary"
      live
      blur={6}
      tint={0.68}
      className={`absolute top-2.5 right-2.5 bottom-2.5 z-(--z-web-chrome) flex w-(--side-panel-width) flex-col rounded-[24px] max-sm:inset-0 max-sm:w-full max-sm:rounded-none ${leaving ? 'pointer-events-none animate-slide-out' : 'animate-slide-in'}`}
      aria-label={shown === 'up-next' ? 'Up Next' : shown === 'devices' ? 'Devices' : 'Lyrics'}
      aria-hidden={leaving || undefined}
      onAnimationEnd={(e) => {
        if (leaving && e.target === e.currentTarget) setShown(null);
      }}
    >
      {shown === 'up-next' ? (
        <UpNext />
      ) : shown === 'devices' ? (
        <DevicesPanel />
      ) : (
        <Suspense fallback={null}>
          <LyricsPanel />
        </Suspense>
      )}
    </GlassCard>
  );
}
