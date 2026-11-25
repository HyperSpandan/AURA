import { Check } from 'lucide-react';
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { GlassCard } from '@/components/liquid-glass/glass-card';

export type MenuPlacement = 'above-start' | 'below-end';

interface MenuProps {
  /** The element the menu opens from. */
  anchor: HTMLElement;
  placement?: MenuPlacement;
  onClose: () => void;
  label: string;
  children: ReactNode;
}

/**
 * Apple's contextual menu (report §8): a glass panel, 185px minimum, 13px
 * items, hairline separators, red for destructive items. Closes on Escape, a
 * click outside, or a scroll of the page under it.
 */
export function Menu({ anchor, placement = 'below-end', onClose, label, children }: MenuProps) {
  const panel = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState<React.CSSProperties>({ visibility: 'hidden' });

  useLayoutEffect(() => {
    if (!anchor) return;
    const rect = anchor.getBoundingClientRect();
    const menuEl = panel.current;
    const menuWidth = menuEl && menuEl.offsetWidth > 0 ? menuEl.offsetWidth : 260;
    const gap = 6;
    const spaceBelow = window.innerHeight - rect.bottom;
    const spaceAbove = rect.top;
    const goAbove = placement.startsWith('above') || (spaceBelow < 320 && spaceAbove > spaceBelow);

    const pos: React.CSSProperties = {
      maxHeight: Math.min(480, goAbove ? rect.top - 2 * gap : window.innerHeight - rect.bottom - 2 * gap),
    };

    if (goAbove) {
      pos.bottom = window.innerHeight - rect.top + gap;
    } else {
      pos.top = rect.bottom + gap;
    }

    const alignStart = placement.endsWith('-start');

    if (alignStart) {
      // If anchor is in a collapsed sidebar rail, open to the right of the rail icon
      const isRail = rect.left < 80 && rect.width <= 48;
      const idealLeft = isRail ? rect.right + 8 : rect.left;
      pos.left = Math.max(12, Math.min(idealLeft, window.innerWidth - menuWidth - 12));
    } else {
      // Align right edge of menu with right edge of anchor, clamped within screen
      const idealRight = window.innerWidth - rect.right;
      if (rect.right < menuWidth + 12) {
        pos.left = 12;
      } else {
        pos.right = Math.max(12, Math.min(idealRight, window.innerWidth - menuWidth - 12));
      }
    }

    setPosition(pos);
  }, [anchor, placement]);

  useEffect(() => {
    panel.current?.querySelector<HTMLElement>('[role^="menuitem"]')?.focus({ preventScroll: true });
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        onClose();
        anchor.focus();
      } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        const items = [...(panel.current?.querySelectorAll<HTMLElement>('[role^="menuitem"]:not(:disabled)') ?? [])];
        const index = items.indexOf(document.activeElement as HTMLElement);
        const next = event.key === 'ArrowDown' ? index + 1 : index - 1;
        items[(next + items.length) % items.length]?.focus();
      }
    };
    const onScroll = (event: Event) => {
      if (!panel.current?.contains(event.target as Node)) onClose();
    };
    document.addEventListener('keydown', onKey, true);
    document.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onClose);
    return () => {
      document.removeEventListener('keydown', onKey, true);
      document.removeEventListener('scroll', onScroll, true);
      window.removeEventListener('resize', onClose);
    };
  }, [anchor, onClose]);

  return createPortal(
    <>
      <div className="fixed inset-0 z-[calc(var(--z-contextual-menus)-1)]" onPointerDown={onClose} aria-hidden="true" />
      <div
        ref={panel}
        className="fixed z-(--z-contextual-menus) max-w-[320px] min-w-[200px] animate-menu-in"
        style={position}
        role="menu"
        aria-label={label}
      >
        <GlassCard blur={6} tint={0.68} className="max-h-[inherit] overflow-y-auto rounded-[14px] p-[5px]">
          {children}
        </GlassCard>
      </div>
    </>,
    document.body,
  );
}

interface MenuItemProps {
  onSelect: () => void;
  icon?: ReactNode;
  /** Shows a checkmark (a radio-style choice); null leaves the space empty. */
  checked?: boolean | null;
  destructive?: boolean;
  disabled?: boolean;
  /** Extra text at the trailing edge. */
  trailing?: ReactNode;
  children: ReactNode;
}

export function MenuItem({ onSelect, icon, checked, destructive, disabled, trailing, children }: MenuItemProps) {
  return (
    <button
      className="flex min-h-7 w-full items-center gap-2 rounded-[5px] px-2 py-1 text-start type-body text-primary enabled:hover:bg-quinary focus-visible:bg-quinary disabled:text-tertiary data-destructive:text-destructive"
      role={checked === undefined ? 'menuitem' : 'menuitemradio'}
      aria-checked={checked === undefined ? undefined : !!checked}
      data-destructive={destructive || undefined}
      disabled={disabled}
      onClick={onSelect}
    >
      {checked !== undefined && <span className="inline-grid w-3.5 flex-none place-items-center text-key">{checked && <Check size={14} strokeWidth={2.6} />}</span>}
      {icon && <span className="inline-grid w-5 flex-none place-items-center text-secondary">{icon}</span>}
      <span className="min-w-0 flex-1 truncate">{children}</span>
      {trailing && <span className="flex-none text-secondary">{trailing}</span>}
    </button>
  );
}

export function MenuSeparator() {
  return <div className="mx-2 my-[5px] border-t-[0.5px] border-t-vibrant-divider" role="separator" />;
}

/** A non-interactive heading inside a menu. */
export function MenuHeader({ children }: { children: ReactNode }) {
  return <div className="px-2 pt-1.5 pb-1 type-subhead-emphasized text-secondary truncate">{children}</div>;
}
