import { fetchJson, findLossless } from '@/addons/client';
import { findJioSaavn } from '@/addons/jiosaavn';
import {
  deleteOfflineTrack,
  getOfflineTrack,
  saveOfflineTrack,
  type OfflineTrackRecord,
} from '@/data/download/offlineStore';
import type { Track } from '@/data/music/types';
import { getInnertube } from './youtube/innertube';
import { resolvePlayer } from './youtube/resolver';
import { trackLog } from './trackLog';

function sanitizeFilename(name: string): string {
  return name.replace(/[/\\?%*:|"<>]/g, '_').trim() || 'Track';
}

export function triggerBrowserDownload(url: string, filename: string): void {
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.rel = 'noreferrer';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

async function fetchArtworkDataUrl(url?: string | null): Promise<string | undefined> {
  if (!url) return undefined;
  try {
    const res = await fetch(url);
    if (!res.ok) return undefined;
    const blob = await res.blob();
    return await new Promise<string>((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.onerror = () => resolve(undefined as unknown as string);
      reader.readAsDataURL(blob);
    });
  } catch {
    return undefined;
  }
}

interface AudioPayload {
  blob: Blob;
  mime: string;
  format: string;
  source: string;
  ext: string;
}

/**
 * Fetches the highest quality audio stream available for a track:
 * 1. Lossless FLAC from add-ons (Monochrome, Internet Archive, Musopen)
 * 2. High-bitrate 320 kbps AAC (M4A) from JioSaavn
 * 3. YouTube adaptive audio stream (Opus / AAC)
 */
async function fetchTrackAudio(track: Track): Promise<AudioPayload> {
  const artist = track.artists.map((a) => a.name).join(', ') || 'Unknown Artist';
  const recording = {
    title: track.title,
    artist,
    duration: track.durationSec ?? 0,
  };

  // 1. Try FLAC from lossless sources
  try {
    const lossless = await findLossless(recording);
    const candidate = lossless.ranked[0];
    if (candidate?.url) {
      trackLog.i(`Fetching lossless FLAC from ${candidate.provider}`, track.videoId);
      const res = await fetch(candidate.url);
      if (res.ok) {
        const blob = await res.blob();
        return {
          blob,
          mime: 'audio/flac',
          format: candidate.bits && candidate.rate ? `FLAC ${candidate.bits}-bit ${(candidate.rate / 1000).toFixed(1)} kHz` : 'FLAC Lossless',
          source: candidate.provider,
          ext: 'flac',
        };
      }
    }
  } catch (e) {
    trackLog.d(`Lossless search skipped: ${String(e)}`, track.videoId);
  }

  // 2. Try JioSaavn 320 kbps
  try {
    const saavn = await findJioSaavn(
      { ...recording, album: track.album?.name },
      fetchJson,
    );
    if (saavn?.stream?.url) {
      trackLog.i(`Fetching 320 kbps AAC from JioSaavn`, track.videoId);
      const res = await fetch(saavn.stream.url);
      if (res.ok) {
        const blob = await res.blob();
        return {
          blob,
          mime: 'audio/mp4',
          format: 'AAC 320kbps',
          source: 'JioSaavn',
          ext: 'm4a',
        };
      }
    }
  } catch (e) {
    trackLog.d(`JioSaavn search skipped: ${String(e)}`, track.videoId);
  }

  // 3. Fallback to YouTube
  trackLog.i(`Resolving YouTube stream for track`, track.videoId);
  const yt = await getInnertube();
  const resolved = await resolvePlayer(yt, track.videoId);
  const format = resolved.info.chooseFormat({ type: 'audio', quality: 'best' });

  const mime = format.mime_type?.split(';')[0]?.toLowerCase() ?? 'audio/mp4';
  const ext = mime.includes('webm') ? 'webm' : mime.includes('opus') ? 'opus' : 'm4a';
  const formatName = format.quality_label || (ext === 'opus' ? 'Opus 160kbps' : 'AAC 128kbps');

  try {
    const stream = await resolved.info.download({ type: 'audio', quality: 'best' });
    const reader = stream.getReader();
    const chunks: Uint8Array[] = [];
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (value) chunks.push(value);
    }
    const blob = new Blob(chunks as BlobPart[], { type: mime });
    return {
      blob,
      mime,
      format: formatName,
      source: 'YouTube',
      ext,
    };
  } catch (streamError) {
    trackLog.w(`Stream chunking failed, attempting decipher URL: ${String(streamError)}`, track.videoId);
    const decipheredUrl = await format.decipher(yt.session.player);
    if (decipheredUrl) {
      const res = await fetch(decipheredUrl);
      if (res.ok) {
        const blob = await res.blob();
        return {
          blob,
          mime,
          format: formatName,
          source: 'YouTube',
          ext,
        };
      }
    }
    throw streamError;
  }
}

/**
 * Downloads a track and saves it inside the web player's offline IndexedDB storage.
 */
export async function downloadTrack(track: Track): Promise<OfflineTrackRecord> {
  trackLog.i(`Starting in-app offline download`, track.videoId);
  const audio = await fetchTrackAudio(track);

  // Fetch artwork so offline playback shows album art even when disconnected
  const artUrl = track.thumbnail;
  const artworkDataUrl = await fetchArtworkDataUrl(artUrl);

  const storedTrack: Track = {
    ...track,
    thumbnail: artworkDataUrl || track.thumbnail,
  };

  const record: OfflineTrackRecord = {
    videoId: track.videoId,
    track: storedTrack,
    audioBlob: audio.blob,
    mimeType: audio.mime,
    format: audio.format,
    source: audio.source,
    sizeBytes: audio.blob.size,
    downloadedAt: Date.now(),
    artworkDataUrl,
  };

  await saveOfflineTrack(record);
  trackLog.i(`Saved track to offline storage (${audio.format}, ${(audio.blob.size / (1024 * 1024)).toFixed(1)} MB)`, track.videoId);
  return record;
}

/**
 * Removes a track from offline storage.
 */
export async function removeDownloadedTrack(videoId: string): Promise<void> {
  await deleteOfflineTrack(videoId);
  trackLog.i(`Removed track from offline storage`, videoId);
}

/**
 * Exports a track as a standalone audio file to the device's downloads folder.
 */
export async function exportTrackFile(track: Track): Promise<{ format: string; source: string }> {
  const existing = await getOfflineTrack(track.videoId);
  const artist = track.artists.map((a) => a.name).join(', ') || 'Unknown Artist';
  const baseName = sanitizeFilename(`${track.title} - ${artist}`);

  if (existing?.audioBlob) {
    const ext = existing.mimeType.includes('flac') ? 'flac' : existing.mimeType.includes('webm') ? 'webm' : 'm4a';
    const blobUrl = URL.createObjectURL(existing.audioBlob);
    triggerBrowserDownload(blobUrl, `${baseName}.${ext}`);
    setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000);
    return { format: existing.format, source: existing.source };
  }

  const audio = await fetchTrackAudio(track);
  const blobUrl = URL.createObjectURL(audio.blob);
  triggerBrowserDownload(blobUrl, `${baseName}.${audio.ext}`);
  setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000);
  return { format: audio.format, source: audio.source };
}
