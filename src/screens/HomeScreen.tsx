import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { GradientHeader } from '../components/GradientHeader';
import { RoundedInfoCard } from '../components/RoundedInfoCard';
import { Colors } from '../theme/colors';
import { EyebrowStyle } from '../theme/typography';
import { Spacing } from '../theme/dimens';
import { useApp } from '../context/AppContext';

interface HomeScreenProps {
  onNavigateToMap: () => void;
  onNavigateToSos: () => void;
  onNavigateToFirstAid: () => void;
  onNavigateToAnalytics?: () => void;
}

/**
 * The Home Dashboard matching HomeScreen.kt: entry point of the app.
 * Clean, fast access to emergency actions without hazards.
 */
export const HomeScreen: React.FC<HomeScreenProps> = ({
  onNavigateToMap,
  onNavigateToSos,
  onNavigateToFirstAid,
  onNavigateToAnalytics,
}) => {
  const insets = useSafeAreaInsets();
  const { isOffline } = useApp();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <GradientHeader
          title="ResQLink"
          subtitle={
            isOffline
              ? 'Offline mode -- mesh relay active'
              : 'Stay safe. Stay informed.'
          }
          iconName="home"
        />

        {/* QUICK ACTIONS */}
        <View style={styles.sectionContainer}>
          <Text style={styles.sectionEyebrow}>QUICK ACTIONS</Text>

          <RoundedInfoCard
            title="Emergency SOS"
            subtitle="Alert responders & mesh peers instantly"
            iconName="emergency"
            accentColor={Colors.Ember}
            onClick={onNavigateToSos}
          />

          {onNavigateToAnalytics && (
            <RoundedInfoCard
              title="Mesh Packet Analytics"
              subtitle="Inspect relayed packets & connected nodes"
              iconName="analytics"
              accentColor={Colors.Signal}
              onClick={onNavigateToAnalytics}
            />
          )}

          <RoundedInfoCard
            title="Interactive Map"
            subtitle="Shelters, safe zones, and live GPS location"
            iconName="map"
            accentColor={Colors.Signal}
            onClick={onNavigateToMap}
          />

          <RoundedInfoCard
            title="First Aid Guide"
            subtitle="Step-by-step offline emergency procedures"
            iconName="local-hospital"
            accentColor={Colors.Moss}
            onClick={onNavigateToFirstAid}
          />
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
  sectionContainer: {
    paddingHorizontal: Spacing.screenPadding,
    paddingTop: Spacing.lg,
  },
  sectionEyebrow: {
    ...EyebrowStyle,
    color: Colors.Slate,
    marginBottom: Spacing.xs,
  },
});
