import { useQuery } from '@tanstack/react-query';
import { ARCHIVE_FILES } from './archiveFiles';

export function normalizeArtist(name: string): string {
  const target = name.split(/[,&/]|\b(feat\.?|featuring)\b/i)[0]?.trim() || name.trim();
  return target
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]/g, '');
}

export function getArchiveSignature(name: string): string | null {
  const norm = normalizeArtist(name);
  const file = ARCHIVE_FILES[norm];
  if (!file) return null;
  const encoded = encodeURIComponent(file).replace(/\+/g, '%20');
  return `https://raw.githubusercontent.com/itsmeadarsh2008/signature-archive/main/assets/${encoded}`;
}

export async function fetchWikimediaSignature(name: string): Promise<string | null> {
  const target = name.split(/[,&/]|\b(feat\.?|featuring)\b/i)[0]?.trim() || name.trim();
  if (!target) return null;

  try {
    const enc = encodeURIComponent(target);
    const searchUrl = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=${enc}+signature&gsrnamespace=6&gsrlimit=10&prop=imageinfo&iiprop=url|mime|size&format=json&origin=*`;
    const res = await fetch(searchUrl, { headers: { Accept: 'application/json' } });
    if (!res.ok) return null;
    const data = await res.json();
    const pages = data?.query?.pages;
    if (!pages) return null;

    for (const id in pages) {
      const page = pages[id];
      const title = (page.title || '').toLowerCase();
      if (title.includes('signature') || title.includes('autograph') || title.includes('sig')) {
        const info = page.imageinfo?.[0];
        if (info?.url) {
          return info.url;
        }
      }
    }
  } catch {
    // Fail silently
  }
  return null;
}

export function useArtistSignature(name: string | undefined | null) {
  return useQuery({
    queryKey: ['artist', 'signature', name],
    queryFn: async (): Promise<string | null> => {
      if (!name) return null;
      const archive = getArchiveSignature(name);
      if (archive) return archive;
      return await fetchWikimediaSignature(name);
    },
    enabled: Boolean(name),
    staleTime: 24 * 60 * 60 * 1000, // 24 hours
  });
}
