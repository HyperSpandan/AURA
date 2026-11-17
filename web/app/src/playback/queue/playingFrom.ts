import type { QueueEntry } from './timeline';
import type { Track } from '@/data/music/types';
import type { NowPlaying } from '@/state/player';

export interface PlayingFrom {
  label: string;
  title: string;
}

/**
 * Derives a human-readable "Playing from" label and title for the full-screen player
 * and Up Next panel, matching Apple Music / Spotify / YouTube Music behavior.
 */
export function getPlayingFrom(
  entry?: QueueEntry | null,
  track?: Track | null,
  nowPlaying?: NowPlaying | null,
): PlayingFrom {
  const isAutoplay = entry?.tier === 'autoplay';
  if (isAutoplay) {
    return {
      label: 'Playing from radio',
      title: 'Similar Music (AutoPlay)',
    };
  }

  const source = entry?.source;
  if (source && source.title) {
    const rawTitle = source.title.trim();
    const type = source.type;

    if (type === 'playlist') {
      return {
        label: 'Playing from playlist',
        title: rawTitle,
      };
    }
    if (type === 'album') {
      return {
        label: 'Playing from album',
        title: rawTitle,
      };
    }
    if (type === 'artist') {
      return {
        label: 'Playing from artist',
        title: rawTitle.replace(/:\s*Top Songs$/i, '').trim(),
      };
    }
    if (type === 'library' || source.id === 'downloads') {
      if (source.id === 'downloads' || rawTitle.toLowerCase() === 'downloads') {
        return {
          label: 'Playing from downloads',
          title: 'Downloads',
        };
      }
      return {
        label: 'Playing from library',
        title: rawTitle,
      };
    }
    if (type === 'search') {
      return {
        label: 'Playing from search',
        title: rawTitle,
      };
    }
    if (type === 'radio') {
      return {
        label: 'Playing from radio',
        title: rawTitle,
      };
    }
    if (type === 'home') {
      return {
        label: 'Playing from',
        title: rawTitle,
      };
    }
    // For type === 'other', if it is distinct from the song's title, use it
    if (rawTitle && (!track || rawTitle.toLowerCase() !== track.title.toLowerCase())) {
      return {
        label: 'Playing from',
        title: rawTitle,
      };
    }
  }

  // Fallbacks from current track metadata:
  if (track?.album?.name) {
    return {
      label: 'Playing from album',
      title: track.album.name,
    };
  }

  const artist = track?.artists?.[0]?.name || nowPlaying?.artist;
  if (artist) {
    return {
      label: 'Playing from artist',
      title: artist,
    };
  }

  return {
    label: 'Playing from',
    title: 'AURA',
  };
}
