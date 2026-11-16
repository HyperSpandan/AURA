import { fetchJson, findLossless, useAddons, type LosslessResult } from '@/addons/client';
import { findJioSaavn } from '@/addons/jiosaavn';
import type { Track } from '@/data/music/types';
import { trackLog } from '@/playback/trackLog';

export interface PrewarmedStream {
  videoId: string;
  url: string;
  mime: string;
  format: string;
  client: 'ADDON' | 'JIOSAAVN';
  cors: boolean;
  durationSec: number;
  statusNote?: string;
  sourceId?: string;
  expiresAt: number;
  audioElement?: HTMLAudioElement;
}

const prewarmedStreams = new Map<string, PrewarmedStream>();
const inFlight = new Map<string, Promise<PrewarmedStream | null>>();

/**
 * Pre-buffers the audio media stream into the browser cache.
 */
function bufferAudio(url: string, cors: boolean): HTMLAudioElement | undefined {
  if (typeof window === 'undefined' || typeof Audio === 'undefined') return undefined;
  try {
    const audio = new Audio();
    if (cors) audio.crossOrigin = 'anonymous';
    audio.preload = 'auto';
    audio.src = url;
    audio.load();
    return audio;
  } catch {
    return undefined;
  }
}

/**
 * Resolves and buffers FLAC / lossless audio for upcoming tracks in the background,
 * exactly like the Android app's readyAhead / ExternalStreams pipeline.
 */
export async function prefetchLosslessTrack(
  track: Track,
  hint?: { title: string; artist: string; album?: string | null },
): Promise<PrewarmedStream | null> {
  const videoId = track.videoId;
  if (!videoId) return null;

  const existing = prewarmedStreams.get(videoId);
  if (existing && existing.expiresAt > Date.now()) {
    return existing;
  }

  const existingInFlight = inFlight.get(videoId);
  if (existingInFlight) return existingInFlight;

  const job = (async (): Promise<PrewarmedStream | null> => {
    const addons = useAddons.getState();
    const anyFlac = addons.lossless && addons.sources.some((s) => s.enabled && !s.ownerOff);
    const anySaavn = addons.jioSaavn;
    if (!anyFlac && !anySaavn) return null;

    const recording = {
      title: hint?.title || track.title,
      artist: hint?.artist || (track.artists ? track.artists.map((a) => a.name).join(', ') : ''),
      duration: track.durationSec || 0,
    };

    // Concurrently prefetch verified FLAC and JioSaavn
    const flacPromise = anyFlac ? findLossless(recording, true).catch(() => null) : Promise.resolve(null);
    const saavnPromise = anySaavn
      ? findJioSaavn({ ...recording, album: hint?.album }, fetchJson).catch(() => null)
      : Promise.resolve(null);

    const [flacResult, saavnResult] = await Promise.all([flacPromise, saavnPromise]);

    // Check if FLAC candidate was found and verified
    const enabled = (source: LosslessResult) => useAddons.getState().sources.some((s) => s.id === source.sourceId && s.enabled);
    const candidates = flacResult?.ranked.filter(enabled) ?? [];

    if (candidates.length > 0) {
      const best = candidates[0]!;
      const format = `${best.provider} · FLAC ${best.bits}-bit ${(best.rate / 1000).toFixed(1)} kHz`;
      const found = `${best.provider} · ${best.stated ? 'FLAC, as the source states it' : 'verified FLAC'}`;
      const statusNote = [found, ...(flacResult?.notes ?? [])].join('\n');
      const audioEl = bufferAudio(best.url, best.cors);

      const prewarmed: PrewarmedStream = {
        videoId,
        url: best.url,
        mime: 'audio/flac',
        format,
        client: 'ADDON',
        cors: best.cors,
        durationSec: best.duration || track.durationSec || 0,
        statusNote,
        sourceId: best.sourceId,
        expiresAt: Date.now() + 10 * 60_000,
        audioElement: audioEl,
      };

      prewarmedStreams.set(videoId, prewarmed);
      trackLog.i(`FLAC audio for next song pre-warmed and ready: ${format}`, videoId);
      return prewarmed;
    }

    // Fallback to JioSaavn 320 kbps if available
    if (saavnResult?.stream) {
      const saavn = saavnResult.stream;
      const format = 'JioSaavn · AAC 320 kbps';
      const audioEl = bufferAudio(saavn.url, true);

      const prewarmed: PrewarmedStream = {
        videoId,
        url: saavn.url,
        mime: 'audio/mp4',
        format,
        client: 'JIOSAAVN',
        cors: true,
        durationSec: track.durationSec || 0,
        statusNote: saavnResult.note,
        expiresAt: Date.now() + 10 * 60_000,
        audioElement: audioEl,
      };

      prewarmedStreams.set(videoId, prewarmed);
      trackLog.i('JioSaavn 320 kbps audio for next song pre-warmed and ready', videoId);
      return prewarmed;
    }

    return null;
  })().finally(() => {
    inFlight.delete(videoId);
  });

  inFlight.set(videoId, job);
  return job;
}

/**
 * Checks if a pre-warmed lossless/high-quality stream is available for this videoId.
 */
export function getPrewarmedStream(videoId: string): PrewarmedStream | null {
  const stream = prewarmedStreams.get(videoId);
  if (!stream) return null;
  if (Date.now() >= stream.expiresAt) {
    consumePrewarmedStream(videoId);
    return null;
  }
  const addons = useAddons.getState();
  if (stream.client === 'ADDON') {
    if (!addons.lossless) return null;
    if (stream.sourceId && !addons.sources.some((s) => s.id === stream.sourceId && s.enabled)) return null;
  } else if (stream.client === 'JIOSAAVN') {
    if (!addons.jioSaavn) return null;
  }
  return stream;
}

/**
 * Retrieves and consumes the pre-warmed stream so it is handed over to the active player.
 */
export function consumePrewarmedStream(videoId: string): PrewarmedStream | null {
  const stream = prewarmedStreams.get(videoId);
  if (!stream) return null;
  prewarmedStreams.delete(videoId);
  if (stream.audioElement) {
    try {
      stream.audioElement.src = '';
      stream.audioElement.remove();
    } catch {
      // Ignore
    }
    stream.audioElement = undefined;
  }
  return stream;
}

/**
 * Clears expired or superseded pre-warmed streams, keeping memory lean.
 */
export function prunePrewarmedStreams(keepVideoIds: Set<string>): void {
  const now = Date.now();
  for (const [id, stream] of prewarmedStreams.entries()) {
    if (now >= stream.expiresAt || !keepVideoIds.has(id)) {
      consumePrewarmedStream(id);
    }
  }
}
