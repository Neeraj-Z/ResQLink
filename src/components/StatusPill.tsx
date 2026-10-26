import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { DataReadoutStyle } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';

interface StatusPillProps {
  text: string;
  color: string;
  style?: ViewStyle;
}

/**
 * Small rounded status badge with a leading indicator dot -- e.g. "● 3 peers
 * connected", "● 28.61390, 77.20900" -- matching StatusPill.kt exactly.
 */
export const StatusPill: React.FC<StatusPillProps> = ({ text, color, style }) => {
  // Convert hex color to rgba for alphas 0.10 and 0.30
  const hexToRgba = (hex: string, alpha: number) => {
    const c = hex.replace('#', '');
    if (c.length === 6) {
      const r = parseInt(c.substring(0, 2), 16);
      const g = parseInt(c.substring(2, 4), 16);
      const b = parseInt(c.substring(4, 6), 16);
      return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }
    return color;
  };

  const bgAlpha = hexToRgba(color, 0.1);
  const borderAlpha = hexToRgba(color, 0.3);

  return (
    <View style={[styles.container, { backgroundColor: bgAlpha, borderColor: borderAlpha }, style]}>
      <View style={[styles.dot, { backgroundColor: color }]} />
      <Text style={[styles.text, { color }]}>{text}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'center',
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xxs + 1,
    borderRadius: Shapes.round,
    borderWidth: 1,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: Spacing.xs,
  },
  text: {
    ...DataReadoutStyle,
  },
});
