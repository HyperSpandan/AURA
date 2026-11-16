import {
  Check,
  Download,
  Image as ImageIcon,
  ListEnd,
  ListStart,
  Loader2,
  MoreHorizontal,
  Plus,
  Share2,
  Shuffle,
  Trash2,
} from 'lucide-react';
import { useCallback, useState } from 'react';
import { Menu, MenuItem, type MenuPlacement, MenuSeparator } from '@/components/Menu';
import { useDownloads } from '@/data/download/useDownloads';
import { useSetListSaved, useSignedIn } from '@/data/music/queries';
import type { Track } from '@/data/music/types';
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
}

export function PlaylistMenu({
  id,
  title,
  tracks,
  thumbnail,
  library,
  className,
  placement,
}: PlaylistMenuProps) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const [copied, setCopied] = useState(false);

  const signedIn = useSignedIn();
  const setSaved = useSetListSaved();
  const playNext = usePlayer((s) => s.playNext);
  const addToQueue = usePlayer((s) => s.addToQueue);

  const downloadedIds = useDownloads((s) => s.downloadedIds);
  const playlistProgress = useDownloads((s) => s.playlistProgress[id]);
  const downloadPlaylist = useDownloads((s) => s.downloadPlaylist);
  const removePlaylist = useDownloads((s) => s.removePlaylist);

  const isDownloading = Boolean(playlistProgress);
  const downloadedCount = tracks.filter((t) => t.videoId && downloadedIds.has(t.videoId)).length;
  const isAllDownloaded = tracks.length > 0 && downloadedCount === tracks.length;

  const close = useCallback(() => setAnchor(null), []);

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

  return (
    <>
      <button
        className={
          className ??
          'ot-press grid size-10 place-items-center rounded-full border-[0.75px] border-quaternary bg-quinary text-primary backdrop-blur-xl transition-[scale,background-color] disabled:opacity-40 enabled:hover:bg-quaternary'
        }
        aria-label={`More options for ${title}`}
        aria-haspopup="menu"
        aria-expanded={anchor !== null}
        data-open={anchor !== null || undefined}
        onClick={(event) => {
          event.stopPropagation();
          setAnchor(anchor ? null : event.currentTarget);
        }}
        onDoubleClick={(event) => event.stopPropagation()}
        title="More options"
      >
        {isDownloading ? (
          <Loader2 size={18} className="animate-spin text-key" />
        ) : (
          <MoreHorizontal size={18} strokeWidth={2.2} />
        )}
      </button>

      {anchor && (
        <Menu anchor={anchor} placement={placement} onClose={close} label={title}>
          {tracks.length > 0 && (
            <>
              <MenuItem
                icon={<ListStart size={15} />}
                onSelect={() => {
                  close();
                  playNext(tracks);
                }}
              >
                Play Next
              </MenuItem>
              <MenuItem
                icon={<ListEnd size={15} />}
                onSelect={() => {
                  close();
                  addToQueue(tracks);
                }}
              >
                Add to Queue
              </MenuItem>
              <MenuItem
                icon={<Shuffle size={15} />}
                onSelect={() => {
                  close();
                  shuffleList(tracks, { source: { title, type: 'playlist', id } });
                }}
              >
                Shuffle
              </MenuItem>
              <MenuSeparator />
            </>
          )}

          {/* Download Playlist option */}
          {tracks.length > 0 && (
            <>
              {isDownloading ? (
                <MenuItem
                  icon={<Loader2 size={15} className="animate-spin text-key" />}
                  disabled
                  onSelect={() => {}}
                >
                  Downloading ({playlistProgress.done}/{playlistProgress.total})…
                </MenuItem>
              ) : isAllDownloaded ? (
                <MenuItem
                  icon={<Trash2 size={15} className="text-red-400" />}
                  destructive
                  onSelect={handleToggleDownload}
                >
                  Remove from downloads
                </MenuItem>
              ) : (
                <MenuItem icon={<Download size={15} />} onSelect={handleToggleDownload}>
                  {downloadedCount > 0
                    ? `Download remaining (${tracks.length - downloadedCount} songs)`
                    : `Download playlist (${tracks.length} songs)`}
                </MenuItem>
              )}

              {!isAllDownloaded && downloadedCount > 0 && !isDownloading && (
                <MenuItem
                  icon={<Trash2 size={15} className="text-red-400" />}
                  destructive
                  onSelect={handleRemoveDownloaded}
                >
                  Remove downloaded ({downloadedCount})
                </MenuItem>
              )}
              <MenuSeparator />
            </>
          )}

          {/* Library Saving */}
          {signedIn && library && (
            <MenuItem
              icon={
                library.saved ? (
                  <Check size={15} strokeWidth={2.4} />
                ) : (
                  <Plus size={15} strokeWidth={2.4} />
                )
              }
              onSelect={() => {
                setSaved.mutate({ playlistId: library.playlistId, saved: !library.saved });
                close();
              }}
            >
              {library.saved ? 'Remove from Library' : 'Add to Library'}
            </MenuItem>
          )}

          {/* Share */}
          <MenuItem icon={<Share2 size={15} />} onSelect={handleShare}>
            {copied ? 'Link Copied!' : 'Share Playlist'}
          </MenuItem>

          {/* Download Artwork */}
          {thumbnail && (
            <MenuItem icon={<ImageIcon size={15} />} onSelect={handleDownloadArtwork}>
              Download Artwork
            </MenuItem>
          )}
        </Menu>
      )}
    </>
  );
}
