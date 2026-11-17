import {
  Download,
  Image as ImageIcon,
  ListEnd,
  ListStart,
  Loader2,
  MoreHorizontal,
  Pin,
  Play,
  Share2,
} from 'lucide-react';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router';
import { Menu, MenuItem, type MenuPlacement, MenuSeparator } from '@/components/Menu';
import { useDownloads } from '@/data/download/useDownloads';
import type { MusicItem } from '@/data/music/types';
import { usePinnedStore } from '@/data/pinned/pinnedStore';
import { usePlayer } from '@/state/player';
import { hrefFor } from './links';
import { playItem } from './play';

export function LockupMenu({
  item,
  className,
  placement,
}: {
  item: MusicItem;
  className?: string;
  placement?: MenuPlacement;
}) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const [copied, setCopied] = useState(false);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  const navigate = useNavigate();
  const playNext = usePlayer((s) => s.playNext);
  const addToQueue = usePlayer((s) => s.addToQueue);

  const isPinned = usePinnedStore((s) => s.isPinned(item.id));
  const togglePin = usePinnedStore((s) => s.togglePin);

  const isDownloading = useDownloads((s) => Boolean(s.playlistProgress[item.id]));
  const downloadPlaylist = useDownloads((s) => s.downloadPlaylist);

  const close = useCallback(() => setAnchor(null), []);

  const getTracks = async () => {
    if (item.kind === 'playlist') {
      const { fetchPlaylist } = await import('@/data/music/api');
      const { page } = await fetchPlaylist(item.id);
      return page.tracks;
    }
    if (item.kind === 'album') {
      const { fetchAlbum } = await import('@/data/music/api');
      const album = await fetchAlbum(item.id);
      return album.tracks.map((t) => ({
        ...t,
        thumbnail: t.thumbnail || album.thumbnail,
        artists: t.artists?.length ? t.artists : album.artists,
        album: t.album || { name: album.title, id: album.id },
      }));
    }
    return [];
  };

  const handlePlayNext = async () => {
    setLoadingAction('next');
    try {
      const tracks = await getTracks();
      if (tracks.length > 0) playNext(tracks);
    } catch {
      // Ignored
    } finally {
      setLoadingAction(null);
      close();
    }
  };

  const handleAddToQueue = async () => {
    setLoadingAction('queue');
    try {
      const tracks = await getTracks();
      if (tracks.length > 0) addToQueue(tracks);
    } catch {
      // Ignored
    } finally {
      setLoadingAction(null);
      close();
    }
  };

  const handleDownload = async () => {
    setLoadingAction('download');
    try {
      const tracks = await getTracks();
      if (tracks.length > 0) void downloadPlaylist(item.id, tracks);
    } catch {
      // Ignored
    } finally {
      setLoadingAction(null);
      close();
    }
  };

  const handleShare = async () => {
    const href = hrefFor(item);
    const url = href ? `${window.location.origin}${href}` : window.location.href;
    if (navigator.share) {
      try {
        await navigator.share({ title: item.title, url });
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
    if (!item.thumbnail) return;
    try {
      const res = await fetch(item.thumbnail);
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = `${item.title} - Artwork.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(blobUrl);
    } catch {
      window.open(item.thumbnail, '_blank');
    }
    close();
  };

  const handleTogglePin = () => {
    togglePin(item);
    close();
  };

  return (
    <>
      <button
        className={
          className ??
          'ot-platter grid size-8 place-items-center rounded-full bg-black/60 text-white opacity-0 hover:bg-black/80 hover:scale-105 transition-all group-hover/art:opacity-100 data-open:opacity-100 shadow-[0_4px_12px_rgb(0_0_0/0.3)]'
        }
        aria-label={`More options for ${item.title}`}
        aria-haspopup="menu"
        aria-expanded={anchor !== null}
        data-open={anchor !== null || undefined}
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          setAnchor(anchor ? null : event.currentTarget);
        }}
        onDoubleClick={(event) => event.stopPropagation()}
        title="More options"
      >
        <MoreHorizontal size={18} strokeWidth={2} />
      </button>

      {anchor && (
        <Menu anchor={anchor} placement={placement} onClose={close} label={item.title}>
          <MenuItem
            icon={<Play size={15} />}
            onSelect={() => {
              close();
              void playItem(item);
            }}
          >
            Play
          </MenuItem>
          <MenuItem
            icon={loadingAction === 'next' ? <Loader2 size={15} className="animate-spin text-key" /> : <ListStart size={15} />}
            onSelect={handlePlayNext}
            disabled={loadingAction !== null}
          >
            Play Next
          </MenuItem>
          <MenuItem
            icon={loadingAction === 'queue' ? <Loader2 size={15} className="animate-spin text-key" /> : <ListEnd size={15} />}
            onSelect={handleAddToQueue}
            disabled={loadingAction !== null}
          >
            Add to Queue
          </MenuItem>

          <MenuSeparator />

          <MenuItem
            icon={
              isDownloading || loadingAction === 'download' ? (
                <Loader2 size={15} className="animate-spin text-key" />
              ) : (
                <Download size={15} />
              )
            }
            onSelect={handleDownload}
            disabled={isDownloading || loadingAction !== null}
          >
            {isDownloading ? 'Downloading…' : item.kind === 'album' ? 'Download Album' : 'Download Playlist'}
          </MenuItem>

          <MenuItem
            icon={<Pin size={15} className={isPinned ? 'text-key fill-key' : ''} />}
            onSelect={handleTogglePin}
          >
            {isPinned ? 'Unpin from Listen again' : 'Pin to Listen again'}
          </MenuItem>

          <MenuItem icon={<Share2 size={15} />} onSelect={handleShare}>
            {copied ? 'Link Copied!' : 'Share'}
          </MenuItem>

          {item.thumbnail && (
            <MenuItem icon={<ImageIcon size={15} />} onSelect={handleDownloadArtwork}>
              Download Artwork
            </MenuItem>
          )}
        </Menu>
      )}
    </>
  );
}
