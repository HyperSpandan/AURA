import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router';
import { GlassButtonGroup, GlassButtonGroupItem, GlassButtonGroupSeparator } from '@/components/liquid-glass/glass-button-group';
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

  return (
    <header
      className={`fixed top-0 right-0 left-(--web-navigation-width) z-[calc(var(--z-web-chrome)-2)] flex h-16 items-center justify-between px-6 max-sm:hidden transition-[background-color,border-color,backdrop-filter,box-shadow,left] duration-200 ease-out ${
        scrolled
          ? 'bg-black/40 backdrop-blur-2xl border-b border-white/[0.08] shadow-[0_4px_24px_rgba(0,0,0,0.28)]'
          : 'bg-black/15 backdrop-blur-xl border-b border-white/[0.06]'
      }`}
    >
      {/* Left: History navigation buttons */}
      <GlassButtonGroup material="clear" size="sm" aria-label="History navigation">
        <GlassButtonGroupItem
          onClick={() => navigate(-1)}
          aria-label="Back"
          title="Back"
          className="!px-2"
        >
          <ChevronLeft size={17} strokeWidth={2.3} />
        </GlassButtonGroupItem>
        <GlassButtonGroupSeparator />
        <GlassButtonGroupItem
          onClick={() => navigate(1)}
          aria-label="Forward"
          title="Forward"
          className="!px-2"
        >
          <ChevronRight size={17} strokeWidth={2.3} />
        </GlassButtonGroupItem>
      </GlassButtonGroup>

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
