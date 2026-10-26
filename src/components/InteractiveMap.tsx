import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import Svg, { Circle, Rect, Line, G, Text as SvgText } from 'react-native-svg';
import { MaterialIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HazardZone, HazardSeverity, SosAlert, ConnectedDevice } from '../data/models/types';
import { EyebrowStyle, Typography } from '../theme/typography';
import { Spacing, Shapes } from '../theme/dimens';
import { useApp } from '../context/AppContext';

interface InteractiveMapProps {
  hazards: HazardZone[];
  activeSosAlerts: SosAlert[];
  userLat: number | null;
  userLng: number | null;
  onLocateUser: () => void;
  onRefresh: () => void;
  isRefreshing: boolean;
}

/**
 * Universal tactical ops-console map engine using SVG.
 * Displays hazards, active SOS alerts, user GPS fix, and connected mesh peers.
 */
export const InteractiveMap: React.FC<InteractiveMapProps> = ({
  hazards,
  activeSosAlerts,
  userLat,
  userLng,
  onLocateUser,
  onRefresh,
  isRefreshing,
}) => {
  const { connectedDevices } = useApp();
  const [selectedHazard, setSelectedHazard] = useState<HazardZone | null>(null);
  const [selectedDevice, setSelectedDevice] = useState<ConnectedDevice | null>(null);
  const [selectedSos, setSelectedSos] = useState<SosAlert | null>(null);

  const getHazardColor = (severity: HazardSeverity): string => {
    switch (severity) {
      case 'EXTREME':
      case 'HIGH':
        return Colors.Ember;
      case 'MODERATE':
        return Colors.Amber;
      case 'LOW':
        return Colors.Moss;
      default:
        return Colors.Signal;
    }
  };

  const screenWidth = Dimensions.get('window').width;
  const screenHeight = Dimensions.get('window').height;
  const centerX = screenWidth / 2;
  const centerY = (screenHeight - 160) / 2;
  const scaleDegToPx = 900;

  const baseLat = userLat ?? 28.6139;
  const baseLng = userLng ?? 77.2090;

  const latLngToSvg = (lat: number, lng: number) => {
    const x = centerX + (lng - baseLng) * scaleDegToPx;
    const y = centerY - (lat - baseLat) * scaleDegToPx;
    return { x, y };
  };

  return (
    <View style={styles.container}>
      <Svg width={screenWidth} height={screenHeight - 160} style={StyleSheet.absoluteFill}>
        <Rect width="100%" height="100%" fill={Colors.Void} />

        {/* Grid lines */}
        {[-150, -100, -50, 0, 50, 100, 150].map((offset, idx) => (
          <G key={`grid-${idx}`}>
            <Line
              x1={0}
              y1={centerY + offset}
              x2={screenWidth}
              y2={centerY + offset}
              stroke={Colors.HairlineDark}
              strokeWidth={1}
              strokeDasharray="4 4"
            />
            <Line
              x1={centerX + offset}
              y1={0}
              x2={centerX + offset}
              y2={screenHeight}
              stroke={Colors.HairlineDark}
              strokeWidth={1}
              strokeDasharray="4 4"
            />
          </G>
        ))}

        {/* Range concentric rings */}
        {[80, 150, 220].map((radius, idx) => (
          <Circle
            key={`ring-${idx}`}
            cx={centerX}
            cy={centerY}
            r={radius}
            stroke={Colors.HairlineGlow}
            strokeWidth={1}
            strokeDasharray="2 4"
            fill="none"
          />
        ))}

        {/* Mesh Peer Connection Lines */}
        {connectedDevices.map((d) => {
          const { x, y } = latLngToSvg(d.latitude, d.longitude);
          return (
            <Line
              key={`mesh-link-${d.id}`}
              x1={centerX}
              y1={centerY}
              x2={x}
              y2={y}
              stroke={Colors.Signal}
              strokeWidth={1}
              strokeDasharray="3 3"
              strokeOpacity={0.6}
            />
          );
        })}

        {/* Hazard Zone Circles */}
        {hazards.map((h) => {
          const { x, y } = latLngToSvg(h.latitude, h.longitude);
          const color = getHazardColor(h.severity);
          const r = Math.max(24, Math.min(80, h.radiusMeters / 120));
          return (
            <G
              key={`tac-h-${h.id}`}
              onPress={() => {
                setSelectedHazard(h);
                setSelectedDevice(null);
                setSelectedSos(null);
              }}
            >
              <Circle
                cx={x}
                cy={y}
                r={r}
                fill={color}
                fillOpacity={0.18}
                stroke={color}
                strokeWidth={2}
              />
              <Circle cx={x} cy={y} r={5} fill={color} />
              <SvgText
                x={x + 8}
                y={y + 4}
                fill="#FFFFFF"
                fontSize={10}
                fontWeight="bold"
              >
                {h.title.length > 20 ? h.title.slice(0, 18) + '…' : h.title}
              </SvgText>
            </G>
          );
        })}

        {/* Connected Mesh Devices */}
        {connectedDevices.map((d) => {
          const { x, y } = latLngToSvg(d.latitude, d.longitude);
          const hasRelayed = d.lastRelayedPacket !== null && d.lastRelayedPacket !== undefined;
          return (
            <G
              key={`tac-peer-${d.id}`}
              onPress={() => {
                setSelectedDevice(d);
                setSelectedHazard(null);
                setSelectedSos(null);
              }}
            >
              <Circle
                cx={x}
                cy={y}
                r={16}
                fill="rgba(53, 224, 200, 0.15)"
                stroke={hasRelayed ? Colors.Ember : Colors.Signal}
                strokeWidth={1.5}
              />
              <Circle cx={x} cy={y} r={5} fill={hasRelayed ? Colors.Ember : Colors.Signal} />
              <SvgText
                x={x + 10}
                y={y + 4}
                fill="#FFFFFF"
                fontSize={9}
                fontWeight="bold"
              >
                {d.deviceName.split(' ')[0]} ({d.distanceMeters}m)
              </SvgText>
            </G>
          );
        })}

        {/* Active SOS Alerts */}
        {activeSosAlerts.map((sos) => {
          const { x, y } = latLngToSvg(sos.latitude, sos.longitude);
          return (
            <G
              key={`tac-sos-${sos.id}`}
              onPress={() => {
                setSelectedSos(sos);
                setSelectedHazard(null);
                setSelectedDevice(null);
              }}
            >
              <Circle cx={x} cy={y} r={18} fill="rgba(235, 87, 87, 0.2)" stroke={Colors.Ember} strokeWidth={2} />
              <Circle cx={x} cy={y} r={7} fill={Colors.Ember} />
              <SvgText x={x + 12} y={y + 4} fill={Colors.Ember} fontSize={11} fontWeight="bold">
                SOS ({sos.senderDeviceName?.split(' ')[0] || 'Alert'})
              </SvgText>
            </G>
          );
        })}

        {/* User GPS Location */}
        {userLat !== null && userLng !== null && (
          <G key="user-loc">
            <Circle
              cx={centerX}
              cy={centerY}
              r={20}
              fill="rgba(53, 224, 200, 0.15)"
              stroke={Colors.Signal}
              strokeWidth={1.5}
            />
            <Circle cx={centerX} cy={centerY} r={6} fill={Colors.Signal} />
          </G>
        )}
      </Svg>

      {/* Floating LIVE MAP Badge */}
      <View style={styles.liveMapBadge}>
        <View style={styles.liveDot} />
        <Text style={styles.liveMapText}>LIVE MESH MAP</Text>
      </View>

      {/* SEVERITY Legend Card */}
      <View style={styles.legendCard}>
        <Text style={styles.legendTitle}>MAP LEGEND</Text>
        <View style={styles.legendRow}>
          <View style={[styles.legendDot, { backgroundColor: Colors.Ember }]} />
          <Text style={styles.legendLabel}>Active SOS Alert</Text>
        </View>
        <View style={styles.legendRow}>
          <View style={[styles.legendDot, { backgroundColor: Colors.Signal }]} />
          <Text style={styles.legendLabel}>Mesh Peer Node</Text>
        </View>
        <View style={styles.legendRow}>
          <View style={[styles.legendDot, { backgroundColor: Colors.Amber }]} />
          <Text style={styles.legendLabel}>Hazard Zone</Text>
        </View>
      </View>

      {/* Action FABs (Refresh + My Location) */}
      <View style={styles.fabColumn}>
        <TouchableOpacity
          activeOpacity={0.8}
          onPress={onRefresh}
          disabled={isRefreshing}
          style={styles.smallFab}
        >
          <MaterialIcons name="refresh" size={20} color={Colors.Signal} />
        </TouchableOpacity>

        <TouchableOpacity
          activeOpacity={0.8}
          onPress={onLocateUser}
          style={styles.largeFab}
        >
          <MaterialIcons name="my-location" size={24} color={Colors.Void} />
        </TouchableOpacity>
      </View>

      {/* Selected Hazard Info Banner */}
      {selectedHazard && (
        <View style={styles.infoBanner}>
          <View style={styles.bannerHeader}>
            <View style={[styles.pillBadge, { backgroundColor: getHazardColor(selectedHazard.severity) }]}>
              <Text style={styles.pillBadgeText}>{selectedHazard.severity}</Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedHazard(null)}>
              <MaterialIcons name="close" size={18} color={Colors.Slate} />
            </TouchableOpacity>
          </View>
          <Text style={styles.bannerTitle}>{selectedHazard.title}</Text>
          <Text style={styles.bannerDesc} numberOfLines={2}>
            {selectedHazard.description}
          </Text>
        </View>
      )}

      {/* Selected Connected Device Info Banner */}
      {selectedDevice && (
        <View style={styles.infoBanner}>
          <View style={styles.bannerHeader}>
            <View style={[styles.pillBadge, { backgroundColor: Colors.Signal }]}>
              <Text style={styles.pillBadgeText}>CONNECTED PEER</Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedDevice(null)}>
              <MaterialIcons name="close" size={18} color={Colors.Slate} />
            </TouchableOpacity>
          </View>
          <Text style={styles.bannerTitle}>{selectedDevice.deviceName}</Text>
          <Text style={styles.bannerDesc}>
            Owner: {selectedDevice.ownerName} · {selectedDevice.distanceMeters}m · {selectedDevice.signalStrength}
          </Text>
          {selectedDevice.lastRelayedPacket && (
            <View style={styles.relayedMiniBox}>
              <Text style={styles.relayedMiniTitle}>RELAYED CUSTOM MESSAGE:</Text>
              <Text style={styles.relayedMiniMsg}>"{selectedDevice.lastRelayedPacket.customMessage}"</Text>
              <Text style={styles.relayedMiniRoute}>
                Route: {selectedDevice.lastRelayedPacket.relayPath.join(' ➔ ')}
              </Text>
            </View>
          )}
        </View>
      )}

      {/* Selected SOS Alert Banner */}
      {selectedSos && (
        <View style={styles.infoBanner}>
          <View style={styles.bannerHeader}>
            <View style={[styles.pillBadge, { backgroundColor: Colors.Ember }]}>
              <Text style={styles.pillBadgeText}>EMERGENCY SOS</Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedSos(null)}>
              <MaterialIcons name="close" size={18} color={Colors.Slate} />
            </TouchableOpacity>
          </View>
          <Text style={styles.bannerTitle}>{selectedSos.senderDeviceName || 'Emergency Alert'}</Text>
          <View style={styles.relayedMiniBox}>
            <Text style={styles.relayedMiniTitle}>TYPED EMERGENCY MESSAGE:</Text>
            <Text style={styles.relayedMiniMsg}>"{selectedSos.message}"</Text>
            <Text style={styles.relayedMiniRoute}>
              Sender: {selectedSos.senderName || 'Local Citizen'} · Blood: {selectedSos.bloodGroup || 'N/A'} · Contact: {selectedSos.emergencyContact || 'N/A'}
            </Text>
          </View>
        </View>
      )}
    </View>
  );
};


const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.Void,
  },
  liveMapBadge: {
    position: 'absolute',
    top: Spacing.md,
    left: Spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(11, 15, 23, 0.92)',
    borderRadius: Shapes.small,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
  },
  liveDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: Colors.Signal,
    marginRight: Spacing.xs,
  },
  liveMapText: {
    ...EyebrowStyle,
    color: Colors.Slate,
  },
  legendCard: {
    position: 'absolute',
    bottom: Spacing.md,
    left: Spacing.md,
    backgroundColor: 'rgba(11, 15, 23, 0.92)',
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    padding: Spacing.sm,
    minWidth: 130,
  },
  legendTitle: {
    ...EyebrowStyle,
    color: Colors.Slate,
    marginBottom: Spacing.xxs,
  },
  legendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 2,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: Spacing.xs,
  },
  legendLabel: {
    ...Typography.labelMedium,
    color: Colors.DarkOnSurface,
  },
  fabColumn: {
    position: 'absolute',
    bottom: Spacing.md,
    right: Spacing.md,
    alignItems: 'center',
  },
  smallFab: {
    width: 42,
    height: 42,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.InkRaised,
    borderWidth: 1,
    borderColor: Colors.HairlineDark,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.xs,
  },
  largeFab: {
    width: 56,
    height: 56,
    borderRadius: Shapes.medium,
    backgroundColor: Colors.Signal,
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoBanner: {
    position: 'absolute',
    bottom: 80,
    left: Spacing.md,
    right: Spacing.md,
    backgroundColor: Colors.InkRaisedHigh,
    borderRadius: Shapes.large,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.md,
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
  },
  bannerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xxs,
  },
  pillBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  pillBadgeText: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#000000',
  },
  bannerTitle: {
    ...Typography.titleMedium,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  bannerDesc: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    marginTop: 2,
  },
  relayedMiniBox: {
    backgroundColor: 'rgba(11, 15, 23, 0.9)',
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.Signal,
    padding: Spacing.xs,
    marginTop: Spacing.xs,
  },
  relayedMiniTitle: {
    ...EyebrowStyle,
    fontSize: 9,
    color: Colors.Signal,
  },
  relayedMiniMsg: {
    ...Typography.bodyMedium,
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontStyle: 'italic',
    marginTop: 1,
  },
  relayedMiniRoute: {
    ...Typography.bodySmall,
    fontSize: 10,
    color: Colors.Slate,
    marginTop: 2,
  },
});

