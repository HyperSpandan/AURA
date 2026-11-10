/** Only the AURA player path on GitHub Pages may use the session bridge. */
export const PRODUCTION_ORIGIN = 'https://hyperspandan.github.io';
export const SITE_ORIGINS = [PRODUCTION_ORIGIN, ...(process.env.AURA_STORE_BUILD === '1' ? [] : ['http://localhost:5173'])];
export function matchPatternsFor(origins: string[]): string[] {
  return origins.flatMap(o => o === PRODUCTION_ORIGIN ? [`${o}/AURA/player/*`] : [`${new URL(o).protocol}//${new URL(o).hostname}/*`]);
}
