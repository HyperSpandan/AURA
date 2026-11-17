import { Trash2, WifiOff } from 'lucide-react';
import { useMemo } from 'react';
import { Artwork } from '@/components/music/Artwork';
import { DetailHeader, detailButton } from '@/components/music/DetailHeader';
import { EmptyState } from '@/components/music/States';
import { TrackList } from '@/components/music/TrackList';
import { playList, shuffleList } from '@/components/music/play';
import { useDownloads } from '@/data/download/useDownloads';
import type { HeaderInfo } from '@/data/music/types';
import { useArtworkColor } from '@/lib/artworkColor';
import { useIsOnline } from '@/lib/useIsOnline';
import { usePageTint } from '@/lib/usePageTint';
import { detailFooter } from './pageStyles';

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 1 ? 1 : 0)} ${units[i]}`;
}

function formatDuration(totalSeconds: number): string {
  if (totalSeconds <= 0) return '';
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) {
    return `${hours} hour${hours > 1 ? 's' : ''}${minutes > 0 ? `, ${minutes} minute${minutes > 1 ? 's' : ''}` : ''}`;
  }
  return `${minutes} minute${minutes > 1 ? 's' : ''}`;
}

function DownloadsCollage({ thumbnails }: { thumbnails: string[] }) {
  if (thumbnails.length >= 4) {
    return (
      <div className="relative aspect-square size-full overflow-hidden rounded-artwork-lg bg-joe artwork-stroke grid grid-cols-2 grid-rows-2">
        {thumbnails.slice(0, 4).map((src, i) => (
          <img
            key={i}
            src={src}
            alt=""
            className="size-full object-cover"
            loading="eager"
            decoding="async"
            draggable={false}
          />
        ))}
      </div>
    );
  }
  return <Artwork src={thumbnails[0] ?? null} displayWidth={270} radius="var(--radius-artwork-large)" eager />;
}

export default function DownloadsPage() {
  const tracks = useDownloads((s) => s.tracks);
  const totalBytes = useDownloads((s) => s.totalBytes);
  const clearAll = useDownloads((s) => s.clearAll);

  const collageThumbnails = useMemo(() => {
    const urls: string[] = [];
    for (const t of tracks) {
      if (t.thumbnail && !urls.includes(t.thumbnail)) {
        urls.push(t.thumbnail);
      }
      if (urls.length === 4) break;
    }
    return urls;
  }, [tracks]);

  const heroThumbnail = collageThumbnails[0] ?? null;
  usePageTint(useArtworkColor(heroThumbnail));

  const totalDurationSec = useMemo(() => {
    return tracks.reduce((sum, t) => sum + (t.durationSec || 0), 0);
  }, [tracks]);

  const context = useMemo(
    () => ({
      source: {
        title: 'Downloads',
        type: 'library' as const,
        id: 'downloads',
      },
    }),
    [],
  );

  const handlePlayAll = (shuffle = false) => {
    if (!tracks.length) return;
    if (shuffle) {
      shuffleList(tracks, context);
    } else {
      playList(tracks, 0, context);
    }
  };

  const handleClearAll = () => {
    if (window.confirm('Are you sure you want to remove all downloaded songs from offline storage?')) {
      void clearAll();
    }
  };

  if (tracks.length === 0) {
    return (
      <EmptyState
        title="No downloaded songs"
        detail="Pick “Download” from any song’s 3-dots menu to listen offline without internet."
      />
    );
  }

  const durationText = formatDuration(totalDurationSec);
  const sizeText = formatBytes(totalBytes);
  const metaParts = ['Offline', `${tracks.length} track${tracks.length === 1 ? '' : 's'}`];
  if (durationText) metaParts.push(durationText);
  if (sizeText) metaParts.push(sizeText);

  const header: HeaderInfo = {
    title: 'Downloads',
    subtitle: 'Playlist',
    meta: metaParts.join(' · '),
    description: null,
    thumbnail: heroThumbnail,
    artists: [],
    explicit: false,
    library: null,
  };

  const isOnline = useIsOnline();

  return (
    <>
      {!isOnline && (
        <div className="mx-(--body-gutter) mb-6 flex items-center gap-2.5 rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-2.5 text-xs text-amber-200 backdrop-blur-md">
          <WifiOff size={15} className="text-amber-400 shrink-0" />
          <span>
            <strong>You’re offline.</strong> All downloaded songs and playlists below are available to play offline.
          </span>
        </div>
      )}
      <DetailHeader
        info={header}
        canPlay={tracks.length > 0}
        onPlay={() => handlePlayAll(false)}
        onShuffle={() => handlePlayAll(true)}
        artworkNode={<DownloadsCollage thumbnails={collageThumbnails} />}
        extra={
          <button
            className={detailButton.circle}
            aria-label="Clear all downloads"
            title="Clear all downloads"
            onClick={handleClearAll}
          >
            <Trash2 size={18} strokeWidth={2.2} />
          </button>
        }
      />
      <TrackList tracks={tracks} variant="playlist" source={context.source} />
      <footer className={detailFooter}>
        <p>{header.meta}</p>
      </footer>
    </>
  );
}
