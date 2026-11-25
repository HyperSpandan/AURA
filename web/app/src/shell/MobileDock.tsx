import { Compass, Home, Library, Search, Settings } from 'lucide-react';
import { NavLink, useLocation } from 'react-router';
import { PauseIcon, PlayIcon, ForwardIcon } from '@/components/icons/Transport';
import { sizedArtwork } from '@/lib/artwork';
import { usePlayer } from '@/state/player';
import { useUi } from '@/state/ui';
import { GlassCard } from '@/components/liquid-glass/glass-card';
import { useControlling } from '@/connect/connect';
import { useShownNowPlaying } from '@/connect/shown';
const TABS = [[Home, '/home', 'Home'], [Compass, '/new', 'Explore'], [Library, '/library', 'Library'], [Search, '/search', 'Search'], [Settings, '/settings', 'Settings']] as const;

export function MobileDock() {
  const fullScreen = useUi(s => s.fullScreen);
  const remote = useControlling();
  const track = useShownNowPlaying();
  const playingHere = usePlayer(s => s.status === 'playing' || s.status === 'buffering');
  const playing = remote ? !!remote.state?.playing : playingHere;
  const { togglePlay, next } = usePlayer.getState();
  const { pathname } = useLocation();
  const tab = TABS.findIndex(([, to]) => pathname === to || pathname.startsWith(to + '/'));
  return <div className={`ot-mobile-dock ${fullScreen ? 'opacity-0 pointer-events-none invisible transition-opacity duration-300' : ''}`}>
    {track && <GlassCard blur={6} tint={0.65} className="ot-mini rounded-[22px]" aria-label="Mini player" data-player-origin>
      <button className="ot-mini-track" onClick={() => useUi.getState().setFullScreen(true)} aria-label={`Open player: ${track.title}`}>
        {track.artworkUrl && <img src={sizedArtwork(track.artworkUrl, 96)} alt="" data-cover-origin />}
        <span key={track.videoId} className="ot-swap-in"><strong>{track.title}</strong><small>{track.artist}</small></span>
      </button>
      <button className="ot-press" aria-label={playing ? 'Pause' : 'Play'} onClick={togglePlay}>{playing ? <PauseIcon size={26} /> : <PlayIcon size={26} />}</button>
      <button className="ot-press" aria-label="Next" onClick={next}><ForwardIcon size={24} /></button>
    </GlassCard>}
    <GlassCard blur={6} tint={0.65} role="navigation" className="ot-bottom-nav rounded-[36px]" aria-label="Main navigation">
      <span className="ot-dock-pill" style={{ '--i': Math.max(0, tab) } as React.CSSProperties} data-none={tab < 0 || undefined} aria-hidden="true" />
      {TABS.map(([Glyph, to, name]) => <NavLink key={to} to={to}><Glyph size={21} /><span>{name}</span></NavLink>)}
    </GlassCard>
  </div>;
}
