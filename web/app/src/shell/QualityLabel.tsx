import { AudioLines, X } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useAddons } from '@/addons/client';
import { usePlayer } from '@/state/player';
import { useControlling } from '@/connect/connect';
import { qualityLabel } from './quality';
export function QualityLabel() {
  const remote = useControlling();
  const track = usePlayer(s => s.nowPlaying);
  const status = usePlayer(s => s.status);
  const search = useAddons(s => s.status);
  const enabled = useAddons(s => s.jioSaavn || s.sources.some(a => a.enabled));
  const upgrading = useAddons(s => s.checkingVideoId === track?.videoId);
  const [details, setDetails] = useState(false);
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { if (details) dialog.current?.showModal(); }, [details]);
  const close = () => { dialog.current?.close(); setDetails(false); };
  const quality = qualityLabel(track?.streamInfo ?? null, enabled && upgrading, status);
  const text = remote ? (remote.state?.quality ?? 'Connected') : quality.text;
  const lossless = remote
    ? (remote.state?.quality ? /flac|lossless|hi-res|master/i.test(remote.state.quality) : false)
    : quality.lossless;
  return <>
    <button className="ot-quality" data-lossless={lossless} onClick={() => setDetails(true)} aria-label={`Audio details: ${text}`}>
      {lossless ? <AudioLines size={17} /> : <span className="ot-hq" aria-hidden="true">HQ</span>}<span>{text}</span>
    </button>
    {/* In the body, not inside the player: the player's idle fade and drag handling would reach it there. */}
    {details && createPortal(<dialog
      ref={dialog}
      className="ot-audio-details"
      aria-label="Audio details"
      onCancel={e => { e.preventDefault(); close(); }}
      // A click on the dimmed area around the sheet lands on the dialog itself.
      onClick={e => { if (e.target === e.currentTarget) close(); }}
      onKeyDown={e => e.stopPropagation()}
    >
      <header><h2>Audio pipeline</h2><button className="ot-audio-close" onClick={close} aria-label="Close"><X size={20} /></button></header>
      <div className="ot-audio-body">
        <p>{remote ? (remote.state?.quality ? `Audio format: ${remote.state.quality}` : 'Connected device playback') : (track?.streamInfo ?? 'Waiting for decoded audio')}</p>
        <p className="whitespace-pre-line">{search}</p><p>The browser and operating system control the final output sample rate.</p>
      </div>
      <button className="ot-audio-done" autoFocus onClick={close}>Done</button>
    </dialog>, document.body)}
  </>;
}
