import { Check } from 'lucide-react';
import { GlassTabs } from '@/components/liquid-glass/glass-tabs';
import { GlassSelect } from '@/components/liquid-glass/glass-select';
import { liquidGlassSupported } from '@/lib/liquidGlass';
import { ACCENTS, PLAYER_STYLES, useAppearance } from '@/state/appearance';
import { Toggle } from './IntegrationSettings';
import { settingsUi } from './settingsStyles';

const label = (value: string) => value.replace(/-/g, ' ').replace(/^./, (s) => s.toUpperCase());

/** A row of choices with the picked one raised, as the app's segmented buttons. */
function Segmented<T extends string>({ title, value, options, onChange }: { title: string; value: T; options: [T, string][]; onChange: (v: T) => void }) {
  return (
    <div className={`${settingsUi.row} flex-wrap gap-3`}>
      <span>{title}</span>
      <GlassTabs
        aria-label={title}
        size="sm"
        value={value}
        onValueChange={(v) => onChange(v as T)}
        options={options.map(([v, name]) => ({ value: v, label: name }))}
      />
    </div>
  );
}

export function AppearanceSettings() {
  const a = useAppearance();
  return (
    <>
      <section className={settingsUi.group} aria-label="Appearance">
        <Segmented title="Theme" value={a.mode} options={[['system', 'System'], ['light', 'Light'], ['dark', 'Dark']]} onChange={(mode) => a.update({ mode })} />
        <Toggle label="Pure black" detail="True black surfaces in dark mode." checked={a.pureBlack} onChange={(pureBlack) => a.update({ pureBlack })} />
        <Toggle
          label="Liquid Glass"
          detail={liquidGlassSupported() ? 'The player and floating buttons bend what’s behind them, like a lens.' : 'Needs Chrome or Edge. This browser keeps frosted glass.'}
          checked={a.liquidGlass && liquidGlassSupported()}
          onChange={(liquidGlass) => a.update({ liquidGlass })}
        />
        <Toggle label="Colors from artwork" detail="Let the playing song color AURA." checked={a.artworkColors} onChange={(artworkColors) => a.update({ artworkColors })} />
        <fieldset className="ot-accent-picker hairline-[16px]">
          <legend>
            Accent color
            {a.artworkColors && <span>Picking one turns off colors from artwork.</span>}
          </legend>
          <div>
            {ACCENTS.map(([name, color]) => (
              <button key={color} aria-label={name} title={name} aria-pressed={!a.artworkColors && a.accent === color} onClick={() => a.update({ accent: color, artworkColors: false })} style={{ background: color }}>
                {!a.artworkColors && a.accent === color && <Check size={16} strokeWidth={3} />}
              </button>
            ))}
          </div>
        </fieldset>
      </section>
      <h3 className={settingsUi.sectionHeader}>Now Playing</h3>
      <section className={settingsUi.group} aria-label="Now Playing">
        <Segmented title="Background" value={a.playerBackground} options={[['mesh', 'Mesh'], ['gradient', 'Gradient'], ['echo', 'Echo glow']]} onChange={(playerBackground) => a.update({ playerBackground })} />
        <div className={settingsUi.controlRow}>
          <span>Player style</span>
          <GlassSelect
            label=""
            className="w-56 [&>label]:hidden"
            value={a.playerStyle}
            onValueChange={(v) => v && a.update({ playerStyle: v as typeof a.playerStyle })}
            options={PLAYER_STYLES.map((v) => ({ value: v, label: label(v) }))}
          />
        </div>
        <Toggle label="Animations" detail="Moving artwork, the player's waves and page motion. Your system's reduced-motion setting also applies." checked={a.motion} onChange={(motion) => a.update({ motion })} />
      </section>
      <p className={settingsUi.footer}>Saved in this browser. Wallpaper colors are in the Android app.</p>
    </>
  );
}
