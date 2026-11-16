import { ArrowDownToLine, HardDrive, Play, Search, Shuffle, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { PageHeader } from '@/components/PageHeader';
import { EmptyState } from '@/components/music/States';
import { TrackList } from '@/components/music/TrackList';
import { useDownloads } from '@/data/download/useDownloads';
import { playList, shuffleList } from '@/components/music/play';
import { seeAllTracks } from './pageStyles';

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 1 ? 1 : 0)} ${units[i]}`;
}

export default function DownloadsPage() {
  const tracks = useDownloads((s) => s.tracks);
  const totalBytes = useDownloads((s) => s.totalBytes);
  const clearAll = useDownloads((s) => s.clearAll);
  const [filter, setFilter] = useState('');

  const filteredTracks = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return tracks;
    return tracks.filter((t) => {
      const matchTitle = t.title.toLowerCase().includes(q);
      const matchArtist = t.artists.some((a) => a.name.toLowerCase().includes(q));
      const matchAlbum = t.album?.name?.toLowerCase().includes(q);
      return matchTitle || matchArtist || matchAlbum;
    });
  }, [tracks, filter]);

  const handlePlayAll = (shuffle = false) => {
    if (!filteredTracks.length) return;
    const context = {
      source: {
        title: 'Downloads',
        type: 'library' as const,
        id: 'downloads',
      },
    };
    if (shuffle) {
      shuffleList(filteredTracks, context);
    } else {
      playList(filteredTracks, 0, context);
    }
  };

  const handleClearAll = () => {
    if (window.confirm('Are you sure you want to remove all downloaded songs from offline storage?')) {
      void clearAll();
    }
  };

  return (
    <>
      <PageHeader title="Downloads" />

      {tracks.length === 0 ? (
        <EmptyState
          title="No downloaded songs yet"
          detail="Pick 'Download to AURA' from any song's 3-dots menu to listen offline without internet."
        />
      ) : (
        <div className="flex flex-col gap-4">
          {/* Header Action & Stats Toolbar */}
          <div className="flex flex-wrap items-center justify-between gap-3 px-1">
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => handlePlayAll(false)}
                className="inline-flex h-9 items-center gap-2 rounded-full bg-key px-4 type-callout-emphasized text-white transition-opacity hover:opacity-90"
              >
                <Play size={15} className="fill-current" />
                <span>Play all</span>
              </button>
              <button
                type="button"
                onClick={() => handlePlayAll(true)}
                className="inline-flex h-9 items-center gap-2 rounded-full bg-quinary px-3.5 type-callout text-primary transition-colors hover:bg-quinary/80"
              >
                <Shuffle size={15} />
                <span>Shuffle</span>
              </button>
              <button
                type="button"
                onClick={handleClearAll}
                className="inline-flex h-9 items-center gap-1.5 rounded-full px-3 type-callout text-secondary transition-colors hover:bg-red-500/10 hover:text-red-400"
                title="Remove all downloaded songs"
              >
                <Trash2 size={14} />
                <span>Clear all</span>
              </button>
            </div>

            <div className="flex items-center gap-3 type-caption text-secondary">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-quinary/60 px-2.5 py-1">
                <HardDrive size={13} className="text-secondary" />
                <span>{tracks.length} song{tracks.length === 1 ? '' : 's'} · {formatBytes(totalBytes)}</span>
              </span>
              <span className="inline-flex items-center gap-1 text-emerald-500">
                <ArrowDownToLine size={13} />
                <span>Offline ready</span>
              </span>
            </div>
          </div>

          {/* Quick Search / Filter Filter */}
          {tracks.length > 5 && (
            <div className="relative px-1">
              <Search
                size={16}
                className="absolute top-1/2 left-4 -translate-y-1/2 text-tertiary"
              />
              <input
                type="text"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Filter downloaded songs…"
                className="h-9 w-full rounded-full border border-white/8 bg-quinary/50 pr-4 pl-10 type-callout text-primary placeholder:text-tertiary focus:border-key/60 focus:outline-none"
              />
            </div>
          )}

          {/* Tracklist */}
          <div className={seeAllTracks}>
            <TrackList
              tracks={filteredTracks}
              variant="playlist"
              source={{ title: 'Downloads', type: 'library', id: 'downloads' }}
            />
          </div>
        </div>
      )}
    </>
  );
}
