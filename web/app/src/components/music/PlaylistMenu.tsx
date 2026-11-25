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
import type { MenuPlacement } from '@/components/Menu';
import {
  GlassDropdownMenu,
  GlassDropdownMenuContent,
  GlassDropdownMenuItem,
  GlassDropdownMenuSeparator,
  GlassDropdownMenuTrigger,
} from '@/components/liquid-glass/glass-dropdown-menu';
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
  const [open, setOpen] = useState(false);
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
    close();
  };

  const isDownloading = Boolean(playlistProgress);
  const downloadedCount = tracks.filter((t) => t.videoId && downloadedIds.has(t.videoId)).length;
  const isAllDownloaded = tracks.length > 0 && downloadedCount === tracks.length;

  const close = useCallback(() => setOpen(false), []);

  const handleShare = async () => {
    const url = window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title: `Listen to ${title} on AURA`, url });
        close();
        return;
      } catch {
        // Fall back to clipboard
      }
    }
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => {
      setCopied(false);
      close();
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
    close();
  };

  const handleToggleDownload = async () => {
    if (isDownloading) return;
    if (isAllDownloaded) {
      await removePlaylist(tracks);
    } else {
      void downloadPlaylist(id, tracks);
    }
    close();
  };

  const handleRemoveDownloaded = async () => {
    await removePlaylist(tracks);
    close();
  };

  const side = placement === 'above-start' ? 'top' : 'bottom';
  const align = placement === 'above-start' ? 'start' : 'end';

  return (
    <GlassDropdownMenu open={open} onOpenChange={setOpen}>
      <GlassDropdownMenuTrigger
        className={
          className
            ? `${className} !px-0`
            : '!size-10 !h-10 !w-10 !px-0'
        }
        aria-label={`More options for ${title}`}
        title="More options"
        onClick={(event) => event.stopPropagation()}
        onDoubleClick={(event) => event.stopPropagation()}
      >
        {isDownloading ? (
          <Loader2 size={18} className="animate-spin text-key" />
        ) : (
          <MoreHorizontal size={18} strokeWidth={2.2} />
        )}
      </GlassDropdownMenuTrigger>

      <GlassDropdownMenuContent side={side} align={align} aria-label={title}>
        {tracks.length > 0 && (
          <>
            <GlassDropdownMenuItem
              onSelect={() => {
                close();
                playNext(tracks);
              }}
            >
              <ListStart size={15} />
              Play Next
            </GlassDropdownMenuItem>
            <GlassDropdownMenuItem
              onSelect={() => {
                close();
                addToQueue(tracks);
              }}
            >
              <ListEnd size={15} />
              Add to Queue
            </GlassDropdownMenuItem>
            <GlassDropdownMenuItem
              onSelect={() => {
                close();
                shuffleList(tracks, { source: { title, type: 'playlist', id } });
              }}
            >
              <Shuffle size={15} />
              Shuffle
            </GlassDropdownMenuItem>
            {onReorder && (
              <GlassDropdownMenuItem
                onSelect={() => {
                  close();
                  onReorder();
                }}
              >
                <ArrowUpDown size={15} />
                Reorder songs
              </GlassDropdownMenuItem>
            )}
            <GlassDropdownMenuSeparator />
          </>
        )}

        {/* Download Playlist option */}
        {tracks.length > 0 && (
          <>
            {isDownloading ? (
              <GlassDropdownMenuItem disabled onSelect={() => {}}>
                <Loader2 size={15} className="animate-spin text-key" />
                Downloading ({playlistProgress.done}/{playlistProgress.total})…
              </GlassDropdownMenuItem>
            ) : isAllDownloaded ? (
              <GlassDropdownMenuItem destructive onSelect={handleToggleDownload}>
                <Trash2 size={15} />
                Remove from downloads
              </GlassDropdownMenuItem>
            ) : (
              <GlassDropdownMenuItem onSelect={handleToggleDownload}>
                <Download size={15} />
                {downloadedCount > 0
                  ? `Download remaining (${tracks.length - downloadedCount} songs)`
                  : `Download playlist (${tracks.length} songs)`}
              </GlassDropdownMenuItem>
            )}

            {!isAllDownloaded && downloadedCount > 0 && !isDownloading && (
              <GlassDropdownMenuItem destructive onSelect={handleRemoveDownloaded}>
                <Trash2 size={15} />
                Remove downloaded ({downloadedCount})
              </GlassDropdownMenuItem>
            )}
            <GlassDropdownMenuSeparator />
          </>
        )}

        {/* Library Saving */}
        {signedIn && library && (
          <GlassDropdownMenuItem
            onSelect={() => {
              setSaved.mutate({ playlistId: library.playlistId, saved: !library.saved });
              close();
            }}
          >
            {library.saved ? (
              <Check size={15} strokeWidth={2.4} />
            ) : (
              <Plus size={15} strokeWidth={2.4} />
            )}
            {library.saved ? 'Remove from Library' : 'Add to Library'}
          </GlassDropdownMenuItem>
        )}

        {/* Pin to Listen again */}
        <GlassDropdownMenuItem onSelect={handleTogglePin}>
          <Pin size={15} className={isPinned ? 'text-key fill-key' : ''} />
          {isPinned ? 'Unpin from Listen again' : 'Pin to Listen again'}
        </GlassDropdownMenuItem>

        {/* Share */}
        <GlassDropdownMenuItem closeOnSelect={false} onSelect={handleShare}>
          <Share2 size={15} />
          {copied ? 'Link Copied!' : 'Share Playlist'}
        </GlassDropdownMenuItem>

        {/* Download Artwork */}
        {thumbnail && (
          <GlassDropdownMenuItem onSelect={handleDownloadArtwork}>
            <ImageIcon size={15} />
            Download Artwork
          </GlassDropdownMenuItem>
        )}
      </GlassDropdownMenuContent>
    </GlassDropdownMenu>
  );
}
