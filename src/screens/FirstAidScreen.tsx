import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
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
import { FirstAidGuide } from '../data/models/types';

/**
 * First Aid module matching FirstAidScreen.kt: content bundled/cached fully offline
 * (CPR, bleeding control, burns, fractures, choking, shock), refreshed opportunistically.
 */
export const FirstAidScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const {
    filteredFirstAidGuides,
    isFirstAidRefreshing,
    refreshFirstAid,
    searchQuery,
    setSearchQuery,
  } = useApp();

  const [expandedGuideId, setExpandedGuideId] = useState<string | null>(null);

  const toggleExpand = (id: string) => {
    setExpandedGuideId((prev) => (prev === id ? null : id));
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
          title="First Aid Guide"
          subtitle="Available fully offline"
          iconName="local-hospital"
        />

        <RefreshRow
          isRefreshing={isFirstAidRefreshing}
          onRefresh={refreshFirstAid}
          label="Synced from Firestore"
        />

        {/* Search Input Bar */}
        <View style={styles.searchWrapper}>
          <View style={styles.searchBar}>
            <MaterialIcons name="search" size={20} color={Colors.Slate} style={styles.searchIcon} />
            <TextInput
              style={styles.searchInput}
              value={searchQuery}
              onChangeText={setSearchQuery}
              placeholder="Search procedures (CPR, burns, bleeding...)"
              placeholderTextColor={Colors.Slate}
              returnKeyType="search"
            />
            {searchQuery.length > 0 && (
              <MaterialIcons
                name="close"
                size={18}
                color={Colors.Slate}
                onPress={() => setSearchQuery('')}
              />
            )}
          </View>
        </View>

        {/* Guides List */}
        <View style={styles.listContainer}>
          {filteredFirstAidGuides.map((guide: FirstAidGuide) => {
            const isExpanded = expandedGuideId === guide.id;

            return (
              <View key={guide.id} style={styles.cardWrapper}>
                <RoundedInfoCard
                  title={guide.title}
                  subtitle={guide.summary}
                  iconName="local-hospital"
                  accentColor={Colors.Moss}
                  onClick={() => toggleExpand(guide.id)}
                >
                  {isExpanded && (
                    <View style={styles.stepsContainer}>
                      {/* Critical Warning Callout if present */}
                      {guide.criticalWarning && (
                        <View style={styles.warningBox}>
                          <MaterialIcons
                            name="warning"
                            size={18}
                            color={Colors.Ember}
                            style={styles.warningIcon}
                          />
                          <Text style={styles.warningText}>{guide.criticalWarning}</Text>
                        </View>
                      )}

                      {/* Numbered Steps */}
                      <Text style={styles.stepsHeading}>PROCEDURE STEPS:</Text>
                      {guide.steps.map((step, idx) => (
                        <View key={idx} style={styles.stepRow}>
                          <View style={styles.stepNumberBadge}>
                            <Text style={styles.stepNumberText}>{idx + 1}</Text>
                          </View>
                          <Text style={styles.stepText}>{step}</Text>
                        </View>
                      ))}
                    </View>
                  )}
                </RoundedInfoCard>
              </View>
            );
          })}
        </View>

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
  searchWrapper: {
    paddingHorizontal: Spacing.screenPadding,
    paddingVertical: Spacing.md,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.DarkSurface,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
  },
  searchIcon: {
    marginRight: Spacing.xs,
  },
  searchInput: {
    flex: 1,
    color: Colors.DarkOnSurface,
    ...Typography.bodyMedium,
  },
  listContainer: {
    paddingHorizontal: Spacing.screenPadding,
  },
  cardWrapper: {
    marginVertical: Spacing.xxs,
  },
  stepsContainer: {
    marginTop: Spacing.md,
    paddingTop: Spacing.sm,
    borderTopWidth: 1,
    borderTopColor: Colors.HairlineDark,
  },
  warningBox: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: 'rgba(255, 79, 62, 0.12)',
    borderWidth: 1,
    borderColor: Colors.Ember,
    borderRadius: Shapes.small,
    padding: Spacing.sm,
    marginBottom: Spacing.md,
  },
  warningIcon: {
    marginRight: Spacing.xs,
    marginTop: 2,
  },
  warningText: {
    ...Typography.bodyMedium,
    color: Colors.Ember,
    flex: 1,
    fontWeight: '600',
    fontSize: 13,
  },
  stepsHeading: {
    ...Typography.labelSmall,
    color: Colors.Slate,
    letterSpacing: 1.2,
    marginBottom: Spacing.xs,
  },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginVertical: Spacing.xs,
  },
  stepNumberBadge: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: Colors.SignalDeep,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.sm,
    marginTop: 1,
  },
  stepNumberText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: Colors.Signal,
  },
  stepText: {
    ...Typography.bodyMedium,
    color: Colors.DarkOnSurface,
    flex: 1,
    lineHeight: 20,
  },
});
