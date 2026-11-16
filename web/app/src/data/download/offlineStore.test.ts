import { describe, expect, it, beforeEach } from 'vitest';
import {
  clearOfflineTracks,
  deleteOfflineTrack,
  getAllOfflineTracks,
  getOfflineStorageUsage,
  getOfflineTrack,
  hasOfflineTrack,
  saveOfflineTrack,
  type OfflineTrackRecord,
} from './offlineStore';

describe('offlineStore', () => {
  const dummyRecord: OfflineTrackRecord = {
    videoId: 'offline_vid_999',
    track: {
      videoId: 'offline_vid_999',
      title: 'Midnight City',
      artists: [{ name: 'M83', id: 'm83' }],
      album: { name: 'Hurry Up, We\'re Dreaming', id: 'alb1' },
      durationSec: 243,
      explicit: false,
      thumbnail: 'https://example.com/art.jpg',
      index: 1,
      plays: null,
    },
    audioBlob: new Blob(['dummy audio bytes'], { type: 'audio/flac' }),
    mimeType: 'audio/flac',
    format: 'FLAC 24-bit 44.1 kHz',
    source: 'Monochrome',
    sizeBytes: 1024 * 1024 * 25,
    downloadedAt: 1700000000000,
  };

  beforeEach(async () => {
    try {
      await clearOfflineTracks();
    } catch {
      // Ignore if indexedDB not in environment
    }
  });

  it('saves and retrieves an offline track record', async () => {
    if (typeof indexedDB === 'undefined') return;

    await saveOfflineTrack(dummyRecord);

    const has = await hasOfflineTrack('offline_vid_999');
    expect(has).toBe(true);

    const retrieved = await getOfflineTrack('offline_vid_999');
    expect(retrieved).not.toBeNull();
    expect(retrieved?.videoId).toBe('offline_vid_999');
    expect(retrieved?.track.title).toBe('Midnight City');
    expect(retrieved?.format).toBe('FLAC 24-bit 44.1 kHz');
    expect(retrieved?.source).toBe('Monochrome');

    const all = await getAllOfflineTracks();
    expect(all).toHaveLength(1);
    expect(all[0]?.videoId).toBe('offline_vid_999');

    const usage = await getOfflineStorageUsage();
    expect(usage.count).toBe(1);
    expect(usage.totalBytes).toBe(1024 * 1024 * 25);

    await deleteOfflineTrack('offline_vid_999');
    expect(await hasOfflineTrack('offline_vid_999')).toBe(false);
    expect(await getOfflineTrack('offline_vid_999')).toBeNull();
  });
});
