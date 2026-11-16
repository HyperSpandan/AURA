import { ChevronLeft, ChevronRight, Share2, X } from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { LogoMark } from '@/components/icons/LogoMark';
import { Artwork } from '@/components/music/Artwork';
import type { ReplaySummary } from '@/data/history/historyStore';

interface ReplayStoryModalProps {
  summary: ReplaySummary;
  userName: string;
  memberSince?: string;
  initialSlide?: number;
  onClose: () => void;
}

const PAGE_DURATION_MS = 6000;

export function ReplayStoryModal({
  summary,
  userName,
  memberSince = '11/26',
  initialSlide = 0,
  onClose,
}: ReplayStoryModalProps) {
  const [slide, setSlide] = useState(initialSlide);
  const [progress, setProgress] = useState(0);
  const [paused, setPaused] = useState(false);
  const [copied, setCopied] = useState(false);

  const minutes = Math.round(summary.listenedMs / 60000);
  const topSong = summary.topSongs[0];
  const topArtist = summary.topArtists[0];
  const topAlbum = summary.topAlbums[0];

  const totalSlides = 5;

  const nextSlide = useCallback(() => {
    setSlide((curr) => {
      if (curr < totalSlides - 1) {
        setProgress(0);
        return curr + 1;
      }
      onClose();
      return curr;
    });
  }, [totalSlides, onClose]);

  const prevSlide = useCallback(() => {
    setSlide((curr) => {
      setProgress(0);
      return Math.max(0, curr - 1);
    });
  }, []);

  useEffect(() => {
    if (paused) return;
    let animId: number;
    const start = Date.now() - progress * PAGE_DURATION_MS;

    const tick = () => {
      const elapsed = Date.now() - start;
      const ratio = Math.min(1, elapsed / PAGE_DURATION_MS);
      setProgress(ratio);
      if (ratio >= 1) {
        nextSlide();
      } else {
        animId = requestAnimationFrame(tick);
      }
    };

    animId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(animId);
  }, [slide, paused, nextSlide, progress]);

  // Keyboard navigation
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') nextSlide();
      else if (e.key === 'ArrowLeft') prevSlide();
      else if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [nextSlide, prevSlide, onClose]);

  const handleShare = async () => {
    const text = `🎵 My 2026 AURA Replay:\n• ${minutes} minutes listened\n• Top Song: ${topSong?.title ?? 'None'}\n• Top Artist: ${topArtist?.title ?? 'None'}\nStreamed on AURA Music`;
    if (navigator.share) {
      try {
        await navigator.share({ title: 'My AURA Replay', text });
        return;
      } catch {
        // Fall back to clipboard
      }
    }
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const slideGradients = [
    'from-[#5E1C5C] to-[#2E1030]',
    'from-[#6B2466] to-[#321234]',
    'from-[#2F4B49] to-[#17221F]',
    'from-[#15558A] to-[#0B2A47]',
    'from-[#4A154B] to-[#1B0C1E]',
  ];

  const collageThumbnails = useMemo(() => {
    const thumbs: string[] = [];
    for (const s of summary.topSongs) {
      if (s.thumbnailUrl && !thumbs.includes(s.thumbnailUrl)) {
        thumbs.push(s.thumbnailUrl);
      }
      if (thumbs.length === 4) break;
    }
    return thumbs;
  }, [summary.topSongs]);

  const modalContent = (
    <div
      className="fixed inset-0 z-[10010] flex items-center justify-center bg-black/85 p-4 backdrop-blur-2xl"
      role="dialog"
      aria-modal="true"
    >
      <div
        className={`relative flex h-[540px] max-h-[82vh] w-full max-w-[400px] flex-col justify-between overflow-hidden rounded-[28px] bg-gradient-to-br ${slideGradients[slide]} p-5 text-white shadow-[0_24px_70px_rgba(0,0,0,0.85)] border border-white/10`}
        onPointerDown={() => setPaused(true)}
        onPointerUp={() => setPaused(false)}
        onPointerLeave={() => setPaused(false)}
      >
        {/* Progress Bar Segments */}
        <div className="flex gap-1.5 pt-0.5">
          {Array.from({ length: totalSlides }).map((_, i) => (
            <div key={i} className="h-1 flex-1 overflow-hidden rounded-full bg-white/25">
              <div
                className="h-full bg-white transition-[width] ease-linear"
                style={{
                  width: i < slide ? '100%' : i === slide ? `${progress * 100}%` : '0%',
                }}
              />
            </div>
          ))}
        </div>

        {/* Story Header */}
        <div className="mt-2.5 flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold tracking-wider text-white/90">
            <span>Replay · 2026</span>
            <LogoMark size={16} className="text-white" />
            <span className="font-bold">AURA</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-7 place-items-center rounded-full bg-white/10 text-white/80 transition-colors hover:bg-white/20 hover:text-white"
            aria-label="Close"
          >
            <X size={16} />
          </button>
        </div>

        {/* Slide Content Area */}
        <div className="relative my-auto flex flex-1 flex-col justify-center py-2">
          {slide === 0 && (
            <div className="space-y-3 animate-fade-in">
              <span className="text-[11px] font-bold tracking-[2px] text-white/70 uppercase">Total Listening</span>
              <h2 className="text-2xl font-black leading-tight sm:text-3xl">
                You spent <span className="text-white underline decoration-pink-500/60 decoration-wavy underline-offset-4">{minutes} minutes</span> with music this year.
              </h2>
              <p className="text-sm font-medium text-white/80">
                Across <span className="font-bold text-white">{summary.totalPlays} plays</span>, {summary.songCount} distinct tracks, and {summary.artistCount} artists.
              </p>
              <div className="mt-3 rounded-2xl bg-white/10 p-3.5 backdrop-blur-md border border-white/10">
                <p className="text-[10px] font-semibold tracking-wider text-white/70 uppercase">Listener Profile</p>
                <p className="mt-0.5 text-base font-bold text-white">{userName}</p>
                <p className="text-[11px] text-white/60">Member since {memberSince}</p>
              </div>
            </div>
          )}

          {slide === 1 && (
            <div className="space-y-3 animate-fade-in">
              <span className="text-[11px] font-bold tracking-[2px] text-white/70 uppercase">Top Song</span>
              <h2 className="text-xl font-black leading-tight sm:text-2xl">
                Your #1 track of the year.
              </h2>
              {topSong ? (
                <div className="flex items-center gap-3.5 rounded-2xl bg-white/10 p-3 backdrop-blur-md border border-white/10">
                  <div className="size-14 shrink-0 overflow-hidden rounded-xl shadow-lg">
                    <Artwork src={topSong.thumbnailUrl} displayWidth={56} radius="10px" eager />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-base font-bold text-white">{topSong.title}</p>
                    <p className="truncate text-xs text-white/80">{topSong.subtitle}</p>
                    <p className="mt-0.5 text-[11px] font-semibold text-pink-300">{topSong.plays} plays</p>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-white/70">Keep listening to reveal your top song!</p>
              )}

              {/* Runners up */}
              {summary.topSongs.length > 1 && (
                <div className="mt-2 space-y-1.5">
                  <p className="text-[10px] font-bold tracking-wider text-white/60 uppercase">Runners Up</p>
                  {summary.topSongs.slice(1, 4).map((song, idx) => (
                    <div key={idx} className="flex items-center gap-2.5 text-xs">
                      <span className="w-3.5 font-bold text-white/50">{idx + 2}</span>
                      <div className="size-7 shrink-0 overflow-hidden rounded-md">
                        <Artwork src={song.thumbnailUrl} displayWidth={28} radius="5px" />
                      </div>
                      <span className="truncate flex-1 font-medium text-white">{song.title}</span>
                      <span className="text-white/60 text-[11px]">{song.plays}p</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {slide === 2 && (
            <div className="space-y-3 animate-fade-in">
              <span className="text-[11px] font-bold tracking-[2px] text-white/70 uppercase">Top Artist</span>
              <h2 className="text-xl font-black leading-tight sm:text-2xl">
                Your soundtrack was led by:
              </h2>
              {topArtist ? (
                <div className="flex items-center gap-3.5 rounded-2xl bg-white/10 p-3 backdrop-blur-md border border-white/10">
                  <div className="size-14 shrink-0 overflow-hidden rounded-full shadow-lg border border-white/20">
                    <Artwork src={topArtist.thumbnailUrl} displayWidth={56} radius="50%" eager />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-base font-bold text-white">{topArtist.title}</p>
                    <p className="mt-0.5 text-[11px] font-semibold text-teal-300">
                      {Math.round(topArtist.listenedMs / 60000)} min · {topArtist.plays} plays
                    </p>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-white/70">Artists will rank as you play more tracks.</p>
              )}

              {summary.topArtists.length > 1 && (
                <div className="mt-2 space-y-1.5">
                  <p className="text-[10px] font-bold tracking-wider text-white/60 uppercase">Other Favorites</p>
                  {summary.topArtists.slice(1, 4).map((art, idx) => (
                    <div key={idx} className="flex items-center gap-2.5 text-xs">
                      <span className="w-3.5 font-bold text-white/50">{idx + 2}</span>
                      <div className="size-7 shrink-0 overflow-hidden rounded-full">
                        <Artwork src={art.thumbnailUrl} displayWidth={28} radius="50%" />
                      </div>
                      <span className="truncate flex-1 font-medium text-white">{art.title}</span>
                      <span className="text-white/60 text-[11px]">{art.plays}p</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {slide === 3 && (
            <div className="space-y-3 animate-fade-in">
              <span className="text-[11px] font-bold tracking-[2px] text-white/70 uppercase">Top Album</span>
              <h2 className="text-xl font-black leading-tight sm:text-2xl">
                Your album on repeat:
              </h2>
              {topAlbum ? (
                <div className="flex items-center gap-3.5 rounded-2xl bg-white/10 p-3 backdrop-blur-md border border-white/10">
                  <div className="size-14 shrink-0 overflow-hidden rounded-xl shadow-lg">
                    <Artwork src={topAlbum.thumbnailUrl} displayWidth={56} radius="10px" eager />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-base font-bold text-white">{topAlbum.title}</p>
                    <p className="truncate text-xs text-white/80">{topAlbum.subtitle}</p>
                    <p className="mt-0.5 text-[11px] font-semibold text-blue-300">
                      {Math.round(topAlbum.listenedMs / 60000)} minutes listened
                    </p>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-white/70">Album stats will appear as full records play.</p>
              )}

              {summary.topAlbums.length > 1 && (
                <div className="mt-2 space-y-1.5">
                  <p className="text-[10px] font-bold tracking-wider text-white/60 uppercase">Other Albums</p>
                  {summary.topAlbums.slice(1, 4).map((alb, idx) => (
                    <div key={idx} className="flex items-center gap-2.5 text-xs">
                      <span className="w-3.5 font-bold text-white/50">{idx + 2}</span>
                      <div className="size-7 shrink-0 overflow-hidden rounded-md">
                        <Artwork src={alb.thumbnailUrl} displayWidth={28} radius="5px" />
                      </div>
                      <span className="truncate flex-1 font-medium text-white">{alb.title}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {slide === 4 && (
            <div className="space-y-3 text-center animate-fade-in">
              <span className="text-[11px] font-bold tracking-[2px] text-white/70 uppercase">2026 Summary</span>
              <h2 className="text-xl font-black text-white sm:text-2xl">Your Year in Music</h2>

              {/* 2x2 Artwork Collage */}
              {collageThumbnails.length > 0 && (
                <div className="mx-auto size-28 overflow-hidden rounded-2xl border border-white/20 shadow-xl grid grid-cols-2 grid-rows-2">
                  {collageThumbnails.map((src, i) => (
                    <img key={i} src={src} alt="" className="size-full object-cover" />
                  ))}
                </div>
              )}

              <div className="grid grid-cols-2 gap-2 text-left">
                <div className="rounded-xl bg-white/10 p-2.5">
                  <span className="text-[9px] font-bold text-white/60 uppercase">Minutes</span>
                  <p className="text-sm font-bold text-white">{minutes}</p>
                </div>
                <div className="rounded-xl bg-white/10 p-2.5">
                  <span className="text-[9px] font-bold text-white/60 uppercase">Top Artist</span>
                  <p className="truncate text-sm font-bold text-white">{topArtist?.title || 'None'}</p>
                </div>
                <div className="rounded-xl bg-white/10 p-2.5">
                  <span className="text-[9px] font-bold text-white/60 uppercase">Top Song</span>
                  <p className="truncate text-sm font-bold text-white">{topSong?.title || 'None'}</p>
                </div>
                <div className="rounded-xl bg-white/10 p-2.5">
                  <span className="text-[9px] font-bold text-white/60 uppercase">User</span>
                  <p className="truncate text-sm font-bold text-white">{userName}</p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Tap targets for previous / next */}
        <div
          className="absolute inset-y-14 left-0 w-1/3 cursor-pointer"
          onClick={prevSlide}
          title="Previous"
        />
        <div
          className="absolute inset-y-14 right-0 w-2/3 cursor-pointer"
          onClick={nextSlide}
          title="Next"
        />

        {/* Bottom controls */}
        <div className="relative z-10 flex items-center justify-between pt-1">
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={prevSlide}
              disabled={slide === 0}
              className="grid size-8 place-items-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20 disabled:opacity-30"
              aria-label="Previous page"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              type="button"
              onClick={nextSlide}
              className="grid size-8 place-items-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20"
              aria-label="Next page"
            >
              <ChevronRight size={18} />
            </button>
          </div>

          <button
            type="button"
            onClick={handleShare}
            className="inline-flex h-8 items-center gap-1.5 rounded-full bg-white/15 px-3 text-xs font-semibold text-white transition-colors hover:bg-white/25"
          >
            <Share2 size={14} />
            <span>{copied ? 'Copied!' : 'Share'}</span>
          </button>
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}
