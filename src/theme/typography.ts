import { TextStyle, Platform } from 'react-native';
import { Colors } from './colors';

const monoFont = Platform.select({
  ios: 'Courier',
  android: 'monospace',
  default: 'monospace',
});

export const Typography = {
  headlineLarge: {
    fontSize: 30,
    lineHeight: 36,
    fontWeight: '600',
    letterSpacing: -0.4,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  headlineMedium: {
    fontSize: 25,
    lineHeight: 31,
    fontWeight: '600',
    letterSpacing: -0.3,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  titleLarge: {
    fontSize: 20,
    lineHeight: 26,
    fontWeight: '600',
    letterSpacing: -0.1,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  titleMedium: {
    fontSize: 16,
    lineHeight: 22,
    fontWeight: '600',
    letterSpacing: 0,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  titleSmall: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '600',
    letterSpacing: 0.1,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  bodyLarge: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
    letterSpacing: 0.1,
    color: Colors.DarkOnSurface,
  } as TextStyle,
  bodyMedium: {
    fontSize: 14,
    lineHeight: 21,
    fontWeight: '400',
    letterSpacing: 0.1,
    color: Colors.Slate,
  } as TextStyle,
  bodySmall: {
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '400',
    letterSpacing: 0.1,
    color: Colors.Slate,
  } as TextStyle,
  labelLarge: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '500',
    letterSpacing: 0.2,
    color: Colors.DarkOnSurface,
  } as TextStyle,

  labelMedium: {
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '500',
    letterSpacing: 0.3,
    color: Colors.Slate,
  } as TextStyle,
  labelSmall: {
    fontSize: 11,
    lineHeight: 14,
    fontWeight: '600',
    letterSpacing: 1.6,
    color: Colors.Slate,
  } as TextStyle,
};

/** Small uppercase section eyebrow (e.g. "NEARBY HAZARDS") */
export const EyebrowStyle: TextStyle = {
  fontSize: 11,
  lineHeight: 14,
  fontWeight: '700',
  letterSpacing: 1.6,
  textTransform: 'uppercase',
  color: Colors.Slate,
};

/**
 * Monospace "data readout" style -- coordinates, hop counts, timestamps, IDs.
 */
export const DataReadoutStyle: TextStyle = {
  fontFamily: monoFont,
  fontWeight: '500',
  fontSize: 13,
  lineHeight: 18,
  letterSpacing: 0.2,
};
