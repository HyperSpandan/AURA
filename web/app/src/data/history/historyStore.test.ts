import { describe, expect, it } from 'vitest';
import { computeReplay } from './historyStore';
import type { Track } from '@/data/music/types';

describe('computeReplay', () => {
  it('computes empty summary when no tracks exist', () => {
    const summary = computeReplay([]);
    expect(summary.totalPlays).toBe(0);
    expect(summary.topSongs).toEqual([]);
    expect(summary.topArtists).toEqual([]);
  });

  it('aggregates track plays and artists properly', () => {
    const fakeTracks: Track[] = [
      {
        videoId: 'vid1',
        title: 'Song One',
        artists: [{ name: 'Artist A', browseId: 'artA' }],
        album: { name: 'Album X', browseId: 'albX' },
        durationSec: 180,
      },
      {
        videoId: 'vid1',
        title: 'Song One',
        artists: [{ name: 'Artist A', browseId: 'artA' }],
        album: { name: 'Album X', browseId: 'albX' },
        durationSec: 180,
      },
      {
        videoId: 'vid2',
        title: 'Song Two',
        artists: [{ name: 'Artist B', browseId: 'artB' }],
        album: { name: 'Album Y', browseId: 'albY' },
        durationSec: 240,
      },
    ];

    const summary = computeReplay(fakeTracks);
    expect(summary.totalPlays).toBe(3);
    expect(summary.topSongs[0]?.title).toBe('Song One');
    expect(summary.topSongs[0]?.plays).toBe(2);
    expect(summary.topArtists[0]?.title).toBe('Artist A');
    expect(summary.topArtists[0]?.plays).toBe(2);
  });
});
