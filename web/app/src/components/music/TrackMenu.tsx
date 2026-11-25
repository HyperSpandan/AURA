import {
  Check,
  Disc,
  Download,
  FileDown,
  Heart,
  Image as ImageIcon,
  Info,
  ListEnd,
  ListPlus,
  ListStart,
  Loader2,
  MessageSquareQuote,
  MoreHorizontal,
  Pin,
  Radio,
  Share2,
  Trash2,
  User,
  Users,
} from 'lucide-react';
import { useCallback, useContext, useState, type ComponentProps } from 'react';
import { useNavigate } from 'react-router';
import type { MenuPlacement } from '@/components/Menu';
import {
  GlassMenu,
  GlassMenuContent,
  GlassMenuGroup,
  GlassMenuItem,
  GlassMenuLabel,
  GlassMenuSeparator,
  GlassMenuSub,
  GlassMenuSubContent,
  GlassMenuSubTrigger,
  GlassMenuTrigger,
} from '@/components/liquid-glass/glass-menu';
import { GlassPopupAnchor, GlassPopupContext } from '@/components/liquid-glass/glass-popup';
import {
  useAddToPlaylist,
  useLikeStatus,
  usePlaylistOptions,
  useSetLiked,
  useSignedIn,
} from '@/data/music/queries';
import type { Track } from '@/data/music/types';
import { useDownloads } from '@/data/download/useDownloads';
import { usePinnedStore } from '@/data/pinned/pinnedStore';
import { exportTrackFile } from '@/playback/download';
import { usePlayer } from '@/state/player';
import { useUi } from '@/state/ui';
import { albumHref, artistHref, formatDuration } from './links';

function PlaylistChoices({ videoId, onDone }: { videoId: string; onDone: () => void }) {
  const options = usePlaylistOptions(videoId, true);
  const add = useAddToPlaylist();
  return (
    <GlassMenuGroup>
      <GlassMenuLabel>Add to Playlist</GlassMenuLabel>
      {options.isPending && (
        <GlassMenuItem disabled onSelect={() => {}}>
          Loading…
        </GlassMenuItem>
      )}
      {options.isError && (
        <GlassMenuItem disabled onSelect={() => {}}>
          Couldn’t load your playlists
        </GlassMenuItem>
      )}
      {options.data?.length === 0 && (
        <GlassMenuItem disabled onSelect={() => {}}>
          No playlists yet
        </GlassMenuItem>
      )}
      {options.data?.map((option) => (
        <GlassMenuItem
          key={option.playlistId}
          onSelect={() => {
            add.mutate({ playlistId: option.playlistId, videoId });
            onDone();
          }}
        >
          {option.title}
        </GlassMenuItem>
      ))}
    </GlassMenuGroup>
  );
}

function TrackDetailsView({ track }: { track: Track }) {
  return (
    <GlassMenuGroup>
      <GlassMenuLabel>Song Info</GlassMenuLabel>
      <div className="px-3 py-1.5 flex flex-col gap-1.5 text-[12px]">
        <div>
          <span className="opacity-55 block text-[10px] uppercase tracking-wider font-semibold">Title</span>
          <span className="font-medium">{track.title}</span>
        </div>
        <div>
          <span className="opacity-55 block text-[10px] uppercase tracking-wider font-semibold">Artist</span>
          <span>{track.artists.map((a) => a.name).join(', ') || 'Unknown'}</span>
        </div>
        {track.album?.name && (
          <div>
            <span className="opacity-55 block text-[10px] uppercase tracking-wider font-semibold">Album</span>
            <span>{track.album.name}</span>
          </div>
        )}
        {track.durationSec && (
          <div>
            <span className="opacity-55 block text-[10px] uppercase tracking-wider font-semibold">Duration</span>
            <span>{formatDuration(track.durationSec)}</span>
          </div>
        )}
        <div>
          <span className="opacity-55 block text-[10px] uppercase tracking-wider font-semibold">YouTube ID</span>
          <span className="font-mono text-[11px] select-all">{track.videoId}</span>
        </div>
      </div>
    </GlassMenuGroup>
  );
}

/** Favorite and Add to Playlist: the account's actions, signed in only. */
function AccountItems({ track, onClose }: { track: Track; onClose: () => void }) {
  const status = useLikeStatus(track.videoId);
  const setLiked = useSetLiked();
  const liked = status.data === 'LIKE';
  return (
    <>
      <GlassMenuItem
        disabled={status.isPending}
        onSelect={() => {
          setLiked.mutate({ videoId: track.videoId, liked: !liked });
          onClose();
        }}
      >
        <Heart size={15} fill={liked ? '#ef4444' : 'none'} className={liked ? 'text-red-500' : 'text-current'} />
        {liked ? 'Unlike' : 'Like'}
      </GlassMenuItem>
      <GlassMenuSub>
        <GlassMenuSubTrigger>
          <ListPlus size={15} />
          Add to Playlist
        </GlassMenuSubTrigger>
        <GlassMenuSubContent className={`${compactMenuClasses} max-w-64`}>
          <PlaylistChoices videoId={track.videoId} onDone={onClose} />
        </GlassMenuSubContent>
      </GlassMenuSub>
    </>
  );
}

function TrackMenuItems({ track, onClose }: { track: Track; onClose: () => void }) {
  const [copied, setCopied] = useState(false);
  const isDownloaded = useDownloads((s) => s.isDownloaded(track.videoId));
  const isDownloading = useDownloads((s) => s.isDownloading(track.videoId));
  const download = useDownloads((s) => s.download);
  const remove = useDownloads((s) => s.remove);
  const navigate = useNavigate();
  const signedIn = useSignedIn();
  const isPinned = usePinnedStore((s) => s.isPinned(track.videoId));
  const togglePin = usePinnedStore((s) => s.togglePin);

  const { playNext, addToQueue } = usePlayer.getState();
  const primaryArtist = track.artists[0];

  const handleStartRadio = () => {
    onClose();
    usePlayer.getState().playTrack(track, {
      title: `${track.title} Radio`,
      type: 'radio',
      id: `RDAMVM${track.videoId}`,
    });
  };

  const handleViewLyrics = () => {
    const player = usePlayer.getState();
    if (player.nowPlaying?.videoId !== track.videoId) {
      player.playTrack(track, { title: track.title, type: 'other' });
    }
    useUi.setState({ sidePanel: 'lyrics' });
    onClose();
  };

  const handleListenTogether = () => {
    onClose();
    const player = usePlayer.getState();
    if (player.nowPlaying?.videoId !== track.videoId) {
      player.playTrack(track, { title: track.title, type: 'other' });
    }
    navigate('/together');
  };

  const handleShare = async () => {
    const base = `${window.location.origin}${window.location.pathname}`.replace(/\/$/, '');
    const url = `${base}/#/watch?v=${track.videoId}`;
    const artistText = track.artists.map((a) => a.name).join(', ');
    const text = `Listen to ${track.title}${artistText ? ` by ${artistText}` : ''} on AURA`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: track.title,
          text,
          url,
        });
        onClose();
        return;
      } catch {
        // User cancelled or share dismissed
      }
    }
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => onClose(), 800);
    } catch {
      onClose();
    }
  };

  const handleTogglePin = () => {
    togglePin(track);
    onClose();
  };

  const handleToggleDownload = async () => {
    if (isDownloading) return;
    if (isDownloaded) {
      await remove(track.videoId);
      onClose();
    } else {
      try {
        await download(track);
        onClose();
      } catch {
        // Handled in store
      }
    }
  };

  const handleExportAudio = async () => {
    try {
      await exportTrackFile(track);
      onClose();
    } catch {
      // Handled
    }
  };

  const handleDownloadArtwork = async () => {
    if (!track.thumbnail) return;
    try {
      const res = await fetch(track.thumbnail);
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = `${track.title} - Artwork.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(blobUrl);
    } catch {
      window.open(track.thumbnail, '_blank');
    }
    onClose();
  };

  return (
    <>
      <GlassMenuItem onSelect={() => (onClose(), playNext([track]))}>
        <ListStart size={15} />
        Play Next
      </GlassMenuItem>
      <GlassMenuItem onSelect={() => (onClose(), addToQueue([track]))}>
        <ListEnd size={15} />
        Add to Queue
      </GlassMenuItem>
      <GlassMenuItem onSelect={handleStartRadio}>
        <Radio size={15} />
        Start Radio
      </GlassMenuItem>

      <GlassMenuSeparator />

      {signedIn ? (
        <AccountItems track={track} onClose={onClose} />
      ) : (
        <>
          <GlassMenuItem onSelect={() => (onClose(), navigate('/settings'))}>
            <Heart size={15} />
            Sign in to Like
          </GlassMenuItem>
          <GlassMenuItem onSelect={() => (onClose(), navigate('/settings'))}>
            <ListPlus size={15} />
            Sign in for Playlists
          </GlassMenuItem>
        </>
      )}

      {(primaryArtist?.id || track.album?.id) && <GlassMenuSeparator />}

      {primaryArtist?.id && (
        <GlassMenuItem
          onSelect={() => {
            onClose();
            const href = artistHref(primaryArtist.id);
            if (href) navigate(href);
          }}
        >
          <User size={15} />
          Go to Artist
        </GlassMenuItem>
      )}
      {track.album?.id && (
        <GlassMenuItem
          onSelect={() => {
            onClose();
            const href = albumHref(track.album?.id ?? null);
            if (href) navigate(href);
          }}
        >
          <Disc size={15} />
          Go to Album
        </GlassMenuItem>
      )}

      <GlassMenuSeparator />

      <GlassMenuItem onSelect={handleViewLyrics}>
        <MessageSquareQuote size={15} />
        View Lyrics
      </GlassMenuItem>
      <GlassMenuItem onSelect={handleListenTogether}>
        <Users size={15} />
        Listen Together
      </GlassMenuItem>

      <GlassMenuSeparator />

      <GlassMenuItem
        destructive={isDownloaded && !isDownloading}
        onSelect={handleToggleDownload}
        disabled={isDownloading}
      >
        {isDownloading ? (
          <Loader2 size={15} className="animate-spin text-key" />
        ) : isDownloaded ? (
          <Trash2 size={15} />
        ) : (
          <Download size={15} />
        )}
        {isDownloading
          ? 'Downloading…'
          : isDownloaded
            ? 'Remove download'
            : 'Download'}
      </GlassMenuItem>
      <GlassMenuItem onSelect={handleExportAudio}>
        <FileDown size={15} />
        Export Audio File
      </GlassMenuItem>
      <GlassMenuItem onSelect={handleTogglePin}>
        <Pin size={15} className={isPinned ? 'text-key fill-key' : ''} />
        {isPinned ? 'Unpin from Listen again' : 'Pin to Listen again'}
      </GlassMenuItem>
      <GlassMenuItem closeOnSelect={false} onSelect={handleShare}>
        <Share2 size={15} />
        {copied ? 'Link Copied!' : 'Share Song'}
      </GlassMenuItem>
      {track.thumbnail && (
        <GlassMenuItem onSelect={handleDownloadArtwork}>
          <ImageIcon size={15} />
          Download Artwork
        </GlassMenuItem>
      )}
      <GlassMenuSub>
        <GlassMenuSubTrigger>
          <Info size={15} />
          Song Credits &amp; Info
        </GlassMenuSubTrigger>
        <GlassMenuSubContent className={`${compactMenuClasses} max-w-72`}>
          <TrackDetailsView track={track} />
        </GlassMenuSubContent>
      </GlassMenuSub>
    </>
  );
}

const compactMenuClasses =
  '!min-w-[185px] !rounded-2xl !p-1.5 !text-[13px] [&_[role=menuitem]]:!h-8 [&_[role=menuitem]]:!rounded-xl [&_[role=menuitem]]:!px-2.5 [&_[role=separator]]:!mx-2.5 [&_[role=separator]]:!my-1';

function TrackMenuButton({ children, ...props }: ComponentProps<'button'>) {
  const popup = useContext(GlassPopupContext);
  return (
    <button data-glass-host="" {...props}>
      {popup && <GlassPopupAnchor popup={popup} />}
      <span className="inline-flex items-center justify-center">{children}</span>
    </button>
  );
}

/**
 * The ⋯ button at the end of a track row (report §14): Play Next and Add to
 * Queue, Start Radio, Go to Artist/Album, Lyrics, Share, and Account actions.
 */
export function TrackMenu({
  track,
  className,
  placement,
}: {
  track: Track;
  className?: string;
  placement?: MenuPlacement;
}) {
  const [open, setOpen] = useState(false);
  const close = useCallback(() => setOpen(false), []);
  const side = placement === 'above-start' ? 'top' : 'bottom';
  const align = placement === 'above-start' ? 'start' : 'end';

  return (
    <GlassMenu open={open} onOpenChange={setOpen}>
      <GlassMenuTrigger asChild>
        <TrackMenuButton
          type="button"
          className={className}
          aria-label={`More options for ${track.title}`}
          onClick={(event) => event.stopPropagation()}
          onDoubleClick={(event) => event.stopPropagation()}
        >
          <MoreHorizontal size={18} strokeWidth={2} />
        </TrackMenuButton>
      </GlassMenuTrigger>
      <GlassMenuContent
        side={side}
        align={align}
        aria-label={track.title}
        className={`${compactMenuClasses} [&_[data-glass-list]]:max-h-[70vh] [&_[data-glass-list]]:overflow-y-auto`}
      >
        <TrackMenuItems track={track} onClose={close} />
      </GlassMenuContent>
    </GlassMenu>
  );
}
