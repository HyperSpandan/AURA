import { create } from 'zustand';
import type { Track } from '@/data/music/types';
import {
  clearOfflineTracks,
  deleteOfflineTrack,
  getAllOfflineTracks,
  type OfflineTrackRecord,
} from './offlineStore';
import { downloadTrack } from '@/playback/download';

interface DownloadsState {
  downloadedIds: Set<string>;
  downloadingIds: Set<string>;
  records: OfflineTrackRecord[];
  tracks: Track[];
  totalBytes: number;
  initialized: boolean;

  init: () => Promise<void>;
  isDownloaded: (videoId: string) => boolean;
  isDownloading: (videoId: string) => boolean;
  download: (track: Track) => Promise<void>;
  remove: (videoId: string) => Promise<void>;
  clearAll: () => Promise<void>;
  refresh: () => Promise<void>;
}

export const useDownloads = create<DownloadsState>()((set, get) => ({
  downloadedIds: new Set<string>(),
  downloadingIds: new Set<string>(),
  records: [],
  tracks: [],
  totalBytes: 0,
  initialized: false,

  init: async () => {
    if (get().initialized) return;
    await get().refresh();
    set({ initialized: true });
  },

  refresh: async () => {
    try {
      const records = await getAllOfflineTracks();
      const downloadedIds = new Set(records.map((r) => r.videoId));
      const tracks = records.map((r) => r.track);
      const totalBytes = records.reduce((sum, r) => sum + (r.sizeBytes || 0), 0);
      set({ records, tracks, downloadedIds, totalBytes });
    } catch {
      // Ignore initial IndexedDB error
    }
  },

  isDownloaded: (videoId: string) => {
    return get().downloadedIds.has(videoId);
  },

  isDownloading: (videoId: string) => {
    return get().downloadingIds.has(videoId);
  },

  download: async (track: Track) => {
    if (get().downloadedIds.has(track.videoId) || get().downloadingIds.has(track.videoId)) {
      return;
    }

    const nextDownloading = new Set(get().downloadingIds);
    nextDownloading.add(track.videoId);
    set({ downloadingIds: nextDownloading });

    try {
      const record = await downloadTrack(track);
      const nextDownloadingAfter = new Set(get().downloadingIds);
      nextDownloadingAfter.delete(track.videoId);

      const nextDownloaded = new Set(get().downloadedIds);
      nextDownloaded.add(track.videoId);

      const nextRecords = [record, ...get().records.filter((r) => r.videoId !== track.videoId)];
      const nextTracks = nextRecords.map((r) => r.track);
      const totalBytes = nextRecords.reduce((sum, r) => sum + (r.sizeBytes || 0), 0);

      set({
        downloadingIds: nextDownloadingAfter,
        downloadedIds: nextDownloaded,
        records: nextRecords,
        tracks: nextTracks,
        totalBytes,
      });
    } catch (error) {
      const nextDownloadingAfter = new Set(get().downloadingIds);
      nextDownloadingAfter.delete(track.videoId);
      set({ downloadingIds: nextDownloadingAfter });
      throw error;
    }
  },

  remove: async (videoId: string) => {
    await deleteOfflineTrack(videoId);
    const nextDownloaded = new Set(get().downloadedIds);
    nextDownloaded.delete(videoId);

    const nextRecords = get().records.filter((r) => r.videoId !== videoId);
    const nextTracks = nextRecords.map((r) => r.track);
    const totalBytes = nextRecords.reduce((sum, r) => sum + (r.sizeBytes || 0), 0);

    set({
      downloadedIds: nextDownloaded,
      records: nextRecords,
      tracks: nextTracks,
      totalBytes,
    });
  },

  clearAll: async () => {
    await clearOfflineTracks();
    set({
      downloadedIds: new Set(),
      downloadingIds: new Set(),
      records: [],
      tracks: [],
      totalBytes: 0,
    });
  },
}));

// Initialize store immediately in browser
if (typeof window !== 'undefined') {
  void useDownloads.getState().init();
}
