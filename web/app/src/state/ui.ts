import { create } from 'zustand';

export type SidePanel = 'up-next' | 'lyrics' | 'devices' | null;

interface UiState {
  /** The right-hand glass panel (report §7); one of Up Next, Lyrics or Devices at a time. */
  sidePanel: SidePanel;
  toggleSidePanel: (panel: Exclude<SidePanel, null>) => void;
  closeSidePanel: () => void;
  /** The full-screen player: artwork, controls and lyrics over the artwork's mesh (report §7). */
  fullScreen: boolean;
  setFullScreen: (open: boolean) => void;
  /** The sidebar as a drawer, below 484px where it's otherwise hidden. */
  drawerOpen: boolean;
  setDrawerOpen: (open: boolean) => void;
  /** The sidebar folded to a rail of icons, on screens wide enough to show it; kept in this browser. */
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  /** The extension install banner, dismissed for this tab only. */
  installBannerDismissed: boolean;
  dismissInstallBanner: () => void;
  /** Support AURA modal dialog */
  supportOpen: boolean;
  setSupportOpen: (open: boolean) => void;
  /** Keyboard shortcuts cheat sheet dialog */
  shortcutsOpen: boolean;
  setShortcutsOpen: (open: boolean) => void;
}

const BANNER_KEY = 'aura:install-banner-dismissed';
const SIDEBAR_KEY = 'aura:sidebar-collapsed';

// Storage can throw (blocked site data); the sidebar then just starts open and isn't remembered.
function remembered(key: string): boolean {
  try {
    return typeof localStorage !== 'undefined' && localStorage.getItem(key) === '1';
  } catch {
    return false;
  }
}

function sessionRemembered(key: string): boolean {
  try {
    return typeof sessionStorage !== 'undefined' && sessionStorage.getItem(key) === '1';
  } catch {
    return false;
  }
}

export const useUi = create<UiState>()((set) => ({
  sidePanel: null,
  toggleSidePanel: (panel) => set((s) => ({ sidePanel: s.sidePanel === panel ? null : panel })),
  closeSidePanel: () => set({ sidePanel: null }),
  fullScreen: false,
  setFullScreen: (fullScreen) => set({ fullScreen }),
  drawerOpen: false,
  setDrawerOpen: (drawerOpen) => set({ drawerOpen }),
  sidebarCollapsed: remembered(SIDEBAR_KEY),
  toggleSidebar: () =>
    set((s) => {
      const sidebarCollapsed = !s.sidebarCollapsed;
      try {
        localStorage.setItem(SIDEBAR_KEY, sidebarCollapsed ? '1' : '0');
      } catch {
        // Not remembered; it still folds for now.
      }
      return { sidebarCollapsed };
    }),
  installBannerDismissed: sessionRemembered(BANNER_KEY),
  dismissInstallBanner: () => {
    try {
      sessionStorage.setItem(BANNER_KEY, '1');
    } catch {
      // ignore
    }
    set({ installBannerDismissed: true });
  },
  supportOpen: false,
  setSupportOpen: (supportOpen) => set({ supportOpen }),
  shortcutsOpen: false,
  setShortcutsOpen: (shortcutsOpen) => set({ shortcutsOpen }),
}));

// For browser-driven tests, next to __bcPlayer.
if (import.meta.env.DEV && typeof window !== 'undefined') Object.assign(window, { __bcUi: useUi });

// A sidebar folded last time is folded before the first paint, so it doesn't ease shut on every load
// (the width eases between the two; see tokens.css). Sidebar keeps the attribute in step after that.
if (typeof document !== 'undefined' && useUi.getState().sidebarCollapsed) {
  document.documentElement.dataset.sidebar = 'collapsed';
}
