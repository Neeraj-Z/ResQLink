import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  ViewStyle,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { MaterialIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { Typography, EyebrowStyle } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';

interface GradientHeaderProps {
  title: string;
  subtitle?: string | null;
  eyebrow?: string | null;
  iconName?: keyof typeof MaterialIcons.glyphMap | null;
  onIconClick?: () => void;
  isIconLoading?: boolean;
  style?: ViewStyle;
}

/**
 * Section header used atop each top-level screen: a dark glass panel with a
 * single cyan hairline seam along the bottom edge, matching GradientHeader.kt.
 */
export const GradientHeader: React.FC<GradientHeaderProps> = ({
  title,
  subtitle,
  eyebrow,
  iconName,
  onIconClick,
  isIconLoading = false,
  style,
}) => {
  return (
    <View style={[styles.wrapper, style]}>
      <LinearGradient
        colors={[Colors.InkRaised, Colors.Ink]}
        start={{ x: 0, y: 0 }}
        end={{ x: 0, y: 1 }}
        style={styles.headerContainer}
      >
        <View style={styles.contentRow}>
          <View style={styles.textContainer}>
            {eyebrow && (
              <Text style={styles.eyebrowText}>{eyebrow.toUpperCase()}</Text>
            )}
            <Text style={styles.titleText}>{title}</Text>
            {subtitle && <Text style={styles.subtitleText}>{subtitle}</Text>}
          </View>

          {iconName && (
            <TouchableOpacity
              activeOpacity={0.8}
              disabled={!onIconClick || isIconLoading}
              onPress={onIconClick}
              style={styles.iconBox}
            >
              {isIconLoading ? (
                <ActivityIndicator size="small" color={Colors.Signal} />
              ) : (
                <MaterialIcons name={iconName} size={22} color={Colors.Signal} />
              )}
            </TouchableOpacity>
          )}
        </View>
      </LinearGradient>

      {/* Signature 2px glowing cyan seam */}
      <LinearGradient
        colors={['rgba(53, 224, 200, 0)', 'rgba(53, 224, 200, 0.7)', 'rgba(53, 224, 200, 0)']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
        style={styles.glowSeam}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    width: '100%',
  },
  headerContainer: {
    width: '100%',
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.lg,
  },
  contentRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  textContainer: {
    flex: 1,
    paddingRight: Spacing.md,
  },
  eyebrowText: {
    ...EyebrowStyle,
    color: Colors.Signal,
    marginBottom: Spacing.xxs,
  },
  titleText: {
    ...Typography.headlineMedium,
    color: '#FFFFFF',
  },
  subtitleText: {
    ...Typography.bodyMedium,
    color: 'rgba(255, 255, 255, 0.6)',
    marginTop: Spacing.xxs,
  },
  iconBox: {
    width: 44,
    height: 44,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.SignalDeep,
    alignItems: 'center',
    justifyContent: 'center',
  },
  glowSeam: {
    width: '100%',
    height: 2,
  },
});
