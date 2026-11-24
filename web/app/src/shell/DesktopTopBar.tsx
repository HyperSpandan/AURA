import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router';
import { SearchField } from '@/components/music/SearchField';

export function DesktopTopBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
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
    'grid size-8 place-items-center rounded-full border border-white/[0.08] bg-white/[0.08] backdrop-blur-md hover:bg-white/[0.15] text-secondary hover:text-primary active:scale-95 transition-all';

  return (
    <header
      className={`fixed top-0 right-0 left-(--web-navigation-width) z-[calc(var(--z-web-chrome)-2)] flex h-16 items-center justify-between px-6 max-sm:hidden transition-[background-color,border-color,backdrop-filter,box-shadow,left] duration-200 ease-out ${
        scrolled
          ? 'bg-black/40 backdrop-blur-2xl border-b border-white/[0.08] shadow-[0_4px_24px_rgba(0,0,0,0.28)]'
          : 'bg-black/15 backdrop-blur-xl border-b border-white/[0.06]'
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

      {/* Right spacer to keep search centered */}
      <div className="w-[72px]" aria-hidden="true" />
    </header>
  );
}
