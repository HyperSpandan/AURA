import { Keyboard, X } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useUi } from '@/state/ui';

interface ShortcutItem {
  keys: string[];
  desc: string;
}

interface ShortcutGroup {
  title: string;
  items: ShortcutItem[];
}

const GROUPS: ShortcutGroup[] = [
  {
    title: 'Playback',
    items: [
      { keys: ['Space', 'K'], desc: 'Play / Pause' },
      { keys: ['N', 'Shift + →'], desc: 'Next track' },
      { keys: ['P', 'Shift + ←'], desc: 'Previous track' },
      { keys: ['←', '→'], desc: 'Seek 5 seconds back / forward' },
      { keys: ['J', 'L'], desc: 'Seek 10 seconds back / forward' },
      { keys: ['0 – 9'], desc: 'Seek to percentage (0% to 90%)' },
    ],
  },
  {
    title: 'Volume',
    items: [
      { keys: ['↑', '↓'], desc: 'Volume up / down (in player)' },
      { keys: ['+', '–'], desc: 'Volume up / down' },
      { keys: ['M'], desc: 'Mute / Unmute' },
    ],
  },
  {
    title: 'Navigation & View',
    items: [
      { keys: ['F'], desc: 'Toggle full-screen player' },
      { keys: ['C'], desc: 'Toggle lyrics' },
      { keys: ['U'], desc: 'Toggle Up Next queue' },
      { keys: ['S'], desc: 'Toggle shuffle' },
      { keys: ['R'], desc: 'Cycle repeat mode' },
      { keys: ['/', '⌘ F'], desc: 'Search' },
      { keys: ['⌘ \\'], desc: 'Toggle sidebar rail' },
      { keys: ['Esc'], desc: 'Close dialogs / full-screen' },
    ],
  },
];

export function KeyboardShortcutsModal() {
  const open = useUi((s) => s.shortcutsOpen);
  const setOpen = useUi((s) => s.setShortcutsOpen);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open) {
      if (!dialog.open) dialog.showModal();
    } else {
      if (dialog.open) dialog.close();
    }
  }, [open]);

  if (!open) return null;

  return createPortal(
    <dialog
      ref={dialogRef}
      className="fixed inset-x-4 top-1/2 -translate-y-1/2 mx-auto my-0 max-w-2xl w-full rounded-3xl bg-[#1c1c1e] text-[#f6ede9] border border-white/10 shadow-2xl p-0 overflow-hidden z-[10005] backdrop:bg-black/60 backdrop:backdrop-blur-md"
      onCancel={(e) => {
        e.preventDefault();
        setOpen(false);
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) setOpen(false);
      }}
    >
      <div className="relative p-6 sm:p-8">
        <button
          onClick={() => setOpen(false)}
          className="absolute top-5 right-5 p-2 rounded-full text-white/50 hover:text-white hover:bg-white/10 transition-colors"
          aria-label="Close keyboard shortcuts"
        >
          <X size={20} />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="size-10 rounded-2xl bg-white/10 flex items-center justify-center text-white">
            <Keyboard size={22} />
          </div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-white">Keyboard Shortcuts</h2>
            <p className="text-xs text-white/60">Control playback and navigation with your keyboard</p>
          </div>
        </div>

        <div className="space-y-6 max-h-[70vh] overflow-y-auto pr-1">
          {GROUPS.map((group) => (
            <div key={group.title}>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-white/40 mb-3">
                {group.title}
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {group.items.map((item) => (
                  <div
                    key={item.desc}
                    className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-white/5 border border-white/[0.06]"
                  >
                    <span className="text-xs text-white/80">{item.desc}</span>
                    <div className="flex items-center gap-1.5 flex-none">
                      {item.keys.map((k) => (
                        <kbd
                          key={k}
                          className="px-2 py-1 text-[11px] font-semibold text-white/90 bg-white/10 border border-white/15 rounded-md shadow-sm font-mono leading-none min-w-[22px] text-center"
                        >
                          {k}
                        </kbd>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t border-white/10 flex items-center justify-between text-xs text-white/50">
          <span>Press <kbd className="px-1.5 py-0.5 rounded bg-white/10 text-white font-mono text-[10px]">?</kbd> anytime to open this helper</span>
          <button
            onClick={() => setOpen(false)}
            className="px-4 py-1.5 rounded-full bg-white text-black font-semibold text-xs hover:bg-white/90 transition-colors"
          >
            Done
          </button>
        </div>
      </div>
    </dialog>,
    document.body,
  );
}
