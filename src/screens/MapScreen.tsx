import React from 'react';
import { View, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { InteractiveMap } from '../components/InteractiveMap';
import { Colors } from '../theme/colors';
import { useApp } from '../context/AppContext';

/**
 * Interactive Map screen matching MapScreen.kt.
 * Renders hazard zones with radius circles and severity pins, active SOS alerts,
 * and user live location marker.
 */
export const MapScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const {
    hazards,
    activeSosAlerts,
    userLatitude,
    userLongitude,
    refreshHazards,
    isHazardsRefreshing,
    refreshLocation,
  } = useApp();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <InteractiveMap
        hazards={hazards}
        activeSosAlerts={activeSosAlerts}
        userLat={userLatitude}
        userLng={userLongitude}
        onLocateUser={refreshLocation}
        onRefresh={refreshHazards}
        isRefreshing={isHazardsRefreshing}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.Void,
  },
});
