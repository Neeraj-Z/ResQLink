import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { HomeScreen } from '../screens/HomeScreen';
import { SosScreen } from '../screens/SosScreen';
import { MapScreen } from '../screens/MapScreen';
import { FirstAidScreen } from '../screens/FirstAidScreen';
import { AnalyticsScreen } from '../screens/AnalyticsScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { ResQLinkBottomBar } from '../components/ResQLinkBottomBar';
import { Colors } from '../theme/colors';

export type ScreenType =
  | 'home'
  | 'map'
  | 'sos'
  | 'first_aid'
  | 'analytics'
  | 'settings';

export const AppNavigator: React.FC = () => {
  const [currentScreen, setCurrentScreen] = useState<ScreenType>('home');
  const [previousScreen, setPreviousScreen] = useState<ScreenType>('home');

  const navigateTo = (screen: ScreenType) => {
    setPreviousScreen(currentScreen);
    setCurrentScreen(screen);
  };

  const handleBack = () => {
    setCurrentScreen(previousScreen === 'sos' ? 'home' : previousScreen);
  };

  const renderScreen = () => {
    switch (currentScreen) {
      case 'home':
        return (
          <HomeScreen
            onNavigateToMap={() => navigateTo('map')}
            onNavigateToSos={() => navigateTo('sos')}
            onNavigateToFirstAid={() => navigateTo('first_aid')}
            onNavigateToAnalytics={() => navigateTo('analytics')}
          />
        );
      case 'map':
        return <MapScreen />;
      case 'sos':
        return (
          <SosScreen
            onBack={handleBack}
            onNavigateToAnalytics={() => navigateTo('analytics')}
          />
        );
      case 'first_aid':
        return <FirstAidScreen />;
      case 'analytics':
        return <AnalyticsScreen />;
      case 'settings':
        return <SettingsScreen />;
      default:
        return (
          <HomeScreen
            onNavigateToMap={() => navigateTo('map')}
            onNavigateToSos={() => navigateTo('sos')}
            onNavigateToFirstAid={() => navigateTo('first_aid')}
            onNavigateToAnalytics={() => navigateTo('analytics')}
          />
        );
    }
  };

  return (
    <View style={styles.rootContainer}>
      <View style={styles.screenContainer}>{renderScreen()}</View>

      {/* Show Bottom Bar on all screens except full-screen SOS modal */}
      {currentScreen !== 'sos' && (
        <ResQLinkBottomBar
          currentRoute={currentScreen}
          onNavigate={(route) => navigateTo(route as ScreenType)}
          onSosClick={() => navigateTo('sos')}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  rootContainer: {
    flex: 1,
    backgroundColor: Colors.Void,
  },
  screenContainer: {
    flex: 1,
  },
});
