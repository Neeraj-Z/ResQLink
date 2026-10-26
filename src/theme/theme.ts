import { Colors } from './colors';
import { Typography, EyebrowStyle, DataReadoutStyle } from './typography';
import { Spacing, IconContainer, Shapes } from './dimens';

export interface ThemeColors {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  onSecondary: string;
  background: string;
  onBackground: string;
  surface: string;
  onSurface: string;
  surfaceVariant: string;
  onSurfaceVariant: string;
  outline: string;
  outlineVariant: string;
  error: string;
}

export const DarkThemeColors: ThemeColors = {
  primary: Colors.Signal,
  onPrimary: Colors.Void,
  primaryContainer: Colors.SignalDeep,
  onPrimaryContainer: Colors.Signal,
  secondary: Colors.Ember,
  onSecondary: '#FFFFFF',
  background: Colors.DarkBackground,
  onBackground: Colors.DarkOnSurface,
  surface: Colors.DarkSurface,
  onSurface: Colors.DarkOnSurface,
  surfaceVariant: Colors.DarkSurfaceVariant,
  onSurfaceVariant: Colors.Slate,
  outline: Colors.Slate,
  outlineVariant: Colors.HairlineDark,
  error: Colors.Ember,
};

export const LightThemeColors: ThemeColors = {
  primary: Colors.SignalDim,
  onPrimary: '#FFFFFF',
  primaryContainer: Colors.LightSurfaceVariant,
  onPrimaryContainer: Colors.SignalDim,
  secondary: Colors.EmberDeep,
  onSecondary: '#FFFFFF',
  background: Colors.LightBackground,
  onBackground: Colors.LightOnSurface,
  surface: Colors.LightSurface,
  onSurface: Colors.LightOnSurface,
  surfaceVariant: Colors.LightSurfaceVariant,
  onSurfaceVariant: Colors.SlateLight,
  outline: Colors.SlateLight,
  outlineVariant: Colors.Hairline,
  error: Colors.EmberDeep,
};

export const Theme = {
  colors: DarkThemeColors,
  typography: Typography,
  eyebrow: EyebrowStyle,
  dataReadout: DataReadoutStyle,
  spacing: Spacing,
  iconContainer: IconContainer,
  shapes: Shapes,
};
