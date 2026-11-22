import { describe, expect, it } from 'vitest';
import { SHORTCUT_GROUPS } from '@/components/KeyboardShortcutsModal';
import { pages } from '@/pageRegistry';

describe('keyboard shortcuts page & groups', () => {
  it('registers /settings/shortcuts in the page registry', () => {
    const route = pages.find((p) => p.path === '/settings/shortcuts');
    expect(route).toBeDefined();
    expect(route?.load).toBeTypeOf('function');
  });

  it('provides structured shortcut categories and valid items', () => {
    expect(SHORTCUT_GROUPS.length).toBeGreaterThanOrEqual(3);
    const titles = SHORTCUT_GROUPS.map((g) => g.title);
    expect(titles).toContain('Playback');
    expect(titles).toContain('Volume');
    expect(titles).toContain('Navigation & View');

    for (const group of SHORTCUT_GROUPS) {
      expect(group.items.length).toBeGreaterThan(0);
      for (const item of group.items) {
        expect(item.keys.length).toBeGreaterThan(0);
        expect(item.desc.trim().length).toBeGreaterThan(0);
      }
    }
  });

  it('includes core playback keys Space and K', () => {
    const playback = SHORTCUT_GROUPS.find((g) => g.title === 'Playback');
    expect(playback).toBeDefined();
    const playPause = playback?.items.find((i) => i.desc.toLowerCase().includes('play'));
    expect(playPause).toBeDefined();
    expect(playPause?.keys).toContain('Space');
    expect(playPause?.keys).toContain('K');
  });
});
