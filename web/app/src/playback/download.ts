import { fetchJson, findLossless } from '@/addons/client';
import { findJioSaavn } from '@/addons/jiosaavn';
import type { Track } from '@/data/music/types';
import { getInnertube } from './youtube/innertube';
import { resolvePlayer } from './youtube/resolver';
import { trackLog } from './trackLog';

function sanitizeFilename(name: string): string {
  return name.replace(/[/\\?%*:|"<>]/g, '_').trim() || 'Track';
}

function triggerBrowserDownload(url: string, filename: string): void {
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.rel = 'noreferrer';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

/**
 * Downloads a track:
 * 1. Checks lossless add-ons for FLAC (Monochrome, Internet Archive, Musopen).
 * 2. Checks JioSaavn add-on for 320 kbps AAC (M4A).
 * 3. Resolves YouTube stream and downloads the best audio format.
 */
export async function downloadTrack(track: Track): Promise<{ format: string; source: string }> {
  const artist = track.artists.map((a) => a.name).join(', ') || 'Unknown Artist';
  const baseName = sanitizeFilename(`${track.title} - ${artist}`);
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
      trackLog.i(`Downloading lossless FLAC from ${candidate.provider}`, track.videoId);
      try {
        const res = await fetch(candidate.url);
        if (res.ok) {
          const blob = await res.blob();
          const blobUrl = URL.createObjectURL(blob);
          triggerBrowserDownload(blobUrl, `${baseName}.flac`);
          setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000);
          return { format: 'FLAC', source: candidate.provider };
        }
      } catch {
        // Direct browser navigation / download link fallback
        triggerBrowserDownload(candidate.url, `${baseName}.flac`);
        return { format: 'FLAC', source: candidate.provider };
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
      trackLog.i(`Downloading 320 kbps AAC from JioSaavn`, track.videoId);
      try {
        const res = await fetch(saavn.stream.url);
        if (res.ok) {
          const blob = await res.blob();
          const blobUrl = URL.createObjectURL(blob);
          triggerBrowserDownload(blobUrl, `${baseName}.m4a`);
          setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000);
          return { format: 'AAC 320kbps', source: 'JioSaavn' };
        }
      } catch {
        triggerBrowserDownload(saavn.stream.url, `${baseName}.m4a`);
        return { format: 'AAC 320kbps', source: 'JioSaavn' };
      }
    }
  } catch (e) {
    trackLog.d(`JioSaavn search skipped: ${String(e)}`, track.videoId);
  }

  // 3. Fallback to YouTube
  trackLog.i(`Resolving YouTube stream for download`, track.videoId);
  const yt = await getInnertube();
  const resolved = await resolvePlayer(yt, track.videoId);
  const format = resolved.info.chooseFormat({ type: 'audio', quality: 'best' });

  const mime = format.mime_type?.split(';')[0]?.toLowerCase() ?? 'audio/mp4';
  const ext = mime.includes('webm') ? 'webm' : mime.includes('opus') ? 'opus' : 'm4a';

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
    const blobUrl = URL.createObjectURL(blob);
    triggerBrowserDownload(blobUrl, `${baseName}.${ext}`);
    setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000);
    return { format: ext.toUpperCase(), source: 'YouTube' };
  } catch (streamError) {
    trackLog.w(`Stream download chunking failed, trying decipher URL: ${String(streamError)}`, track.videoId);
    const decipheredUrl = await format.decipher(yt.session.player);
    if (decipheredUrl) {
      triggerBrowserDownload(decipheredUrl, `${baseName}.${ext}`);
      return { format: ext.toUpperCase(), source: 'YouTube' };
    }
    throw streamError;
  }
}
