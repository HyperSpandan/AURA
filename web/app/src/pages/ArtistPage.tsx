import { useMemo, useState, type CSSProperties } from 'react';
import { useParams } from 'react-router';
import { PlayIcon } from '@/components/icons/Transport';
import { Artwork } from '@/components/music/Artwork';
import { browseHref } from '@/components/music/links';
import { playList } from '@/components/music/play';
import { Shelves, TrackShelf } from '@/components/music/ShelfView';
import { LoadError, NeedsExtension } from '@/components/music/States';
import { useArtistSignature } from '@/data/artist/artistSignature';
import { requestAppleAccess, useAppleArtistArt, useUngrantedAppleOrigins, type AppleArtistArt } from '@/data/apple/artistArt';
import { useArtist, useYouTubeReady } from '@/data/music/queries';
import type { ArtworkColor } from '@/lib/artworkColor';
import { usePageTint } from '@/lib/usePageTint';
import { ArtistSkeleton } from '@/skeletons/PageSkeletons';

function luminance(color: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(color.slice(i, i + 2), 16) / 255);
  return 0.2126 * r! + 0.7152 * g! + 0.0722 * b!;
}

/**
 * The artist's name, their handwritten signature with "Signed by", or Apple's title logo
 * in its place (matching the Android app's ArtistNameOrSignature).
 */
function ArtistName({ name, apple }: { name: string; apple: AppleArtistArt | null }) {
  const { data: signature } = useArtistSignature(name);
  const logo = apple?.logoUrl ?? null;
  const [logoShown, setLogoShown] = useState<string | null>(null);
  const [sigShown, setSigShown] = useState(false);

  // If a signature is available, render the artist's handwritten signature with "Signed by"
  if (signature) {
    return (
      <div className="flex flex-col items-center justify-center transition-all duration-300">
        <img
          key={signature}
          src={signature}
          alt={`${name}'s signature`}
          className="mx-auto mt-0 mb-1 block h-auto max-h-[min(calc((100vw-var(--web-navigation-width))/1.93*0.35),140px)] w-auto max-w-[min(82%,460px)] object-contain [filter:brightness(0)_invert(1)_drop-shadow(0_2px_14px_rgba(0,0,0,0.85))]"
          onLoad={() => setSigShown(true)}
          style={{ display: sigShown ? 'block' : 'none' }}
        />
        {sigShown ? (
          <div className="mt-1 text-sm font-normal text-white/85 tracking-wide [text-shadow:0_1px_8px_rgba(0,0,0,0.9)]">
            Signed by <strong className="font-semibold text-white">{name}</strong>
          </div>
        ) : (
          <h1 className="type-header-emphasized text-[clamp(34px,5vw,64px)] leading-[1.05] [text-shadow:0_2px_20px_#00000059]">
            {name}
          </h1>
        )}
      </div>
    );
  }

  const isLogoVisible = !!logo && logoShown === logo;
  return (
    <h1
      className="type-header-emphasized text-[clamp(34px,5vw,64px)] leading-[1.05] [text-shadow:0_2px_20px_#00000059] data-logo:w-full data-logo:[text-shadow:none]"
      data-logo={isLogoVisible || undefined}
    >
      {!isLogoVisible && name}
      {logo && (
        <img
          key={logo}
          className="mx-auto mt-0 mb-1.5 block h-auto max-h-[min(calc((100vw-var(--web-navigation-width))/1.93*0.3),21vh)] w-auto max-w-[min(82%,640px)] [filter:drop-shadow(0_2px_16px_#00000040)]"
          src={logo}
          alt={isLogoVisible ? name : ''}
          hidden={!isLogoVisible}
          style={{ aspectRatio: String(apple!.logoAspect) }}
          decoding="async"
          onLoad={() => setLogoShown(logo)}
        />
      )}
    </h1>
  );
}

/**
 * An artist (report §4): full-bleed hero with the name and Play, Top Songs,
 * then shelves. Where Apple Music has the artist, the page takes Apple's
 * header as the phone does: its photograph, its title logo over it, its
 * colour behind the whole page, and its colour on the Play button.
 */
export default function ArtistPage() {
  const id = decodeURIComponent(useParams().id ?? '');
  const { missing } = useYouTubeReady();
  const artist = useArtist(id);
  const apple = useAppleArtistArt(artist.data?.name);
  const appleUngranted = useUngrantedAppleOrigins();
  const tint = useMemo<ArtworkColor | null>(
    () => (apple ? { background: apple.background, dark: luminance(apple.background) < 0.6 } : null),
    [apple],
  );
  usePageTint(tint);

  if (missing) return <NeedsExtension />;
  if (artist.isError) return <LoadError error={artist.error} onRetry={() => void artist.refetch()} />;
  if (!artist.data) return <ArtistSkeleton />;

  const data = artist.data;
  const topSongs = { title: `${data.name}: Top Songs`, type: 'artist' as const, id: data.id };
  return (
    <>
      {/* The hero (report §4): full-bleed, ~1.93:1 of the content column, the name and actions centred near the bottom. */}
      <header className="relative max-h-[70vh] overflow-hidden">
        <div className="ot-parallax relative w-full">
          <Artwork src={data.heroImage} displayWidth={1200} aspect={1165 / 604} eager />
          {/* Apple's photograph fades in over YouTube's once it arrives. */}
          {apple && (
            <img
              key={apple.heroUrl}
              className="absolute inset-0 size-full object-cover opacity-0 transition-opacity duration-400 ease-in data-loaded:opacity-100"
              src={apple.heroUrl}
              alt=""
              decoding="async"
              onLoad={(e) => e.currentTarget.setAttribute('data-loaded', '')}
            />
          )}
        </div>
        {/* The image melts into the page at the bottom (Apple's masked blur frame), into Apple's colour when the page is tinted with it. */}
        <div
          className="pointer-events-none absolute inset-0 bg-[linear-gradient(180deg,transparent_45%,color-mix(in_srgb,var(--page-tint,var(--pageBG))_70%,transparent)_80%,var(--page-tint,var(--pageBG))_100%)]"
          aria-hidden="true"
        />
        <div className="ot-detail-text absolute inset-x-0 bottom-6 flex flex-col items-center gap-1.5 px-(--body-gutter) text-center">
          <ArtistName name={data.name} apple={apple} />
          {data.audience && <p className="type-callout-emphasized text-secondary">{data.audience}</p>}
          <button
            // The 56px key-colour play button.
            className="ot-press mt-2.5 grid size-14 place-items-center rounded-full bg-(--play-fill,var(--keyColorBG)) text-(--play-glyph,#fff) shadow-[0_10px_28px_-10px_#000a,inset_0_0_2px_#ffffff1a] disabled:opacity-40 enabled:hover:brightness-110"
            // Apple's fill for the Play circle, with the triangle in the page's colour,
            // as the phone cuts it out of the circle; white without a key colour.
            style={
              apple
                ? ({ '--play-fill': apple.keyColor ?? '#ffffff', '--play-glyph': apple.background } as CSSProperties)
                : undefined
            }
            aria-label={`Play ${data.name}`}
            disabled={!data.topSongs.length}
            onClick={() => playList(data.topSongs, 0, { source: topSongs })}
          >
            <PlayIcon size={26} />
          </button>
          {!apple && appleUngranted.length > 0 && (
            // The lyrics panel's permission notice, as a pill over the photograph.
            <div className="mt-3 flex items-center gap-2.5 rounded-[999px] bg-[#0000004d] py-1.5 pr-2 pl-3.5 type-callout text-[#ffffffcc] [backdrop-filter:blur(20px)_saturate(1.6)]">
              <span>Apple Music artist art needs the extension’s permission.</span>
              <button
                className="flex-none rounded-[999px] bg-[#ffffff26] px-2.5 py-1 type-callout-emphasized text-white hover:bg-[#ffffff40]"
                // Stays until the extension confirms the grant (checked when this tab comes back).
                onClick={requestAppleAccess}
              >
                Allow
              </button>
            </div>
          )}
        </div>
      </header>

      {data.topSongs.length > 0 && (
        <TrackShelf
          title="Top Songs"
          to={data.topSongsSeeAll ? browseHref(data.topSongsSeeAll) : null}
          tracks={data.topSongs}
          source={topSongs}
        />
      )}

      <Shelves shelves={data.shelves} />

      {data.description && (
        <section className="mx-(--body-gutter) mt-3 mb-10" aria-label={`About ${data.name}`}>
          <h2 className="type-title-2-emphasized">About {data.name}</h2>
          <p className="mt-2 max-w-[720px] type-body-tall whitespace-pre-line text-secondary">{data.description}</p>
        </section>
      )}
    </>
  );
}
