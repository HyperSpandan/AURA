import { useEffect } from 'react';
import { Navigate, useParams, useSearchParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { DetailHeader } from '@/components/music/DetailHeader';
import { playList, shuffleList } from '@/components/music/play';
import { LoadError, NeedsExtension } from '@/components/music/States';
import { TrackList } from '@/components/music/TrackList';
import { fetchRadio, fetchTrackLinks } from '@/data/music/api';
import { useYouTubeReady } from '@/data/music/queries';
import type { HeaderInfo, Track } from '@/data/music/types';
import { useArtworkColor } from '@/lib/artworkColor';
import { usePageTint } from '@/lib/usePageTint';
import { AlbumSkeleton } from '@/skeletons/PageSkeletons';
import { usePlayer } from '@/state/player';
import { detailFooter } from './pageStyles';

export default function WatchPage() {
  const [searchParams] = useSearchParams();
  const params = useParams();
  const videoId = searchParams.get('v') || params.id || null;

  const { missing } = useYouTubeReady();

  const trackQuery = useQuery({
    queryKey: ['music', 'watch-track', videoId],
    queryFn: () => (videoId ? fetchTrackLinks(videoId) : null),
    enabled: !!videoId,
  });

  const radioQuery = useQuery({
    queryKey: ['music', 'watch-radio', videoId],
    queryFn: () => (videoId ? fetchRadio(videoId) : []),
    enabled: !!videoId,
  });

  const track = trackQuery.data;
  usePageTint(useArtworkColor(track?.thumbnail));

  useEffect(() => {
    if (!videoId) return;
    const player = usePlayer.getState();
    if (player.nowPlaying?.videoId !== videoId) {
      if (track) {
        player.playTrack(track, {
          title: `${track.title} Radio`,
          type: 'radio',
          id: `RDAMVM${videoId}`,
        });
      } else {
        void player.playVideo(videoId);
      }
    }
  }, [videoId, track]);

  if (!videoId) {
    return <Navigate to="/home" replace />;
  }

  if (missing) return <NeedsExtension />;
  if (trackQuery.isError) {
    return <LoadError error={trackQuery.error} onRetry={() => void trackQuery.refetch()} />;
  }
  if (!track) {
    return <AlbumSkeleton />;
  }

  const artists = track.artists.map((a) => a.name).join(', ') || 'Unknown Artist';
  const radioTracks: Track[] = radioQuery.data?.map((c) => c.track) ?? [];
  const allTracks: Track[] = [track, ...radioTracks.filter((t) => t.videoId !== track.videoId)];

  const headerInfo: HeaderInfo = {
    title: track.title,
    subtitle: artists,
    meta: track.album?.name ?? 'Single',
    thumbnail: track.thumbnail,
    artists: track.artists,
    description: `Shared on AURA. Playing next from ${track.title} Radio.`,
    explicit: track.explicit,
    library: null,
  };

  const context = {
    source: { title: `${track.title} Radio`, type: 'radio' as const, id: `RDAMVM${videoId}` },
    fallbackArtwork: track.thumbnail,
    fallbackArtists: track.artists,
  };

  return (
    <>
      <DetailHeader
        info={headerInfo}
        canPlay={true}
        onPlay={() => playList(allTracks, 0, context)}
        onShuffle={() => shuffleList(allTracks, context)}
      />
      <div className="px-(--body-gutter) mb-4">
        <h2 className="type-title-2-emphasized text-primary">Playing & Up Next</h2>
      </div>
      <TrackList
        tracks={allTracks}
        variant="playlist"
        fallbackArtwork={track.thumbnail}
        fallbackArtists={track.artists}
        source={context.source}
      />
      <footer className={detailFooter}>
        <p>{track.title}</p>
        <p>{artists}</p>
        {track.album?.name && <p>{track.album.name}</p>}
      </footer>
    </>
  );
}
