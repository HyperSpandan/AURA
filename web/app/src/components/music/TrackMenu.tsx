import {
  Check,
  ChevronLeft,
  ChevronRight,
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
  Radio,
  Share2,
  Trash2,
  User,
  Users,
} from 'lucide-react';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router';
import { Menu, MenuHeader, MenuItem, type MenuPlacement, MenuSeparator } from '@/components/Menu';
import {
  useAddToPlaylist,
  useLikeStatus,
  usePlaylistOptions,
  useSetLiked,
  useSignedIn,
} from '@/data/music/queries';
import type { Track } from '@/data/music/types';
import { useDownloads } from '@/data/download/useDownloads';
import { exportTrackFile } from '@/playback/download';
import { usePlayer } from '@/state/player';
import { useUi } from '@/state/ui';
import { albumHref, artistHref, formatDuration } from './links';

function PlaylistChoices({ videoId, onDone, onBack }: { videoId: string; onDone: () => void; onBack: () => void }) {
  const options = usePlaylistOptions(videoId, true);
  const add = useAddToPlaylist();
  return (
    <>
      <MenuItem icon={<ChevronLeft size={14} />} onSelect={onBack}>
        Back
      </MenuItem>
      <MenuSeparator />
      <MenuHeader>Add to Playlist</MenuHeader>
      {options.isPending && (
        <MenuItem disabled onSelect={() => {}}>
          Loading…
        </MenuItem>
      )}
      {options.isError && (
        <MenuItem disabled onSelect={() => {}}>
          Couldn’t load your playlists
        </MenuItem>
      )}
      {options.data?.length === 0 && (
        <MenuItem disabled onSelect={() => {}}>
          No playlists yet
        </MenuItem>
      )}
      {options.data?.map((option) => (
        <MenuItem
          key={option.playlistId}
          onSelect={() => {
            add.mutate({ playlistId: option.playlistId, videoId });
            onDone();
          }}
        >
          {option.title}
        </MenuItem>
      ))}
    </>
  );
}

function TrackDetailsView({ track, onBack }: { track: Track; onBack: () => void }) {
  return (
    <>
      <MenuItem icon={<ChevronLeft size={14} />} onSelect={onBack}>
        Back
      </MenuItem>
      <MenuSeparator />
      <MenuHeader>Song Info</MenuHeader>
      <div className="px-2 py-1.5 flex flex-col gap-2 type-callout">
        <div>
          <span className="text-secondary block text-xs uppercase tracking-wider font-semibold">Title</span>
          <span className="text-primary font-medium">{track.title}</span>
        </div>
        <div>
          <span className="text-secondary block text-xs uppercase tracking-wider font-semibold">Artist</span>
          <span className="text-primary">{track.artists.map((a) => a.name).join(', ') || 'Unknown'}</span>
        </div>
        {track.album?.name && (
          <div>
            <span className="text-secondary block text-xs uppercase tracking-wider font-semibold">Album</span>
            <span className="text-primary">{track.album.name}</span>
          </div>
        )}
        {track.durationSec && (
          <div>
            <span className="text-secondary block text-xs uppercase tracking-wider font-semibold">Duration</span>
            <span className="text-primary">{formatDuration(track.durationSec)}</span>
          </div>
        )}
        <div>
          <span className="text-secondary block text-xs uppercase tracking-wider font-semibold">YouTube ID</span>
          <span className="text-primary font-mono text-xs select-all">{track.videoId}</span>
        </div>
      </div>
    </>
  );
}

/** Favorite and Add to Playlist: the account's actions, signed in only. */
function AccountItems({ track, onClose, onPlaylists }: { track: Track; onClose: () => void; onPlaylists: () => void }) {
  const status = useLikeStatus(track.videoId);
  const setLiked = useSetLiked();
  const liked = status.data === 'LIKE';
  return (
    <>
      <MenuItem
        icon={<Heart size={15} fill={liked ? '#ef4444' : 'none'} className={liked ? 'text-red-500' : 'text-current'} />}
        disabled={status.isPending}
        onSelect={() => {
          setLiked.mutate({ videoId: track.videoId, liked: !liked });
          onClose();
        }}
      >
        {liked ? 'Unlike' : 'Like'}
      </MenuItem>
      <MenuItem icon={<ListPlus size={15} />} trailing={<ChevronRight size={14} />} onSelect={onPlaylists}>
        Add to Playlist
      </MenuItem>
    </>
  );
}

function TrackMenuItems({ track, onClose }: { track: Track; onClose: () => void }) {
  const [view, setView] = useState<'main' | 'playlists' | 'details'>('main');
  const [copied, setCopied] = useState(false);
  const isDownloaded = useDownloads((s) => s.isDownloaded(track.videoId));
  const isDownloading = useDownloads((s) => s.isDownloading(track.videoId));
  const download = useDownloads((s) => s.download);
  const remove = useDownloads((s) => s.remove);
  const navigate = useNavigate();
  const signedIn = useSignedIn();

  if (view === 'playlists') {
    return <PlaylistChoices videoId={track.videoId} onDone={onClose} onBack={() => setView('main')} />;
  }

  if (view === 'details') {
    return <TrackDetailsView track={track} onBack={() => setView('main')} />;
  }

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
      <MenuItem icon={<ListStart size={15} />} onSelect={() => (onClose(), playNext([track]))}>
        Play Next
      </MenuItem>
      <MenuItem icon={<ListEnd size={15} />} onSelect={() => (onClose(), addToQueue([track]))}>
        Add to Queue
      </MenuItem>
      <MenuItem icon={<Radio size={15} />} onSelect={handleStartRadio}>
        Start Radio
      </MenuItem>

      <MenuSeparator />

      {signedIn ? (
        <AccountItems track={track} onClose={onClose} onPlaylists={() => setView('playlists')} />
      ) : (
        <>
          <MenuItem icon={<Heart size={15} />} onSelect={() => (onClose(), navigate('/settings'))}>
            Sign in to Like
          </MenuItem>
          <MenuItem icon={<ListPlus size={15} />} onSelect={() => (onClose(), navigate('/settings'))}>
            Sign in for Playlists
          </MenuItem>
        </>
      )}

      {(primaryArtist?.id || track.album?.id) && <MenuSeparator />}

      {primaryArtist?.id && (
        <MenuItem
          icon={<User size={15} />}
          onSelect={() => {
            onClose();
            const href = artistHref(primaryArtist.id);
            if (href) navigate(href);
          }}
        >
          Go to Artist
        </MenuItem>
      )}
      {track.album?.id && (
        <MenuItem
          icon={<Disc size={15} />}
          onSelect={() => {
            onClose();
            const href = albumHref(track.album?.id ?? null);
            if (href) navigate(href);
          }}
        >
          Go to Album
        </MenuItem>
      )}

      <MenuSeparator />

      <MenuItem icon={<MessageSquareQuote size={15} />} onSelect={handleViewLyrics}>
        View Lyrics
      </MenuItem>
      <MenuItem icon={<Users size={15} />} onSelect={handleListenTogether}>
        Listen Together
      </MenuItem>

      <MenuSeparator />

      <MenuItem
        icon={
          isDownloading ? (
            <Loader2 size={15} className="animate-spin text-key" />
          ) : isDownloaded ? (
            <Trash2 size={15} className="text-red-400" />
          ) : (
            <Download size={15} />
          )
        }
        onSelect={handleToggleDownload}
        disabled={isDownloading}
      >
        {isDownloading
          ? 'Downloading…'
          : isDownloaded
            ? 'Remove download'
            : 'Download'}
      </MenuItem>
      <MenuItem icon={<FileDown size={15} />} onSelect={handleExportAudio}>
        Export Audio File
      </MenuItem>
      <MenuItem icon={<Share2 size={15} />} onSelect={handleShare}>
        {copied ? 'Link Copied!' : 'Share Song'}
      </MenuItem>
      {track.thumbnail && (
        <MenuItem icon={<ImageIcon size={15} />} onSelect={handleDownloadArtwork}>
          Download Artwork
        </MenuItem>
      )}
      <MenuItem icon={<Info size={15} />} trailing={<ChevronRight size={14} />} onSelect={() => setView('details')}>
        Song Credits & Info
      </MenuItem>
    </>
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
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const close = useCallback(() => setAnchor(null), []);
  return (
    <>
      <button
        className={className}
        aria-label={`More options for ${track.title}`}
        aria-haspopup="menu"
        aria-expanded={anchor !== null}
        data-open={anchor !== null || undefined}
        onClick={(event) => {
          event.stopPropagation();
          setAnchor(anchor ? null : event.currentTarget);
        }}
        onDoubleClick={(event) => event.stopPropagation()}
      >
        <MoreHorizontal size={18} strokeWidth={2} />
      </button>
      {anchor && (
        <Menu anchor={anchor} placement={placement} onClose={close} label={track.title}>
          <TrackMenuItems track={track} onClose={close} />
        </Menu>
      )}
    </>
  );
}
