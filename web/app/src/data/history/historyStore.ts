import type { Track } from '@/data/music/types';

export interface PlayRecord {
  videoId: string;
  title: string;
  artist: string;
  thumbnailUrl: string | null;
  durationSec: number | null;
  albumName: string | null;
  playedAt: number;
  listenedMs: number;
}

export interface TopEntry {
  key: string;
  title: string;
  subtitle: string;
  thumbnailUrl: string | null;
  plays: number;
  listenedMs: number;
  track?: Track;
}

export interface ReplaySummary {
  totalPlays: number;
  listenedMs: number;
  songCount: number;
  artistCount: number;
  albumCount: number;
  topSongs: TopEntry[];
  topArtists: TopEntry[];
  topAlbums: TopEntry[];
}

const STORAGE_KEY = 'aura:history_records';
const MAX_RECORDS = 500;

function loadStoredRecords(): PlayRecord[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as PlayRecord[];
  } catch {
    return [];
  }
}

function saveStoredRecords(records: PlayRecord[]): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(records.slice(0, MAX_RECORDS)));
  } catch {
    // Ignore storage write error
  }
}

let memoryRecords: PlayRecord[] = loadStoredRecords();

export function recordPlay(track: Track, listenedMs?: number): void {
  if (!track.videoId || !track.title) return;
  const now = Date.now();
  const artist = track.artists?.map((a) => a.name).join(', ') || 'Unknown Artist';
  const duration = track.durationSec ?? 180;
  const actualListened = listenedMs ?? Math.min(duration * 1000, 180_000);

  const entry: PlayRecord = {
    videoId: track.videoId,
    title: track.title,
    artist,
    thumbnailUrl: track.thumbnail ?? null,
    durationSec: track.durationSec ?? null,
    albumName: track.album?.name ?? null,
    playedAt: now,
    listenedMs: actualListened,
  };

  memoryRecords = [entry, ...memoryRecords.filter((r) => r.playedAt !== now)].slice(0, MAX_RECORDS);
  saveStoredRecords(memoryRecords);
}

export function getLocalRecords(): PlayRecord[] {
  if (!memoryRecords.length) {
    memoryRecords = loadStoredRecords();
  }
  return memoryRecords;
}

/**
 * Computes ReplaySummary by combining local records with any recent tracks (e.g. from YouTube Music recent query).
 */
export function computeReplay(recentTracks: Track[] = []): ReplaySummary {
  const local = getLocalRecords();
  const allRecords: PlayRecord[] = [...local];

  // Merge recentTracks if not already covered in local records
  const seenVideoIds = new Set(local.map((r) => r.videoId));
  for (let i = 0; i < recentTracks.length; i++) {
    const t = recentTracks[i]!;
    if (!t.videoId || !t.title) continue;
    if (!seenVideoIds.has(t.videoId)) {
      seenVideoIds.add(t.videoId);
      allRecords.push({
        videoId: t.videoId,
        title: t.title,
        artist: t.artists?.map((a) => a.name).join(', ') || 'Unknown Artist',
        thumbnailUrl: t.thumbnail ?? null,
        durationSec: t.durationSec ?? null,
        albumName: t.album?.name ?? null,
        playedAt: Date.now() - (i + 1) * 3600_000,
        listenedMs: (t.durationSec ?? 210) * 1000,
      });
    }
  }

  // Aggregate stats
  const totalPlays = allRecords.length;
  const listenedMs = allRecords.reduce((sum, r) => sum + (r.listenedMs || 180_000), 0);

  // Group top songs
  const songsMap = new Map<string, { record: PlayRecord; plays: number; ms: number }>();
  for (const r of allRecords) {
    const current = songsMap.get(r.videoId);
    if (current) {
      current.plays += 1;
      current.ms += r.listenedMs;
    } else {
      songsMap.set(r.videoId, { record: r, plays: 1, ms: r.listenedMs });
    }
  }

  const topSongs: TopEntry[] = Array.from(songsMap.entries())
    .map(([key, data]) => ({
      key,
      title: data.record.title,
      subtitle: data.record.artist,
      thumbnailUrl: data.record.thumbnailUrl,
      plays: data.plays,
      listenedMs: data.ms,
    }))
    .sort((a, b) => b.plays - a.plays || b.listenedMs - a.listenedMs)
    .slice(0, 20);

  // Group top artists
  const artistsMap = new Map<string, { name: string; plays: number; ms: number; thumb: string | null }>();
  for (const r of allRecords) {
    const mainArtist = r.artist.split(/[,&]/)[0]?.trim() || r.artist;
    if (!mainArtist) continue;
    const current = artistsMap.get(mainArtist);
    if (current) {
      current.plays += 1;
      current.ms += r.listenedMs;
      if (!current.thumb && r.thumbnailUrl) current.thumb = r.thumbnailUrl;
    } else {
      artistsMap.set(mainArtist, { name: mainArtist, plays: 1, ms: r.listenedMs, thumb: r.thumbnailUrl });
    }
  }

  const topArtists: TopEntry[] = Array.from(artistsMap.entries())
    .map(([key, data]) => ({
      key,
      title: data.name,
      subtitle: '',
      thumbnailUrl: data.thumb,
      plays: data.plays,
      listenedMs: data.ms,
    }))
    .sort((a, b) => b.plays - a.plays || b.listenedMs - a.listenedMs)
    .slice(0, 20);

  // Group top albums
  const albumsMap = new Map<string, { name: string; artist: string; plays: number; ms: number; thumb: string | null }>();
  for (const r of allRecords) {
    if (!r.albumName) continue;
    const current = albumsMap.get(r.albumName);
    if (current) {
      current.plays += 1;
      current.ms += r.listenedMs;
      if (!current.thumb && r.thumbnailUrl) current.thumb = r.thumbnailUrl;
    } else {
      albumsMap.set(r.albumName, { name: r.albumName, artist: r.artist, plays: 1, ms: r.listenedMs, thumb: r.thumbnailUrl });
    }
  }

  const topAlbums: TopEntry[] = Array.from(albumsMap.entries())
    .map(([key, data]) => ({
      key,
      title: data.name,
      subtitle: data.artist,
      thumbnailUrl: data.thumb,
      plays: data.plays,
      listenedMs: data.ms,
    }))
    .sort((a, b) => b.plays - a.plays || b.listenedMs - a.listenedMs)
    .slice(0, 20);

  return {
    totalPlays,
    listenedMs,
    songCount: songsMap.size,
    artistCount: artistsMap.size,
    albumCount: albumsMap.size,
    topSongs,
    topArtists,
    topAlbums,
  };
}
