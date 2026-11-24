import { useVirtualizer } from '@tanstack/react-virtual';
import { ArrowDown, ArrowDownToLine, ArrowUp, GripVertical } from 'lucide-react';
import { type KeyboardEvent, type PointerEvent, useEffect, useRef, useState } from 'react';
import { PrefetchLink } from '@/components/PrefetchLink';
import { PauseIcon, PlayIcon } from '@/components/icons/Transport';
import { useDownloads } from '@/data/download/useDownloads';
import type { ArtistRef, Track } from '@/data/music/types';
import { usePlayer } from '@/state/player';
import { Artwork } from './Artwork';
import { ExplicitBadge } from './ExplicitBadge';
import { albumHref, artistHref, formatDuration } from './links';
import { playList, playTrack, type ListContext } from './play';
import { TrackMenu } from './TrackMenu';

type Variant = 'album' | 'playlist';

const ROW_HEIGHT: Record<Variant, number> = { album: 46, playlist: 54 };
/** Below this, plain rows: an album or a short playlist gains nothing from virtualizing. */
const VIRTUALIZE_FROM = 80;

interface TrackListProps {
  tracks: Track[];
  variant: Variant;
  /** Album artwork, for rows that have none of their own (album tracks). */
  fallbackArtwork?: string | null;
  /** The album's artists, credited when a track has none of its own. */
  fallbackArtists?: ArtistRef[];
  /** Called when the list nears its end, to load the next page of a long playlist. */
  onNearEnd?: () => void;
  /** Where the list comes from: playing a row queues the list from there. Without it, a row plays alone. */
  source?: ListContext['source'];
  /** Enables drag-and-drop / arrow reordering for editable playlists. */
  reordering?: boolean;
  onMove?: (fromIndex: number, toIndex: number) => void;
}

/* Track lists (report §3 album rows, §14 playlist rows). */

const cell = 'flex h-full min-w-0 items-center';
// 396 / 274 / 274 / 141 at 1440px (report §14).
const playlistColumns = 'grid-cols-[minmax(0,36.5fr)_minmax(0,25.25fr)_minmax(0,25.25fr)_minmax(0,13fr)]';
// Artist and album: playlists only, and not below 768px.
const secondary = `${cell} pe-2.5 max-md:hidden`;
const tertiary = `${cell} pe-4 max-md:hidden`;
// A row from another catalogue with no YouTube Music song (yet) is dimmed.
const dimmed = 'group-data-unavailable/row:opacity-45';
const link = 'text-inherit hover:underline group-data-current/row:text-primary';

/**
 * A hairline on top of every row, hidden on hover and around the playing row;
 * a fill on hover, a stronger one for the playing row.
 */
const row = [
  'group/row relative grid items-center bg-transparent transition-[background-color] duration-150 ease-out hover:bg-tracklist-hover hover:duration-0 data-unavailable:cursor-default',
  'data-current:bg-[color-mix(in_srgb,var(--systemPrimary)_12%,transparent)]',
  'before:pointer-events-none before:absolute before:inset-x-0 before:top-0 before:border-t-[0.5px] before:border-t-divider',
  'hover:before:border-t-transparent data-current:before:border-t-transparent [&:hover+&]:before:border-t-transparent [&[data-current]+&]:before:border-t-transparent',
].join(' ');

const rowVariant: Record<Variant, string> = {
  album: 'h-[46px] grid-cols-[minmax(0,1fr)_auto] rounded-row',
  playlist: `h-[54px] ${playlistColumns} rounded-[12px] max-md:grid-cols-[minmax(0,1fr)_auto]`,
};

function Artists({ artists }: { artists: ArtistRef[] }) {
  return (
    <>
      {artists.map((artist, i) => {
        const href = artistHref(artist.id);
        return (
          <span key={`${artist.name}-${i}`}>
            {i > 0 && ', '}
            {href ? (
              <PrefetchLink to={href} className={link}>
                {artist.name}
              </PrefetchLink>
            ) : (
              artist.name
            )}
          </span>
        );
      })}
    </>
  );
}

const time: Record<Variant, string> = {
  album: 'relative justify-end pe-3 type-body tabular-nums',
  playlist: 'relative justify-end pe-[18px] type-body tabular-nums',
};

interface RowProps {
  track: Track;
  variant: Variant;
  position: number;
  total: number;
  fallbackArtwork: string | null | undefined;
  onPlayRow: () => void;
  reordering?: boolean;
  dragActive?: boolean;
  dragging?: boolean;
  shiftY?: number;
  onMove?: (fromIndex: number, toIndex: number) => void;
  onDragPointerDown?: (index: number, e: PointerEvent<HTMLElement>) => void;
  onDragPointerMove?: (e: PointerEvent<HTMLElement>) => void;
  onDragPointerUp?: (e: PointerEvent<HTMLElement>) => void;
  onDragPointerCancel?: () => void;
}

function Row({
  track,
  variant,
  position,
  total,
  fallbackArtwork,
  onPlayRow,
  reordering,
  dragActive,
  dragging,
  shiftY = 0,
  onMove,
  onDragPointerDown,
  onDragPointerMove,
  onDragPointerUp,
  onDragPointerCancel,
}: RowProps) {
  const current = usePlayer((s) => s.nowPlaying?.videoId === track.videoId);
  const playing = usePlayer((s) => s.nowPlaying?.videoId === track.videoId && (s.status === 'playing' || s.status === 'buffering'));
  const togglePlay = usePlayer((s) => s.togglePlay);
  const isDownloaded = useDownloads((s) => s.isDownloaded(track.videoId));
  const unavailable = track.unavailable;
  const onPlay = () => (unavailable ? undefined : current ? togglePlay() : onPlayRow());
  const index = position - 1;

  const handleArrowKey = reordering && onMove
    ? (e: KeyboardEvent<HTMLElement>) => {
        if (e.key === 'ArrowUp' && index > 0) {
          e.preventDefault();
          e.stopPropagation();
          onMove(index, index - 1);
        } else if (e.key === 'ArrowDown' && index < total - 1) {
          e.preventDefault();
          e.stopPropagation();
          onMove(index, index + 1);
        }
      }
    : undefined;

  return (
    <div
      className={`${row} ${rowVariant[variant]}${dragging ? ' z-30 will-change-transform bg-[color-mix(in_srgb,var(--systemPrimary)_14%,rgb(24_24_28))] shadow-[0_12px_30px_rgb(0_0_0/0.5)] ring-1 ring-white/20' : ''}${reordering ? ' cursor-grab active:cursor-grabbing select-none touch-none focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-key/70' : ''}`}
      style={
        dragging
          ? undefined
          : dragActive
            ? {
                transform: shiftY !== 0 ? `translate3d(0, ${shiftY}px, 0)` : 'translate3d(0, 0, 0)',
                transition: 'transform 150ms cubic-bezier(0.2, 0, 0, 1)',
              }
            : undefined
      }
      data-variant={variant}
      data-current={current || undefined}
      data-unavailable={unavailable}
      title={unavailable === 'matching' ? 'Looking for this on YouTube Music…' : unavailable === 'missing' ? 'Not found on YouTube Music' : undefined}
      tabIndex={reordering ? 0 : undefined}
      onPointerDown={
        reordering
          ? (e) => {
              const target = e.target as HTMLElement;
              if (target.closest('button[data-arrow], a')) return;
              onDragPointerDown?.(index, e);
            }
          : undefined
      }
      onPointerMove={reordering ? (e) => onDragPointerMove?.(e) : undefined}
      onPointerUp={reordering ? (e) => onDragPointerUp?.(e) : undefined}
      onPointerCancel={reordering ? () => onDragPointerCancel?.() : undefined}
      onKeyDown={handleArrowKey}
      onDoubleClick={reordering ? undefined : onPlay}
    >
      <div className={`${cell} gap-3`}>
        {reordering && (
          <span
            className="ms-1.5 grid size-6 shrink-0 place-items-center text-secondary/80"
            aria-hidden="true"
          >
            <GripVertical size={16} />
          </span>
        )}
        <div className={`relative grid w-10 shrink-0 place-items-center${variant === 'playlist' && !reordering ? ' ms-[7px]' : ''}`}>
          {variant === 'playlist' ? (
            <>
              <Artwork src={track.thumbnail ?? fallbackArtwork ?? null} displayWidth={40} radius="var(--radius-artwork-small)" />
              {current && (
                // The playing row's marker: a still play glyph over the artwork (or where the number was).
                <span
                  className="absolute inset-0 grid place-items-center rounded-artwork-sm bg-[rgb(0_0_0/0.45)] text-white group-hover/row:invisible"
                  aria-label="Now playing"
                >
                  <PlayIcon size={18} />
                </span>
              )}
            </>
          ) : (
            <span className="type-body text-secondary group-hover/row:invisible">
              {current ? (
                <span className="grid place-items-center text-primary group-hover/row:invisible" aria-label="Now playing">
                  <PlayIcon size={14} />
                </span>
              ) : (
                (track.index ?? position)
              )}
            </span>
          )}
          <button
            className={`absolute inset-0 grid place-items-center rounded-artwork-sm opacity-0 group-hover/row:opacity-100 focus-visible:opacity-100 group-data-unavailable/row:hidden ${
              variant === 'playlist' ? 'bg-lockup-hover text-white' : 'text-primary'
            }`}
            aria-label={playing ? `Pause ${track.title}` : `Play ${track.title}`}
            onClick={onPlay}
            disabled={!!unavailable}
          >
            {playing ? <PauseIcon size={variant === 'playlist' ? 18 : 16} /> : <PlayIcon size={variant === 'playlist' ? 18 : 16} />}
          </button>
        </div>
        <div
          className={`flex min-w-0 flex-col ${dimmed} group-data-[unavailable=matching]/row:animate-matching motion-reduce:group-data-[unavailable=matching]/row:animate-none`}
        >
          <div className="flex min-w-0 items-center gap-1.5">
            <span className="truncate type-body text-primary">{track.title}</span>
            {track.explicit && <ExplicitBadge />}
            {isDownloaded && (
              <span
                className="inline-flex items-center text-emerald-500 shrink-0"
                title="Downloaded for offline playback"
              >
                <ArrowDownToLine size={13} strokeWidth={2.5} />
              </span>
            )}
          </div>
          {variant === 'album' && track.artists.length > 0 && (
            <div className="truncate type-callout">
              <Artists artists={track.artists} />
            </div>
          )}
        </div>
      </div>
      {variant === 'playlist' && (
        <>
          <div className={`${secondary} ${dimmed}`}>
            <span className="truncate type-body">
              <Artists artists={track.artists} />
            </span>
          </div>
          <div className={`${tertiary} ${dimmed}`}>
            <span className="truncate type-body">
              {track.album &&
                (albumHref(track.album.id) ? (
                  <PrefetchLink to={albumHref(track.album.id)!} className={link}>
                    {track.album.name}
                  </PrefetchLink>
                ) : (
                  track.album.name
                ))}
            </span>
          </div>
        </>
      )}
      <div className={`${cell} ${reordering ? 'relative justify-end pe-2' : time[variant]} ${dimmed}`}>
        {reordering && onMove ? (
          <div
            className="flex items-center gap-0.5"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              data-arrow="up"
              disabled={position <= 1}
              onClick={() => onMove(position - 1, position - 2)}
              aria-label={`Move ${track.title} up`}
              className="grid size-7 place-items-center rounded-lg text-secondary transition hover:bg-quinary hover:text-primary disabled:opacity-30"
            >
              <ArrowUp size={14} />
            </button>
            <button
              type="button"
              data-arrow="down"
              disabled={position >= total}
              onClick={() => onMove(position - 1, position)}
              aria-label={`Move ${track.title} down`}
              className="grid size-7 place-items-center rounded-lg text-secondary transition hover:bg-quinary hover:text-primary disabled:opacity-30"
            >
              <ArrowDown size={14} />
            </button>
          </div>
        ) : (
          <>
            {/* The ⋯ button (TrackMenu) takes the time's place on hover, as Apple's rows do. */}
            <span className={unavailable ? undefined : 'group-focus-within/row:invisible group-hover/row:invisible group-has-[[data-open]]/row:invisible'}>
              {unavailable === 'missing' ? 'Not found' : formatDuration(track.durationSec)}
            </span>
            {!unavailable && (
              <TrackMenu
                track={track}
                className="absolute end-2.5 hidden size-7 place-items-center rounded-full text-key group-focus-within/row:grid group-hover/row:grid hover:bg-quinary data-open:grid"
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Header() {
  return (
    <div className={`grid h-8 items-center ${playlistColumns} type-callout-emphasized text-secondary max-md:hidden`} aria-hidden="true">
      <div className={`${cell} gap-3 ps-[7px]`}>Song</div>
      <div className={secondary}>Artist</div>
      <div className={tertiary}>Album</div>
      <div className={`${cell} ${time.playlist}`}>Time</div>
    </div>
  );
}

/**
 * Album and playlist track lists (report §3, §14): hairline-divided rows,
 * hover fill, the number (or artwork) turning into a play button. Long lists
 * are virtualized against the page's scroll container (prompt §3).
 */
export function TrackList({ tracks, variant, fallbackArtwork, fallbackArtists, onNearEnd, source, reordering, onMove }: TrackListProps) {
  const rowHeight = ROW_HEIGHT[variant];
  const container = useRef<HTMLDivElement>(null);
  const [scrollElement, setScrollElement] = useState<HTMLElement | null>(null);
  useEffect(() => setScrollElement(document.getElementById('scrollable-page')), []);

  const dragRef = useRef<{
    el: HTMLElement;
    fromIndex: number;
    startY: number;
    startScrollTop: number;
    scrollRect: DOMRect | null;
    hoverIndex: number;
    dy: number;
    rafId: number | null;
  } | null>(null);
  const [dragVisual, setDragVisual] = useState<{
    fromIndex: number;
    hoverIndex: number;
  } | null>(null);

  useEffect(() => {
    if (!reordering && dragRef.current) {
      if (dragRef.current.rafId !== null) cancelAnimationFrame(dragRef.current.rafId);
      dragRef.current.el.style.transform = '';
      dragRef.current = null;
      setDragVisual(null);
    }
  }, [reordering]);

  const handleDragPointerDown = (index: number, e: PointerEvent<HTMLElement>) => {
    if (!reordering || e.button !== 0) return;
    e.preventDefault();
    const el = e.currentTarget;
    el.setPointerCapture(e.pointerId);
    el.style.transform = 'translate3d(0, 0, 0) scale(1.012)';
    dragRef.current = {
      el,
      fromIndex: index,
      startY: e.clientY,
      startScrollTop: scrollElement?.scrollTop ?? 0,
      scrollRect: scrollElement ? scrollElement.getBoundingClientRect() : null,
      hoverIndex: index,
      dy: 0,
      rafId: null,
    };
    setDragVisual({ fromIndex: index, hoverIndex: index });
  };

  const handleDragPointerMove = (e: PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag) return;
    if (scrollElement && drag.scrollRect) {
      const edge = 72;
      if (e.clientY < drag.scrollRect.top + edge) {
        scrollElement.scrollTop = Math.max(0, scrollElement.scrollTop - 10);
      } else if (e.clientY > drag.scrollRect.bottom - edge) {
        scrollElement.scrollTop += 10;
      }
    }
    const scrollDelta = (scrollElement?.scrollTop ?? 0) - drag.startScrollTop;
    const dy = e.clientY - drag.startY + scrollDelta;
    drag.dy = dy;
    if (drag.rafId === null) {
      drag.rafId = requestAnimationFrame(() => {
        const current = dragRef.current;
        if (!current) return;
        current.rafId = null;
        current.el.style.transform = `translate3d(0, ${current.dy}px, 0) scale(1.012)`;
      });
    }
    const rawTarget = drag.fromIndex + Math.round(dy / rowHeight);
    const hoverIndex = Math.max(0, Math.min(tracks.length - 1, rawTarget));
    if (hoverIndex !== drag.hoverIndex) {
      drag.hoverIndex = hoverIndex;
      setDragVisual({ fromIndex: drag.fromIndex, hoverIndex });
    }
  };

  const handleDragPointerUp = (e: PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag) return;
    if (drag.rafId !== null) cancelAnimationFrame(drag.rafId);
    drag.el.style.transform = '';
    if (e.currentTarget.hasPointerCapture(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId);
    }
    const { fromIndex, hoverIndex } = drag;
    dragRef.current = null;
    setDragVisual(null);
    if (fromIndex !== hoverIndex) {
      onMove?.(fromIndex, hoverIndex);
    }
  };

  const handleDragPointerCancel = () => {
    const drag = dragRef.current;
    if (drag) {
      if (drag.rafId !== null) cancelAnimationFrame(drag.rafId);
      drag.el.style.transform = '';
    }
    dragRef.current = null;
    setDragVisual(null);
  };

  const shiftForIndex = (i: number): number => {
    if (!dragVisual) return 0;
    const { fromIndex, hoverIndex } = dragVisual;
    if (i === fromIndex) return 0;
    if (fromIndex < hoverIndex && i > fromIndex && i <= hoverIndex) return -rowHeight;
    if (fromIndex > hoverIndex && i >= hoverIndex && i < fromIndex) return rowHeight;
    return 0;
  };
  const playRow = (i: number) => {
    // Rows from another catalogue that found no YouTube Music song stay out of the queue.
    if (source) {
      const playable = tracks.filter((t) => !t.unavailable);
      return playList(playable, Math.max(0, playable.indexOf(tracks[i]!)), { source, fallbackArtwork, fallbackArtists });
    }
    const track = tracks[i]!;
    playTrack(track.artists.length || !fallbackArtists ? track : { ...track, artists: fallbackArtists }, fallbackArtwork);
  };
  const virtual = tracks.length >= VIRTUALIZE_FROM;

  const virtualizer = useVirtualizer({
    count: virtual ? tracks.length : 0,
    getScrollElement: () => scrollElement,
    estimateSize: () => ROW_HEIGHT[variant],
    overscan: 10,
    scrollMargin: container.current?.offsetTop ?? 0,
  });

  const items = virtual ? virtualizer.getVirtualItems() : [];
  const last = items[items.length - 1];
  useEffect(() => {
    if (!onNearEnd) return;
    if (virtual ? last && last.index >= tracks.length - 20 : true) onNearEnd();
  }, [last?.index, tracks.length, virtual, onNearEnd]);

  return (
    <div className="mx-(--body-gutter) type-callout text-secondary" data-variant={variant} role="table" aria-label="Songs">
      {variant === 'playlist' && <Header />}
      <div ref={container}>
        {virtual ? (
          <div style={{ height: virtualizer.getTotalSize(), position: 'relative' }}>
            {items.map((item) => {
              const isDragged = dragVisual?.fromIndex === item.index;
              return (
              <div
                key={item.key}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  zIndex: isDragged ? 30 : undefined,
                  transform: `translateY(${item.start - virtualizer.options.scrollMargin}px)`,
                }}
              >
                <Row
                  track={tracks[item.index]!}
                  variant={variant}
                  position={item.index + 1}
                  total={tracks.length}
                  fallbackArtwork={fallbackArtwork}
                  onPlayRow={() => playRow(item.index)}
                  reordering={reordering}
                  dragActive={dragVisual !== null}
                  dragging={isDragged}
                  shiftY={shiftForIndex(item.index)}
                  onMove={onMove}
                  onDragPointerDown={handleDragPointerDown}
                  onDragPointerMove={handleDragPointerMove}
                  onDragPointerUp={handleDragPointerUp}
                  onDragPointerCancel={handleDragPointerCancel}
                />
              </div>
              );
            })}
          </div>
        ) : (
          (() => {
            const seenCounts = new Map<string, number>();
            return tracks.map((track, i) => {
              const base = track.videoId || `${track.title}::${track.artists.map((a) => a.name).join(',')}`;
              const occ = seenCounts.get(base) ?? 0;
              seenCounts.set(base, occ + 1);
              const isDragged = dragVisual?.fromIndex === i;
              return (
                <Row
                  key={`${base}#${occ}`}
                  track={track}
                  variant={variant}
                  position={i + 1}
                  total={tracks.length}
                  fallbackArtwork={fallbackArtwork}
                  onPlayRow={() => playRow(i)}
                  reordering={reordering}
                  dragActive={dragVisual !== null}
                  dragging={isDragged}
                  shiftY={shiftForIndex(i)}
                  onMove={onMove}
                  onDragPointerDown={handleDragPointerDown}
                  onDragPointerMove={handleDragPointerMove}
                  onDragPointerUp={handleDragPointerUp}
                  onDragPointerCancel={handleDragPointerCancel}
                />
              );
            });
          })()
        )}
      </div>
    </div>
  );
}
