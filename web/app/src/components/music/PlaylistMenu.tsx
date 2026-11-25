import {
  ArrowUpDown,
  Check,
  Download,
  Image as ImageIcon,
  ListEnd,
  ListStart,
  Loader2,
  MoreHorizontal,
  Pin,
  Plus,
  Share2,
  Shuffle,
  Trash2,
} from 'lucide-react';
import { useCallback, useState } from 'react';
import { type MenuPlacement } from '@/components/Menu';
import { GlassButtonGroup, GlassButtonGroupItem } from '@/components/liquid-glass/glass-button-group';
import {
  GlassMenu,
  GlassMenuContent,
  GlassMenuItem,
  GlassMenuSeparator,
  GlassMenuTrigger,
} from '@/components/liquid-glass/glass-menu';
import { useDownloads } from '@/data/download/useDownloads';
import { useSetListSaved, useSignedIn } from '@/data/music/queries';
import type { Track } from '@/data/music/types';
import { usePinnedStore } from '@/data/pinned/pinnedStore';
import { usePlayer } from '@/state/player';
import { shuffleList } from './play';

export interface PlaylistMenuProps {
  id: string;
  title: string;
  tracks: Track[];
  thumbnail?: string | null;
  library?: { playlistId: string; saved: boolean } | null;
  className?: string;
  placement?: MenuPlacement;
  onReorder?: () => void;
}

export function PlaylistMenu({
  id,
  title,
  tracks,
  thumbnail,
  library,
  className,
  placement,
  onReorder,
}: PlaylistMenuProps) {
  const [copied, setCopied] = useState(false);

  const signedIn = useSignedIn();
  const setSaved = useSetListSaved();
  const playNext = usePlayer((s) => s.playNext);
  const addToQueue = usePlayer((s) => s.addToQueue);

  const downloadedIds = useDownloads((s) => s.downloadedIds);
  const playlistProgress = useDownloads((s) => s.playlistProgress[id]);
  const downloadPlaylist = useDownloads((s) => s.downloadPlaylist);
  const removePlaylist = useDownloads((s) => s.removePlaylist);

  const isPinned = usePinnedStore((s) => s.isPinned(id));
  const togglePin = usePinnedStore((s) => s.togglePin);

  const handleTogglePin = () => {
    togglePin({
      id,
      kind: 'playlist',
      title,
      subtitle: `${tracks.length} songs`,
      thumbnail: thumbnail ?? null,
      artists: [],
      album: null,
      wide: false,
      durationSec: null,
      explicit: false,
    });
  };

  const isDownloading = Boolean(playlistProgress);
  const downloadedCount = tracks.filter((t) => t.videoId && downloadedIds.has(t.videoId)).length;
  const isAllDownloaded = tracks.length > 0 && downloadedCount === tracks.length;

  const handleShare = async () => {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title: `Listen to ${title} on AURA`, url });
        return;
      } catch {
        // Fall back to clipboard
      }
    }
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => {
      setCopied(false);
    }, 1500);
  };

  const handleDownloadArtwork = async () => {
    if (!thumbnail) return;
    try {
      const res = await fetch(thumbnail);
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = `${title} - Artwork.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(blobUrl);
    } catch {
      window.open(thumbnail, '_blank');
    }
  };

  const handleToggleDownload = async () => {
    if (isDownloading) return;
    if (isAllDownloaded) {
      await removePlaylist(tracks);
    } else {
      void downloadPlaylist(id, tracks);
    }
  };

  const handleRemoveDownloaded = async () => {
    await removePlaylist(tracks);
  };

  return (
    <GlassMenu>
      <GlassButtonGroup material="clear" size="lg">
        <GlassMenuTrigger asChild>
          <GlassButtonGroupItem
            className={className ? `${className} !px-2.5` : '!px-2.5'}
            aria-label={`More options for ${title}`}
            onClick={(event) => event.stopPropagation()}
            onDoubleClick={(event) => event.stopPropagation()}
            title="More options"
          >
            {isDownloading ? (
              <Loader2 size={18} className="animate-spin text-key" />
            ) : (
              <MoreHorizontal size={18} strokeWidth={2.2} />
            )}
          </GlassButtonGroupItem>
        </GlassMenuTrigger>
      </GlassButtonGroup>

      <GlassMenuContent
        side={placement?.startsWith('above') ? 'top' : 'bottom'}
        align={placement?.endsWith('-start') ? 'start' : 'end'}
        className="min-w-[220px]"
      >
        {tracks.length > 0 && (
          <>
            <GlassMenuItem onSelect={() => playNext(tracks)}>
              <ListStart />
              <span>Play Next</span>
            </GlassMenuItem>
            <GlassMenuItem onSelect={() => addToQueue(tracks)}>
              <ListEnd />
              <span>Add to Queue</span>
            </GlassMenuItem>
            <GlassMenuItem onSelect={() => shuffleList(tracks, { source: { title, type: 'playlist', id } })}>
              <Shuffle />
              <span>Shuffle</span>
            </GlassMenuItem>
            {onReorder && (
              <GlassMenuItem onSelect={onReorder}>
                <ArrowUpDown />
                <span>Reorder songs</span>
              </GlassMenuItem>
            )}
            <GlassMenuSeparator />
          </>
        )}

        {/* Download Playlist option */}
        {tracks.length > 0 && (
          <>
            {isDownloading ? (
              <GlassMenuItem disabled onSelect={() => {}}>
                <Loader2 className="animate-spin text-key" />
                <span>Downloading ({playlistProgress.done}/{playlistProgress.total})…</span>
              </GlassMenuItem>
            ) : isAllDownloaded ? (
              <GlassMenuItem destructive onSelect={handleToggleDownload}>
                <Trash2 />
                <span>Remove from downloads</span>
              </GlassMenuItem>
            ) : (
              <GlassMenuItem onSelect={handleToggleDownload}>
                <Download />
                <span>
                  {downloadedCount > 0
                    ? `Download remaining (${tracks.length - downloadedCount} songs)`
                    : `Download playlist (${tracks.length} songs)`}
                </span>
              </GlassMenuItem>
            )}

            {!isAllDownloaded && downloadedCount > 0 && !isDownloading && (
              <GlassMenuItem destructive onSelect={handleRemoveDownloaded}>
                <Trash2 />
                <span>Remove downloaded ({downloadedCount})</span>
              </GlassMenuItem>
            )}
            <GlassMenuSeparator />
          </>
        )}

        {/* Library Saving */}
        {signedIn && library && (
          <GlassMenuItem onSelect={() => setSaved.mutate({ playlistId: library.playlistId, saved: !library.saved })}>
            {library.saved ? <Check strokeWidth={2.4} /> : <Plus strokeWidth={2.4} />}
            <span>{library.saved ? 'Remove from Library' : 'Add to Library'}</span>
          </GlassMenuItem>
        )}

        {/* Pin to Listen again */}
        <GlassMenuItem onSelect={handleTogglePin}>
          <Pin className={isPinned ? 'text-key fill-key' : ''} />
          <span>{isPinned ? 'Unpin from Listen again' : 'Pin to Listen again'}</span>
        </GlassMenuItem>

        {/* Share */}
        <GlassMenuItem onSelect={handleShare}>
          <Share2 />
          <span>{copied ? 'Link Copied!' : 'Share Playlist'}</span>
        </GlassMenuItem>

        {/* Download Artwork */}
        {thumbnail && (
          <GlassMenuItem onSelect={handleDownloadArtwork}>
            <ImageIcon />
            <span>Download Artwork</span>
          </GlassMenuItem>
        )}
      </GlassMenuContent>
    </GlassMenu>
  );
}
