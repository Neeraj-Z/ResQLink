import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  ViewStyle,
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Spacing } from '../theme/dimens';

interface RefreshRowProps {
  isRefreshing: boolean;
  onRefresh: () => void;
  label?: string;
  style?: ViewStyle;
}

/**
 * A quiet "Synced from Firestore" row with a manual refresh action, matching RefreshRow.kt.
 */
export const RefreshRow: React.FC<RefreshRowProps> = ({
  isRefreshing,
  onRefresh,
  label = 'Synced from Firestore',
  style,
}) => {
  return (
    <View style={[styles.container, style]}>
      <Text style={styles.labelText}>{label}</Text>
      {isRefreshing ? (
        <ActivityIndicator size="small" color={Colors.Signal} style={styles.indicator} />
      ) : (
        <TouchableOpacity
          activeOpacity={0.7}
          onPress={onRefresh}
          style={styles.refreshButton}
        >
          <MaterialIcons name="refresh" size={20} color={Colors.Slate} />
        </TouchableOpacity>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.screenPadding,
    paddingVertical: Spacing.xs,
  },
  labelText: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    flex: 1,
  },
  indicator: {
    width: 32,
    height: 32,
  },
  refreshButton: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
