import { createRefractedRenderer } from '../web/app/src/components/refracted-renderer';

function init() {
  const canvas = document.getElementById('refracted-canvas') as HTMLCanvasElement;
  if (!canvas) return;

  const text = 'AURA';
  const fontFamily = "'ReactDrops Square Display', 'Arial Black', sans-serif";
  const fontWeight = 900;
  const letterSpacing = 'normal';
  const extraBold = 2.25;
  const cornerRounding = 1;

  let renderer: any = null;

  function setup() {
    if (!canvas.clientWidth || !canvas.clientHeight) return;
    if (!renderer) {
      renderer = createRefractedRenderer(
        canvas,
        text,
        fontFamily,
        fontWeight,
        letterSpacing,
        extraBold,
        cornerRounding
      );
    }
    if (renderer) {
      renderer.resize(true);
    }
  }

  // Handle ResizeObserver
  if (typeof ResizeObserver !== 'undefined') {
    const ro = new ResizeObserver(() => {
      setup();
    });
    ro.observe(canvas);
    if (canvas.parentElement) ro.observe(canvas.parentElement);
  }

  // Handle font loading
  if (document.fonts) {
    document.fonts.ready.then(() => {
      setup();
    });
  }

  setup();

  window.addEventListener('resize', () => {
    if (renderer) renderer.resize(true);
  });

  let startTime = performance.now();

  function loop(now: number) {
    requestAnimationFrame(loop);
    if (!renderer) {
      setup();
      if (!renderer) return;
    }

    const elapsed = (now - startTime) / 1000.0;
    renderer.render(
      elapsed,       // time
      13.2,          // duration
      6.9,           // openingDuration
      elapsed,       // revealTime (character lift)
      1.75,          // liftDuration
      0.55,          // stagger
      'power4.out',  // ease
      false,         // beamFollowsLift
      0, 0,          // pointerX, pointerY
      0,             // pointerInfluence
      false,         // cursorMovesBeam
      false,         // fixedBeam
      0.5,           // beamPosition
      0,             // lightTheme (0 = dark)
      1.2,           // intensity
      1.0,           // dispersion
      1.0,           // colorIntensity
      'auto',        // beamColor
      'auto',        // spectrumWarmColor
      'auto',        // spectrumMiddleColor
      'auto'         // spectrumCoolColor
    );
  }
  requestAnimationFrame(loop);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
