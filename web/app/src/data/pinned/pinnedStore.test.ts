import { beforeEach, describe, expect, it } from 'vitest';
import { usePinnedStore } from './pinnedStore';
import type { MusicItem, Track } from '@/data/music/types';

describe('pinnedStore', () => {
  const dummyTrack: Track = {
    videoId: 'pin_vid_1',
    title: 'Starboy',
    artists: [{ name: 'The Weeknd', id: 'weeknd' }],
    album: { name: 'Starboy', id: 'alb_1' },
    durationSec: 230,
    explicit: true,
    thumbnail: 'https://example.com/thumb.jpg',
    index: 1,
    plays: '10M',
  };

  const dummyPlaylist: MusicItem = {
    id: 'pin_pl_1',
    kind: 'playlist',
    title: 'Feel Good',
    subtitle: 'Playlist',
    thumbnail: 'https://example.com/pl.jpg',
    artists: [],
    album: null,
    wide: false,
    durationSec: null,
    explicit: false,
  };

  beforeEach(() => {
    usePinnedStore.setState({
      items: [],
      pinnedIds: new Set(),
    });
  });

  it('pins and unpins tracks and playlists properly', () => {
    expect(usePinnedStore.getState().isPinned('pin_vid_1')).toBe(false);

    usePinnedStore.getState().pin(dummyTrack);
    expect(usePinnedStore.getState().isPinned('pin_vid_1')).toBe(true);
    expect(usePinnedStore.getState().items).toHaveLength(1);
    expect(usePinnedStore.getState().items[0]?.title).toBe('Starboy');

    // Toggle playlist pin
    usePinnedStore.getState().togglePin(dummyPlaylist);
    expect(usePinnedStore.getState().isPinned('pin_pl_1')).toBe(true);
    expect(usePinnedStore.getState().items).toHaveLength(2);

    // Unpin track
    usePinnedStore.getState().unpin('pin_vid_1');
    expect(usePinnedStore.getState().isPinned('pin_vid_1')).toBe(false);
    expect(usePinnedStore.getState().items).toHaveLength(1);
    expect(usePinnedStore.getState().items[0]?.title).toBe('Feel Good');
  });
});
