import type { CSSProperties } from 'react';

export function LogoMark({ size = 20, className }: { size?: number; className?: string }) {
  const style: CSSProperties = {
    display: 'inline-flex',
    width: size,
    height: size,
  };
  return (
    <span
      aria-hidden="true"
      className={`items-center justify-center overflow-hidden rounded-[26%] bg-black ${className ?? ''}`}
      style={style}
    >
      <img
        src={`${import.meta.env.BASE_URL}splash-logo.png`}
        alt=""
        draggable={false}
        className="h-full w-full scale-[1.45] object-contain"
      />
    </span>
  );
}
