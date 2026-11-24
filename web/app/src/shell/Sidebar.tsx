import { ArrowUpRight, Library as LibraryIcon, ListMusic, PanelLeftClose, PanelLeftOpen, Settings } from 'lucide-react';
import { useEffect } from 'react';
import { useMatch } from 'react-router';
import { SiSpotify } from 'react-icons/si';
import { PrefetchLink, PrefetchNavLink } from '@/components/PrefetchLink';
import { LogoMark } from '@/components/icons/LogoMark';
import {
  AllPlaylistsIcon,
  HomeIcon,
  NewIcon,
  SearchIcon,
  type NavIconProps,
} from '@/components/icons/NavIcons';
import { hrefFor } from '@/components/music/links';
import { config } from '@/config';
import { LIBRARY_SECTIONS } from '@/data/music/library';
import { useLibraryPlaylists, useSignedIn } from '@/data/music/queries';
import { useSpotifyPlaylists } from '@/data/spotify/queries';
import { useSpotifySignedIn } from '@/extension/useSpotifyAuth';
import { useLiquidGlass } from '@/lib/liquidGlass';
import { useUi } from '@/state/ui';
import { AccountButton } from './AccountButton';

interface NavItem {
  to: string;
  label: string;
  Icon: (props: NavIconProps) => React.ReactNode;
}

const PRIMARY: NavItem[] = [
  { to: '/home', label: 'Home', Icon: HomeIcon },
  { to: '/new', label: 'New', Icon: NewIcon },
  {
    to: '/library',
    label: 'Library',
    Icon: ({ size = 24 }: NavIconProps) => <LibraryIcon size={size - 4} strokeWidth={2} />,
  },
];

// A sidebar row; NavLink sets aria-current on the page's own row.
const item =
  'ot-nav-item relative group flex h-9 items-center gap-2.5 px-2.5 py-1 rounded-nav type-body-reduced text-primary transition-colors duration-150 hover:bg-white/[0.07]';
// Apple's glyphs, drawn at 24px (their 24-unit box), which the user preferred to Apple's own 28px.
const icon = 'inline-grid size-6 flex-none place-items-center text-primary group-aria-[current=page]:text-key';
const navLabel = 'ot-side-label min-w-0 truncate';
// "Library", "Playlists" (report §1: 12px/15px 600, secondary, padding 4px 8px).
const sectionHeader = 'ot-side-header mt-4 px-2.5 py-1 type-callout-emphasized text-secondary';
const list = 'flex flex-col';

/** The folded rail names each icon in a tooltip; the open sidebar already shows the names. */
const tip = (label: string) => (useUi.getState().sidebarCollapsed ? label : undefined);

function LiquidNavLink({
  to,
  end,
  className,
  onClick,
  title,
  children,
}: {
  to: string;
  end?: boolean;
  className?: string;
  onClick?: () => void;
  title?: string;
  children: React.ReactNode;
}) {
  const match = useMatch({ path: to, end: Boolean(end) });
  const glass = useLiquidGlass<HTMLDivElement>(12, 10);
  return (
    <div ref={match ? glass : undefined} className={match ? 'glass-pill relative mb-1 rounded-nav' : 'relative mb-1 rounded-nav'}>
      <PrefetchNavLink
        to={to}
        end={end}
        className={className ?? item}
        onClick={onClick}
        title={title}
      >
        {children}
      </PrefetchNavLink>
    </div>
  );
}

function NavList({ items, onNavigate }: { items: NavItem[]; onNavigate: () => void }) {
  useUi((s) => s.sidebarCollapsed);
  return (
    <ul className={list}>
      {items.map(({ to, label, Icon }) => (
        <li key={to}>
          <LiquidNavLink to={to} onClick={onNavigate} title={tip(label)}>
            <Icon size={24} className={icon} />
            <span className={navLabel}>{label}</span>
          </LiquidNavLink>
        </li>
      ))}
    </ul>
  );
}

/** Playlists (report §1), once YouTube requests are made as the user. */
function PlaylistsNav({ onNavigate }: { onNavigate: () => void }) {
  const playlists = useLibraryPlaylists();
  return (
    <>
      <h2 className={sectionHeader}>Playlists</h2>
      <ul className={list}>
        <li>
          <LiquidNavLink to="/library/playlists" onClick={onNavigate} title={tip(LIBRARY_SECTIONS.playlists.title)}>
            <AllPlaylistsIcon size={24} className={icon} />
            <span className={navLabel}>{LIBRARY_SECTIONS.playlists.title}</span>
          </LiquidNavLink>
        </li>
        {playlists.data?.map((playlist) => (
          // Each playlist's glyph is the same one, so the folded rail leaves them out.
          <li key={playlist.id} className="ot-side-extra">
            <LiquidNavLink to={hrefFor(playlist) ?? '/library/playlists'} onClick={onNavigate}>
              <span className={icon}>
                <ListMusic size={17} strokeWidth={1.8} />
              </span>
              <span className={navLabel}>{playlist.title}</span>
            </LiquidNavLink>
          </li>
        ))}
      </ul>
    </>
  );
}

/** Spotify's playlists, once the extension can read them as the user (prompt §7.1). */
function SpotifyNav({ onNavigate }: { onNavigate: () => void }) {
  const playlists = useSpotifyPlaylists();
  return (
    <>
      <h2 className={sectionHeader}>Spotify</h2>
      <ul className={list}>
        <li>
          <LiquidNavLink to="/spotify" end onClick={onNavigate} title={tip('All Spotify Playlists')}>
            {/* Spotify's own mark, so it reads apart from All Playlists above it, folded or not. */}
            <span className={icon}>
              <SiSpotify size={20} />
            </span>
            <span className={navLabel}>All Spotify Playlists</span>
          </LiquidNavLink>
        </li>
        {playlists.data?.map((playlist) => (
          <li key={playlist.id} className="ot-side-extra">
            <LiquidNavLink to={`/spotify/playlist/${encodeURIComponent(playlist.id)}`} onClick={onNavigate}>
              <span className={icon}>
                <ListMusic size={17} strokeWidth={1.8} />
              </span>
              <span className={navLabel}>{playlist.name}</span>
            </LiquidNavLink>
          </li>
        ))}
      </ul>
    </>
  );
}

/** The left glass sidebar (report §1). */
export function Sidebar() {
  const drawerOpen = useUi((s) => s.drawerOpen);
  const setDrawerOpen = useUi((s) => s.setDrawerOpen);
  const signedIn = useSignedIn();
  const spotifySignedIn = useSpotifySignedIn();
  const folded = useUi((s) => s.sidebarCollapsed);
  const toggleSidebar = useUi((s) => s.toggleSidebar);
  const toggleGlass = useLiquidGlass<HTMLButtonElement>(10, 10);
  const dockGlass = useLiquidGlass<HTMLDivElement>(16, 14);
  const close = () => setDrawerOpen(false);
  // The page and the player pill take their left edge from --web-navigation-width (tokens.css).
  useEffect(() => {
    if (folded) document.documentElement.dataset.sidebar = 'collapsed';
    else delete document.documentElement.dataset.sidebar;
  }, [folded]);

  return (
    <>
      {/* Below 484px the sidebar is a drawer opened from the top bar's menu button. */}
      <nav
        className="ot-side ot-side-glass absolute top-2.5 bottom-2.5 left-2.5 z-(--z-web-chrome) flex w-[calc(var(--web-navigation-width)-14px)] flex-col rounded-[24px] max-sm:inset-y-0 max-sm:left-0 max-sm:w-[min(300px,82vw)] max-sm:rounded-none max-sm:-translate-x-full max-sm:shadow-glass max-sm:transition-transform max-sm:duration-(--duration-panel) max-sm:ease-panel max-sm:data-[open=true]:translate-none"
        data-open={drawerOpen}
        data-folded={folded}
        aria-label="Main"
      >
        <div className="ot-side-top flex h-14 items-center justify-between pr-3 pl-5">
          <PrefetchLink to="/home" className="ot-side-wordmark inline-flex items-center gap-2 rounded-[6px]" aria-label="AURA, Home" onClick={close}>
            {/* White on dark, as Apple's own wordmark; it follows the label colour so it stays visible in light. */}
            <LogoMark size={22} className="text-primary" />
            <span className="type-[700_19px/24px] tracking-[-0.01em]">AURA</span>
          </PrefetchLink>
          {/* Folds the sidebar to a rail of icons and back. Phones have the drawer instead. */}
          <button
            ref={toggleGlass}
            className="glass-pill relative grid size-8 flex-none place-items-center rounded-full text-secondary transition-colors duration-150 hover:text-primary max-sm:hidden"
            onClick={toggleSidebar}
            aria-label={folded ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-expanded={!folded}
            title={`${folded ? 'Expand' : 'Collapse'} sidebar (${navigator.platform.startsWith('Mac') ? '⌘' : 'Ctrl'}+\\)`}
          >
            {folded ? <PanelLeftOpen size={17} strokeWidth={1.9} /> : <PanelLeftClose size={17} strokeWidth={1.9} />}
          </button>
        </div>
        <div className="ot-side-scroll min-h-0 flex-1 overflow-y-auto px-3 [scrollbar-width:none]">
          <NavList items={PRIMARY} onNavigate={close} />
          {signedIn && <PlaylistsNav onNavigate={close} />}
          {spotifySignedIn && <SpotifyNav onNavigate={close} />}
        </div>
        <div
          ref={dockGlass}
          className="ot-side-bottom glass-pill relative mx-2.5 mb-2.5 flex flex-col gap-2 rounded-[20px] p-2.5"
        >
          <div className="ot-side-full flex flex-col">
            <AccountButton />
          </div>
          <div className="ot-side-compact hidden flex-col">
            <AccountButton compact />
          </div>
          <div className="ot-side-foot flex flex-col gap-1.5 pt-1">
            <div className={`flex items-center ${folded ? 'justify-center' : 'justify-between'}`}>
              {!folded && (
                <a className="ot-side-android inline-flex items-center gap-1 ps-1 type-callout text-primary hover:underline" href={config.androidAppUrl} target="_blank" rel="noreferrer">
                  <span>Get the Android app</span>
                  <ArrowUpRight size={14} strokeWidth={2} />
                </a>
              )}
              <PrefetchNavLink
                to="/settings"
                className={`grid place-items-center rounded-full text-secondary transition-colors hover:bg-white/[0.08] hover:text-primary aria-[current=page]:bg-white/[0.12] aria-[current=page]:text-primary ${
                  folded ? 'size-9 self-center' : 'size-7'
                }`}
                aria-label="Settings"
                title="Settings"
                onClick={close}
              >
                <Settings size={folded ? 19 : 17} strokeWidth={1.9} />
              </PrefetchNavLink>
            </div>
            <div className="ot-side-full px-1 text-[11px] text-tertiary">
              Crafted by{' '}
              <a href="https://github.com/HyperSpandan" target="_blank" rel="noreferrer" className="text-secondary hover:text-primary underline transition-colors">
                Spandan Dhage
              </a>
            </div>
          </div>
        </div>
      </nav>
      {drawerOpen && <div className="absolute inset-0 z-[calc(var(--z-web-chrome)-1)] hidden bg-scrim max-sm:block" onClick={close} aria-hidden="true" />}
    </>
  );
}

