import { Maximize2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useUi } from '@/state/ui';
import { positionAt, type Device } from './session';

const seconds = (length: string | null | undefined): number => {
  const parts = length?.split(':').map(Number) ?? [];
  return parts.length && parts.every(Number.isFinite) ? parts.reduce((a, b) => a * 60 + b, 0) : 0;
};

/**
 * The player bar's middle while another of the person's devices plays: its
 * song, "Playing on" it, and how far in, counted on from its last report.
 */
export function RemoteLcd({ device }: { device: Device }) {
  const [now, setNow] = useState(Date.now);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 500);
    return () => clearInterval(timer);
  }, []);
  const s = device.state;
  const track = s?.track;
  const total = seconds(track?.length) * 1000;
  const done = total ? Math.min(1, positionAt(device, now) / total) : 0;
  const openFullScreen = useUi((s) => s.setFullScreen);

  return (
    <div
      className="group/lcd relative grid h-full items-center gap-x-2 pb-1.5 grid-cols-[34px_minmax(0,1fr)] cursor-pointer select-none"
      onClick={() => openFullScreen(true)}
      role="button"
      tabIndex={0}
      aria-label="Open full screen player"
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          openFullScreen(true);
        }
      }}
    >
      <button
        type="button"
        className="group/art relative block h-[34px] w-[34px] overflow-hidden rounded-[6px] bg-joe p-0 transition-[filter,opacity]"
        aria-label="Open full screen player"
        data-cover-origin
        onClick={(e) => {
          e.stopPropagation();
          openFullScreen(true);
        }}
      >
        {track?.thumb && (
          <img
            src={track.thumb}
            alt=""
            referrerPolicy="no-referrer"
            decoding="async"
            className="size-full object-cover"
          />
        )}
        <span
          className="absolute inset-0 grid place-items-center bg-[rgb(0_0_0/0.5)] text-white opacity-0 transition-opacity duration-200 group-hover/pill:opacity-100 group-focus-visible/art:opacity-100"
          aria-hidden="true"
        >
          <Maximize2 size={14} strokeWidth={2.25} />
        </span>
      </button>

      <div className="flex min-w-0 flex-col">
        <p className="truncate type-body-emphasized text-primary">{track?.title ?? 'Something of its own'}</p>
        <p className="truncate type-callout-medium text-key">
          {track?.artist ? `${track.artist} · ` : ''}Playing on {device.name || 'another device'}
        </p>
      </div>

      <div className="absolute right-0 bottom-0 left-0 h-[3px] rounded-full bg-quaternary">
        <div className="h-full rounded-full bg-key-bg transition-[width] duration-300" style={{ width: `${done * 100}%` }} />
      </div>
    </div>
  );
}
