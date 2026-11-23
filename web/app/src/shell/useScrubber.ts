import { useEffect, useRef, useState } from 'react';
import { getAudio, usePlayer } from '@/state/player';
import { useControlling, seekRemote } from '@/connect/connect';
import { positionAt } from '@/connect/session';
import { lengthSec } from '@/connect/shown';

export function clock(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const s = Math.floor(seconds);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

/**
 * A seek line drawn from the media clock every frame while playing, by writing
 * `--progress` on the bar and the times into their spans: no React state, so
 * nothing re-renders. Pressing or dragging on the scrubber seeks on release.
 * Shared by the LCD and the full-screen player.
 */
export function useScrubber() {
  const bar = useRef<HTMLDivElement>(null);
  const elapsed = useRef<HTMLSpanElement>(null);
  const remaining = useRef<HTMLSpanElement>(null);
  const status = usePlayer((s) => s.status);
  const deck = usePlayer((s) => s.deck);
  const seek = usePlayer((s) => s.seek);
  const remote = useControlling();
  const [dragging, setDragging] = useState<number | null>(null);

  useEffect(() => {
    const element = bar.current;
    if (!element) return;
    let frame = 0;

    if (remote) {
      const dur = lengthSec(remote.state?.track?.length);
      const draw = () => {
        const time = dragging !== null && dur > 0 ? dragging * dur : positionAt(remote, Date.now()) / 1000;
        const fraction = dur > 0 ? Math.min(1, Math.max(0, time / dur)) : 0;
        element.style.setProperty('--progress', String(fraction));
        if (elapsed.current) elapsed.current.textContent = clock(time);
        if (remaining.current) remaining.current.textContent = `-${clock(Math.max(0, dur - time))}`;
        if (remote.state?.playing || dragging !== null) frame = requestAnimationFrame(draw);
      };
      draw();
      return () => cancelAnimationFrame(frame);
    } else {
      const audio = getAudio();
      if (!audio) return;
      const draw = () => {
        const duration = audio.duration;
        const time = dragging !== null && Number.isFinite(duration) ? dragging * duration : audio.currentTime;
        const fraction = duration > 0 ? time / duration : 0;
        element.style.setProperty('--progress', String(Math.min(1, Math.max(0, fraction))));
        if (elapsed.current) elapsed.current.textContent = clock(time);
        if (remaining.current) remaining.current.textContent = `-${clock((duration || 0) - time)}`;
        if (!audio.paused || dragging !== null) frame = requestAnimationFrame(draw);
      };
      draw();
      const onSeeked = () => draw();
      audio.addEventListener('seeked', onSeeked);
      return () => {
        cancelAnimationFrame(frame);
        audio.removeEventListener('seeked', onSeeked);
      };
    }
  }, [status, deck, dragging, remote]);

  const fractionAt = (event: React.PointerEvent<HTMLElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    return Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
  };

  const handlers = {
    onPointerDown: (e: React.PointerEvent<HTMLElement>) => {
      if (!getAudio() && !remote) return;
      e.currentTarget.setPointerCapture(e.pointerId);
      setDragging(fractionAt(e));
    },
    onPointerMove: (e: React.PointerEvent<HTMLElement>) => {
      if (dragging !== null) setDragging(fractionAt(e));
    },
    onPointerUp: (e: React.PointerEvent<HTMLElement>) => {
      if (dragging === null) return;
      const frac = fractionAt(e);
      if (remote) {
        const dur = lengthSec(remote.state?.track?.length);
        if (dur > 0) seekRemote(frac * dur * 1000);
      } else {
        const audio = getAudio();
        if (audio && Number.isFinite(audio.duration)) seek(frac * audio.duration);
      }
      setDragging(null);
    },
    onPointerCancel: () => setDragging(null),
  };

  return { bar, elapsed, remaining, dragging: dragging !== null, handlers };
}

