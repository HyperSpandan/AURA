// @vitest-environment happy-dom
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { handleKeyDown, typing } from './useKeyboardShortcuts';
import { usePlayer } from '@/state/player';
import { useUi } from '@/state/ui';

describe('useKeyboardShortcuts', () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    usePlayer.setState({
      volume: 0.8,
      muted: false,
      status: 'playing',
      nowPlaying: {
        videoId: 'test-v1',
        title: 'Song',
        artist: 'Artist',
        artworkUrl: null,
        durationSec: 200,
        streamInfo: null,
      },
    });
    useUi.setState({
      fullScreen: false,
      shortcutsOpen: false,
      sidePanel: null,
    });
  });

  afterEach(() => {
    document.body.innerHTML = '';
  });

  describe('typing detection', () => {
    it('detects input fields and contenteditable', () => {
      const input = document.createElement('input');
      const textarea = document.createElement('textarea');
      const select = document.createElement('select');
      const editable = document.createElement('div');
      editable.contentEditable = 'true';
      const textbox = document.createElement('div');
      textbox.setAttribute('role', 'textbox');

      expect(typing(input)).toBe(true);
      expect(typing(textarea)).toBe(true);
      expect(typing(select)).toBe(true);
      expect(typing(editable)).toBe(true);
      expect(typing(textbox)).toBe(true);
    });

    it('returns false for regular non-text elements', () => {
      const button = document.createElement('button');
      const a = document.createElement('a');
      const div = document.createElement('div');
      const body = document.body;

      expect(typing(button)).toBe(false);
      expect(typing(a)).toBe(false);
      expect(typing(div)).toBe(false);
      expect(typing(body)).toBe(false);
      expect(typing(null)).toBe(false);
    });
  });

  describe('keyboard actions via handleKeyDown', () => {
    it('toggles play/pause with Space', () => {
      const togglePlaySpy = vi.fn();
      usePlayer.setState({ togglePlay: togglePlaySpy });

      const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });
      handleKeyDown(event, mockNavigate);

      expect(event.defaultPrevented).toBe(true);
      expect(togglePlaySpy).toHaveBeenCalledTimes(1);
    });

    it('toggles play/pause with Space even if a button was focused, and blurs it', () => {
      const togglePlaySpy = vi.fn();
      usePlayer.setState({ togglePlay: togglePlaySpy });

      const btn = document.createElement('button');
      document.body.appendChild(btn);
      btn.focus();
      const blurSpy = vi.spyOn(btn, 'blur');

      const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });
      Object.defineProperty(event, 'target', { value: btn, writable: false });

      handleKeyDown(event, mockNavigate);

      expect(event.defaultPrevented).toBe(true);
      expect(blurSpy).toHaveBeenCalled();
      expect(togglePlaySpy).toHaveBeenCalledTimes(1);
    });

    it('does not toggle play when typing in an input', () => {
      const togglePlaySpy = vi.fn();
      usePlayer.setState({ togglePlay: togglePlaySpy });

      const input = document.createElement('input');
      document.body.appendChild(input);

      const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });
      Object.defineProperty(event, 'target', { value: input, writable: false });

      handleKeyDown(event, mockNavigate);

      expect(event.defaultPrevented).toBe(false);
      expect(togglePlaySpy).not.toHaveBeenCalled();
    });

    it('toggles play/pause with K', () => {
      const togglePlaySpy = vi.fn();
      usePlayer.setState({ togglePlay: togglePlaySpy });

      const event = new KeyboardEvent('keydown', { key: 'k', cancelable: true });
      handleKeyDown(event, mockNavigate);

      expect(event.defaultPrevented).toBe(true);
      expect(togglePlaySpy).toHaveBeenCalledTimes(1);
    });

    it('seeks backward and forward with ArrowLeft and ArrowRight', () => {
      const seekSpy = vi.fn();
      usePlayer.setState({ seek: seekSpy });

      const left = new KeyboardEvent('keydown', { key: 'ArrowLeft', cancelable: true });
      handleKeyDown(left, mockNavigate);
      expect(left.defaultPrevented).toBe(true);
      expect(seekSpy).toHaveBeenCalledWith(0); // Math.max(0, 0 - 5)

      const right = new KeyboardEvent('keydown', { key: 'ArrowRight', cancelable: true });
      handleKeyDown(right, mockNavigate);
      expect(right.defaultPrevented).toBe(true);
      expect(seekSpy).toHaveBeenCalledWith(5);
    });

    it('jumps to next track with N and previous track with P', () => {
      const nextSpy = vi.fn();
      const prevSpy = vi.fn();
      usePlayer.setState({ next: nextSpy, previous: prevSpy });

      const nEvent = new KeyboardEvent('keydown', { key: 'n', cancelable: true });
      handleKeyDown(nEvent, mockNavigate);
      expect(nEvent.defaultPrevented).toBe(true);
      expect(nextSpy).toHaveBeenCalledTimes(1);

      const pEvent = new KeyboardEvent('keydown', { key: 'p', cancelable: true });
      handleKeyDown(pEvent, mockNavigate);
      expect(pEvent.defaultPrevented).toBe(true);
      expect(prevSpy).toHaveBeenCalledTimes(1);
    });

    it('toggles mute with M', () => {
      const muteSpy = vi.fn();
      usePlayer.setState({ toggleMute: muteSpy });

      const mEvent = new KeyboardEvent('keydown', { key: 'm', cancelable: true });
      handleKeyDown(mEvent, mockNavigate);
      expect(mEvent.defaultPrevented).toBe(true);
      expect(muteSpy).toHaveBeenCalledTimes(1);
    });

    it('toggles shuffle with S and repeat with R', () => {
      const shuffleSpy = vi.fn();
      const repeatSpy = vi.fn();
      usePlayer.setState({ toggleShuffle: shuffleSpy, cycleRepeat: repeatSpy });

      const sEvent = new KeyboardEvent('keydown', { key: 's', cancelable: true });
      handleKeyDown(sEvent, mockNavigate);
      expect(shuffleSpy).toHaveBeenCalledTimes(1);

      const rEvent = new KeyboardEvent('keydown', { key: 'r', cancelable: true });
      handleKeyDown(rEvent, mockNavigate);
      expect(repeatSpy).toHaveBeenCalledTimes(1);
    });

    it('toggles full-screen player with F', () => {
      expect(useUi.getState().fullScreen).toBe(false);

      const fEvent = new KeyboardEvent('keydown', { key: 'f', cancelable: true });
      handleKeyDown(fEvent, mockNavigate);
      expect(fEvent.defaultPrevented).toBe(true);
      expect(useUi.getState().fullScreen).toBe(true);
    });

    it('opens keyboard shortcuts dialog with ?', () => {
      expect(useUi.getState().shortcutsOpen).toBe(false);

      const qEvent = new KeyboardEvent('keydown', { key: '?', cancelable: true });
      handleKeyDown(qEvent, mockNavigate);
      expect(qEvent.defaultPrevented).toBe(true);
      expect(useUi.getState().shortcutsOpen).toBe(true);
    });

    it('adjusts volume with + and -', () => {
      const setVolumeSpy = vi.fn();
      usePlayer.setState({ volume: 0.8, setVolume: setVolumeSpy });

      const plus = new KeyboardEvent('keydown', { key: '+', cancelable: true });
      handleKeyDown(plus, mockNavigate);
      expect(plus.defaultPrevented).toBe(true);
      expect(setVolumeSpy).toHaveBeenCalledWith(0.85);

      const minus = new KeyboardEvent('keydown', { key: '-', cancelable: true });
      handleKeyDown(minus, mockNavigate);
      expect(minus.defaultPrevented).toBe(true);
      expect(setVolumeSpy).toHaveBeenCalledWith(0.75);
    });
  });
});
