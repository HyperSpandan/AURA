import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { getAudio, usePlayer } from '@/state/player';
import { useUi } from '@/state/ui';

/** Typing in an editable field or text input shouldn't trigger player shortcuts. */
export function typing(target: EventTarget | null): boolean {
  const element = target as HTMLElement | null;
  if (!element) return false;
  return (
    element.isContentEditable ||
    /^(INPUT|TEXTAREA|SELECT)$/.test(element.tagName) ||
    element.getAttribute('role') === 'textbox'
  );
}

function focusSearch(navigate?: (path: string) => void) {
  const field = document.querySelector<HTMLInputElement>('input[type="search"]');
  if (field) return field.focus();
  navigate?.('/search');
  // The search page is lazy: focus its field once it has rendered (for up to 3 s).
  const started = performance.now();
  const attempt = () => {
    const field = document.querySelector<HTMLInputElement>('input[type="search"]');
    if (field) field.focus();
    else if (performance.now() - started < 3000) setTimeout(attempt, 50);
  };
  attempt();
}

/**
 * Global web player keyboard shortcut handler:
 * - Space / K: Play / Pause toggle
 * - ← / →: Seek 5 seconds backward / forward
 * - J / L: Seek 10 seconds backward / forward
 * - Shift + ← / Cmd+← / P / <: Previous track
 * - Shift + → / Cmd+→ / N / >: Next track
 * - ↑ / ↓: Volume up / down (in full player or with Cmd/Ctrl)
 * - + / -: Volume up / down
 * - M: Mute / Unmute toggle
 * - S: Shuffle toggle
 * - R: Repeat mode cycle
 * - F: Full-screen player toggle (when not typing)
 * - C: Lyrics toggle
 * - U: Up Next toggle
 * - 0–9: Jump to percentage (0% to 90%)
 * - / or ⌘+F: Focus search
 * - ⌘+\: Toggle sidebar
 * - ?: Show keyboard shortcuts help cheat sheet
 * - Escape: Close dialogs / full-screen / panels
 */
export function handleKeyDown(event: KeyboardEvent, navigate?: (path: string) => void) {
  if (event.defaultPrevented || event.altKey) return;

  const command = event.metaKey || event.ctrlKey;

  // 1. Command shortcuts that always work
  if (command && (event.key === 'f' || event.key === 'F')) {
    event.preventDefault();
    return focusSearch(navigate);
  }
  if (command && event.key === '\\') {
    event.preventDefault();
    return useUi.getState().toggleSidebar();
  }

  // 2. Ignore if typing in text inputs or contenteditable
  if (typing(event.target)) return;

  const ui = useUi.getState();
  const player = usePlayer.getState();

  // 3. Escape: Close modals, full-screen player, or side panel
  if (event.key === 'Escape') {
    if (ui.shortcutsOpen) {
      ui.setShortcutsOpen(false);
      return;
    }
    if ((event.target as HTMLElement | null)?.closest('[role="menu"], [role="dialog"]:not([data-full-screen])')) return;
    if (ui.fullScreen) ui.setFullScreen(false);
    else if (ui.sidePanel) ui.closeSidePanel();
    return;
  }

  // If inside an active popup menu, let it handle its own navigation
  if ((event.target as HTMLElement | null)?.closest('[role="menu"]')) return;

  // 4. Play / Pause: Space or K
  if ((event.key === ' ' || event.key === 'k' || event.key === 'K') && !command) {
    event.preventDefault();
    // If a button or link was clicked and kept focus, blur it so Space doesn't re-click it
    const el = event.target as HTMLElement | null;
    if (el && (el.tagName === 'BUTTON' || el.tagName === 'A')) {
      el.blur();
    }
    return player.togglePlay();
  }

  // 5. Next track:
  // - Shift + ArrowRight
  // - Cmd/Ctrl + ArrowRight
  // - 'n' or 'N' (without command)
  // - '>' (Shift + '.')
  if (
    (command && event.key === 'ArrowRight') ||
    (event.shiftKey && event.key === 'ArrowRight') ||
    ((event.key === 'n' || event.key === 'N') && !command) ||
    (event.key === '>' && !command)
  ) {
    event.preventDefault();
    return player.next();
  }

  // 6. Previous track:
  // - Shift + ArrowLeft
  // - Cmd/Ctrl + ArrowLeft
  // - 'p' or 'P' (without command)
  // - '<' (Shift + ',')
  if (
    (command && event.key === 'ArrowLeft') ||
    (event.shiftKey && event.key === 'ArrowLeft') ||
    ((event.key === 'p' || event.key === 'P') && !command) ||
    (event.key === '<' && !command)
  ) {
    event.preventDefault();
    return player.previous();
  }

  // 7. Seeking:
  // - ArrowLeft: -5s
  // - ArrowRight: +5s
  // - J: -10s
  // - L: +10s
  if (event.key === 'ArrowLeft' && !command && !event.shiftKey) {
    event.preventDefault();
    const audio = getAudio();
    const current = audio?.currentTime ?? 0;
    return player.seek(Math.max(0, current - 5));
  }
  if (event.key === 'ArrowRight' && !command && !event.shiftKey) {
    event.preventDefault();
    const audio = getAudio();
    const current = audio?.currentTime ?? 0;
    return player.seek(current + 5);
  }
  if ((event.key === 'j' || event.key === 'J') && !command) {
    event.preventDefault();
    const audio = getAudio();
    const current = audio?.currentTime ?? 0;
    return player.seek(Math.max(0, current - 10));
  }
  if ((event.key === 'l' || event.key === 'L') && !command) {
    event.preventDefault();
    const audio = getAudio();
    const current = audio?.currentTime ?? 0;
    return player.seek(current + 10);
  }

  // 8. Volume:
  // - ArrowUp: +5% (in fullScreen or with command)
  // - ArrowDown: -5% (in fullScreen or with command)
  // - '+' or '=': +5%
  // - '-' or '_': -5%
  // - M: Mute toggle
  if (
    (command && event.key === 'ArrowUp') ||
    (ui.fullScreen && event.key === 'ArrowUp') ||
    ((event.key === '=' || event.key === '+') && !command)
  ) {
    event.preventDefault();
    return player.setVolume(Math.min(1, Math.round((player.volume + 0.05) * 100) / 100));
  }
  if (
    (command && event.key === 'ArrowDown') ||
    (ui.fullScreen && event.key === 'ArrowDown') ||
    ((event.key === '-' || event.key === '_') && !command)
  ) {
    event.preventDefault();
    return player.setVolume(Math.max(0, Math.round((player.volume - 0.05) * 100) / 100));
  }
  if ((event.key === 'm' || event.key === 'M') && !command) {
    event.preventDefault();
    return player.toggleMute();
  }

  // 9. Playback modes:
  // - S: Toggle Shuffle
  // - R: Cycle Repeat
  if ((event.key === 's' || event.key === 'S') && !command) {
    event.preventDefault();
    return player.toggleShuffle();
  }
  if ((event.key === 'r' || event.key === 'R') && !command) {
    event.preventDefault();
    return player.cycleRepeat();
  }

  // 10. View & Panels:
  // - F: Toggle Full-screen Player
  // - C: Toggle Lyrics
  // - U: Toggle Up Next
  if ((event.key === 'f' || event.key === 'F') && !command) {
    event.preventDefault();
    return ui.setFullScreen(!ui.fullScreen);
  }
  if ((event.key === 'c' || event.key === 'C') && !command) {
    event.preventDefault();
    if (ui.fullScreen) {
      document.querySelector<HTMLButtonElement>('button[aria-label="Lyrics"]')?.click();
    } else {
      ui.toggleSidePanel('lyrics');
    }
    return;
  }
  if ((event.key === 'u' || event.key === 'U') && !command) {
    event.preventDefault();
    if (ui.fullScreen) {
      document.querySelector<HTMLButtonElement>('button[aria-label="Up Next"]')?.click();
    } else {
      ui.toggleSidePanel('up-next');
    }
    return;
  }

  // 11. Percentage seek: 0-9
  if (!command && !event.shiftKey && event.key >= '0' && event.key <= '9') {
    const audio = getAudio();
    const dur = audio?.duration || player.nowPlaying?.durationSec;
    if (dur && Number.isFinite(dur) && dur > 0) {
      event.preventDefault();
      const pct = Number(event.key) / 10;
      return player.seek(pct * dur);
    }
  }

  // 12. Search: /
  if (event.key === '/' && !command) {
    event.preventDefault();
    return focusSearch(navigate);
  }

  // 13. Help Cheat Sheet: ?
  if (event.key === '?' || (event.shiftKey && event.key === '/')) {
    event.preventDefault();
    return ui.setShortcutsOpen(!ui.shortcutsOpen);
  }
}

export function useKeyboardShortcuts() {
  const navigate = useNavigate();

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => handleKeyDown(event, navigate);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [navigate]);
}
