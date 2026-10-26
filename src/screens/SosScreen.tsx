import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Animated,
  Easing,
  ScrollView,
  ActivityIndicator,
  Modal,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialIcons } from '@expo/vector-icons';
import { StatusPill } from '../components/StatusPill';
import { Colors } from '../theme/colors';
import { Typography, EyebrowStyle, DataReadoutStyle } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';
import { useApp } from '../context/AppContext';
import { ConnectedDevice, MeshRelayedPacket } from '../data/models/types';

interface SosScreenProps {
  onBack: () => void;
  onNavigateToAnalytics?: () => void;
}

const PRESET_MESSAGES = [
  'Trapped by floodwater, need boat rescue',
  'Severe injury, medical assistance needed',
  'Structural collapse, multiple people trapped',
  'Elderly & children need immediate evacuation',
];

export const SosScreen: React.FC<SosScreenProps> = ({ onBack, onNavigateToAnalytics }) => {
  const insets = useSafeAreaInsets();
  const {
    user,
    updateUser,
    sosSendState,
    sosMessage,
    setSosMessage,
    locationStatus,
    userLatitude,
    userLongitude,
    locationAccuracyMeters,
    meshRelayState,
    connectedDevices,
    incomingPeerAlert,
    sendSos,
    resetSos,
    refreshLocation,
    dismissIncomingPeerAlert,
  } = useApp();

  const [selectedDeviceModal, setSelectedDeviceModal] = useState<ConnectedDevice | null>(null);
  const [showDevicesListModal, setShowDevicesListModal] = useState<boolean>(false);
  const [showNameEditModal, setShowNameEditModal] = useState<boolean>(false);
  const [customDeviceNameInput, setCustomDeviceNameInput] = useState<string>(user.deviceName || '');

  // Radar expanding ring animation (1800ms restart)
  const ringAnim = useRef(new Animated.Value(0)).current;
  // Button scale pulse animation (1100ms reverse)
  const pulseAnim = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    refreshLocation();

    // Radar ring loop
    const ringLoop = Animated.loop(
      Animated.timing(ringAnim, {
        toValue: 1,
        duration: 1800,
        easing: Easing.linear,
        useNativeDriver: true,
      })
    );

    // Subtle button pulse loop
    const pulseLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 1.06,
          duration: 1100,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 1.0,
          duration: 1100,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    );

    ringLoop.start();
    pulseLoop.start();

    return () => {
      ringLoop.stop();
      pulseLoop.stop();
    };
  }, []);

  const ringScale = ringAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [1, 1.6],
  });

  const ringOpacity = ringAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [0.5, 0],
  });

  const isSending = sosSendState === 'SENDING';
  const isSent = sosSendState === 'SENT_LOCALLY' || sosSendState === 'SYNCED';

  // Format coordinates
  const locationText = (() => {
    switch (locationStatus) {
      case 'LOCATING':
        return 'Getting your location…';
      case 'AVAILABLE':
        if (userLatitude !== null && userLongitude !== null) {
          const coords = `${userLatitude.toFixed(5)}, ${userLongitude.toFixed(5)}`;
          return locationAccuracyMeters !== null
            ? `${coords} (±${Math.round(locationAccuracyMeters)}m)`
            : coords;
        }
        return 'Location locked';
      case 'PERMISSION_DENIED':
        return 'Location permission needed for accuracy';
      case 'UNAVAILABLE':
      default:
        return 'Location unavailable — SOS will still send';
    }
  })();

  const locationColor = (() => {
    switch (locationStatus) {
      case 'AVAILABLE':
        return Colors.Moss;
      case 'LOCATING':
        return Colors.Slate;
      case 'PERMISSION_DENIED':
      case 'UNAVAILABLE':
      default:
        return Colors.Amber;
    }
  })();

  const meshText = (() => {
    if (meshRelayState.type === 'Ready') {
      const count = connectedDevices.length;
      return count > 0
        ? `${count} real mesh node${count === 1 ? '' : 's'} connected (tap to view)`
        : 'Searching for nearby peers… (tap to view)';
    }
    if (meshRelayState.type === 'Error') {
      return 'Mesh unavailable';
    }
    return 'Starting mesh radio… (tap to view)';
  })();

  const meshColor = (() => {
    if (meshRelayState.type === 'Ready' && connectedDevices.length > 0) {
      return Colors.Moss;
    }
    if (meshRelayState.type === 'Error') {
      return Colors.Ember;
    }
    return Colors.Slate;
  })();

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'RESCUE_TEAM':
        return Colors.Ember;
      case 'VOLUNTEER':
        return Colors.Signal;
      case 'CITIZEN':
      default:
        return Colors.Moss;
    }
  };

  const handleSaveDeviceName = () => {
    if (customDeviceNameInput.trim()) {
      updateUser({ ...user, deviceName: customDeviceNameInput.trim() });
      setShowNameEditModal(false);
    }
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Top App Bar */}
      <View style={styles.topBar}>
        <TouchableOpacity activeOpacity={0.7} onPress={onBack} style={styles.backButton}>
          <MaterialIcons name="arrow-back" size={24} color={Colors.DarkOnSurface} />
        </TouchableOpacity>
        <Text style={styles.topBarTitle}>Emergency SOS</Text>
        <TouchableOpacity
          activeOpacity={0.7}
          onPress={() => {
            setCustomDeviceNameInput(user.deviceName || '');
            setShowNameEditModal(true);
          }}
          style={styles.deviceBadgeBtn}
        >
          <MaterialIcons name="perm-device-information" size={16} color={Colors.Signal} />
          <Text style={styles.deviceBadgeText} numberOfLines={1}>
            {user.deviceName || 'My Device'}
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={styles.contentContainer}
        showsVerticalScrollIndicator={false}
      >
        {/* Incoming Real Peer SOS Alert Banner */}
        {incomingPeerAlert && (
          <View style={styles.incomingAlertCard}>
            <View style={styles.incomingHeader}>
              <View style={styles.incomingTitleRow}>
                <MaterialIcons name="warning" size={20} color={Colors.Ember} />
                <Text style={styles.incomingTitle}>LIVE INCOMING EMERGENCY ALERT</Text>
              </View>
              <TouchableOpacity onPress={dismissIncomingPeerAlert} style={styles.incomingClose}>
                <MaterialIcons name="close" size={18} color={Colors.Slate} />
              </TouchableOpacity>
            </View>

            <View style={styles.incomingDeviceRow}>
              <MaterialIcons name="smartphone" size={16} color={Colors.Signal} />
              <Text style={styles.incomingDeviceName}>{incomingPeerAlert.senderDeviceName}</Text>
              <Text style={styles.incomingSenderName}>({incomingPeerAlert.senderName})</Text>
            </View>

            {/* Custom Typed Message Quote */}
            <View style={styles.customMessageHighlightBox}>
              <Text style={styles.customMessageQuoteLabel}>RELAYED CUSTOM MESSAGE:</Text>
              <Text style={styles.customMessageQuoteText}>
                "{incomingPeerAlert.customMessage}"
              </Text>
            </View>

            {/* Telemetry info */}
            <View style={styles.incomingTelemetryGrid}>
              <Text style={styles.incomingTelemetryItem}>
                <Text style={{ color: Colors.Slate }}>GPS Fix: </Text>
                {incomingPeerAlert.latitude.toFixed(5)}, {incomingPeerAlert.longitude.toFixed(5)} (±{incomingPeerAlert.accuracyMeters}m)
              </Text>
              <Text style={styles.incomingTelemetryItem}>
                <Text style={{ color: Colors.Slate }}>Blood Group: </Text>
                {incomingPeerAlert.bloodGroup || 'N/A'} · Contact: {incomingPeerAlert.emergencyContact || 'N/A'}
              </Text>
              <Text style={styles.incomingTelemetryItem}>
                <Text style={{ color: Colors.Slate }}>Relay Route: </Text>
                {incomingPeerAlert.relayPath.join(' ➔ ')}
              </Text>
            </View>
          </View>
        )}


        {isSent ? (
          <View style={styles.sentContainer}>
            <View style={styles.sentIconCircle}>
              <MaterialIcons name="check" size={48} color={Colors.Moss} />
            </View>
            <Text style={styles.sentTitle}>Your SOS has been recorded</Text>
            <Text style={styles.sentDesc}>
              Your custom emergency message and medical telemetry are actively being broadcasted and relayed to all connected mesh devices.
            </Text>
            {onNavigateToAnalytics && (
              <TouchableOpacity
                activeOpacity={0.8}
                onPress={onNavigateToAnalytics}
                style={styles.analyticsButton}
              >
                <MaterialIcons name="analytics" size={20} color="#0B0F17" />
                <Text style={styles.analyticsButtonText}>View in Analytics Dashboard</Text>
              </TouchableOpacity>
            )}
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={resetSos}
              style={styles.resetButton}
            >
              <Text style={styles.resetButtonText}>Send Another Alert</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.sosButtonSection}>
            {/* Radar Expanding Ring */}
            {!isSending && (
              <Animated.View
                style={[
                  styles.radarRing,
                  {
                    transform: [{ scale: ringScale }],
                    opacity: ringOpacity,
                  },
                ]}
              />
            )}

            {/* Pulsing SOS Button */}
            <Animated.View
              style={[
                styles.sosButtonWrapper,
                { transform: [{ scale: isSending ? 1 : pulseAnim }] },
              ]}
            >
              <TouchableOpacity
                activeOpacity={0.9}
                disabled={isSending}
                onPress={() => sendSos()}
                style={styles.sosButton}
              >
                {isSending ? (
                  <ActivityIndicator size="large" color="#FFFFFF" />
                ) : (
                  <View style={styles.sosInnerContent}>
                    <MaterialIcons name="emergency" size={48} color="#FFFFFF" />
                    <Text style={styles.sosMainText}>SOS</Text>
                  </View>
                )}
              </TouchableOpacity>
            </Animated.View>

            <Text style={styles.sosInstructionText}>
              {'Press in a real emergency.\nYour custom message & location relay instantly.'}
            </Text>
          </View>
        )}

        {/* Message Input with Custom Type Box */}
        <View style={styles.inputCard}>
          <Text style={styles.inputLabel}>CUSTOM EMERGENCY MESSAGE</Text>
          <TextInput
            style={styles.input}
            value={sosMessage}
            onChangeText={setSosMessage}
            placeholder="Type specific emergency details (e.g. trapped, injuries, exact floor)..."
            placeholderTextColor={Colors.Slate}
            multiline
          />

          {/* Quick preset chips */}
          <Text style={styles.chipsLabel}>QUICK MESSAGE TEMPLATES:</Text>
          <View style={styles.chipsContainer}>
            {PRESET_MESSAGES.map((msg, idx) => (
              <TouchableOpacity
                key={idx}
                activeOpacity={0.7}
                onPress={() => setSosMessage(msg)}
                style={[
                  styles.chip,
                  sosMessage === msg && { borderColor: Colors.Signal, backgroundColor: Colors.SignalDeep },
                ]}
              >
                <Text
                  style={[
                    styles.chipText,
                    sosMessage === msg && { color: Colors.Signal, fontWeight: 'bold' },
                  ]}
                  numberOfLines={1}
                >
                  {msg}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Status Indicators (Location & Mesh) */}
        <View style={styles.statusRow}>
          <View style={styles.statusCol}>
            <Text style={styles.sectionEyebrow}>DEVICE GPS LOCATION</Text>
            <StatusPill text={locationText} color={locationColor} />
          </View>
          <View style={styles.statusCol}>
            <Text style={styles.sectionEyebrow}>MESH NETWORK</Text>
            <TouchableOpacity
              activeOpacity={0.7}
              onPress={() => setShowDevicesListModal(true)}
            >
              <StatusPill text={meshText} color={meshColor} />
            </TouchableOpacity>
          </View>
        </View>

        {/* CONNECTED DEVICES & RELAYED INFO SECTION */}
        <View style={styles.connectedSection}>
          <View style={styles.connectedSectionHeader}>
            <View>
              <Text style={styles.connectedSectionTitle}>
                LIVE CONNECTED MESH PEERS ({connectedDevices.length})
              </Text>
              <Text style={styles.connectedSectionSub}>
                Real-time peer devices receiving your custom message and relayed telemetry
              </Text>
            </View>
          </View>

          {connectedDevices.length === 0 ? (
            <View style={styles.searchingMeshCard}>
              <View style={styles.scanningAnimRow}>
                <ActivityIndicator size="small" color={Colors.Signal} />
                <Text style={styles.searchingMeshTitle}>SCANNING LOCAL MESH FREQUENCIES</Text>
              </View>
              <Text style={styles.searchingMeshDesc}>
                No other devices in range. Open ResQLink in another browser tab or device to form a live peer-to-peer cluster.
              </Text>
            </View>
          ) : (
            connectedDevices.map((device) => {
              const hasRelayedPacket = device.lastRelayedPacket !== null && device.lastRelayedPacket !== undefined;
              const packet = device.lastRelayedPacket;

              return (
                <TouchableOpacity
                  key={device.id}
                  activeOpacity={0.8}
                  onPress={() => setSelectedDeviceModal(device)}
                  style={styles.deviceCard}
                >
                  {/* Device Header */}
                  <View style={styles.deviceCardTop}>
                    <View style={styles.deviceMainInfo}>
                      <View style={styles.deviceIconBg}>
                        <MaterialIcons name="smartphone" size={22} color={Colors.Signal} />
                      </View>
                      <View style={styles.deviceTexts}>
                        <Text style={styles.deviceNameText}>{device.deviceName}</Text>
                        <Text style={styles.deviceOwnerText}>
                          Owner: {device.ownerName} · {device.distanceMeters}m away
                        </Text>
                      </View>
                    </View>

                    <View style={styles.deviceBadges}>
                      <View style={[styles.roleBadge, { backgroundColor: getRoleColor(device.role) + '22', borderColor: getRoleColor(device.role) }]}>
                        <Text style={[styles.roleBadgeText, { color: getRoleColor(device.role) }]}>
                          {device.role.replace('_', ' ')}
                        </Text>
                      </View>
                      <Text style={styles.rssiText}>{device.signalStrength.split(' ')[0]} RSSI</Text>
                    </View>
                  </View>


                {/* Device Hardware Specs Row */}
                <View style={styles.deviceSpecsRow}>
                  <View style={styles.specItem}>
                    <MaterialIcons name="battery-charging-full" size={14} color={Colors.Moss} />
                    <Text style={styles.specText}>{device.batteryPercent}% Batt</Text>
                  </View>
                  <View style={styles.specItem}>
                    <MaterialIcons name="bluetooth" size={14} color={Colors.Signal} />
                    <Text style={styles.specText}>BLE 5.3 Mesh</Text>
                  </View>
                  <View style={styles.specItem}>
                    <MaterialIcons name="sync" size={14} color={device.status === 'RELAYING' ? Colors.Amber : Colors.Moss} />
                    <Text style={[styles.specText, { color: device.status === 'RELAYING' ? Colors.Amber : Colors.Moss }]}>
                      {device.status}
                    </Text>
                  </View>
                </View>

                {/* RELAYED PAYLOAD BREAKDOWN */}
                {hasRelayedPacket && packet ? (
                  <View style={styles.relayedBox}>
                    <View style={styles.relayedBoxHeader}>
                      <View style={styles.relayedStatusBadge}>
                        <MaterialIcons name="cell-tower" size={14} color={Colors.Signal} />
                        <Text style={styles.relayedStatusText}>RELAYED PACKET DELIVERED</Text>
                      </View>
                      <Text style={styles.hopCountText}>Hop {packet.hopCount} · {packet.transport}</Text>
                    </View>

                    {/* Typed Custom Message */}
                    <View style={styles.relayedMessageContent}>
                      <Text style={styles.relayedMsgHeader}>CUSTOM MESSAGE TYPED:</Text>
                      <Text style={styles.relayedMsgBody}>"{packet.customMessage}"</Text>
                    </View>

                    {/* All Info Relayed */}
                    <View style={styles.relayedInfoDetails}>
                      <View style={styles.relayedDetailRow}>
                        <Text style={styles.relayedDetailKey}>Sender Device:</Text>
                        <Text style={styles.relayedDetailVal}>{packet.senderDeviceName}</Text>
                      </View>
                      <View style={styles.relayedDetailRow}>
                        <Text style={styles.relayedDetailKey}>Target Device:</Text>
                        <Text style={styles.relayedDetailVal}>{packet.targetDeviceName}</Text>
                      </View>
                      <View style={styles.relayedDetailRow}>
                        <Text style={styles.relayedDetailKey}>Relayed GPS:</Text>
                        <Text style={styles.relayedDetailVal}>
                          {packet.latitude.toFixed(5)}, {packet.longitude.toFixed(5)} (±{packet.accuracyMeters}m)
                        </Text>
                      </View>
                      <View style={styles.relayedDetailRow}>
                        <Text style={styles.relayedDetailKey}>Medical Info:</Text>
                        <Text style={styles.relayedDetailVal}>
                          Blood {packet.bloodGroup || 'N/A'} · Contact {packet.emergencyContact || 'N/A'}
                        </Text>
                      </View>
                      <View style={styles.relayedDetailRow}>
                        <Text style={styles.relayedDetailKey}>Transmission Path:</Text>
                        <Text style={styles.relayedDetailPath}>{packet.relayPath.join(' ➔ ')}</Text>
                      </View>
                    </View>
                  </View>
                ) : (
                  <View style={styles.readyToRelayBox}>
                    <MaterialIcons name="wifi-tethering" size={16} color={Colors.Slate} />
                    <Text style={styles.readyToRelayText}>
                      Standby: Will receive custom message and telemetry instantly on SOS trigger.
                    </Text>
                  </View>
                )}
              </TouchableOpacity>
            );
          }))}
        </View>

        <View style={{ height: Spacing.xxl }} />
      </ScrollView>

      {/* Device Telemetry Breakdown Modal */}
      <Modal visible={selectedDeviceModal !== null} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            {selectedDeviceModal && (
              <>
                <View style={styles.modalHeader}>
                  <View>
                    <Text style={styles.modalTitle}>{selectedDeviceModal.deviceName}</Text>
                    <Text style={styles.modalSub}>
                      {selectedDeviceModal.ownerName} · {selectedDeviceModal.role}
                    </Text>
                  </View>
                  <TouchableOpacity onPress={() => setSelectedDeviceModal(null)} style={styles.modalClose}>
                    <MaterialIcons name="close" size={24} color={Colors.DarkOnSurface} />
                  </TouchableOpacity>
                </View>

                <ScrollView style={styles.modalBody}>
                  <Text style={styles.modalSectionTitle}>DEVICE HARDWARE & NETWORK SPECS</Text>
                  <View style={styles.modalSpecsCard}>
                    <Text style={styles.modalSpecLine}>Device ID: {selectedDeviceModal.id}</Text>
                    <Text style={styles.modalSpecLine}>Proper Name: {selectedDeviceModal.deviceName}</Text>
                    <Text style={styles.modalSpecLine}>Signal RSSI: {selectedDeviceModal.signalStrength}</Text>
                    <Text style={styles.modalSpecLine}>Proximity: ~{selectedDeviceModal.distanceMeters} meters</Text>
                    <Text style={styles.modalSpecLine}>Battery Level: {selectedDeviceModal.batteryPercent}%</Text>
                    <Text style={styles.modalSpecLine}>Status: {selectedDeviceModal.status}</Text>
                    <Text style={styles.modalSpecLine}>
                      Coordinates: {selectedDeviceModal.latitude.toFixed(4)}, {selectedDeviceModal.longitude.toFixed(4)}
                    </Text>
                  </View>

                  <Text style={[styles.modalSectionTitle, { marginTop: Spacing.md }]}>
                    LATEST RELAYED PACKET TELEMETRY
                  </Text>
                  {selectedDeviceModal.lastRelayedPacket ? (
                    <View style={styles.modalRelayCard}>
                      <Text style={styles.modalMsgHeader}>CUSTOM TYPED MESSAGE:</Text>
                      <Text style={styles.modalMsgBody}>
                        "{selectedDeviceModal.lastRelayedPacket.customMessage}"
                      </Text>

                      <View style={{ height: 1, backgroundColor: Colors.HairlineDark, marginVertical: Spacing.sm }} />

                      <Text style={styles.modalDetailText}>
                        Sender Device: {selectedDeviceModal.lastRelayedPacket.senderDeviceName}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Sender Name: {selectedDeviceModal.lastRelayedPacket.senderName} ({selectedDeviceModal.lastRelayedPacket.senderPhone})
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Receiving Device: {selectedDeviceModal.lastRelayedPacket.targetDeviceName}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        GPS Coordinates: {selectedDeviceModal.lastRelayedPacket.latitude.toFixed(5)}, {selectedDeviceModal.lastRelayedPacket.longitude.toFixed(5)}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Medical Profile: Blood {selectedDeviceModal.lastRelayedPacket.bloodGroup || 'N/A'} · Contact {selectedDeviceModal.lastRelayedPacket.emergencyContact || 'N/A'}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Medical Notes: {selectedDeviceModal.lastRelayedPacket.medicalNotes || 'None'}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Relay Route Path: {selectedDeviceModal.lastRelayedPacket.relayPath.join(' ➔ ')}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Hop Count: {selectedDeviceModal.lastRelayedPacket.hopCount} | Transport: {selectedDeviceModal.lastRelayedPacket.transport}
                      </Text>
                      <Text style={styles.modalDetailText}>
                        Delivery Status: {selectedDeviceModal.lastRelayedPacket.deliveryStatus} (Confirmed)
                      </Text>
                    </View>
                  ) : (
                    <View style={styles.modalEmptyRelay}>
                      <Text style={{ color: Colors.Slate }}>No active SOS relayed yet to this device.</Text>
                    </View>
                  )}
                </ScrollView>

                <TouchableOpacity
                  onPress={() => setSelectedDeviceModal(null)}
                  style={styles.modalDoneBtn}
                >
                  <Text style={styles.modalDoneText}>Close Inspection</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        </View>
      </Modal>

      {/* Connected Devices List Modal */}
      <Modal visible={showDevicesListModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <View>
                <Text style={styles.modalTitle}>Connected Mesh Devices</Text>
                <Text style={styles.modalSub}>
                  {connectedDevices.length} node{connectedDevices.length === 1 ? '' : 's'} in active cluster
                </Text>
              </View>
              <TouchableOpacity onPress={() => setShowDevicesListModal(false)} style={styles.modalClose}>
                <MaterialIcons name="close" size={24} color={Colors.DarkOnSurface} />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalBody}>
              {connectedDevices.length === 0 ? (
                <View style={{ paddingVertical: Spacing.xl, alignItems: 'center' }}>
                  <ActivityIndicator size="small" color={Colors.Signal} />
                  <Text style={{ ...Typography.bodyMedium, color: Colors.Slate, marginTop: Spacing.sm, textAlign: 'center' }}>
                    Searching for nearby Bluetooth & Wi-Fi mesh nodes...
                  </Text>
                </View>
              ) : (
                connectedDevices.map((device) => (
                  <TouchableOpacity
                    key={device.id}
                    activeOpacity={0.7}
                    onPress={() => {
                      setShowDevicesListModal(false);
                      setSelectedDeviceModal(device);
                    }}
                    style={{
                      backgroundColor: Colors.DarkSurface,
                      padding: Spacing.md,
                      borderRadius: Shapes.medium,
                      marginBottom: Spacing.sm,
                      borderWidth: 1,
                      borderColor: Colors.HairlineDark,
                    }}
                  >
                    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ ...Typography.titleSmall, color: '#FFFFFF', fontWeight: 'bold' }}>
                        {device.deviceName}
                      </Text>
                      <StatusPill text={device.status} color={Colors.Moss} />
                    </View>
                    <Text style={{ ...Typography.bodySmall, color: Colors.Slate, marginTop: 2 }}>
                      Owner: {device.ownerName} · {device.distanceMeters}m away
                    </Text>
                    <Text style={{ ...Typography.bodySmall, color: Colors.Signal, marginTop: 2 }}>
                      {device.signalStrength} RSSI · {device.batteryPercent}% Battery · BLE 5.3 Mesh
                    </Text>
                  </TouchableOpacity>
                ))
              )}
            </ScrollView>

            <TouchableOpacity
              onPress={() => setShowDevicesListModal(false)}
              style={styles.modalDoneBtn}
            >
              <Text style={styles.modalDoneText}>Close</Text>
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
  topBar: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.md,
    backgroundColor: Colors.DarkSurface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.HairlineDark,
  },
  backButton: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topBarTitle: {
    ...Typography.titleLarge,
    color: '#FFFFFF',
    fontWeight: '600',
  },
  deviceBadgeBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.SignalDeep,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
    borderRadius: Shapes.small,
    borderWidth: 1,
    borderColor: Colors.Signal,
    gap: 4,
    maxWidth: 160,
  },
  deviceBadgeText: {
    ...Typography.labelSmall,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  simBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.SignalDeep,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
    borderRadius: Shapes.small,
    borderWidth: 1,
    borderColor: Colors.Signal,
    gap: 4,
  },
  simBtnText: {
    ...Typography.labelSmall,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  contentContainer: {
    alignItems: 'center',
    padding: Spacing.md,
    paddingBottom: Spacing.xxxl,
  },
  incomingAlertCard: {
    width: '100%',
    backgroundColor: 'rgba(235, 87, 87, 0.12)',
    borderWidth: 1.5,
    borderColor: Colors.Ember,
    borderRadius: Shapes.large,
    padding: Spacing.md,
    marginBottom: Spacing.md,
  },
  incomingHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  incomingTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  incomingTitle: {
    ...EyebrowStyle,
    color: Colors.Ember,
    fontWeight: 'bold',
  },
  incomingClose: {
    padding: 4,
  },
  incomingDeviceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: Spacing.xs,
  },
  incomingDeviceName: {
    ...Typography.labelLarge,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  incomingSenderName: {
    ...Typography.bodySmall,
    color: Colors.Slate,
  },
  customMessageHighlightBox: {
    backgroundColor: 'rgba(11, 15, 23, 0.85)',
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.sm,
    marginVertical: Spacing.xs,
  },
  customMessageQuoteLabel: {
    ...EyebrowStyle,
    fontSize: 10,
    color: Colors.Signal,
    marginBottom: 2,
  },
  customMessageQuoteText: {
    ...Typography.bodyLarge,
    color: '#FFFFFF',
    fontStyle: 'italic',
    fontWeight: '600',
  },
  incomingTelemetryGrid: {
    marginTop: Spacing.xs,
    gap: 2,
  },
  incomingTelemetryItem: {
    ...Typography.bodySmall,
    color: Colors.DarkOnSurface,
  },
  sosButtonSection: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: Spacing.md,
    width: 240,
    height: 240,
  },
  radarRing: {
    position: 'absolute',
    width: 180,
    height: 180,
    borderRadius: 90,
    borderWidth: 2,
    borderColor: Colors.Ember,
  },
  sosButtonWrapper: {
    width: 180,
    height: 180,
    borderRadius: 90,
    backgroundColor: Colors.Ember,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 8,
    shadowColor: Colors.Ember,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.5,
    shadowRadius: 16,
  },
  sosButton: {
    width: '100%',
    height: '100%',
    borderRadius: 90,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sosInnerContent: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  sosMainText: {
    fontSize: 26,
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: 2,
    marginTop: -4,
  },
  sosInstructionText: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    textAlign: 'center',
    marginTop: Spacing.lg,
    lineHeight: 18,
  },
  sentContainer: {
    alignItems: 'center',
    marginVertical: Spacing.lg,
    paddingHorizontal: Spacing.md,
  },
  sentIconCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: 'rgba(61, 220, 151, 0.15)',
    borderWidth: 1,
    borderColor: Colors.Moss,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.md,
  },
  sentTitle: {
    ...Typography.headlineMedium,
    color: '#FFFFFF',
    textAlign: 'center',
    marginBottom: Spacing.xs,
  },
  sentDesc: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: Spacing.md,
  },
  analyticsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    width: '100%',
    paddingVertical: 14,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.Signal,
    marginBottom: Spacing.sm,
  },
  analyticsButtonText: {
    ...Typography.labelLarge,
    color: '#0B0F17',
    fontWeight: 'bold',
  },
  resetButton: {
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.InkRaised,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
  },
  resetButtonText: {
    ...Typography.labelLarge,
    color: Colors.Signal,
  },
  inputCard: {
    width: '100%',
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
    marginVertical: Spacing.sm,
  },
  inputLabel: {
    ...EyebrowStyle,
    color: Colors.Slate,
    marginBottom: Spacing.xs,
  },
  input: {
    width: '100%',
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    color: Colors.DarkOnSurface,
    ...Typography.bodyMedium,
    minHeight: 60,
    textAlignVertical: 'top',
  },
  chipsLabel: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Slate,
    marginTop: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  chipsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  chip: {
    backgroundColor: Colors.InkRaised,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.small,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  chipText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.DarkOnSurface,
  },
  statusRow: {
    width: '100%',
    flexDirection: 'row',
    gap: Spacing.sm,
    marginVertical: Spacing.xs,
  },
  statusCol: {
    flex: 1,
    alignItems: 'center',
  },
  sectionEyebrow: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Slate,
    marginBottom: Spacing.xxs,
  },
  connectedSection: {
    width: '100%',
    marginTop: Spacing.lg,
  },
  connectedSectionHeader: {
    marginBottom: Spacing.sm,
  },
  connectedSectionTitle: {
    ...EyebrowStyle,
    fontSize: 13,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  connectedSectionSub: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    marginTop: 2,
  },
  searchingMeshCard: {
    width: '100%',
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.lg,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.sm,
  },
  scanningAnimRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  searchingMeshTitle: {
    ...EyebrowStyle,
    fontSize: 12,
    color: Colors.Signal,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
  searchingMeshDesc: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    textAlign: 'center',
    marginTop: 4,
  },
  deviceCard: {
    width: '100%',
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
  },
  deviceCardTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  deviceMainInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  deviceIconBg: {
    width: 38,
    height: 38,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.InkRaised,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
  },
  deviceTexts: {
    flex: 1,
  },
  deviceNameText: {
    ...Typography.titleMedium,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  deviceOwnerText: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    marginTop: 2,
  },
  deviceBadges: {
    alignItems: 'flex-end',
    gap: 4,
  },
  roleBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
  },
  roleBadgeText: {
    fontSize: 9,
    fontWeight: 'bold',
  },
  rssiText: {
    ...Typography.labelSmall,
    fontSize: 10,
    color: Colors.Slate,
  },
  deviceSpecsRow: {
    flexDirection: 'row',
    gap: Spacing.md,
    marginTop: Spacing.sm,
    paddingTop: Spacing.xs,
    borderTopWidth: 1,
    borderTopColor: Colors.HairlineDark,
  },
  specItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  specText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.DarkOnSurface,
  },
  relayedBox: {
    marginTop: Spacing.sm,
    backgroundColor: 'rgba(11, 15, 23, 0.92)',
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.sm,
  },
  relayedBoxHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  relayedStatusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  relayedStatusText: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  hopCountText: {
    ...Typography.bodySmall,
    fontSize: 10,
    color: Colors.Slate,
  },
  relayedMessageContent: {
    backgroundColor: 'rgba(53, 224, 200, 0.08)',
    borderLeftWidth: 3,
    borderLeftColor: Colors.Signal,
    padding: Spacing.xs,
    borderRadius: 4,
    marginVertical: 4,
  },
  relayedMsgHeader: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Signal,
  },
  relayedMsgBody: {
    ...Typography.bodyMedium,
    color: '#FFFFFF',
    fontWeight: '600',
    marginTop: 2,
  },
  relayedInfoDetails: {
    marginTop: Spacing.xs,
    gap: 3,
  },
  relayedDetailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  relayedDetailKey: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
  },
  relayedDetailVal: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.DarkOnSurface,
    fontWeight: '500',
  },
  relayedDetailPath: {
    ...Typography.bodySmall,
    fontSize: 10,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  readyToRelayBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: Spacing.xs,
    paddingTop: Spacing.xs,
  },
  readyToRelayText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
    flex: 1,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(6, 9, 15, 0.85)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: Colors.InkRaised,
    borderTopLeftRadius: Shapes.large,
    borderTopRightRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.lg,
    maxHeight: '85%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  modalTitle: {
    ...Typography.titleLarge,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  modalSub: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    marginTop: 2,
  },
  modalClose: {
    padding: 4,
  },
  modalBody: {
    marginVertical: Spacing.sm,
  },
  modalSectionTitle: {
    ...EyebrowStyle,
    color: Colors.Signal,
    marginBottom: Spacing.xs,
  },
  modalSpecsCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.medium,
    padding: Spacing.md,
    gap: 4,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
  },
  modalSpecLine: {
    ...Typography.bodySmall,
    color: Colors.DarkOnSurface,
  },
  modalRelayCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.medium,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.Signal,
    gap: 4,
  },
  modalMsgHeader: {
    ...EyebrowStyle,
    color: Colors.Signal,
    fontSize: 10,
  },
  modalMsgBody: {
    ...Typography.titleMedium,
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontStyle: 'italic',
  },
  modalDetailText: {
    ...Typography.bodySmall,
    color: Colors.DarkOnSurface,
  },
  modalEmptyRelay: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.medium,
    padding: Spacing.md,
    alignItems: 'center',
  },
  modalDoneBtn: {
    marginTop: Spacing.md,
    backgroundColor: Colors.SignalDeep,
    borderRadius: Shapes.medium,
    paddingVertical: Spacing.sm,
    alignItems: 'center',
  },
  modalDoneText: {
    ...Typography.labelLarge,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
});

