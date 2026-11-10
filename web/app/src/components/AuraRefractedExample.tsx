import { RefractedText } from './refracted-text';

export function Example() {
  return (
    <div style={{ background: '#050505', padding: '3rem', containerType: 'inline-size' }}>
      <RefractedText
        text={"AURA"}
        theme={"dark"}
        topColor={"auto"}
        bottomColor={"auto"}
        beamColor={"auto"}
        spectrumWarmColor={"auto"}
        spectrumMiddleColor={"auto"}
        spectrumCoolColor={"auto"}
        reveal={true}
        stagger={0.55}
        liftDuration={1.75}
        ease={"power4.out"}
        beamFollowsLift={false}
        animateBeam={true}
        openingDuration={6.9}
        duration={13.2}
        intensity={1.2}
        dispersion={1}
        colorIntensity={1}
        beamPosition={0.5}
        fixedBeam={false}
        paused={false}
        cursorMovesBeam={false}
        fontSize={"400px"}
        fontFamily={"'ReactDrops Square Display', 'Arial Black', sans-serif"}
        fontWeight={900}
        letterSpacing={"normal"}
        extraBold={2.25}
        fontHeight={80}
        cornerRounding={1}
      />
    </div>
  );
}
