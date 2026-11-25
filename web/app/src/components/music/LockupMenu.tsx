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
import { useCallback, useContext, useState, type ComponentProps } from 'react';
import { useNavigate } from 'react-router';
import type { MenuPlacement } from '@/components/Menu';
import {
  GlassDropdownMenu,
  GlassDropdownMenuContent,
  GlassDropdownMenuItem,
  GlassDropdownMenuSeparator,
  GlassDropdownMenuTrigger,
} from '@/components/liquid-glass/glass-dropdown-menu';
import { GlassPopupAnchor, GlassPopupContext } from '@/components/liquid-glass/glass-popup';
import { useDownloads } from '@/data/download/useDownloads';
import type { MusicItem } from '@/data/music/types';
import { usePinnedStore } from '@/data/pinned/pinnedStore';
import { usePlayer } from '@/state/player';
import { hrefFor } from './links';
import { playItem } from './play';

const compactDropdownClasses =
  '!min-w-[165px] !rounded-xl !p-1 !text-[12px] [&_[role=menuitem]]:!h-7 [&_[role=menuitem]]:!gap-2 [&_[role=menuitem]]:!rounded-lg [&_[role=menuitem]]:!px-2 [&_svg]:!size-3.5 [&_[role=separator]]:!mx-2 [&_[role=separator]]:!my-0.5';

function LockupMenuButton({ className = '', children, ...props }: ComponentProps<'button'>) {
  const popup = useContext(GlassPopupContext);
  return (
    <button
      data-glass-host=""
      {...props}
      className={`${className} [&:not([data-open])>canvas]:!opacity-0`}
    >
      {popup && <GlassPopupAnchor popup={popup} />}
      <span className="inline-flex items-center justify-center">{children}</span>
    </button>
  );
}

export function LockupMenu({
  item,
  className,
  placement,
}: {
  item: MusicItem;
  className?: string;
  placement?: MenuPlacement;
}) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [loadingAction, setLoadingAction] = useState<string | null>(null);

  const navigate = useNavigate();
  const playNext = usePlayer((s) => s.playNext);
  const addToQueue = usePlayer((s) => s.addToQueue);

  const isPinned = usePinnedStore((s) => s.isPinned(item.id));
  const togglePin = usePinnedStore((s) => s.togglePin);

  const isDownloading = useDownloads((s) => Boolean(s.playlistProgress[item.id]));
  const downloadPlaylist = useDownloads((s) => s.downloadPlaylist);

  const close = useCallback(() => setOpen(false), []);

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

  const side = placement === 'above-start' ? 'top' : 'bottom';
  const align = placement === 'above-start' ? 'start' : 'end';

  return (
    <GlassDropdownMenu open={open} onOpenChange={setOpen}>
      <GlassDropdownMenuTrigger asChild>
        <LockupMenuButton
          type="button"
          className={
            className ??
            'ot-platter grid size-8 place-items-center rounded-full bg-black/60 text-white opacity-0 hover:bg-black/80 hover:scale-105 transition-all group-hover/art:opacity-100 data-open:opacity-100 shadow-[0_4px_12px_rgb(0_0_0/0.3)] [&:not([data-open])]:!bg-black/60'
          }
          aria-label={`More options for ${item.title}`}
          title="More options"
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
          }}
          onDoubleClick={(event) => event.stopPropagation()}
        >
          <MoreHorizontal size={18} strokeWidth={2} />
        </LockupMenuButton>
      </GlassDropdownMenuTrigger>

      <GlassDropdownMenuContent side={side} align={align} aria-label={item.title} className={compactDropdownClasses}>
        <GlassDropdownMenuItem
          onSelect={() => {
            close();
            void playItem(item);
          }}
        >
          <Play size={15} />
          Play
        </GlassDropdownMenuItem>
        <GlassDropdownMenuItem
          onSelect={handlePlayNext}
          disabled={loadingAction !== null}
        >
          {loadingAction === 'next' ? <Loader2 size={15} className="animate-spin text-key" /> : <ListStart size={15} />}
          Play Next
        </GlassDropdownMenuItem>
        <GlassDropdownMenuItem
          onSelect={handleAddToQueue}
          disabled={loadingAction !== null}
        >
          {loadingAction === 'queue' ? <Loader2 size={15} className="animate-spin text-key" /> : <ListEnd size={15} />}
          Add to Queue
        </GlassDropdownMenuItem>

        <GlassDropdownMenuSeparator />

        <GlassDropdownMenuItem
          onSelect={handleDownload}
          disabled={isDownloading || loadingAction !== null}
        >
          {isDownloading || loadingAction === 'download' ? (
            <Loader2 size={15} className="animate-spin text-key" />
          ) : (
            <Download size={15} />
          )}
          {isDownloading ? 'Downloading…' : item.kind === 'album' ? 'Download Album' : 'Download Playlist'}
        </GlassDropdownMenuItem>

        <GlassDropdownMenuItem onSelect={handleTogglePin}>
          <Pin size={15} className={isPinned ? 'text-key fill-key' : ''} />
          {isPinned ? 'Unpin from Listen again' : 'Pin to Listen again'}
        </GlassDropdownMenuItem>

        <GlassDropdownMenuItem closeOnSelect={false} onSelect={handleShare}>
          <Share2 size={15} />
          {copied ? 'Link Copied!' : 'Share'}
        </GlassDropdownMenuItem>

        {item.thumbnail && (
          <GlassDropdownMenuItem onSelect={handleDownloadArtwork}>
            <ImageIcon size={15} />
            Download Artwork
          </GlassDropdownMenuItem>
        )}
      </GlassDropdownMenuContent>
    </GlassDropdownMenu>
  );
}
