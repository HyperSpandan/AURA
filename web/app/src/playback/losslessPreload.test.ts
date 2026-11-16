import { describe, expect, it, vi, beforeEach } from 'vitest';
import {
  consumePrewarmedStream,
  getPrewarmedStream,
  prefetchLosslessTrack,
  prunePrewarmedStreams,
} from './losslessPreload';
import { useAddons } from '@/addons/client';
import * as clientModule from '@/addons/client';
import type { Track } from '@/data/music/types';

describe('losslessPreload', () => {
  const dummyTrack: Track = {
    videoId: 'test_vid_123',
    title: 'Test Song',
    artists: [{ name: 'Test Artist', id: 'artist1' }],
    durationSec: 200,
    album: { name: 'Test Album', id: 'album1' },
    thumbnail: 'https://example.com/art.jpg',
  };

  beforeEach(() => {
    consumePrewarmedStream('test_vid_123');
    useAddons.setState({
      lossless: true,
      jioSaavn: true,
      sources: [{ id: 'src1', name: 'Monochrome', url: 'https://src1.example/manifest.json', enabled: true }],
    });
  });

  it('stores and retrieves pre-warmed streams when prefetching FLAC', async () => {
    expect(getPrewarmedStream('test_vid_123')).toBeNull();

    vi.spyOn(clientModule, 'findLossless').mockResolvedValue({
      ranked: [
        {
          url: 'https://audio.example/flac/test.flac',
          provider: 'Monochrome',
          bits: 24,
          rate: 44100,
          duration: 200,
          sourceId: 'src1',
          cors: true,
          stated: false,
        },
      ],
      notes: [],
      transient: false,
    });

    const prewarmed = await prefetchLosslessTrack(dummyTrack, { title: 'Test Song', artist: 'Test Artist' });
    expect(prewarmed).not.toBeNull();
    expect(prewarmed?.mime).toBe('audio/flac');
    expect(prewarmed?.client).toBe('ADDON');
    expect(prewarmed?.format).toContain('FLAC 24-bit 44.1 kHz');

    // Should be accessible via getPrewarmedStream
    const cached = getPrewarmedStream('test_vid_123');
    expect(cached).toBe(prewarmed);

    // Consuming removes it from cache
    const consumed = consumePrewarmedStream('test_vid_123');
    expect(consumed).toBe(prewarmed);
    expect(getPrewarmedStream('test_vid_123')).toBeNull();
  });

  it('prunes prewarmed streams when tracks leave upcoming queue', async () => {
    vi.spyOn(clientModule, 'findLossless').mockResolvedValue({
      ranked: [
        {
          url: 'https://audio.example/flac/test.flac',
          provider: 'Monochrome',
          bits: 24,
          rate: 44100,
          duration: 200,
          sourceId: 'src1',
          cors: true,
          stated: false,
        },
      ],
      notes: [],
      transient: false,
    });

    await prefetchLosslessTrack(dummyTrack);
    expect(getPrewarmedStream('test_vid_123')).not.toBeNull();

    // Prune tracks not in the keep set
    prunePrewarmedStreams(new Set(['other_vid']));
    expect(getPrewarmedStream('test_vid_123')).toBeNull();
  });

  it('ignores pre-warmed FLAC stream if lossless toggle is disabled', async () => {
    vi.spyOn(clientModule, 'findLossless').mockResolvedValue({
      ranked: [
        {
          url: 'https://audio.example/flac/test.flac',
          provider: 'Monochrome',
          bits: 24,
          rate: 44100,
          duration: 200,
          sourceId: 'src1',
          cors: true,
          stated: false,
        },
      ],
      notes: [],
      transient: false,
    });

    await prefetchLosslessTrack(dummyTrack);
    expect(getPrewarmedStream('test_vid_123')).not.toBeNull();

    useAddons.setState({ lossless: false });
    expect(getPrewarmedStream('test_vid_123')).toBeNull();
  });
});
