import React, { useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Modal,
  TextInput,
  Alert,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { GradientHeader } from '../components/GradientHeader';
import { Colors } from '../theme/colors';
import { Typography, EyebrowStyle, DataReadoutStyle } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';
import { useApp } from '../context/AppContext';
import { MeshRelayedPacket } from '../data/models/types';

export const AnalyticsScreen: React.FC = () => {
  const insets = useSafeAreaInsets();
  const {
    user,
    activeSosAlerts,
    hazards,
    connectedDevices,
    relayedPackets,
    simulateRelayPacket,
    clearRelayedPackets,
  } = useApp();

  // State for message inspection modal
  const [selectedPacket, setSelectedPacket] = useState<MeshRelayedPacket | null>(null);

  // Filtering & search state
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'THROUGH' | 'BY'>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  // Local device identifier
  const thisDeviceName = user.deviceName || 'ResQLink Device (This Device)';

  // Helper to determine if a packet was relayed BY or THROUGH this device
  const getPacketDirection = (packet: MeshRelayedPacket): 'BY_THIS_DEVICE' | 'THROUGH_THIS_DEVICE' => {
    if (packet.direction) {
      return packet.direction;
    }
    if (
      packet.senderId === user.id ||
      packet.senderName === user.name ||
      packet.senderDeviceName.toLowerCase().includes('this device')
    ) {
      return 'BY_THIS_DEVICE';
    }
    return 'THROUGH_THIS_DEVICE';
  };

  // Filtered and searched packets
  const filteredPackets = useMemo(() => {
    return relayedPackets.filter((packet) => {
      const direction = getPacketDirection(packet);

      if (activeFilter === 'THROUGH' && direction !== 'THROUGH_THIS_DEVICE') {
        return false;
      }
      if (activeFilter === 'BY' && direction !== 'BY_THIS_DEVICE') {
        return false;
      }

      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const matchesName = packet.senderName.toLowerCase().includes(query);
        const matchesDevice = packet.senderDeviceName.toLowerCase().includes(query);
        const matchesTarget = packet.targetDeviceName.toLowerCase().includes(query);
        const matchesMessage = packet.customMessage.toLowerCase().includes(query);
        const matchesPhone = packet.senderPhone ? packet.senderPhone.toLowerCase().includes(query) : false;
        const matchesPath = packet.relayPath.some((node) => node.toLowerCase().includes(query));

        return matchesName || matchesDevice || matchesTarget || matchesMessage || matchesPhone || matchesPath;
      }

      return true;
    });
  }, [relayedPackets, activeFilter, searchQuery, user]);

  // Statistics calculation
  const totalRelayed = relayedPackets.length;
  const relayedThroughCount = relayedPackets.filter(
    (p) => getPacketDirection(p) === 'THROUGH_THIS_DEVICE'
  ).length;
  const relayedByCount = relayedPackets.filter(
    (p) => getPacketDirection(p) === 'BY_THIS_DEVICE'
  ).length;

  const extremeHazards = hazards.filter((h) => h.severity === 'EXTREME' || h.severity === 'HIGH').length;
  const moderateHazards = hazards.filter((h) => h.severity === 'MODERATE').length;
  const lowHazards = hazards.filter((h) => h.severity === 'LOW').length;

  const handleSimulate = async (dir: 'THROUGH_THIS_DEVICE' | 'BY_THIS_DEVICE') => {
    try {
      const newPkt = await simulateRelayPacket(dir);
      setActionFeedback(
        dir === 'THROUGH_THIS_DEVICE'
          ? `Simulated transit message relayed through ${thisDeviceName}`
          : `Simulated broadcast relayed by ${thisDeviceName}`
      );
      setTimeout(() => setActionFeedback(null), 3500);
      // Auto-open new packet for immediate inspection demonstration
      setSelectedPacket(newPkt);
    } catch (e) {
      console.error(e);
    }
  };

  const handleReRelay = (packet: MeshRelayedPacket) => {
    setActionFeedback(`Packet ${packet.id.slice(0, 12)} re-relayed across active mesh peers!`);
    setTimeout(() => setActionFeedback(null), 3500);
    setSelectedPacket(null);
  };

  const formatTimeAgo = (epoch: number) => {
    const diff = Math.max(0, Date.now() - epoch);
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  };

  const formatExactTime = (epoch: number) => {
    const d = new Date(epoch);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <GradientHeader
          eyebrow="TELEMETRY & MESH RELAY"
          title="Analytics"
          subtitle="Real-time messages relayed through & by this device"
          iconName="analytics"
        />

        {/* Action feedback toast */}
        {actionFeedback && (
          <View style={styles.feedbackBanner}>
            <Ionicons name="checkmark-circle" size={18} color={Colors.Signal} />
            <Text style={styles.feedbackText}>{actionFeedback}</Text>
          </View>
        )}

        <View style={styles.contentContainer}>
          {/* Quick Simulation Testing Bar */}
          <View style={styles.simulationBar}>
            <View style={styles.simulationLeft}>
              <View style={styles.livePulseDot} />
              <Text style={styles.simulationLabel}>MESH NODE: {thisDeviceName.toUpperCase()}</Text>
            </View>
            <View style={styles.simulationButtonsRow}>
              <TouchableOpacity
                style={styles.simPillButtonCyan}
                activeOpacity={0.8}
                onPress={() => handleSimulate('THROUGH_THIS_DEVICE')}
              >
                <Ionicons name="swap-horizontal" size={14} color="#0B0F17" />
                <Text style={styles.simPillTextCyan}>+ Relay Through</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.simPillButtonOutline}
                activeOpacity={0.8}
                onPress={() => handleSimulate('BY_THIS_DEVICE')}
              >
                <Ionicons name="radio-outline" size={14} color={Colors.Amber} />
                <Text style={styles.simPillTextAmber}>+ Relay By</Text>
              </TouchableOpacity>

              {relayedPackets.length > 0 && (
                <TouchableOpacity
                  style={styles.simPillButtonClear}
                  activeOpacity={0.8}
                  onPress={async () => {
                    await clearRelayedPackets();
                    setActionFeedback('Relayed messages log cleared.');
                    setTimeout(() => setActionFeedback(null), 3000);
                  }}
                >
                  <Ionicons name="trash-outline" size={14} color={Colors.Slate} />
                  <Text style={styles.simPillTextClear}>Clear</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>

          {/* Top Row: Total Relayed & Connected Peers */}
          <View style={styles.cardRow}>
            <View style={styles.cardCol}>
              <View style={styles.statCard}>
                <View style={styles.statHeaderRow}>
                  <Text style={styles.statValue}>{totalRelayed.toString()}</Text>
                  <Ionicons name="git-network-outline" size={20} color={Colors.Signal} />
                </View>
                <Text style={styles.statLabel}>TOTAL RELAYED MESSAGES</Text>
                <Text style={styles.statSub}>Traversed this mesh node</Text>
              </View>
            </View>

            <View style={styles.cardCol}>
              <View style={styles.statCard}>
                <View style={styles.statHeaderRow}>
                  <Text style={styles.statValue}>{connectedDevices.length.toString()}</Text>
                  <Ionicons name="cellular-outline" size={20} color={Colors.Signal} />
                </View>
                <Text style={styles.statLabel}>ACTIVE MESH PEERS</Text>
                <Text style={styles.statSub}>Direct radio range</Text>
              </View>
            </View>
          </View>

          {/* Second Row: Relayed Through vs Relayed By */}
          <View style={styles.cardRow}>
            <View style={styles.cardCol}>
              <TouchableOpacity
                activeOpacity={0.8}
                onPress={() => setActiveFilter(activeFilter === 'THROUGH' ? 'ALL' : 'THROUGH')}
                style={[
                  styles.statCard,
                  activeFilter === 'THROUGH' && styles.statCardActiveCyan,
                ]}
              >
                <View style={styles.statHeaderRow}>
                  <Text style={[styles.statValue, { color: Colors.Signal }]}>
                    {relayedThroughCount.toString()}
                  </Text>
                  <View style={styles.iconChipCyan}>
                    <Ionicons name="swap-horizontal" size={15} color={Colors.Signal} />
                  </View>
                </View>
                <Text style={styles.statLabel}>RELAYED THROUGH DEVICE</Text>
                <Text style={styles.statSub}>Transit hops forwarded</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.cardCol}>
              <TouchableOpacity
                activeOpacity={0.8}
                onPress={() => setActiveFilter(activeFilter === 'BY' ? 'ALL' : 'BY')}
                style={[
                  styles.statCard,
                  activeFilter === 'BY' && styles.statCardActiveAmber,
                ]}
              >
                <View style={styles.statHeaderRow}>
                  <Text style={[styles.statValue, { color: Colors.Amber }]}>
                    {relayedByCount.toString()}
                  </Text>
                  <View style={styles.iconChipAmber}>
                    <Ionicons name="radio" size={15} color={Colors.Amber} />
                  </View>
                </View>
                <Text style={styles.statLabel}>RELAYED BY THIS DEVICE</Text>
                <Text style={styles.statSub}>Broadcast from this node</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* MAIN SECTION: Relayed Messages Log with Click-to-Inspect */}
          <View style={styles.sectionHeaderContainer}>
            <View>
              <Text style={styles.sectionEyebrow}>DEVICE RELAY TELEMETRY LOG</Text>
              <Text style={styles.sectionSubtitle}>
                Click any message to inspect full content, identity & routing data
              </Text>
            </View>
          </View>

          {/* Filter Tabs & Search Bar */}
          <View style={styles.controlsCard}>
            {/* Tabs */}
            <View style={styles.tabBar}>
              <TouchableOpacity
                activeOpacity={0.7}
                style={[styles.tabButton, activeFilter === 'ALL' && styles.tabButtonActive]}
                onPress={() => setActiveFilter('ALL')}
              >
                <Text
                  style={[
                    styles.tabButtonText,
                    activeFilter === 'ALL' && styles.tabButtonTextActive,
                  ]}
                >
                  All ({totalRelayed})
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                activeOpacity={0.7}
                style={[styles.tabButton, activeFilter === 'THROUGH' && styles.tabButtonActive]}
                onPress={() => setActiveFilter('THROUGH')}
              >
                <View style={styles.tabBadgeRow}>
                  <View style={[styles.miniDot, { backgroundColor: Colors.Signal }]} />
                  <Text
                    style={[
                      styles.tabButtonText,
                      activeFilter === 'THROUGH' && styles.tabButtonTextActive,
                    ]}
                  >
                    Through Device ({relayedThroughCount})
                  </Text>
                </View>
              </TouchableOpacity>

              <TouchableOpacity
                activeOpacity={0.7}
                style={[styles.tabButton, activeFilter === 'BY' && styles.tabButtonActive]}
                onPress={() => setActiveFilter('BY')}
              >
                <View style={styles.tabBadgeRow}>
                  <View style={[styles.miniDot, { backgroundColor: Colors.Amber }]} />
                  <Text
                    style={[
                      styles.tabButtonText,
                      activeFilter === 'BY' && styles.tabButtonTextActive,
                    ]}
                  >
                    By Device ({relayedByCount})
                  </Text>
                </View>
              </TouchableOpacity>
            </View>

            {/* Search Input */}
            <View style={styles.searchInputContainer}>
              <Ionicons name="search" size={16} color={Colors.Slate} style={{ marginRight: 8 }} />
              <TextInput
                style={styles.searchInput}
                placeholder="Search by sender, device name, or message text..."
                placeholderTextColor={Colors.Slate}
                value={searchQuery}
                onChangeText={setSearchQuery}
                autoCapitalize="none"
                clearButtonMode="while-editing"
              />
              {searchQuery.length > 0 && (
                <TouchableOpacity onPress={() => setSearchQuery('')}>
                  <Ionicons name="close-circle" size={18} color={Colors.Slate} />
                </TouchableOpacity>
              )}
            </View>
          </View>

          {/* Relayed Messages List */}
          {filteredPackets.length === 0 ? (
            <View style={styles.emptyStateCard}>
              <Ionicons name="radio-outline" size={42} color={Colors.Slate} />
              <Text style={styles.emptyStateTitle}>No Relayed Messages Yet</Text>
              <Text style={styles.emptyStateDesc}>
                {searchQuery
                  ? `No messages matching "${searchQuery}".`
                  : 'Messages will appear here in real time as they are broadcast by this device or relayed through nearby mesh peers.'}
              </Text>
            </View>
          ) : (
            filteredPackets.map((packet) => {
              const isThrough = getPacketDirection(packet) === 'THROUGH_THIS_DEVICE';
              const isCritical = packet.severity === 'CRITICAL';
              const isHigh = packet.severity === 'HIGH';

              return (
                <TouchableOpacity
                  key={packet.id}
                  activeOpacity={0.82}
                  onPress={() => setSelectedPacket(packet)}
                  style={[
                    styles.relayCard,
                    isThrough ? styles.relayCardThrough : styles.relayCardBy,
                  ]}
                >
                  {/* Top Bar: Direction Badge, Severity, and Time */}
                  <View style={styles.relayCardHeader}>
                    <View style={styles.relayDirectionBadgeGroup}>
                      <View
                        style={[
                          styles.directionPill,
                          isThrough ? styles.directionPillThrough : styles.directionPillBy,
                        ]}
                      >
                        <Ionicons
                          name={isThrough ? 'swap-horizontal' : 'radio'}
                          size={13}
                          color={isThrough ? Colors.Signal : Colors.Amber}
                          style={{ marginRight: 4 }}
                        />
                        <Text
                          style={[
                            styles.directionPillText,
                            { color: isThrough ? Colors.Signal : Colors.Amber },
                          ]}
                        >
                          {isThrough ? 'RELAYED THROUGH THIS DEVICE' : 'RELAYED BY THIS DEVICE'}
                        </Text>
                      </View>
                    </View>

                    <View style={styles.headerRightBadges}>
                      <View
                        style={[
                          styles.severityPill,
                          {
                            backgroundColor: isCritical
                              ? 'rgba(255, 79, 62, 0.18)'
                              : isHigh
                              ? 'rgba(255, 178, 62, 0.18)'
                              : 'rgba(53, 224, 200, 0.18)',
                            borderColor: isCritical
                              ? Colors.Ember
                              : isHigh
                              ? Colors.Amber
                              : Colors.Signal,
                          },
                        ]}
                      >
                        <Text
                          style={[
                            styles.severityPillText,
                            {
                              color: isCritical
                                ? Colors.Ember
                                : isHigh
                                ? Colors.Amber
                                : Colors.Signal,
                            },
                          ]}
                        >
                          {packet.severity}
                        </Text>
                      </View>

                      <Text style={styles.timeAgoText}>{formatTimeAgo(packet.createdAtEpochMillis)}</Text>
                    </View>
                  </View>

                  {/* Identification Row: Sender & Target */}
                  <View style={styles.identificationContainer}>
                    <View style={styles.senderBlock}>
                      <View style={styles.avatarMini}>
                        <Ionicons
                          name={isThrough ? 'person' : 'person-circle'}
                          size={16}
                          color={isThrough ? Colors.Signal : Colors.Amber}
                        />
                      </View>
                      <View style={{ flex: 1 }}>
                        <Text style={styles.senderNameText} numberOfLines={1}>
                          {packet.senderName}
                        </Text>
                        <Text style={styles.senderDeviceText} numberOfLines={1}>
                          {packet.senderDeviceName}
                        </Text>
                      </View>
                    </View>

                    <View style={styles.routingArrowBox}>
                      <Ionicons name="arrow-forward" size={16} color={Colors.Slate} />
                    </View>

                    <View style={styles.targetBlock}>
                      <Text style={styles.targetLabel}>TARGET DESTINATION</Text>
                      <Text style={styles.targetDeviceText} numberOfLines={1}>
                        {packet.targetDeviceName}
                      </Text>
                    </View>
                  </View>

                  {/* Multi-Device Relay Chain Visualization */}
                  <View style={styles.relayChainContainer}>
                    <Text style={styles.relayChainTitle}>RELAY ROUTE HOPS ({packet.relayPath.length} NODES):</Text>
                    <ScrollView
                      horizontal
                      showsHorizontalScrollIndicator={false}
                      contentContainerStyle={styles.relayChainRow}
                    >
                      {packet.relayPath.map((nodeName, idx) => {
                        const isThisNode =
                          nodeName.toLowerCase().includes('this device') ||
                          nodeName === thisDeviceName;

                        return (
                          <React.Fragment key={`${packet.id}-node-${idx}`}>
                            <View
                              style={[
                                styles.nodePill,
                                isThisNode && styles.nodePillCurrentDevice,
                              ]}
                            >
                              {isThisNode && (
                                <Ionicons
                                  name="shield-checkmark"
                                  size={11}
                                  color={Colors.Signal}
                                  style={{ marginRight: 3 }}
                                />
                              )}
                              <Text
                                style={[
                                  styles.nodePillText,
                                  isThisNode && styles.nodePillTextCurrent,
                                ]}
                              >
                                {isThisNode ? `★ ${nodeName}` : nodeName}
                              </Text>
                            </View>

                            {idx < packet.relayPath.length - 1 && (
                              <Ionicons
                                name="chevron-forward"
                                size={12}
                                color={Colors.Slate}
                                style={{ marginHorizontal: 3 }}
                              />
                            )}
                          </React.Fragment>
                        );
                      })}
                    </ScrollView>
                  </View>

                  {/* Clickable Message Content Highlight Box */}
                  <View style={styles.messageExcerptBox}>
                    <View style={styles.quoteIconRow}>
                      <Ionicons name="chatbubble-ellipses-outline" size={15} color={Colors.Signal} />
                      <Text style={styles.messageBoxLabel}>RELAYED MESSAGE PAYLOAD</Text>
                      <View style={styles.clickCueBadge}>
                        <Text style={styles.clickCueText}>TAP TO EXPAND</Text>
                        <Ionicons name="chevron-forward" size={12} color={Colors.Signal} />
                      </View>
                    </View>
                    <Text style={styles.messageContentText} numberOfLines={2}>
                      "{packet.customMessage}"
                    </Text>
                  </View>

                  {/* Bottom Strip: GPS Coordinates, Transport, Signal & Tap CTA */}
                  <View style={styles.relayCardBottomStrip}>
                    <View style={styles.telemetryTagGroup}>
                      <View style={styles.telemetryTag}>
                        <Ionicons name="location-outline" size={12} color={Colors.Slate} />
                        <Text style={styles.telemetryTagText}>
                          {packet.latitude.toFixed(3)}°, {packet.longitude.toFixed(3)}°
                        </Text>
                      </View>

                      <View style={styles.telemetryTag}>
                        <Ionicons name="wifi-outline" size={12} color={Colors.Slate} />
                        <Text style={styles.telemetryTagText}>{packet.transport.replace('_', ' ')}</Text>
                      </View>

                      {packet.rssi && (
                        <View style={styles.telemetryTag}>
                          <Ionicons name="pulse-outline" size={12} color={Colors.Slate} />
                          <Text style={styles.telemetryTagText}>{packet.rssi}</Text>
                        </View>
                      )}
                    </View>

                    <View style={styles.inspectCta}>
                      <Text style={styles.inspectCtaText}>Inspect Data</Text>
                      <Ionicons name="open-outline" size={13} color={Colors.Signal} />
                    </View>
                  </View>
                </TouchableOpacity>
              );
            })
          )}

          {/* Connected Mesh Peers Breakdown */}
          <View style={styles.breakdownCard}>
            <View style={styles.breakdownHeaderRow}>
              <Text style={styles.breakdownTitle}>CONNECTED MESH PEERS & RELAYS</Text>
              <Text style={styles.breakdownBadge}>{connectedDevices.length} ACTIVE</Text>
            </View>

            {connectedDevices.length === 0 ? (
              <View style={styles.noPeersBox}>
                <Ionicons name="radio" size={24} color={Colors.Slate} />
                <Text style={styles.noPeersText}>
                  No external peer nodes broadcasting in local range currently. Run ResQLink in another tab/device to cluster via mesh!
                </Text>
              </View>
            ) : (
              connectedDevices.map((device) => (
                <View key={device.id} style={styles.deviceRelayRow}>
                  <View style={styles.deviceInfoCol}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                      <Text style={styles.deviceRowName}>{device.deviceName}</Text>
                      <View style={styles.peerRoleBadge}>
                        <Text style={styles.peerRoleText}>{device.role}</Text>
                      </View>
                    </View>
                    <Text style={styles.deviceRowSub}>
                      {device.ownerName} · {device.distanceMeters}m · {device.signalStrength} · Batt: {device.batteryPercent}%
                    </Text>
                    {device.lastRelayedPacket && (
                      <Text style={styles.deviceRelayMsg} numberOfLines={2}>
                        Last Relayed: "{device.lastRelayedPacket.customMessage}"
                      </Text>
                    )}
                  </View>
                  <View style={styles.deviceStatusBadge}>
                    <Text
                      style={[
                        styles.deviceStatusText,
                        { color: device.lastRelayedPacket ? Colors.Signal : Colors.Slate },
                      ]}
                    >
                      {device.lastRelayedPacket ? 'RELAYED' : 'STANDBY'}
                    </Text>
                  </View>
                </View>
              ))
            )}
          </View>

          {/* Hazard Severity Distribution */}
          <View style={styles.breakdownCard}>
            <Text style={styles.breakdownTitle}>HAZARD SEVERITY BREAKDOWN</Text>

            <View style={styles.breakdownRow}>
              <View style={styles.breakdownLabelGroup}>
                <View style={[styles.bulletDot, { backgroundColor: Colors.Ember }]} />
                <Text style={styles.breakdownLabel}>Extreme / High Hazards</Text>
              </View>
              <Text style={[styles.breakdownCount, { color: Colors.Ember }]}>{extremeHazards}</Text>
            </View>

            <View style={styles.breakdownRow}>
              <View style={styles.breakdownLabelGroup}>
                <View style={[styles.bulletDot, { backgroundColor: Colors.Amber }]} />
                <Text style={styles.breakdownLabel}>Moderate Severity</Text>
              </View>
              <Text style={[styles.breakdownCount, { color: Colors.Amber }]}>{moderateHazards}</Text>
            </View>

            <View style={styles.breakdownRow}>
              <View style={styles.breakdownLabelGroup}>
                <View style={[styles.bulletDot, { backgroundColor: Colors.Moss }]} />
                <Text style={styles.breakdownLabel}>Low / Advisory</Text>
              </View>
              <Text style={[styles.breakdownCount, { color: Colors.Moss }]}>{lowHazards}</Text>
            </View>
          </View>
        </View>

        <View style={{ height: Spacing.xl }} />
      </ScrollView>

      {/* FULL MESSAGE & RELAY TELEMETRY INSPECTION MODAL */}
      <Modal
        visible={selectedPacket !== null}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setSelectedPacket(null)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            {selectedPacket && (
              <ScrollView
                style={styles.modalScroll}
                contentContainerStyle={styles.modalContent}
                showsVerticalScrollIndicator={false}
              >
                {/* Modal Header */}
                <View style={styles.modalHeaderRow}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.modalEyebrow}>RELAYED PACKET TELEMETRY</Text>
                    <Text style={styles.modalTitle}>Message & Hop Inspector</Text>
                  </View>

                  <TouchableOpacity
                    style={styles.modalCloseBtn}
                    onPress={() => setSelectedPacket(null)}
                  >
                    <Ionicons name="close" size={22} color="#FFFFFF" />
                  </TouchableOpacity>
                </View>

                {/* Packet Direction Banner */}
                {(() => {
                  const isThrough = getPacketDirection(selectedPacket) === 'THROUGH_THIS_DEVICE';
                  return (
                    <View
                      style={[
                        styles.modalBanner,
                        isThrough ? styles.modalBannerThrough : styles.modalBannerBy,
                      ]}
                    >
                      <Ionicons
                        name={isThrough ? 'swap-horizontal' : 'radio'}
                        size={20}
                        color={isThrough ? Colors.Signal : Colors.Amber}
                        style={{ marginRight: 10 }}
                      />
                      <View style={{ flex: 1 }}>
                        <Text
                          style={[
                            styles.modalBannerTitle,
                            { color: isThrough ? Colors.Signal : Colors.Amber },
                          ]}
                        >
                          {isThrough
                            ? 'RELAYED THROUGH THIS DEVICE'
                            : 'RELAYED BY THIS DEVICE'}
                        </Text>
                        <Text style={styles.modalBannerSub}>
                          {isThrough
                            ? `Received from peer and routed through ${thisDeviceName} via BLE Mesh.`
                            : `Originated by ${thisDeviceName} and broadcast across mesh cluster.`}
                        </Text>
                      </View>
                    </View>
                  );
                })()}

                {/* Full Relayed Message Section */}
                <View style={styles.modalSectionCard}>
                  <View style={styles.modalSectionHeader}>
                    <Ionicons name="chatbubbles" size={18} color={Colors.Signal} />
                    <Text style={styles.modalSectionTitle}>FULL RELAYED MESSAGE CONTENT</Text>
                  </View>

                  <View style={styles.fullMessageQuoteBox}>
                    <Text style={styles.fullMessageQuoteMark}>“</Text>
                    <Text style={styles.fullMessageText}>
                      {selectedPacket.customMessage}
                    </Text>
                  </View>

                  <View style={styles.messageMetaRow}>
                    <View style={styles.metaBadge}>
                      <Text style={styles.metaBadgeLabel}>SEVERITY:</Text>
                      <Text
                        style={[
                          styles.metaBadgeVal,
                          {
                            color:
                              selectedPacket.severity === 'CRITICAL'
                                ? Colors.Ember
                                : selectedPacket.severity === 'HIGH'
                                ? Colors.Amber
                                : Colors.Signal,
                          },
                        ]}
                      >
                        {selectedPacket.severity}
                      </Text>
                    </View>

                    <View style={styles.metaBadge}>
                      <Text style={styles.metaBadgeLabel}>STATUS:</Text>
                      <Text style={[styles.metaBadgeVal, { color: Colors.Moss }]}>
                        {selectedPacket.deliveryStatus}
                      </Text>
                    </View>

                    <View style={styles.metaBadge}>
                      <Text style={styles.metaBadgeLabel}>TRANSMITTED:</Text>
                      <Text style={styles.metaBadgeVal}>
                        {formatExactTime(selectedPacket.createdAtEpochMillis)}
                      </Text>
                    </View>
                  </View>
                </View>

                {/* Device-by-Device Multi-Hop Pathway */}
                <View style={styles.modalSectionCard}>
                  <View style={styles.modalSectionHeader}>
                    <Ionicons name="git-commit" size={18} color={Colors.Signal} />
                    <Text style={styles.modalSectionTitle}>RELAY ROUTE & MESH NODES CHAIN</Text>
                  </View>

                  <Text style={styles.modalSectionDesc}>
                    Visual trace of devices through which this message traversed:
                  </Text>

                  <View style={styles.hopTimeline}>
                    {selectedPacket.relayPath.map((nodeName, idx) => {
                      const isFirst = idx === 0;
                      const isLast = idx === selectedPacket.relayPath.length - 1;
                      const isThisDevice =
                        nodeName.toLowerCase().includes('this device') ||
                        nodeName === thisDeviceName;

                      return (
                        <View key={`timeline-node-${idx}`} style={styles.timelineRow}>
                          <View style={styles.timelineIndicatorCol}>
                            <View
                              style={[
                                styles.timelineDot,
                                isThisDevice
                                  ? styles.timelineDotCurrent
                                  : isFirst
                                  ? styles.timelineDotOrigin
                                  : styles.timelineDotPeer,
                              ]}
                            >
                              <Ionicons
                                name={
                                  isFirst
                                    ? 'radio'
                                    : isThisDevice
                                    ? 'shield-checkmark'
                                    : 'navigate'
                                }
                                size={12}
                                color="#FFFFFF"
                              />
                            </View>
                            {!isLast && <View style={styles.timelineLine} />}
                          </View>

                          <View style={styles.timelineContentCol}>
                            <View style={styles.timelineTitleRow}>
                              <Text
                                style={[
                                  styles.timelineNodeName,
                                  isThisDevice && { color: Colors.Signal, fontWeight: '700' },
                                ]}
                              >
                                {nodeName}
                              </Text>
                              <View
                                style={[
                                  styles.hopBadge,
                                  isThisDevice && styles.hopBadgeCurrent,
                                ]}
                              >
                                <Text style={styles.hopBadgeText}>
                                  {isFirst
                                    ? 'ORIGIN'
                                    : isThisDevice
                                    ? 'THIS NODE (TRANSIT)'
                                    : isLast
                                    ? 'DESTINATION'
                                    : `HOP ${idx}`}
                                </Text>
                              </View>
                            </View>
                            <Text style={styles.timelineNodeSub}>
                              {isFirst
                                ? `Created and broadcasted by ${selectedPacket.senderName}`
                                : isThisDevice
                                ? `Received over BLE Mesh, verified payload, and forwarded`
                                : `Target receiver & processing station`}
                            </Text>
                          </View>
                        </View>
                      );
                    })}
                  </View>
                </View>

                {/* Sender & Target Identification Grid */}
                <View style={styles.modalSectionCard}>
                  <View style={styles.modalSectionHeader}>
                    <Ionicons name="id-card" size={18} color={Colors.Signal} />
                    <Text style={styles.modalSectionTitle}>DEVICE & SENDER IDENTIFICATION</Text>
                  </View>

                  <View style={styles.gridContainer}>
                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>SENDER NAME</Text>
                      <Text style={styles.gridValue}>{selectedPacket.senderName}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>SENDER PHONE</Text>
                      <Text style={styles.gridValue}>{selectedPacket.senderPhone || 'Not provided'}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>SENDER DEVICE MODEL</Text>
                      <Text style={styles.gridValue}>{selectedPacket.senderDeviceName}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>SENDER NODE ID</Text>
                      <Text style={[styles.gridValue, styles.monoText]}>{selectedPacket.senderId}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>TARGET RECIPIENT</Text>
                      <Text style={styles.gridValue}>{selectedPacket.targetDeviceName}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>TARGET NODE ID</Text>
                      <Text style={[styles.gridValue, styles.monoText]}>{selectedPacket.targetDeviceId}</Text>
                    </View>
                  </View>
                </View>

                {/* Geographic & Medical Triage Data Relayed */}
                <View style={styles.modalSectionCard}>
                  <View style={styles.modalSectionHeader}>
                    <Ionicons name="medkit" size={18} color={Colors.Signal} />
                    <Text style={styles.modalSectionTitle}>RELAYED TRIAGE & LOCATION DATA</Text>
                  </View>

                  <View style={styles.gridContainer}>
                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>GPS COORDINATES</Text>
                      <Text style={[styles.gridValue, styles.monoText]}>
                        {selectedPacket.latitude.toFixed(5)}°, {selectedPacket.longitude.toFixed(5)}°
                      </Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>LOCATION ACCURACY</Text>
                      <Text style={styles.gridValue}>±{selectedPacket.accuracyMeters ?? 10} meters</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>BLOOD GROUP</Text>
                      <Text style={[styles.gridValue, { color: Colors.Ember, fontWeight: 'bold' }]}>
                        {selectedPacket.bloodGroup || 'Not Specified'}
                      </Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>EMERGENCY CONTACT</Text>
                      <Text style={styles.gridValue}>{selectedPacket.emergencyContact || 'None'}</Text>
                    </View>

                    <View style={[styles.gridItem, { width: '100%' }]}>
                      <Text style={styles.gridLabel}>MEDICAL & RESCUE NOTES</Text>
                      <Text style={styles.gridValue}>
                        {selectedPacket.medicalNotes || 'No specific medical notes provided in payload.'}
                      </Text>
                    </View>
                  </View>
                </View>

                {/* Mesh Transport Diagnostics */}
                <View style={styles.modalSectionCard}>
                  <View style={styles.modalSectionHeader}>
                    <Ionicons name="hardware-chip" size={18} color={Colors.Signal} />
                    <Text style={styles.modalSectionTitle}>RADIO & TRANSPORT DIAGNOSTICS</Text>
                  </View>

                  <View style={styles.gridContainer}>
                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>TRANSPORT LAYER</Text>
                      <Text style={styles.gridValue}>{selectedPacket.transport}</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>SIGNAL STRENGTH (RSSI)</Text>
                      <Text style={[styles.gridValue, { color: Colors.Signal }]}>
                        {selectedPacket.rssi || '-54 dBm'}
                      </Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>TOTAL HOPS TRAVERSED</Text>
                      <Text style={styles.gridValue}>{selectedPacket.hopCount} Hops</Text>
                    </View>

                    <View style={styles.gridItem}>
                      <Text style={styles.gridLabel}>PACKET ID</Text>
                      <Text style={[styles.gridValue, styles.monoText]}>{selectedPacket.id}</Text>
                    </View>
                  </View>
                </View>

                {/* Modal Action Buttons */}
                <View style={styles.modalActionsRow}>
                  <TouchableOpacity
                    style={styles.modalActionReRelayBtn}
                    onPress={() => handleReRelay(selectedPacket)}
                  >
                    <Ionicons name="repeat" size={18} color="#0B0F17" style={{ marginRight: 6 }} />
                    <Text style={styles.modalActionReRelayText}>Re-relay via Mesh</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={styles.modalActionCloseBtn}
                    onPress={() => setSelectedPacket(null)}
                  >
                    <Text style={styles.modalActionCloseText}>Close Inspector</Text>
                  </TouchableOpacity>
                </View>

                <View style={{ height: Spacing.xl }} />
              </ScrollView>
            )}
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
    padding: Spacing.screenPadding,
  },

  // Feedback banner
  feedbackBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(53, 224, 200, 0.12)',
    borderWidth: 1,
    borderColor: Colors.Signal,
    borderRadius: Shapes.medium,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    marginHorizontal: Spacing.screenPadding,
    marginTop: Spacing.sm,
    gap: 8,
  },
  feedbackText: {
    ...Typography.bodyMedium,
    color: Colors.Signal,
    flex: 1,
    fontWeight: '500',
  },

  // Simulation test action bar
  simulationBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.InkRaised,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    marginBottom: Spacing.md,
    flexWrap: 'wrap',
    gap: 8,
  },
  simulationLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  livePulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.Signal,
  },
  simulationLabel: {
    ...EyebrowStyle,
    fontSize: 10,
    color: Colors.Signal,
  },
  simulationButtonsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  simPillButtonCyan: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.Signal,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Shapes.small,
    gap: 4,
  },
  simPillTextCyan: {
    ...EyebrowStyle,
    color: '#0B0F17',
    fontSize: 10,
    fontWeight: '700',
  },
  simPillButtonOutline: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 178, 62, 0.12)',
    borderWidth: 1,
    borderColor: Colors.Amber,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Shapes.small,
    gap: 4,
  },
  simPillTextAmber: {
    ...EyebrowStyle,
    color: Colors.Amber,
    fontSize: 10,
    fontWeight: '700',
  },
  simPillButtonClear: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: Shapes.small,
    gap: 4,
  },
  simPillTextClear: {
    ...EyebrowStyle,
    color: Colors.Slate,
    fontSize: 10,
    fontWeight: '600',
  },

  // Top Stat Cards
  cardRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  cardCol: {
    flex: 1,
  },
  statCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
  },
  statCardActiveCyan: {
    borderColor: Colors.Signal,
    backgroundColor: 'rgba(53, 224, 200, 0.08)',
  },
  statCardActiveAmber: {
    borderColor: Colors.Amber,
    backgroundColor: 'rgba(255, 178, 62, 0.08)',
  },
  statHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xxs,
  },
  statValue: {
    ...Typography.headlineMedium,
    ...DataReadoutStyle,
    fontSize: 26,
    lineHeight: 30,
    color: Colors.Signal,
  },
  iconChipCyan: {
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: 'rgba(53, 224, 200, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconChipAmber: {
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: 'rgba(255, 178, 62, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  statLabel: {
    ...EyebrowStyle,
    fontSize: 10,
    color: Colors.DarkOnSurface,
    marginTop: 2,
  },
  statSub: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
    marginTop: 2,
  },

  // Section Header
  sectionHeaderContainer: {
    marginTop: Spacing.md,
    marginBottom: Spacing.xs,
  },
  sectionEyebrow: {
    ...EyebrowStyle,
    color: Colors.Signal,
    fontSize: 12,
  },
  sectionSubtitle: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    marginTop: 2,
  },

  // Controls (Tabs & Search)
  controlsCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.sm,
    marginTop: Spacing.xs,
    marginBottom: Spacing.sm,
  },
  tabBar: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: Spacing.xs,
  },
  tabButton: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: Shapes.medium,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.InkRaised,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  tabButtonActive: {
    backgroundColor: Colors.SignalDeep,
    borderColor: Colors.Signal,
  },
  tabButtonText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
    fontWeight: '600',
  },
  tabButtonTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  tabBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  miniDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },

  searchInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.Void,
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Platform.OS === 'ios' ? 8 : 4,
  },
  searchInput: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 13,
  },

  // Empty state
  emptyStateCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.sm,
  },
  emptyStateTitle: {
    ...Typography.titleMedium,
    color: '#FFFFFF',
    marginTop: Spacing.sm,
  },
  emptyStateDesc: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    textAlign: 'center',
    marginTop: 4,
    maxWidth: 280,
  },
  emptyActionBtn: {
    marginTop: Spacing.md,
    backgroundColor: Colors.SignalDeep,
    borderWidth: 1,
    borderColor: Colors.Signal,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: Shapes.medium,
  },
  emptyActionBtnText: {
    ...Typography.labelMedium,
    color: Colors.Signal,
    fontWeight: '600',
  },

  // Relayed Message Card
  relayCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
    marginTop: Spacing.sm,
  },
  relayCardThrough: {
    borderLeftWidth: 3,
    borderLeftColor: Colors.Signal,
  },
  relayCardBy: {
    borderLeftWidth: 3,
    borderLeftColor: Colors.Amber,
  },
  relayCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.xs,
    flexWrap: 'wrap',
    gap: 6,
  },
  relayDirectionBadgeGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  directionPill: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Shapes.small,
  },
  directionPillThrough: {
    backgroundColor: 'rgba(53, 224, 200, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(53, 224, 200, 0.4)',
  },
  directionPillBy: {
    backgroundColor: 'rgba(255, 178, 62, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(255, 178, 62, 0.4)',
  },
  directionPillText: {
    ...EyebrowStyle,
    fontSize: 9,
    fontWeight: '700',
  },
  headerRightBadges: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  severityPill: {
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: Shapes.small,
    borderWidth: 1,
  },
  severityPillText: {
    ...EyebrowStyle,
    fontSize: 9,
    fontWeight: '700',
  },
  timeAgoText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
  },

  // Identification row inside card
  identificationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.InkRaised,
    borderRadius: Shapes.medium,
    padding: Spacing.sm,
    marginTop: Spacing.xxs,
    marginBottom: Spacing.xs,
  },
  senderBlock: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  avatarMini: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: Colors.Void,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
  },
  senderNameText: {
    ...Typography.titleSmall,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  senderDeviceText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
  },
  routingArrowBox: {
    paddingHorizontal: 6,
  },
  targetBlock: {
    flex: 1,
    alignItems: 'flex-end',
  },
  targetLabel: {
    ...EyebrowStyle,
    fontSize: 8,
    color: Colors.Slate,
  },
  targetDeviceText: {
    ...Typography.titleSmall,
    fontSize: 12,
    color: Colors.Signal,
    fontWeight: '600',
    marginTop: 1,
  },

  // Multi-Device Relay Chain Visualization
  relayChainContainer: {
    marginTop: 4,
    marginBottom: 8,
  },
  relayChainTitle: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Slate,
    marginBottom: 4,
  },
  relayChainRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  nodePill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.small,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  nodePillCurrentDevice: {
    backgroundColor: Colors.SignalDeep,
    borderColor: Colors.Signal,
  },
  nodePillText: {
    ...Typography.bodySmall,
    fontSize: 10,
    color: Colors.Slate,
    fontWeight: '500',
  },
  nodePillTextCurrent: {
    color: '#FFFFFF',
    fontWeight: '700',
  },

  // Message excerpt
  messageExcerptBox: {
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    padding: Spacing.sm,
    marginTop: 4,
    marginBottom: Spacing.xs,
  },
  quoteIconRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  messageBoxLabel: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Signal,
    flex: 1,
    marginLeft: 6,
  },
  clickCueBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(53, 224, 200, 0.1)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Shapes.small,
    gap: 2,
  },
  clickCueText: {
    ...EyebrowStyle,
    fontSize: 8,
    color: Colors.Signal,
    fontWeight: 'bold',
  },
  messageContentText: {
    ...Typography.bodyMedium,
    color: Colors.DarkOnSurface,
    fontStyle: 'italic',
    lineHeight: 18,
  },

  // Bottom Strip
  relayCardBottomStrip: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: Colors.HairlineDark,
    paddingTop: Spacing.xs,
    marginTop: 2,
  },
  telemetryTagGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flexWrap: 'wrap',
  },
  telemetryTag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  telemetryTagText: {
    ...DataReadoutStyle,
    fontSize: 11,
    color: Colors.Slate,
  },
  inspectCta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  inspectCtaText: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Signal,
    fontWeight: '600',
  },

  // Connected peers list styles
  breakdownCard: {
    backgroundColor: Colors.DarkSurface,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
    marginTop: Spacing.md,
  },
  breakdownHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.md,
  },
  breakdownTitle: {
    ...EyebrowStyle,
    color: Colors.Slate,
  },
  breakdownBadge: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Signal,
    backgroundColor: Colors.SignalDeep,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Shapes.small,
  },
  noPeersBox: {
    alignItems: 'center',
    paddingVertical: Spacing.md,
    gap: 6,
  },
  noPeersText: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    textAlign: 'center',
    maxWidth: 280,
  },
  deviceRelayRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.HairlineDark,
  },
  deviceInfoCol: {
    flex: 1,
    paddingRight: Spacing.sm,
  },
  deviceRowName: {
    ...Typography.titleSmall,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  peerRoleBadge: {
    backgroundColor: Colors.InkRaised,
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 4,
  },
  peerRoleText: {
    ...EyebrowStyle,
    fontSize: 8,
    color: Colors.Slate,
  },
  deviceRowSub: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
    marginTop: 2,
  },
  deviceRelayMsg: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Signal,
    fontStyle: 'italic',
    marginTop: 3,
  },
  deviceStatusBadge: {
    backgroundColor: Colors.InkRaised,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Shapes.small,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
  },
  deviceStatusText: {
    ...EyebrowStyle,
    fontSize: 9,
    fontWeight: 'bold',
  },

  // Hazard breakdown styles
  breakdownRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.xs,
    borderBottomWidth: 1,
    borderBottomColor: Colors.HairlineDark,
  },
  breakdownLabelGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bulletDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: Spacing.sm,
  },
  breakdownLabel: {
    ...Typography.bodyMedium,
    color: Colors.DarkOnSurface,
  },
  breakdownCount: {
    ...DataReadoutStyle,
    fontSize: 15,
    fontWeight: 'bold',
  },

  // MODAL STYLES
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(6, 9, 15, 0.85)',
    justifyContent: 'flex-end',
  },
  modalContainer: {
    backgroundColor: Colors.DarkSurface,
    borderTopLeftRadius: Shapes.extraLarge,
    borderTopRightRadius: Shapes.extraLarge,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    maxHeight: '90%',
    width: '100%',
  },
  modalScroll: {
    flexGrow: 0,
  },
  modalContent: {
    padding: Spacing.lg,
  },
  modalHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: Spacing.md,
  },
  modalEyebrow: {
    ...EyebrowStyle,
    color: Colors.Signal,
    fontSize: 11,
  },
  modalTitle: {
    ...Typography.titleLarge,
    color: '#FFFFFF',
    fontWeight: '700',
    marginTop: 2,
  },
  modalCloseBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.InkRaised,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
  },

  // Modal direction banner
  modalBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: Shapes.large,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    borderWidth: 1,
  },
  modalBannerThrough: {
    backgroundColor: 'rgba(53, 224, 200, 0.1)',
    borderColor: Colors.Signal,
  },
  modalBannerBy: {
    backgroundColor: 'rgba(255, 178, 62, 0.1)',
    borderColor: Colors.Amber,
  },
  modalBannerTitle: {
    ...EyebrowStyle,
    fontSize: 11,
    fontWeight: '700',
  },
  modalBannerSub: {
    ...Typography.bodySmall,
    color: Colors.DarkOnSurface,
    marginTop: 2,
  },

  // Modal Section Card
  modalSectionCard: {
    backgroundColor: Colors.InkRaised,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.md,
    marginBottom: Spacing.md,
  },
  modalSectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: Spacing.sm,
  },
  modalSectionTitle: {
    ...EyebrowStyle,
    color: Colors.Signal,
    fontSize: 11,
  },
  modalSectionDesc: {
    ...Typography.bodySmall,
    color: Colors.Slate,
    marginBottom: Spacing.sm,
  },

  // Full message quote box
  fullMessageQuoteBox: {
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
    position: 'relative',
  },
  fullMessageQuoteMark: {
    position: 'absolute',
    top: 4,
    left: 8,
    fontSize: 32,
    lineHeight: 32,
    color: 'rgba(53, 224, 200, 0.3)',
    fontWeight: 'bold',
  },
  fullMessageText: {
    ...Typography.bodyLarge,
    color: '#FFFFFF',
    fontSize: 15,
    lineHeight: 22,
    fontWeight: '500',
    paddingLeft: 14,
  },
  messageMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 8,
  },
  metaBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  metaBadgeLabel: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Slate,
  },
  metaBadgeVal: {
    ...DataReadoutStyle,
    fontSize: 11,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },

  // Hop timeline
  hopTimeline: {
    paddingLeft: 4,
  },
  timelineRow: {
    flexDirection: 'row',
    minHeight: 52,
  },
  timelineIndicatorCol: {
    alignItems: 'center',
    width: 28,
    marginRight: 10,
  },
  timelineDot: {
    width: 24,
    height: 24,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  timelineDotOrigin: {
    backgroundColor: Colors.Amber,
  },
  timelineDotCurrent: {
    backgroundColor: Colors.Signal,
  },
  timelineDotPeer: {
    backgroundColor: Colors.SlateLight,
  },
  timelineLine: {
    flex: 1,
    width: 2,
    backgroundColor: Colors.HairlineDark,
    marginVertical: 2,
  },
  timelineContentCol: {
    flex: 1,
    paddingBottom: Spacing.sm,
  },
  timelineTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  timelineNodeName: {
    ...Typography.titleSmall,
    color: '#FFFFFF',
    fontWeight: '600',
  },
  hopBadge: {
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: 4,
    paddingHorizontal: 6,
    paddingVertical: 1,
  },
  hopBadgeCurrent: {
    backgroundColor: Colors.SignalDeep,
    borderColor: Colors.Signal,
  },
  hopBadgeText: {
    ...EyebrowStyle,
    fontSize: 8,
    color: Colors.Slate,
  },
  timelineNodeSub: {
    ...Typography.bodySmall,
    fontSize: 11,
    color: Colors.Slate,
    marginTop: 2,
  },

  // Grid for data items
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  gridItem: {
    width: '48%',
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    padding: Spacing.sm,
  },
  gridLabel: {
    ...EyebrowStyle,
    fontSize: 8,
    color: Colors.Slate,
    marginBottom: 2,
  },
  gridValue: {
    ...Typography.bodyMedium,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
  },
  monoText: {
    ...DataReadoutStyle,
    fontSize: 12,
  },

  // Modal Actions
  modalActionsRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    marginTop: Spacing.xs,
  },
  modalActionReRelayBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.Signal,
    borderRadius: Shapes.medium,
    paddingVertical: Spacing.md,
  },
  modalActionReRelayText: {
    ...Typography.labelLarge,
    color: '#0B0F17',
    fontWeight: '700',
  },
  modalActionCloseBtn: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.Void,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    borderRadius: Shapes.medium,
    paddingVertical: Spacing.md,
  },
  modalActionCloseText: {
    ...Typography.labelLarge,
    color: '#FFFFFF',
    fontWeight: '600',
  },
});
