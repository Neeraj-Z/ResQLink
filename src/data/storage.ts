import AsyncStorage from '@react-native-async-storage/async-storage';
import { HazardZone, FirstAidGuide, SosAlert, User, MeshRelayedPacket } from './models/types';
import { SeedHazards, SeedFirstAidGuides, SeedUser, SeedRelayedPackets } from './seedData';

const KEYS = {
  HAZARDS: '@resqlink_hazards_clean_v2',
  FIRST_AID: '@resqlink_first_aid_v1',
  SOS_ALERTS: '@resqlink_sos_alerts_clean_v2',
  RELAYED_PACKETS: '@resqlink_relayed_packets_clean_v2',
  USER: '@resqlink_user_clean_v2',
  SETTINGS_DARK_THEME: '@resqlink_dark_theme',
  SETTINGS_VOLUNTEER: '@resqlink_volunteer_mode',
  SETTINGS_LANGUAGE: '@resqlink_language',
};

export const Storage = {
  async getHazards(): Promise<HazardZone[]> {
    try {
      const data = await AsyncStorage.getItem(KEYS.HAZARDS);
      if (data) return JSON.parse(data);
      await AsyncStorage.setItem(KEYS.HAZARDS, JSON.stringify(SeedHazards));
      return SeedHazards;
    } catch {
      return SeedHazards;
    }
  },

  async saveHazards(hazards: HazardZone[]): Promise<void> {
    try {
      await AsyncStorage.setItem(KEYS.HAZARDS, JSON.stringify(hazards));
    } catch (e) {
      console.error('Failed to save hazards', e);
    }
  },

  async getFirstAidGuides(): Promise<FirstAidGuide[]> {
    try {
      const data = await AsyncStorage.getItem(KEYS.FIRST_AID);
      if (data) return JSON.parse(data);
      await AsyncStorage.setItem(KEYS.FIRST_AID, JSON.stringify(SeedFirstAidGuides));
      return SeedFirstAidGuides;
    } catch {
      return SeedFirstAidGuides;
    }
  },

  async saveFirstAidGuides(guides: FirstAidGuide[]): Promise<void> {
    try {
      await AsyncStorage.setItem(KEYS.FIRST_AID, JSON.stringify(guides));
    } catch (e) {
      console.error('Failed to save first aid guides', e);
    }
  },

  async getSosAlerts(): Promise<SosAlert[]> {
    try {
      const data = await AsyncStorage.getItem(KEYS.SOS_ALERTS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  async saveSosAlert(alert: SosAlert): Promise<void> {
    try {
      const existing = await this.getSosAlerts();
      const updated = [alert, ...existing.filter((a) => a.id !== alert.id)];
      await AsyncStorage.setItem(KEYS.SOS_ALERTS, JSON.stringify(updated));
    } catch (e) {
      console.error('Failed to save SOS alert', e);
    }
  },

  async getRelayedPackets(): Promise<MeshRelayedPacket[]> {
    try {
      const data = await AsyncStorage.getItem(KEYS.RELAYED_PACKETS);
      if (data) return JSON.parse(data);
      await AsyncStorage.setItem(KEYS.RELAYED_PACKETS, JSON.stringify(SeedRelayedPackets));
      return SeedRelayedPackets;
    } catch {
      return SeedRelayedPackets;
    }
  },

  async saveRelayedPacket(packet: MeshRelayedPacket): Promise<void> {
    try {
      const existing = await this.getRelayedPackets();
      const updated = [packet, ...existing.filter((p) => p.id !== packet.id)];
      await AsyncStorage.setItem(KEYS.RELAYED_PACKETS, JSON.stringify(updated));
    } catch (e) {
      console.error('Failed to save relayed packet', e);
    }
  },

  async saveRelayedPackets(packets: MeshRelayedPacket[]): Promise<void> {
    try {
      await AsyncStorage.setItem(KEYS.RELAYED_PACKETS, JSON.stringify(packets));
    } catch (e) {
      console.error('Failed to save relayed packets', e);
    }
  },

  async getUser(): Promise<User> {
    try {
      const data = await AsyncStorage.getItem(KEYS.USER);
      if (data) return JSON.parse(data);
      await AsyncStorage.setItem(KEYS.USER, JSON.stringify(SeedUser));
      return SeedUser;
    } catch {
      return SeedUser;
    }
  },

  async getSettings(): Promise<{ isDarkTheme: boolean; isVolunteer: boolean; language: string }> {
    try {
      const dark = await AsyncStorage.getItem(KEYS.SETTINGS_DARK_THEME);
      const volunteer = await AsyncStorage.getItem(KEYS.SETTINGS_VOLUNTEER);
      const lang = await AsyncStorage.getItem(KEYS.SETTINGS_LANGUAGE);
      return {
        isDarkTheme: dark !== null ? JSON.parse(dark) : true, // Default dark-first
        isVolunteer: volunteer !== null ? JSON.parse(volunteer) : false,
        language: lang || 'en',
      };
    } catch {
      return { isDarkTheme: true, isVolunteer: false, language: 'en' };
    }
  },

  async saveSettings(settings: { isDarkTheme?: boolean; isVolunteer?: boolean; language?: string }): Promise<void> {
    try {
      if (settings.isDarkTheme !== undefined) {
        await AsyncStorage.setItem(KEYS.SETTINGS_DARK_THEME, JSON.stringify(settings.isDarkTheme));
      }
      if (settings.isVolunteer !== undefined) {
        await AsyncStorage.setItem(KEYS.SETTINGS_VOLUNTEER, JSON.stringify(settings.isVolunteer));
      }
      if (settings.language !== undefined) {
        await AsyncStorage.setItem(KEYS.SETTINGS_LANGUAGE, settings.language);
      }
    } catch (e) {
      console.error('Failed to save settings', e);
    }
  },
};
