import { useCallback, useMemo } from 'react';
import { useParams } from 'react-router';
import { DetailHeader } from '@/components/music/DetailHeader';
import { PlaylistMenu } from '@/components/music/PlaylistMenu';
import { playList, shuffleList } from '@/components/music/play';
import { LoadError, LoadMoreError, NeedsExtension } from '@/components/music/States';
import { TrackList } from '@/components/music/TrackList';
import { LIKED_MUSIC_PLAYLIST } from '@/data/music/library';
import { usePlaylist, useYouTubeReady } from '@/data/music/queries';
import { LIKED_ID } from '@/data/spotify/api';
import { useSpotifyRows } from '@/data/spotify/queries';
import { useSpotifySignedIn } from '@/extension/useSpotifyAuth';
import { useArtworkColor } from '@/lib/artworkColor';
import { usePageTint } from '@/lib/usePageTint';
import { PlaylistSkeleton } from '@/skeletons/PageSkeletons';
import { detailFooter } from './pageStyles';

/** A playlist (report §14): header, then the artwork track list, loading on as you scroll. */
export default function PlaylistPage() {
  const id = decodeURIComponent(useParams().id ?? '');
  const isLikedPage = id === LIKED_MUSIC_PLAYLIST;
  const spotifySignedIn = useSpotifySignedIn();
  const spotifyRows = useSpotifyRows(isLikedPage && spotifySignedIn ? LIKED_ID : '');
  const { missing } = useYouTubeReady();
  const playlist = usePlaylist(id);
  const header = playlist.data?.pages[0]?.header ?? null;
  usePageTint(useArtworkColor(header?.thumbnail));

  const { hasNextPage, isFetchingNextPage, isFetchNextPageError, fetchNextPage } = playlist;
  const onNearEnd = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage && !isFetchNextPageError) void fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, isFetchNextPageError, fetchNextPage]);

  const ytmTracks = playlist.data?.pages.flatMap((page) => page.tracks) ?? [];
  const tracks = useMemo(() => {
    if (!isLikedPage || !spotifySignedIn || !spotifyRows.rows.length) return ytmTracks;
    const spotifyPlayable = spotifyRows.rows.filter((t) => !t.unavailable);
    const seen = new Set(ytmTracks.map((t) => t.videoId));
    const merged = [...ytmTracks];
    for (const t of spotifyPlayable) {
      if (!seen.has(t.videoId)) {
        seen.add(t.videoId);
        merged.push(t);
      }
    }
    return merged;
  }, [isLikedPage, spotifySignedIn, ytmTracks, spotifyRows.rows]);

  const headerInfo = useMemo(() => {
    if (!header) return null;
    if (!isLikedPage || !spotifySignedIn) return header;
    return {
      ...header,
      meta: `${tracks.length} songs${spotifyRows.matching > 0 ? ` • matching ${spotifyRows.matching} from Spotify…` : ''}`,
    };
  }, [header, isLikedPage, spotifySignedIn, tracks.length, spotifyRows.matching]);

  if (missing) return <NeedsExtension />;
  if (playlist.isError && !playlist.data) return <LoadError error={playlist.error} onRetry={() => void playlist.refetch()} />;
  if (!playlist.data || !headerInfo) return <PlaylistSkeleton />;

  const context = { source: { title: headerInfo.title, type: 'playlist' as const, id } };
  return (
    <>
      <DetailHeader
        info={headerInfo}
        canPlay={tracks.length > 0}
        onPlay={() => playList(tracks, 0, context)}
        onShuffle={() => shuffleList(tracks, context)}
        menu={
          <PlaylistMenu
            id={id}
            title={headerInfo.title}
            tracks={tracks}
            thumbnail={headerInfo.thumbnail}
            library={headerInfo.library}
          />
        }
      />
      <TrackList tracks={tracks} variant="playlist" onNearEnd={onNearEnd} source={context.source} />
      {isFetchingNextPage && <div className="flex justify-center py-5 type-callout text-secondary">Loading more songs…</div>}
      {playlist.isFetchNextPageError && <LoadMoreError onRetry={() => void fetchNextPage()} />}
      <footer className={detailFooter}>
        <p>{headerInfo.meta}</p>
      </footer>
    </>
  );
}
