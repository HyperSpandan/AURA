import { create } from 'zustand';
import type { MusicItem, Track } from '@/data/music/types';

export interface PinnedItem {
  id: string;
  kind: 'song' | 'video' | 'playlist' | 'album';
  title: string;
  subtitle: string;
  thumbnail: string | null;
  pinnedAt: number;
  item: MusicItem | Track;
}

interface PinnedState {
  pinnedIds: Set<string>;
  items: PinnedItem[];
  isPinned: (id: string) => boolean;
  togglePin: (item: MusicItem | Track) => boolean;
  pin: (item: MusicItem | Track) => void;
  unpin: (id: string) => void;
}

const STORAGE_KEY = 'aura:pinned_items';

function loadPinned(): PinnedItem[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as PinnedItem[];
  } catch {
    return [];
  }
}

function savePinned(items: PinnedItem[]): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Ignore storage errors
  }
}

function normalizeItem(input: MusicItem | Track): PinnedItem {
  if ('videoId' in input) {
    const artist = input.artists?.map((a) => a.name).join(', ') || 'Song';
    return {
      id: input.videoId,
      kind: 'song',
      title: input.title,
      subtitle: artist,
      thumbnail: input.thumbnail ?? null,
      pinnedAt: Date.now(),
      item: input,
    };
  }

  const kind = (input.kind === 'album' || input.kind === 'playlist' ? input.kind : 'song') as PinnedItem['kind'];
  return {
    id: input.id,
    kind,
    title: input.title,
    subtitle: input.subtitle || (kind === 'album' ? 'Album' : 'Playlist'),
    thumbnail: input.thumbnail ?? null,
    pinnedAt: Date.now(),
    item: input,
  };
}

const initialItems = loadPinned();

export const usePinnedStore = create<PinnedState>()((set, get) => ({
  items: initialItems,
  pinnedIds: new Set(initialItems.map((i) => i.id)),

  isPinned: (id: string) => {
    return get().pinnedIds.has(id);
  },

  pin: (item: MusicItem | Track) => {
    const normalized = normalizeItem(item);
    if (get().pinnedIds.has(normalized.id)) return;

    const nextItems = [normalized, ...get().items];
    const nextIds = new Set(get().pinnedIds);
    nextIds.add(normalized.id);

    set({ items: nextItems, pinnedIds: nextIds });
    savePinned(nextItems);
  },

  unpin: (id: string) => {
    const nextItems = get().items.filter((i) => i.id !== id);
    const nextIds = new Set(get().pinnedIds);
    nextIds.delete(id);

    set({ items: nextItems, pinnedIds: nextIds });
    savePinned(nextItems);
  },

  togglePin: (item: MusicItem | Track) => {
    const id = 'videoId' in item ? item.videoId : item.id;
    if (get().pinnedIds.has(id)) {
      get().unpin(id);
      return false;
    } else {
      get().pin(item);
      return true;
    }
  },
}));
