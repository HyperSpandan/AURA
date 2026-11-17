import {
  ArrowDownToLine,
  ChevronRight,
  Clock,
  Disc3,
  Heart,
  History,
  ListMusic,
  Mic2,
  Pin,
  Radio,
  Sparkles,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { PrefetchLink } from '@/components/PrefetchLink';
import { SquareLockup } from '@/components/music/Lockups';
import { TrackShelf } from '@/components/music/ShelfView';
import { ReplayCards } from '@/components/library/ReplayCards';
import { ReplayStoryModal } from '@/components/library/ReplayStoryModal';
import { useDownloads } from '@/data/download/useDownloads';
import { computeReplay } from '@/data/history/historyStore';
import { LIKED_MUSIC_PLAYLIST } from '@/data/music/library';
import { useLibrary, useLibraryPlaylists, usePlaylist, useSignedIn } from '@/data/music/queries';
import { usePinnedStore } from '@/data/pinned/pinnedStore';
import type { MusicItem, Track } from '@/data/music/types';
import { useYtAuth } from '@/extension/useYtAuth';

export default function LibraryHomePage() {
  const signedIn = useSignedIn();
  const ytAuth = useYtAuth();
  const downloads = useDownloads((s) => s.tracks);
  const [showStory, setShowStory] = useState(false);
  const [storySlide, setStorySlide] = useState(0);

  // Queries for library data
  const recentQuery = useLibrary('recent');
  const likedPlaylistQuery = usePlaylist(LIKED_MUSIC_PLAYLIST);
  const playlistsQuery = useLibraryPlaylists();
  const pinnedItems = usePinnedStore((s) => s.items);

  const recentTracks: Track[] = useMemo(() => {
    return recentQuery.data?.pages.flatMap((page) => page.data.tracks) ?? [];
  }, [recentQuery.data]);

  const likedTracksCount = useMemo(() => {
    const meta = likedPlaylistQuery.data?.pages[0]?.header?.meta;
    const match = meta?.match(/(\d+)\s+(?:tracks|songs)/i);
    if (match?.[1]) return parseInt(match[1], 10);
    const list = likedPlaylistQuery.data?.pages.flatMap((page) => page.tracks) ?? [];
    return list.length || 71;
  }, [likedPlaylistQuery.data]);

  const userName = useMemo(() => {
    if (ytAuth.state === 'ready' && ytAuth.auth.active?.name) {
      return ytAuth.auth.active.name;
    }
    return 'Spandan Gamer';
  }, [ytAuth]);

  // Compute replay stats from listening history + recent tracks
  const replaySummary = useMemo(() => {
    return computeReplay(recentTracks);
  }, [recentTracks]);

  const memberSince = '11/26';

  const userPlaylists: MusicItem[] = useMemo(() => {
    if (playlistsQuery.data && playlistsQuery.data.length > 0) {
      return playlistsQuery.data.slice(0, 8).map((p) => ({
        kind: 'playlist',
        id: p.id,
        title: p.title,
        subtitle: 'Playlist',
        thumbnail: p.thumbnail,
        artists: [],
        album: null,
        wide: false,
        durationSec: null,
        explicit: false,
      }));
    }
    return [];
  }, [playlistsQuery.data]);

  const shortcutTiles = [
    {
      to: `/playlist/${LIKED_MUSIC_PLAYLIST}`,
      title: 'Liked',
      subtitle: signedIn ? `${likedTracksCount} songs` : 'Your favorites',
      icon: Heart,
      iconColor: 'text-rose-400',
      badgeBg: 'bg-rose-500/15',
    },
    {
      to: '/library/downloads',
      title: 'Downloads',
      subtitle: `${downloads.length} ${downloads.length === 1 ? 'song' : 'songs'}`,
      icon: ArrowDownToLine,
      iconColor: 'text-emerald-400',
      badgeBg: 'bg-emerald-500/15',
    },
    {
      to: '/library/recent',
      title: 'Recently Played',
      subtitle: 'Listening history',
      icon: History,
      iconColor: 'text-amber-400',
      badgeBg: 'bg-amber-500/15',
    },
    {
      to: '/library/playlists',
      title: 'Playlists',
      subtitle: `${userPlaylists.length || 'All'} playlists`,
      icon: ListMusic,
      iconColor: 'text-purple-400',
      badgeBg: 'bg-purple-500/15',
    },
    {
      to: '/library/artists',
      title: 'Artists',
      subtitle: 'Your artists',
      icon: Mic2,
      iconColor: 'text-sky-400',
      badgeBg: 'bg-sky-500/15',
    },
    {
      to: '/library/albums',
      title: 'Albums',
      subtitle: 'Saved albums',
      icon: Disc3,
      iconColor: 'text-indigo-400',
      badgeBg: 'bg-indigo-500/15',
    },
    {
      to: '/library/made-for-you',
      title: 'Made for You',
      subtitle: 'Personalized mixes',
      icon: Sparkles,
      iconColor: 'text-amber-300',
      badgeBg: 'bg-amber-500/15',
    },
    {
      to: '/together',
      title: 'Listen Together',
      subtitle: 'Rooms with friends',
      icon: Radio,
      iconColor: 'text-teal-400',
      badgeBg: 'bg-teal-500/15',
    },
  ];

  return (
    <div className="space-y-8 pb-12">
      {/* Header with History Quick-link */}
      <header className="ot-home-heading">
        <div>
          <h1 className="type-header-emphasized">Library</h1>
        </div>
        <div>
          <PrefetchLink
            className="ot-circle-button"
            to="/library/recent"
            aria-label="Listening history"
            title="Listening history"
          >
            <Clock size={20} />
          </PrefetchLink>
        </div>
      </header>

      {/* Your Listening Experience Cards (Replay) */}
      <section className="space-y-3">
        <div className="mx-(--body-gutter) flex items-center justify-between">
          <h2 className="type-title-2-emphasized text-primary">Your Listening Experience</h2>
          <button
            type="button"
            onClick={() => {
              setStorySlide(0);
              setShowStory(true);
            }}
            className="inline-flex items-center gap-1 type-callout-emphasized text-key hover:underline"
          >
            <span>Play Replay</span>
            <ChevronRight size={16} />
          </button>
        </div>
        <ReplayCards
          summary={replaySummary}
          userName={userName}
          memberSince={memberSince}
          onOpenStory={(slide) => {
            setStorySlide(slide ?? 0);
            setShowStory(true);
          }}
        />
      </section>

      {/* Shortcut Tiles */}
      <section className="mx-(--body-gutter)">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {shortcutTiles.map((tile) => {
            const Icon = tile.icon;
            return (
              <PrefetchLink
                key={tile.to}
                to={tile.to}
                className="group relative flex items-center gap-3.5 rounded-[20px] border border-white/5 bg-quinary/60 p-4 backdrop-blur-md transition-all duration-200 hover:scale-[1.02] hover:bg-quinary hover:shadow-lg active:scale-98"
              >
                <div
                  className={`grid size-11 shrink-0 place-items-center rounded-2xl ${tile.badgeBg} ${tile.iconColor} shadow-inner transition-transform duration-200 group-hover:scale-110`}
                >
                  <Icon size={22} strokeWidth={2.2} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-bold text-primary group-hover:text-white">
                    {tile.title}
                  </p>
                  <p className="truncate text-xs text-secondary">{tile.subtitle}</p>
                </div>
              </PrefetchLink>
            );
          })}
        </div>
      </section>

      {/* Pinned - Listen Again */}
      {pinnedItems.length > 0 && (
        <section className="mx-(--body-gutter) space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="type-title-2-emphasized text-primary flex items-center gap-2">
              <Pin size={18} className="text-key fill-key" />
              <span>Listen again</span>
            </h2>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8">
            {pinnedItems.map((p) => {
              const item: MusicItem =
                'kind' in p.item && (p.item.kind === 'playlist' || p.item.kind === 'album')
                  ? (p.item as MusicItem)
                  : {
                      kind: 'song',
                      id: (p.item as Track).videoId,
                      title: p.item.title,
                      subtitle: (p.item as Track).artists?.map((a) => a.name).join(', ') || 'Song',
                      artists: (p.item as Track).artists ?? [],
                      album: (p.item as Track).album ?? null,
                      thumbnail: p.item.thumbnail ?? null,
                      wide: false,
                      durationSec: (p.item as Track).durationSec ?? null,
                      explicit: (p.item as Track).explicit ?? false,
                    };
              return <SquareLockup key={p.id} item={item} />;
            })}
          </div>
        </section>
      )}

      {/* Recently Played Shelf */}
      {recentTracks.length > 0 && (
        <section className="space-y-3">
          <TrackShelf
            title="Recently Played"
            to="/library/recent"
            tracks={recentTracks.slice(0, 10)}
            source={{ title: 'Recently Played', type: 'library', id: 'recent' }}
          />
        </section>
      )}

      {/* Downloaded Songs Shelf (if any) */}
      {downloads.length > 0 && (
        <section className="space-y-3">
          <TrackShelf
            title="Downloaded Songs"
            to="/library/downloads"
            tracks={downloads.slice(0, 10)}
            source={{ title: 'Downloads', type: 'library', id: 'downloads' }}
          />
        </section>
      )}

      {/* Playlists Shelf */}
      {userPlaylists.length > 0 && (
        <section className="mx-(--body-gutter) space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="type-title-2-emphasized text-primary">Playlists</h2>
            <PrefetchLink
              to="/library/playlists"
              className="type-callout-emphasized text-key hover:underline"
            >
              Show all
            </PrefetchLink>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8">
            {userPlaylists.map((playlist) => (
              <SquareLockup key={playlist.id} item={playlist} />
            ))}
          </div>
        </section>
      )}

      {/* Replay Story Modal */}
      {showStory && (
        <ReplayStoryModal
          summary={replaySummary}
          userName={userName}
          memberSince={memberSince}
          initialSlide={storySlide}
          onClose={() => setShowStory(false)}
        />
      )}
    </div>
  );
}
