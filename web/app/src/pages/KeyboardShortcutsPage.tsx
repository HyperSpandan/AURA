import { HelpCircle, Keyboard } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { SHORTCUT_GROUPS } from '@/components/KeyboardShortcutsModal';
import { useUi } from '@/state/ui';
import { settingsUi } from './settingsStyles';

/**
 * Settings › Keyboard shortcuts: a reference page of all keyboard shortcuts
 * available across the web player for controlling playback, volume, and navigation.
 */
export default function KeyboardShortcutsPage() {
  const setShortcutsOpen = useUi((s) => s.setShortcutsOpen);

  return (
    <div className={settingsUi.centered}>
      <PageHeader title="Keyboard shortcuts" back={{ to: '/settings', label: 'Settings' }} />
      <div className={settingsUi.body}>
        <div className="mx-4 mb-6 flex flex-col gap-2">
          <p className="type-body-tall text-secondary">
            Control playback, volume, and navigation from anywhere in AURA.
            Keyboard shortcuts are active across the app and automatically pause whenever you are typing in an input field or search bar.
          </p>
          <div className="flex items-center gap-2 mt-1">
            <span className="type-callout text-secondary">Quick tip: Press</span>
            <kbd className="px-2 py-0.5 text-xs font-mono font-semibold rounded bg-quaternary border border-white/10 text-primary">
              ?
            </kbd>
            <span className="type-callout text-secondary">anytime to open the instant floating cheat sheet.</span>
          </div>
        </div>

        {SHORTCUT_GROUPS.map((group) => (
          <div key={group.title} className="mb-6 last:mb-2">
            <h2 className={settingsUi.sectionHeader}>{group.title}</h2>
            <section className={settingsUi.group}>
              {group.items.map((item) => (
                <div key={item.desc} className={settingsUi.integrationPlain}>
                  <span className={settingsUi.integrationName}>{item.desc}</span>
                  <div className="flex items-center gap-1.5 flex-none">
                    {item.keys.map((k) => (
                      <kbd
                        key={k}
                        className="px-2 py-1 text-xs font-mono font-semibold text-primary bg-quaternary border border-white/10 rounded-md shadow-sm min-w-[24px] text-center"
                      >
                        {k}
                      </kbd>
                    ))}
                  </div>
                </div>
              ))}
            </section>
          </div>
        ))}

        <div className="mx-4 mt-6 p-4 rounded-2xl bg-quaternary/40 border border-white/[0.06] flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="size-9 rounded-xl bg-white/10 flex items-center justify-center text-primary flex-none">
              <Keyboard size={18} />
            </div>
            <div className="min-w-0">
              <p className="type-body-emphasized text-primary">Overlay Cheat Sheet</p>
              <p className="type-caption text-secondary">Open the floating modal cheat sheet directly</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setShortcutsOpen(true)}
            className="flex-none rounded-pill bg-quaternary px-4 py-2 type-body-emphasized text-primary hover:bg-white/15 transition-colors cursor-pointer"
          >
            Open overlay
          </button>
        </div>

        <p className={settingsUi.footer}>
          On macOS, use Command (⌘); on Windows and Linux, use Ctrl. Number keys (0–9) jump directly to track percentages.
        </p>
      </div>
    </div>
  );
}
