import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Spacing } from '../theme/dimens';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export interface TabItem {
  key: string;
  label: string;
  iconName: keyof typeof MaterialIcons.glyphMap;
}

export const TABS: TabItem[] = [
  { key: 'home', label: 'Home', iconName: 'home' },
  { key: 'map', label: 'Map', iconName: 'map' },
  { key: 'first_aid', label: 'First Aid', iconName: 'local-hospital' },
  { key: 'analytics', label: 'Analytics', iconName: 'analytics' },
];

interface ResQLinkBottomBarProps {
  currentRoute: string;
  onNavigate: (route: string) => void;
  onSosClick: () => void;
}

/**
 * Bottom navigation for compact-width (phone) layouts matching ResQLinkBottomBar.kt.
 * Dark, hairline-topped bar with cyan indicator on the selected tab and a raised
 * circular SOS action with Ember outer glow ring.
 */
export const ResQLinkBottomBar: React.FC<ResQLinkBottomBarProps> = ({
  currentRoute,
  onNavigate,
  onSosClick,
}) => {
  const insets = useSafeAreaInsets();
  const leftTabs = TABS.slice(0, 2); // Home, Map
  const rightTabs = TABS.slice(2);   // First Aid, Hazards, Analytics

  const renderTab = (tab: TabItem) => {
    const isSelected = currentRoute === tab.key;
    return (
      <TouchableOpacity
        key={tab.key}
        activeOpacity={0.7}
        onPress={() => onNavigate(tab.key)}
        style={styles.tabItem}
      >
        <View
          style={[
            styles.iconWrapper,
            isSelected && styles.selectedIconWrapper,
          ]}
        >
          <MaterialIcons
            name={tab.iconName}
            size={22}
            color={isSelected ? Colors.Signal : Colors.Slate}
          />
        </View>
        <Text
          style={[
            styles.tabLabel,
            { color: isSelected ? Colors.Signal : Colors.Slate },
          ]}
          numberOfLines={1}
        >
          {tab.label}
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <View style={[styles.bottomBarContainer, { paddingBottom: Math.max(insets.bottom, 8) }]}>
      {/* Top Hairline border */}
      <View style={styles.topBorder} />

      <View style={styles.tabBarRow}>
        {/* Left tabs: Home, Map */}
        <View style={styles.tabsGroup}>{leftTabs.map(renderTab)}</View>

        {/* Center Raised SOS Button */}
        <View style={styles.sosButtonAnchor}>
          <TouchableOpacity
            activeOpacity={0.85}
            onPress={onSosClick}
            style={styles.sosGlowRing}
          >
            <View style={styles.sosInnerButton}>
              <MaterialIcons name="emergency" size={24} color="#FFFFFF" />
              <Text style={styles.sosText}>SOS</Text>
            </View>
          </TouchableOpacity>
        </View>

        {/* Right tabs: First Aid, Hazards, Analytics */}
        <View style={styles.tabsGroup}>{rightTabs.map(renderTab)}</View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  bottomBarContainer: {
    backgroundColor: Colors.DarkSurface,
    width: '100%',
  },
  topBorder: {
    width: '100%',
    height: 1,
    backgroundColor: Colors.HairlineDark,
  },
  tabBarRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 64,
    paddingHorizontal: Spacing.xs,
  },
  tabsGroup: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  tabItem: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
    paddingHorizontal: 4,
    minWidth: 48,
  },
  iconWrapper: {
    paddingHorizontal: 12,
    paddingVertical: 3,
    borderRadius: 16,
    marginBottom: 2,
  },
  selectedIconWrapper: {
    backgroundColor: 'rgba(53, 224, 200, 0.14)',
  },
  tabLabel: {
    ...Typography.labelMedium,
    fontSize: 11,
  },
  sosButtonAnchor: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 68,
    marginTop: -16,
  },
  sosGlowRing: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: 'rgba(255, 79, 62, 0.18)', // Ember glow ring
    alignItems: 'center',
    justifyContent: 'center',
    ...Platform.select({
      ios: {
        shadowColor: Colors.Ember,
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.35,
        shadowRadius: 8,
      },
      android: {
        elevation: 6,
      },
    }),
  },
  sosInnerButton: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: Colors.Ember,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sosText: {
    fontSize: 10,
    fontWeight: '800',
    color: '#FFFFFF',
    letterSpacing: 0.5,
    marginTop: -2,
  },
});
