import { useMemo } from 'react';
import { LogoMark } from '@/components/icons/LogoMark';
import { GlassCard } from '@/components/liquid-glass/glass-card';
import type { ReplaySummary } from '@/data/history/historyStore';

interface ReplayCardsProps {
  summary: ReplaySummary;
  userName: string;
  memberSince?: string;
  onOpenStory: (slideIndex?: number) => void;
}

export function ReplayCards({
  summary,
  userName,
  memberSince = '11/26',
  onOpenStory,
}: ReplayCardsProps) {
  const minutes = Math.round(summary.listenedMs / 60000);
  const topSong = summary.topSongs[0];
  const topArtist = summary.topArtists[0];
  const topAlbum = summary.topAlbums[0];

  const cards = useMemo(
    () => [
      {
        value: `${minutes}`,
        label: 'MINUTES LISTENED',
        detail: `${summary.totalPlays} Plays · 2026`,
        accent: '#ff375f',
      },
      {
        value: topSong?.title || 'Discovering music',
        label: 'TOP SONG',
        detail: topSong ? `${topSong.subtitle} · ${topSong.plays} Plays` : 'Keep listening to reveal',
        accent: '#bf5af2',
      },
      {
        value: topArtist?.title || 'Your favorite sound',
        label: 'TOP ARTIST',
        detail: topArtist ? `${Math.round(topArtist.listenedMs / 60000)} min · ${topArtist.plays} Plays` : 'Plays will rank artists',
        accent: '#30d158',
      },
      {
        value: topAlbum?.title || 'Featured albums',
        label: 'TOP ALBUM',
        detail: topAlbum ? `${topAlbum.subtitle} · ${Math.round(topAlbum.listenedMs / 60000)} min` : 'Album plays will appear here',
        accent: '#0a84ff',
      },
    ],
    [minutes, summary.totalPlays, topSong, topArtist, topAlbum],
  );

  return (
    <div className="relative">
      <div className="flex gap-3.5 overflow-x-auto px-(--body-gutter) scroll-px-(--body-gutter) pb-2 pt-1 scrollbar-none snap-x snap-mandatory sm:gap-4">
        {cards.map((card, i) => (
          <GlassCard
            key={i}
            onClick={() => onOpenStory(i)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onOpenStory(i);
              }
            }}
            role="button"
            tabIndex={0}
            className="group flex h-[184px] w-[264px] shrink-0 cursor-pointer snap-start flex-col justify-between p-4 transition-transform duration-200 hover:scale-[1.015] sm:w-[292px] sm:p-5"
          >
            {/* Top row */}
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-semibold tracking-[1.4px] uppercase opacity-70 sm:text-[11px]">
                Your listening experience
              </span>
              <LogoMark size={20} className="opacity-80 transition-transform duration-300 group-hover:scale-110" />
            </div>

            {/* Middle value & label */}
            <div className="my-auto">
              <p className="line-clamp-2 text-2xl font-bold leading-tight tracking-[-0.02em] sm:text-[26px]">
                {card.value}
              </p>
              <span
                className="mt-1 inline-block text-[10px] font-semibold tracking-[1.6px] uppercase sm:text-[11px]"
                style={{ color: card.accent }}
              >
                {card.label}
              </span>
            </div>

            {/* Bottom info */}
            <div className="flex items-end justify-between">
              <div className="min-w-0 pr-2">
                <p className="truncate text-xs font-semibold tracking-[1.2px] uppercase sm:text-[13px]">
                  {userName}
                </p>
                <p className="truncate text-[10px] opacity-65 sm:text-[11px]">{card.detail}</p>
              </div>
              <div className="shrink-0 text-right">
                <span className="block text-[8px] font-semibold tracking-[1.2px] uppercase opacity-60">
                  Member since
                </span>
                <span className="text-sm font-semibold tracking-[1.4px] tabular-nums sm:text-[15px]">
                  {memberSince}
                </span>
              </div>
            </div>
          </GlassCard>
        ))}
      </div>
    </div>
  );
}
