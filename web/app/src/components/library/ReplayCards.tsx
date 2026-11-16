import { useMemo } from 'react';
import { LogoMark } from '@/components/icons/LogoMark';
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
        gradient: 'from-[#5E1C5C] to-[#2E1030]',
        border: 'border-[#8e2a8c]/25',
      },
      {
        value: topSong?.title || 'Discovering music',
        label: 'TOP SONG',
        detail: topSong ? `${topSong.subtitle} · ${topSong.plays} Plays` : 'Keep listening to reveal',
        gradient: 'from-[#6B2466] to-[#321234]',
        border: 'border-[#9c3595]/25',
      },
      {
        value: topArtist?.title || 'Your favorite sound',
        label: 'TOP ARTIST',
        detail: topArtist ? `${Math.round(topArtist.listenedMs / 60000)} min · ${topArtist.plays} Plays` : 'Plays will rank artists',
        gradient: 'from-[#2F4B49] to-[#17221F]',
        border: 'border-[#436b68]/25',
      },
      {
        value: topAlbum?.title || 'Featured albums',
        label: 'TOP ALBUM',
        detail: topAlbum ? `${topAlbum.subtitle} · ${Math.round(topAlbum.listenedMs / 60000)} min` : 'Album plays will appear here',
        gradient: 'from-[#15558A] to-[#0B2A47]',
        border: 'border-[#2071b5]/25',
      },
    ],
    [minutes, summary.totalPlays, topSong, topArtist, topAlbum],
  );

  return (
    <div className="relative -mx-(--body-gutter) px-(--body-gutter)">
      <div className="flex gap-3.5 overflow-x-auto pb-2 pt-1 scrollbar-none snap-x snap-mandatory">
        {cards.map((card, i) => (
          <div
            key={i}
            onClick={() => onOpenStory(i)}
            role="button"
            tabIndex={0}
            className={`group relative flex h-[190px] w-[280px] shrink-0 cursor-pointer snap-start flex-col justify-between overflow-hidden rounded-[24px] bg-gradient-to-br ${card.gradient} border ${card.border} p-5 shadow-[0_12px_28px_-8px_rgba(0,0,0,0.5)] transition-all duration-300 hover:scale-[1.02] hover:shadow-[0_18px_36px_-6px_rgba(0,0,0,0.65)] sm:w-[320px]`}
          >
            {/* Top row */}
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold tracking-[1.6px] text-white/80 uppercase sm:text-[11px]">
                Your listening experience
              </span>
              <LogoMark size={22} className="text-white/80 transition-transform duration-300 group-hover:scale-110" />
            </div>

            {/* Middle value & label */}
            <div className="my-auto">
              <p className="line-clamp-2 text-2xl font-black leading-tight text-white drop-shadow-[0_4px_8px_rgba(0,0,0,0.5)] sm:text-[28px]">
                {card.value}
              </p>
              <span className="mt-0.5 inline-block text-[10px] font-bold tracking-[2px] text-white/70 uppercase sm:text-[11px]">
                {card.label}
              </span>
            </div>

            {/* Bottom info */}
            <div className="flex items-end justify-between">
              <div className="min-w-0 pr-2">
                <p className="truncate text-xs font-bold tracking-[1.4px] text-white uppercase sm:text-[13px]">
                  {userName}
                </p>
                <p className="truncate text-[10px] text-white/70 sm:text-[11px]">{card.detail}</p>
              </div>
              <div className="shrink-0 text-right">
                <span className="block text-[8px] font-bold tracking-[1.2px] text-white/70 uppercase">
                  Member since
                </span>
                <span className="text-sm font-bold tracking-[1.8px] text-white sm:text-[15px]">
                  {memberSince}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
