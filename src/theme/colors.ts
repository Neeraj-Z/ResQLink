// ResQLink design tokens (v2: "near-future ops console")
// A dark, deep-space base with one cyan signal accent -- the visual
// language of a control-room display, not a consumer social app. Ember
// stays reserved for exactly one thing (SOS / extreme severity) so it
// still reads as genuine alarm, not just "the app's red".

export const Colors = {
  // Core neutrals -- deep space, not pure black
  Void: '#06090F',          // deepest background
  Ink: '#0B0F17',           // primary dark surface
  InkRaised: '#121826',     // elevated dark surface (cards)
  InkRaisedHigh: '#1A2233', // highest elevation (sheets, dialogs)
  Paper: '#F4F6F9',         // light-theme background (cool, not cream)
  PaperSurface: '#FFFFFF',
  PaperRaised: '#EDF0F5',

  Slate: '#7C8AA0',         // secondary text, dark theme
  SlateLight: '#4C5872',    // secondary text, light theme
  Hairline: '#DDE2EA',      // light-mode borders
  HairlineDark: '#232B3D',  // dark-mode borders
  HairlineGlow: '#2A5A6B',  // dark-mode borders on active/focused elements

  // Signal accent -- the app's one "technology" color, used for structure,
  // active states, data readouts, and mesh/connectivity indicators.
  Signal: '#35E0C8',        // cyan-teal, the core accent
  SignalDim: '#1E8A7A',     // muted variant for secondary UI
  SignalDeep: '#0E4A48',    // deep variant for containers/fills

  // Reserved strictly for SOS and EXTREME/HIGH hazard severity
  Ember: '#FF4F3E',
  EmberDeep: '#C22E22',
  EmberGlow: '#4A1712',     // low-alpha-equivalent solid for glow backgrounds

  Moss: '#3DDC97',          // resolved / safe / LOW severity
  Amber: '#FFB23E',         // caution / MODERATE severity

  // Light theme surfaces
  LightBackground: '#F4F6F9',
  LightSurface: '#FFFFFF',
  LightSurfaceVariant: '#EDF0F5',
  LightOnSurface: '#10141C',

  // Dark theme surfaces (primary theme -- this app is designed dark-first)
  DarkBackground: '#06090F',
  DarkSurface: '#0B0F17',
  DarkSurfaceVariant: '#121826',
  DarkOnSurface: '#E7ECF3',
};
