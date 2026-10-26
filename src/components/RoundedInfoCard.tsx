import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ViewStyle,
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Spacing, Shapes, IconContainer } from '../theme/dimens';

interface RoundedInfoCardProps {
  title: string;
  subtitle?: string | null;
  iconName?: keyof typeof MaterialIcons.glyphMap | null;
  accentColor?: string;
  onClick?: () => void;
  trailing?: React.ReactNode;
  children?: React.ReactNode;
  style?: ViewStyle;
}

/**
 * The app's standard row card: flat surface, hairline border, matching RoundedInfoCard.kt.
 */
export const RoundedInfoCard: React.FC<RoundedInfoCardProps> = ({
  title,
  subtitle,
  iconName,
  accentColor = Colors.Signal,
  onClick,
  trailing,
  children,
  style,
}) => {
  const hexToRgba = (hex: string, alpha: number) => {
    const c = hex.replace('#', '');
    if (c.length === 6) {
      const r = parseInt(c.substring(0, 2), 16);
      const g = parseInt(c.substring(2, 4), 16);
      const b = parseInt(c.substring(4, 6), 16);
      return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }
    return accentColor;
  };

  const iconBg = hexToRgba(accentColor, 0.12);
  const iconBorder = hexToRgba(accentColor, 0.35);

  const CardWrapper = onClick ? TouchableOpacity : View;

  return (
    <CardWrapper
      activeOpacity={onClick ? 0.75 : 1}
      onPress={onClick}
      style={[styles.card, style]}
    >
      <View style={styles.contentRow}>
        {iconName && (
          <View
            style={[
              styles.iconBox,
              { backgroundColor: iconBg, borderColor: iconBorder },
            ]}
          >
            <MaterialIcons name={iconName} size={20} color={accentColor} />
          </View>
        )}

        <View style={styles.textColumn}>
          <Text style={styles.titleText}>{title}</Text>
          {subtitle ? <Text style={styles.subtitleText}>{subtitle}</Text> : null}
          {children}
        </View>

        {trailing && <View style={styles.trailingContainer}>{trailing}</View>}
      </View>
    </CardWrapper>
  );
};

const styles = StyleSheet.create({
  card: {
    width: '100%',
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    marginVertical: Spacing.xxs,
  },
  contentRow: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.md,
  },
  iconBox: {
    width: IconContainer.md,
    height: IconContainer.md,
    borderRadius: Shapes.medium,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.sm,
  },
  textColumn: {
    flex: 1,
    justifyContent: 'center',
  },
  titleText: {
    ...Typography.titleMedium,
    color: Colors.DarkOnSurface,
    fontWeight: '600',
  },
  subtitleText: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    marginTop: Spacing.xxs,
  },
  trailingContainer: {
    marginLeft: Spacing.sm,
  },
});
