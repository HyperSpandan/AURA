import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useDownloads } from './useDownloads';
import * as offlineStore from './offlineStore';
import * as downloadModule from '@/playback/download';
import type { Track } from '@/data/music/types';

describe('useDownloads', () => {
  const dummyTrack: Track = {
    videoId: 'dl_track_1',
    title: 'Starboy',
    artists: [{ name: 'The Weeknd', id: 'weeknd' }],
    album: { name: 'Starboy', id: 'alb_starboy' },
    durationSec: 230,
    explicit: true,
    thumbnail: 'https://example.com/starboy.jpg',
    index: 1,
    plays: '2B',
  };

  beforeEach(() => {
    useDownloads.setState({
      downloadedIds: new Set(),
      downloadingIds: new Set(),
      records: [],
      tracks: [],
      totalBytes: 0,
      initialized: true,
    });
  });

  it('tracks download state and reflects in store', async () => {
    expect(useDownloads.getState().isDownloaded('dl_track_1')).toBe(false);
    expect(useDownloads.getState().isDownloading('dl_track_1')).toBe(false);

    vi.spyOn(downloadModule, 'downloadTrack').mockResolvedValue({
      videoId: 'dl_track_1',
      track: dummyTrack,
      audioBlob: new Blob(['audio']),
      mimeType: 'audio/flac',
      format: 'FLAC 24-bit 44.1 kHz',
      source: 'Monochrome',
      sizeBytes: 15_000_000,
      downloadedAt: Date.now(),
    });

    await useDownloads.getState().download(dummyTrack);

    expect(useDownloads.getState().isDownloaded('dl_track_1')).toBe(true);
    expect(useDownloads.getState().isDownloading('dl_track_1')).toBe(false);
    expect(useDownloads.getState().tracks).toHaveLength(1);
    expect(useDownloads.getState().tracks[0]?.title).toBe('Starboy');
    expect(useDownloads.getState().totalBytes).toBe(15_000_000);

    vi.spyOn(offlineStore, 'deleteOfflineTrack').mockResolvedValue();
    await useDownloads.getState().remove('dl_track_1');

    expect(useDownloads.getState().isDownloaded('dl_track_1')).toBe(false);
    expect(useDownloads.getState().tracks).toHaveLength(0);
    expect(useDownloads.getState().totalBytes).toBe(0);
  });
});
