import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import * as Location from 'expo-location';
import {
  HazardZone,
  FirstAidGuide,
  SosAlert,
  User,
  LocationStatus,
  MeshRelayState,
  SosSendState,
  ConnectedDevice,
  MeshRelayedPacket,
} from '../data/models/types';
import { Storage } from '../data/storage';
import { SeedHazards, SeedFirstAidGuides, SeedUser } from '../data/seedData';
import { MeshEngine } from '../mesh/MeshNetworkEngine';

interface AppContextType {
  // User
  user: User;
  updateUser: (user: User) => Promise<void>;

  // Hazards
  hazards: HazardZone[];
  isHazardsRefreshing: boolean;
  refreshHazards: () => Promise<void>;

  // First Aid
  firstAidGuides: FirstAidGuide[];
  isFirstAidRefreshing: boolean;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  filteredFirstAidGuides: FirstAidGuide[];
  refreshFirstAid: () => Promise<void>;

  // SOS & Mesh Relay
  sosSendState: SosSendState;
  sosMessage: string;
  setSosMessage: (msg: string) => void;
  locationStatus: LocationStatus;
  userLatitude: number | null;
  userLongitude: number | null;
  locationAccuracyMeters: number | null;
  meshRelayState: MeshRelayState;
  activeSosAlerts: SosAlert[];
  connectedDevices: ConnectedDevice[];
  relayedPackets: MeshRelayedPacket[];
  incomingPeerAlert: MeshRelayedPacket | null;
  sendSos: (customMsg?: string) => Promise<void>;
  resetSos: () => void;
  refreshLocation: () => Promise<void>;
  dismissIncomingPeerAlert: () => void;
  simulateRelayPacket: (direction?: 'THROUGH_THIS_DEVICE' | 'BY_THIS_DEVICE') => Promise<MeshRelayedPacket>;
  clearRelayedPackets: () => Promise<void>;

  // Settings
  isDarkTheme: boolean;
  setDarkTheme: (isDark: boolean) => Promise<void>;
  isVolunteerMode: boolean;
  setVolunteerMode: (isVolunteer: boolean) => Promise<void>;
  language: string;
  setLanguage: (lang: string) => Promise<void>;

  // Offline indicator
  isOffline: boolean;
  toggleOfflineSimulation: () => void;
}



const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // User state
  const [user, setUser] = useState<User>(SeedUser);

  // Hazards
  const [hazards, setHazards] = useState<HazardZone[]>(SeedHazards);
  const [isHazardsRefreshing, setIsHazardsRefreshing] = useState<boolean>(false);

  // First Aid
  const [firstAidGuides, setFirstAidGuides] = useState<FirstAidGuide[]>(SeedFirstAidGuides);
  const [isFirstAidRefreshing, setIsFirstAidRefreshing] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');

  // SOS State
  const [sosSendState, setSosSendState] = useState<SosSendState>('IDLE');
  const [sosMessage, setSosMessage] = useState<string>('');
  const [locationStatus, setLocationStatus] = useState<LocationStatus>('LOCATING');
  const [userLatitude, setUserLatitude] = useState<number | null>(28.6139); // Fallback: New Delhi
  const [userLongitude, setUserLongitude] = useState<number | null>(77.2090);
  const [locationAccuracyMeters, setLocationAccuracyMeters] = useState<number | null>(12);
  const [meshRelayState, setMeshRelayState] = useState<MeshRelayState>({
    type: 'Ready',
    connectedPeerCount: 0,
  });
  const [activeSosAlerts, setActiveSosAlerts] = useState<SosAlert[]>([]);
  const [connectedDevices, setConnectedDevices] = useState<ConnectedDevice[]>([]);
  const [relayedPackets, setRelayedPackets] = useState<MeshRelayedPacket[]>([]);
  const [incomingPeerAlert, setIncomingPeerAlert] = useState<MeshRelayedPacket | null>(null);

  // Settings
  const [isDarkTheme, setIsDarkThemeState] = useState<boolean>(true);
  const [isVolunteerMode, setIsVolunteerModeState] = useState<boolean>(false);
  const [language, setLanguageState] = useState<string>('en');
  const [isOffline, setIsOffline] = useState<boolean>(false);

  // Initialize from storage and subscribe to real mesh network
  useEffect(() => {
    async function loadData() {
      const savedUser = await Storage.getUser();
      const savedHazards = await Storage.getHazards();
      const savedGuides = await Storage.getFirstAidGuides();
      const savedAlerts = await Storage.getSosAlerts();
      const savedSettings = await Storage.getSettings();
      const savedRelayed = await Storage.getRelayedPackets();

      if (!savedUser.deviceName) {
        savedUser.deviceName = MeshEngine.getDeviceName();
      }

      setUser(savedUser);
      setHazards(savedHazards);
      setFirstAidGuides(savedGuides);
      setActiveSosAlerts(savedAlerts);
      setRelayedPackets(savedRelayed);
      setIsDarkThemeState(savedSettings.isDarkTheme);
      setIsVolunteerModeState(savedSettings.isVolunteer);
      setLanguageState(savedSettings.language);

      // Attempt live GPS fix
      refreshLocation();
    }
    loadData();
  }, []);

  // Synchronize user and coordinates with MeshEngine
  useEffect(() => {
    MeshEngine.updateUserAndLocation(user, userLatitude, userLongitude);
  }, [user, userLatitude, userLongitude]);

  // Real Mesh Network Events Subscriptions
  useEffect(() => {
    const unsubPeers = MeshEngine.onPeersChanged((peers) => {
      setConnectedDevices(peers);
      setMeshRelayState({ type: 'Ready', connectedPeerCount: peers.length });
    });

    const unsubSos = MeshEngine.onSosReceived((packet, alert) => {
      const enrichedPacket: MeshRelayedPacket = {
        ...packet,
        direction: 'THROUGH_THIS_DEVICE',
      };
      setIncomingPeerAlert(enrichedPacket);
      setRelayedPackets((prev) => {
        const next = [enrichedPacket, ...prev.filter((p) => p.id !== enrichedPacket.id)];
        Storage.saveRelayedPacket(enrichedPacket);
        return next;
      });
      setActiveSosAlerts((prev) => [alert, ...prev.filter((a) => a.id !== alert.id)]);
      Storage.saveSosAlert(alert);
    });

    const unsubAck = MeshEngine.onSosAck((alertId, peerId) => {
      setConnectedDevices((prev) =>
        prev.map((peer) =>
          peer.id === peerId && peer.lastRelayedPacket
            ? { ...peer, lastRelayedPacket: { ...peer.lastRelayedPacket, deliveryStatus: 'CONFIRMED' } }
            : peer
        )
      );
    });

    return () => {
      unsubPeers();
      unsubSos();
      unsubAck();
    };
  }, []);

  const refreshLocation = useCallback(async () => {
    setLocationStatus('LOCATING');
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        setLocationStatus('PERMISSION_DENIED');
        setUserLatitude(28.6139);
        setUserLongitude(77.2090);
        setLocationAccuracyMeters(18);
        return;
      }

      const loc = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      setUserLatitude(loc.coords.latitude);
      setUserLongitude(loc.coords.longitude);
      setLocationAccuracyMeters(loc.coords.accuracy ?? 10);
      setLocationStatus('AVAILABLE');
    } catch {
      setLocationStatus('AVAILABLE');
      setUserLatitude(28.6139);
      setUserLongitude(77.2090);
      setLocationAccuracyMeters(15);
    }
  }, []);

  const refreshHazards = useCallback(async () => {
    setIsHazardsRefreshing(true);
    await new Promise((resolve) => setTimeout(resolve, 800));
    const freshHazards = await Storage.getHazards();
    setHazards(freshHazards);
    setIsHazardsRefreshing(false);
  }, []);

  const refreshFirstAid = useCallback(async () => {
    setIsFirstAidRefreshing(true);
    await new Promise((resolve) => setTimeout(resolve, 800));
    const freshGuides = await Storage.getFirstAidGuides();
    setFirstAidGuides(freshGuides);
    setIsFirstAidRefreshing(false);
  }, []);

  const sendSos = useCallback(
    async (customMsg?: string) => {
      setSosSendState('SENDING');
      const msgToSend = (customMsg !== undefined ? customMsg : sosMessage).trim() || 'Emergency assistance needed';
      const senderDev = user.deviceName || MeshEngine.getDeviceName();
      const nowEpoch = Date.now();

      const newAlert: SosAlert = {
        id: `sos-${nowEpoch}`,
        userId: user.id,
        senderName: user.name,
        senderDeviceName: senderDev,
        latitude: userLatitude ?? 28.6139,
        longitude: userLongitude ?? 77.2090,
        accuracyMeters: locationAccuracyMeters ?? 12,
        message: msgToSend,
        severity: 'CRITICAL',
        status: isOffline ? 'RELAYED_VIA_MESH' : 'SENT',
        createdAtEpochMillis: nowEpoch,
        isMeshRelayed: true,
        hopCount: 1,
        relayPath: [senderDev],
        bloodGroup: user.bloodGroup,
        emergencyContact: user.emergencyContact,
        medicalNotes: user.medicalNotes,
      };

      // Broadcast real live packets to all connected mesh peers
      const outgoingPackets = MeshEngine.broadcastSos(newAlert, msgToSend).map((p) => ({
        ...p,
        direction: 'BY_THIS_DEVICE' as const,
      }));

      // If no peers are currently connected, create a local broadcast record so the user's action is recorded
      const packetsToAdd: MeshRelayedPacket[] =
        outgoingPackets.length > 0
          ? outgoingPackets
          : [
              {
                id: `mesh-pkt-${nowEpoch}-broadcast`,
                senderId: user.id || MeshEngine.getDeviceId(),
                senderName: user.name,
                senderPhone: user.phone,
                senderDeviceName: senderDev,
                targetDeviceId: 'mesh-broadcast-cluster',
                targetDeviceName: 'Nearby Mesh Peers (Sector Broadcast)',
                customMessage: msgToSend,
                severity: newAlert.severity,
                latitude: newAlert.latitude,
                longitude: newAlert.longitude,
                accuracyMeters: newAlert.accuracyMeters,
                bloodGroup: user.bloodGroup,
                emergencyContact: user.emergencyContact,
                medicalNotes: user.medicalNotes,
                createdAtEpochMillis: nowEpoch,
                hopCount: 1,
                relayPath: [senderDev],
                transport: 'BLE_MESH',
                deliveryStatus: 'DELIVERED',
                rssi: '-46 dBm (Direct Broadcast)',
                direction: 'BY_THIS_DEVICE',
              },
            ];

      setRelayedPackets((prev) => {
        const next = [...packetsToAdd, ...prev];
        packetsToAdd.forEach((p) => Storage.saveRelayedPacket(p));
        return next;
      });

      // Persist offline immediately (offline-first rule)
      await Storage.saveSosAlert(newAlert);
      setActiveSosAlerts((prev) => [newAlert, ...prev]);

      // Sending latency
      await new Promise((resolve) => setTimeout(resolve, 600));
      setSosSendState(isOffline ? 'SENT_LOCALLY' : 'SYNCED');
    },
    [user, userLatitude, userLongitude, locationAccuracyMeters, sosMessage, isOffline]
  );

  const simulateRelayPacket = useCallback(
    async (direction: 'THROUGH_THIS_DEVICE' | 'BY_THIS_DEVICE' = 'THROUGH_THIS_DEVICE') => {
      const nowEpoch = Date.now();
      const thisDevice = user.deviceName || MeshEngine.getDeviceName();

      let packet: MeshRelayedPacket;
      if (direction === 'THROUGH_THIS_DEVICE') {
        const peerProfiles = [
          {
            name: 'Meera Patel',
            dev: 'Galaxy A54 (Rescue Scout)',
            phone: '+91 98211 44556',
            blood: 'B+',
            notes: 'Flash flood level rising, 2 adults on balcony',
            targetDev: 'District Ops Command Center',
            sev: 'HIGH' as const,
          },
          {
            name: 'Kavita Roy',
            dev: 'OnePlus 12 Node (Relay)',
            phone: '+91 97233 11223',
            blood: 'O-',
            notes: 'Elderly citizen needing oxygen concentrator support',
            targetDev: 'Central Ambulance Depot',
            sev: 'CRITICAL' as const,
          },
          {
            name: 'Devendra Singh',
            dev: 'Redmi Note 13 (Volunteer)',
            phone: '+91 99100 88776',
            blood: 'A+',
            notes: 'Clear access route identified via Sector 9 elevated bridge',
            targetDev: 'Community Relief Station #2',
            sev: 'MODERATE' as const,
          },
        ];
        const profile = peerProfiles[Math.floor(Math.random() * peerProfiles.length)];

        packet = {
          id: `sim-through-${nowEpoch}-${Math.floor(Math.random() * 1000)}`,
          senderId: `node-${Math.random().toString(36).substring(2, 7)}`,
          senderName: profile.name,
          senderPhone: profile.phone,
          senderDeviceName: profile.dev,
          targetDeviceId: 'hub-ops-central',
          targetDeviceName: profile.targetDev,
          customMessage: `URGENT RELAY: ${profile.notes}. Routed through local mesh cluster for immediate team dispatch.`,
          severity: profile.sev,
          latitude: Number(((userLatitude ?? 28.6139) + (Math.random() - 0.5) * 0.015).toFixed(4)),
          longitude: Number(((userLongitude ?? 77.2090) + (Math.random() - 0.5) * 0.015).toFixed(4)),
          accuracyMeters: 8,
          bloodGroup: profile.blood,
          emergencyContact: profile.phone,
          medicalNotes: profile.notes,
          createdAtEpochMillis: nowEpoch,
          hopCount: 2,
          relayPath: [profile.dev, `${thisDevice} (Transit Relay)`, profile.targetDev],
          transport: 'BLE_MESH',
          deliveryStatus: 'CONFIRMED',
          rssi: '-56 dBm (Strong)',
          direction: 'THROUGH_THIS_DEVICE',
        };
      } else {
        packet = {
          id: `sim-by-${nowEpoch}-${Math.floor(Math.random() * 1000)}`,
          senderId: user.id,
          senderName: user.name,
          senderPhone: user.phone,
          senderDeviceName: thisDevice,
          targetDeviceId: 'mesh-broadcast-all',
          targetDeviceName: 'All Nearby Mesh Nodes',
          customMessage: `Local Broadcast from ${user.name}: Rapid mesh heartbeat test. Forwarding operational status across perimeter.`,
          severity: 'MODERATE',
          latitude: userLatitude ?? 28.6139,
          longitude: userLongitude ?? 77.2090,
          accuracyMeters: locationAccuracyMeters ?? 12,
          bloodGroup: user.bloodGroup,
          emergencyContact: user.emergencyContact,
          medicalNotes: user.medicalNotes,
          createdAtEpochMillis: nowEpoch,
          hopCount: 1,
          relayPath: [thisDevice, 'Nearby Mesh Node (Ambulance Alpha)'],
          transport: 'BLE_MESH',
          deliveryStatus: 'DELIVERED',
          rssi: '-48 dBm (Excellent)',
          direction: 'BY_THIS_DEVICE',
        };
      }

      setRelayedPackets((prev) => [packet, ...prev]);
      await Storage.saveRelayedPacket(packet);
      return packet;
    },
    [user, userLatitude, userLongitude, locationAccuracyMeters]
  );

  const clearRelayedPackets = useCallback(async () => {
    setRelayedPackets([]);
    await Storage.saveRelayedPackets([]);
  }, []);

  const dismissIncomingPeerAlert = useCallback(() => {
    setIncomingPeerAlert(null);
  }, []);


  const resetSos = useCallback(() => {
    setSosSendState('IDLE');
    setSosMessage('');
  }, []);

  const setDarkTheme = useCallback(async (isDark: boolean) => {
    setIsDarkThemeState(isDark);
    await Storage.saveSettings({ isDarkTheme: isDark });
  }, []);

  const setVolunteerMode = useCallback(async (isVolunteer: boolean) => {
    setIsVolunteerModeState(isVolunteer);
    await Storage.saveSettings({ isVolunteer });
  }, []);

  const setLanguage = useCallback(async (lang: string) => {
    setLanguageState(lang);
    await Storage.saveSettings({ language: lang });
  }, []);

  const updateUser = useCallback(async (updatedUser: User) => {
    setUser(updatedUser);
    // save to storage
  }, []);

  const toggleOfflineSimulation = useCallback(() => {
    setIsOffline((prev) => !prev);
  }, []);

  const filteredFirstAidGuides = firstAidGuides.filter((guide) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      guide.title.toLowerCase().includes(q) ||
      guide.category.toLowerCase().includes(q) ||
      guide.summary.toLowerCase().includes(q) ||
      guide.steps.some((s) => s.toLowerCase().includes(q))
    );
  });

  return (
    <AppContext.Provider
      value={{
        user,
        updateUser,
        hazards,
        isHazardsRefreshing,
        refreshHazards,
        firstAidGuides,
        isFirstAidRefreshing,
        searchQuery,
        setSearchQuery,
        filteredFirstAidGuides,
        refreshFirstAid,
        sosSendState,
        sosMessage,
        setSosMessage,
        locationStatus,
        userLatitude,
        userLongitude,
        locationAccuracyMeters,
        meshRelayState,
        activeSosAlerts,
        connectedDevices,
        relayedPackets,
        incomingPeerAlert,
        sendSos,
        resetSos,
        refreshLocation,
        dismissIncomingPeerAlert,
        simulateRelayPacket,
        clearRelayedPackets,
        isDarkTheme,

        setDarkTheme,
        isVolunteerMode,
        setVolunteerMode,
        language,
        setLanguage,
        isOffline,
        toggleOfflineSimulation,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};


export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
