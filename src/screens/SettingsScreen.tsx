import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Switch,
  TouchableOpacity,
  Modal,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialIcons } from '@expo/vector-icons';
import { GradientHeader } from '../components/GradientHeader';
import { RoundedInfoCard } from '../components/RoundedInfoCard';
import { StatusPill } from '../components/StatusPill';
import { Colors } from '../theme/colors';
import { Typography, EyebrowStyle } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';
import { useApp } from '../context/AppContext';

/**
 * Settings module matching SettingsScreen.kt: theme, language, and role/mode toggles
 * plus offline digital identity card.
 */
export const SettingsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const {
    isDarkTheme,
    setDarkTheme,
    isVolunteerMode,
    setVolunteerMode,
    language,
    setLanguage,
    user,
    isOffline,
    toggleOfflineSimulation,
  } = useApp();

  const [showQrModal, setShowQrModal] = useState<boolean>(false);
  const [showLanguageModal, setShowLanguageModal] = useState<boolean>(false);

  const languages = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'Hindi (हिंदी)' },
    { code: 'bn', label: 'Bengali (বাংলা)' },
    { code: 'te', label: 'Telugu (తెలుగు)' },
    { code: 'ta', label: 'Tamil (தமிழ்)' },
  ];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <GradientHeader
          eyebrow="Module"
          title="Settings"
          iconName="settings"
        />

        <View style={styles.contentContainer}>
          <Text style={styles.sectionEyebrow}>APP PREFERENCES</Text>

          <RoundedInfoCard
            title="Dark theme"
            subtitle="Follow system, or force light/dark"
            trailing={
              <Switch
                value={isDarkTheme}
                onValueChange={setDarkTheme}
                trackColor={{ false: Colors.HairlineDark, true: Colors.SignalDeep }}
                thumbColor={isDarkTheme ? Colors.Signal : Colors.Slate}
              />
            }
          />

          <RoundedInfoCard
            title="Language"
            subtitle={`Current: ${language.toUpperCase()} (${languages.find((l) => l.code === language)?.label || 'English'})`}
            onClick={() => setShowLanguageModal(true)}
          />

          <RoundedInfoCard
            title="Volunteer / rescue mode"
            subtitle="Unlocks responder tools"
            trailing={
              <Switch
                value={isVolunteerMode}
                onValueChange={setVolunteerMode}
                trackColor={{ false: Colors.HairlineDark, true: Colors.SignalDeep }}
                thumbColor={isVolunteerMode ? Colors.Signal : Colors.Slate}
              />
            }
          />

          <Text style={[styles.sectionEyebrow, { marginTop: Spacing.lg }]}>
            OFFLINE SIMULATION & DIAGNOSTICS
          </Text>

          <RoundedInfoCard
            title="Offline Mode"
            subtitle={isOffline ? 'Simulating zero connectivity' : 'Connected to live network'}
            trailing={
              <Switch
                value={isOffline}
                onValueChange={toggleOfflineSimulation}
                trackColor={{ false: Colors.HairlineDark, true: Colors.EmberGlow }}
                thumbColor={isOffline ? Colors.Ember : Colors.Slate}
              />
            }
          />

          <Text style={[styles.sectionEyebrow, { marginTop: Spacing.lg }]}>
            EMERGENCY IDENTITY (OFFLINE)
          </Text>

          <RoundedInfoCard
            title={user.name}
            subtitle={`Blood Group: ${user.bloodGroup || 'N/A'} · Contact: ${user.emergencyContact || 'N/A'}`}
            iconName="badge"
            accentColor={Colors.Signal}
            onClick={() => setShowQrModal(true)}
          />
        </View>

        <View style={{ height: Spacing.xl }} />
      </ScrollView>

      {/* Language Selector Modal */}
      <Modal visible={showLanguageModal} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>Select Language</Text>
            {languages.map((l) => (
              <TouchableOpacity
                key={l.code}
                activeOpacity={0.7}
                onPress={() => {
                  setLanguage(l.code);
                  setShowLanguageModal(false);
                }}
                style={styles.langOption}
              >
                <Text
                  style={[
                    styles.langText,
                    language === l.code && { color: Colors.Signal, fontWeight: 'bold' },
                  ]}
                >
                  {l.label}
                </Text>
                {language === l.code && (
                  <MaterialIcons name="check" size={20} color={Colors.Signal} />
                )}
              </TouchableOpacity>
            ))}
            <TouchableOpacity
              onPress={() => setShowLanguageModal(false)}
              style={styles.modalCloseBtn}
            >
              <Text style={styles.modalCloseText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Offline Identity QR Modal */}
      <Modal visible={showQrModal} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>Offline Medical ID</Text>
            <Text style={styles.modalSubtitle}>
              Scannable by first responders with zero network connectivity.
            </Text>

            <View style={styles.qrPlaceholder}>
              <MaterialIcons name="qr-code-2" size={140} color={Colors.Signal} />
            </View>

            <View style={styles.idDetails}>
              <Text style={styles.idDetailText}>Name: {user.name}</Text>
              <Text style={styles.idDetailText}>Blood Type: {user.bloodGroup}</Text>
              <Text style={styles.idDetailText}>Emergency Contact: {user.emergencyContact}</Text>
              <Text style={styles.idDetailText}>Notes: {user.medicalNotes}</Text>
            </View>

            <TouchableOpacity
              onPress={() => setShowQrModal(false)}
              style={styles.modalCloseBtn}
            >
              <Text style={styles.modalCloseText}>Done</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
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
  contentContainer: {
    paddingHorizontal: Spacing.screenPadding,
    paddingVertical: Spacing.lg,
  },
  sectionEyebrow: {
    ...EyebrowStyle,
    color: Colors.Slate,
    marginBottom: Spacing.xs,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(6, 9, 15, 0.85)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.lg,
  },
  modalBox: {
    width: '100%',
    maxWidth: 380,
    backgroundColor: Colors.InkRaised,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.lg,
  },
  modalTitle: {
    ...Typography.titleLarge,
    color: '#FFFFFF',
    marginBottom: Spacing.xxs,
  },
  modalSubtitle: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    marginBottom: Spacing.md,
  },
  langOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.HairlineDark,
  },
  langText: {
    ...Typography.bodyMedium,
    color: Colors.DarkOnSurface,
  },
  qrPlaceholder: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.lg,
    marginVertical: Spacing.md,
  },
  idDetails: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.medium,
    padding: Spacing.md,
    marginBottom: Spacing.md,
  },
  idDetailText: {
    ...Typography.bodyMedium,
    color: Colors.DarkOnSurface,
    marginVertical: 2,
  },
  modalCloseBtn: {
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.SignalDeep,
    borderRadius: Shapes.medium,
    marginTop: Spacing.xs,
  },
  modalCloseText: {
    ...Typography.labelLarge,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
});
