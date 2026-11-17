import { ArrowDownToLine, ChevronLeft, ChevronRight, WifiOff } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router';
import { PrefetchLink } from '@/components/PrefetchLink';
import { SearchField } from '@/components/music/SearchField';
import { useIsOnline } from '@/lib/useIsOnline';

export function DesktopTopBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isOnline = useIsOnline();
  const [scrolled, setScrolled] = useState(false);

  const query = location.pathname === '/search' ? (searchParams.get('q') ?? '') : '';

  useEffect(() => {
    const page = document.getElementById('scrollable-page');
    if (!page) return;
    const onScroll = () => setScrolled(page.scrollTop > 8);
    onScroll();
    page.addEventListener('scroll', onScroll, { passive: true });
    return () => page.removeEventListener('scroll', onScroll);
  }, []);

  const handleSearch = (q: string) => {
    if (q.trim()) {
      navigate(`/search?q=${encodeURIComponent(q.trim())}`);
    } else if (location.pathname === '/search') {
      navigate('/search');
    }
  };

  const navButton =
    'grid size-8 place-items-center rounded-full bg-white/[0.06] hover:bg-white/[0.12] text-secondary hover:text-primary active:scale-95 transition-all';

  return (
    <header
      className={`fixed top-0 right-0 left-(--web-navigation-width) z-[calc(var(--z-web-chrome)-2)] flex h-16 items-center justify-between px-6 max-sm:hidden transition-[background-color,border-color,backdrop-filter,box-shadow,left] duration-200 ease-out ${
        scrolled
          ? 'bg-page/80 backdrop-blur-2xl border-b border-white/5 shadow-[0_4px_24px_rgba(0,0,0,0.25)]'
          : 'bg-transparent border-b border-transparent'
      }`}
    >
      {/* Left: History navigation buttons */}
      <div className="flex items-center gap-2">
        <button
          className={navButton}
          onClick={() => navigate(-1)}
          aria-label="Back"
          title="Back"
        >
          <ChevronLeft size={19} strokeWidth={2.4} />
        </button>
        <button
          className={navButton}
          onClick={() => navigate(1)}
          aria-label="Forward"
          title="Forward"
        >
          <ChevronRight size={19} strokeWidth={2.4} />
        </button>
      </div>

      {/* Center: Search Field */}
      <div className="flex-1 flex justify-center px-4 max-w-[560px]">
        <SearchField
          initial={query}
          onSearch={handleSearch}
          className="w-full"
          placeholder="Search songs, albums, artists, lyrics…"
        />
      </div>

      {/* Right: Offline status or Downloads shortcut */}
      <div className="flex items-center gap-3">
        {!isOnline && (
          <PrefetchLink
            to="/downloads"
            className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/15 border border-amber-500/30 px-3 py-1 text-xs font-medium text-amber-300 hover:bg-amber-500/25 transition-all shadow-sm"
            title="You are offline. Click to view downloads"
          >
            <WifiOff size={13} strokeWidth={2.2} />
            <span>Offline</span>
          </PrefetchLink>
        )}
        <PrefetchLink
          to="/downloads"
          className="grid size-8 place-items-center rounded-full bg-white/[0.06] hover:bg-white/[0.12] text-secondary hover:text-primary active:scale-95 transition-all"
          aria-label="Downloads"
          title="Downloads"
        >
          <ArrowDownToLine size={17} strokeWidth={2} />
        </PrefetchLink>
      </div>
    </header>
  );
}
