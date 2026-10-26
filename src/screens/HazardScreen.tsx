import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialIcons } from '@expo/vector-icons';
import { GradientHeader } from '../components/GradientHeader';
import { RefreshRow } from '../components/RefreshRow';
import { RoundedInfoCard } from '../components/RoundedInfoCard';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';
import { useApp } from '../context/AppContext';
import { HazardSeverity, HazardZone } from '../data/models/types';

/**
 * Hazard Information module matching HazardScreen.kt: cached disaster/hazard zones,
 * color-coded by severity. Refreshes via RefreshRow.
 */
export const HazardScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const { hazards, isHazardsRefreshing, refreshHazards } = useApp();
  const [selectedHazard, setSelectedHazard] = useState<HazardZone | null>(null);

  const getSeverityColor = (severity: HazardSeverity): string => {
    switch (severity) {
      case 'EXTREME':
      case 'HIGH':
        return Colors.Ember;
      case 'MODERATE':
        return Colors.Amber;
      case 'LOW':
        return Colors.Moss;
      default:
        return Colors.Signal;
    }
  };

  const formatText = (text: string) => {
    return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <GradientHeader
          eyebrow="Module"
          title="Hazard Information"
          subtitle="Cached for offline access"
          iconName="warning"
        />

        <RefreshRow
          isRefreshing={isHazardsRefreshing}
          onRefresh={refreshHazards}
          label="Synced from Firestore"
        />

        {hazards.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>No active hazards in your cached area.</Text>
          </View>
        ) : (
          <View style={styles.listContainer}>
            {hazards.map((hazard) => {
              const color = getSeverityColor(hazard.severity);
              const isExpanded = selectedHazard?.id === hazard.id;

              return (
                <View key={hazard.id} style={styles.cardWrapper}>
                  <RoundedInfoCard
                    title={hazard.title}
                    subtitle={`${formatText(hazard.type)} · ${formatText(hazard.severity)} · ${hazard.description}`}
                    iconName="warning"
                    accentColor={color}
                    onClick={() =>
                      setSelectedHazard(isExpanded ? null : hazard)
                    }
                  >
                    {isExpanded && (
                      <View style={styles.expandedDetails}>
                        <View style={styles.detailRow}>
                          <Text style={styles.detailLabel}>Radius:</Text>
                          <Text style={styles.detailValue}>
                            {(hazard.radiusMeters / 1000).toFixed(1)} km
                          </Text>
                        </View>
                        <View style={styles.detailRow}>
                          <Text style={styles.detailLabel}>Source:</Text>
                          <Text style={styles.detailValue}>{hazard.source}</Text>
                        </View>
                        <View style={styles.detailRow}>
                          <Text style={styles.detailLabel}>Coordinates:</Text>
                          <Text style={styles.detailValue}>
                            {hazard.latitude.toFixed(4)}, {hazard.longitude.toFixed(4)}
                          </Text>
                        </View>
                      </View>
                    )}
                  </RoundedInfoCard>
                </View>
              );
            })}
          </View>
        )}

        <View style={{ height: Spacing.xl }} />
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.Void,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: Spacing.xxl,
  },
  listContainer: {
    paddingHorizontal: Spacing.screenPadding,
  },
  cardWrapper: {
    marginVertical: Spacing.xxs,
  },
  emptyContainer: {
    padding: Spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    textAlign: 'center',
  },
  expandedDetails: {
    marginTop: Spacing.sm,
    paddingTop: Spacing.xs,
    borderTopWidth: 1,
    borderTopColor: Colors.HairlineDark,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: 2,
  },
  detailLabel: {
    ...Typography.labelMedium,
    color: Colors.Slate,
  },
  detailValue: {
    ...Typography.labelMedium,
    color: Colors.DarkOnSurface,
    fontWeight: '600',
  },
});
