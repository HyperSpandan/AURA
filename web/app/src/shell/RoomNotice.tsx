import { useEffect, useState } from 'react';
import { GlassCard } from '@/components/liquid-glass/glass-card';
import { useRoom } from '@/together/room';

/** A room's short messages ("Only the host can change the song"), for a few seconds over the page. */
export function RoomNotice() {
  const notice = useRoom((s) => s.notice);
  const [shown, setShown] = useState(false);
  useEffect(() => {
    if (!notice) return;
    setShown(true);
    const timer = setTimeout(() => setShown(false), 3500);
    return () => clearTimeout(timer);
  }, [notice]);
  if (!notice) return null;
  return (
    <GlassCard key={notice.at} blur={4} tint={0.65} className="ot-room-notice rounded-[20px]" data-shown={shown || undefined} role="status">
      {notice.text}
    </GlassCard>
  );
}
