// @vitest-environment happy-dom
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { createRoot } from 'react-dom/client';
import React, { act } from 'react';
import { FullScreenPlayer } from './FullScreenPlayer';
import { useConnect } from '@/connect/connect';
import { useUi } from '@/state/ui';
import { usePlayer } from '@/state/player';
import { MemoryRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

describe('FullScreenPlayer', () => {
  const queryClient = new QueryClient();

  beforeEach(() => {
    vi.clearAllMocks();
    usePlayer.setState({
      nowPlaying: null,
      status: 'idle',
      queue: [],
      index: -1,
    });
    useUi.setState({
      fullScreen: true,
    });
  });

  it('renders without crashing when controlling a remote device', () => {
    const remoteDevice = {
      id: 'phone_abc',
      name: 'AURA Android',
      form: 'phone',
      lastSeen: Date.now(),
      receivedAt: Date.now(),
      since: Date.now(),
      state: {
        type: 'state' as const,
        from: 'phone_abc',
        seq: 1,
        at: Date.now(),
        playing: true,
        track: {
          id: 'test_123',
          title: 'Earrings',
          artist: 'Malcolm Todd',
          thumb: 'https://example.com/thumb.jpg',
          length: '3:00',
          album: 'Sweet Boy',
        },
      },
    };

    useConnect.setState({
      view: {
        myId: 'web_xyz',
        myName: 'Web Player',
        relays: 1,
        devices: [remoteDevice],
        activeId: 'web_xyz', // Browser thinks it is activeId!
        pending: [],
      },
    });

    const div = document.createElement('div');
    document.body.appendChild(div);
    const root = createRoot(div);

    act(() => {
      root.render(
        React.createElement(
          QueryClientProvider,
          { client: queryClient },
          React.createElement(
            MemoryRouter,
            {},
            React.createElement(FullScreenPlayer, { closing: false })
          )
        )
      );
    });

    expect(useUi.getState().fullScreen).toBe(true);
  });
});
