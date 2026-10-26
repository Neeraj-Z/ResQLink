import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import MapView, { Circle as MapCircle, Marker as MapMarker, PROVIDER_DEFAULT } from 'react-native-maps';
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
  const [useTacticalView, setUseTacticalView] = useState<boolean>(false);
  const [selectedHazard, setSelectedHazard] = useState<HazardZone | null>(null);
  const [selectedDevice, setSelectedDevice] = useState<ConnectedDevice | null>(null);


  const defaultRegion = {
    latitude: userLat ?? 28.6139,
    longitude: userLng ?? 77.2090,
    latitudeDelta: 0.25,
    longitudeDelta: 0.25,
  };

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

  const getHazardPinColor = (severity: HazardSeverity): string => {
    switch (severity) {
      case 'EXTREME':
      case 'HIGH':
        return '#FF3B30';
      case 'MODERATE':
        return '#FF9500';
      case 'LOW':
        return '#34C759';
      default:
        return '#007AFF';
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

  const renderTacticalView = () => (
    <View style={styles.tacticalContainer}>
      <Svg width={screenWidth} height={screenHeight - 160} style={StyleSheet.absoluteFill}>
        <Rect width="100%" height="100%" fill={Colors.Void} />

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

        {hazards.map((h) => {
          const { x, y } = latLngToSvg(h.latitude, h.longitude);
          const color = getHazardColor(h.severity);
          const r = Math.max(24, Math.min(80, h.radiusMeters / 120));
          return (
            <G key={`tac-h-${h.id}`}>
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

        {activeSosAlerts.map((sos) => {
          const { x, y } = latLngToSvg(sos.latitude, sos.longitude);
          return (
            <G key={`tac-sos-${sos.id}`}>
              <Circle cx={x} cy={y} r={14} fill="none" stroke={Colors.Ember} strokeWidth={1.5} />
              <Circle cx={x} cy={y} r={6} fill={Colors.Ember} />
              <SvgText x={x + 10} y={y + 4} fill={Colors.Ember} fontSize={11} fontWeight="bold">
                SOS ALERT
              </SvgText>
            </G>
          );
        })}

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
    </View>
  );

  return (
    <View style={styles.container}>
      {useTacticalView ? (
        renderTacticalView()
      ) : (
        <MapView
          style={StyleSheet.absoluteFill}
          provider={PROVIDER_DEFAULT}
          initialRegion={defaultRegion}
          showsUserLocation={true}
          showsMyLocationButton={false}
          showsCompass={true}
          customMapStyle={darkMapStyle}
        >
          {hazards.map((h) => {
            const color = getHazardColor(h.severity);
            const pinColor = getHazardPinColor(h.severity);
            return (
              <React.Fragment key={h.id}>
                <MapCircle
                  center={{ latitude: h.latitude, longitude: h.longitude }}
                  radius={h.radiusMeters}
                  strokeColor={color}
                  fillColor={color + '2E'}
                  strokeWidth={2}
                />
                <MapMarker
                  coordinate={{ latitude: h.latitude, longitude: h.longitude }}
                  title={h.title}
                  description={`${h.severity} · ${h.type} (${Math.round(h.radiusMeters / 1000)}km radius)`}
                  pinColor={pinColor}
                  onPress={() => setSelectedHazard(h)}
                />
              </React.Fragment>
            );
          })}

          {/* Connected Mesh Devices Markers */}
          {connectedDevices.map((device) => (
            <MapMarker
              key={`peer-${device.id}`}
              coordinate={{ latitude: device.latitude, longitude: device.longitude }}
              title={device.deviceName}
              description={
                device.lastRelayedPacket
                  ? `[RELAYED]: "${device.lastRelayedPacket.customMessage}"`
                  : `${device.ownerName} (${device.distanceMeters}m, ${device.signalStrength})`
              }
              pinColor="#35E0C8"
              onPress={() => setSelectedDevice(device)}
            />
          ))}

          {activeSosAlerts.map((sos) => (
            <MapMarker
              key={sos.id}
              coordinate={{ latitude: sos.latitude, longitude: sos.longitude }}
              title={`SOS: ${sos.senderDeviceName || 'Emergency Alert'}`}
              description={`"${sos.message}" - ${sos.senderName || 'Citizen'}`}
              pinColor="#FF3B30"
            />
          ))}

          {userLat !== null && userLng !== null && (
            <MapMarker
              coordinate={{ latitude: userLat, longitude: userLng }}
              title="Your location"
              description="Live GPS fix"
              pinColor="#007AFF"
            />
          )}
        </MapView>
      )}

      <View style={styles.liveMapBadge}>
        <View style={styles.liveDot} />
        <Text style={styles.liveMapText}>LIVE MESH MAP</Text>
      </View>

      <TouchableOpacity
        activeOpacity={0.8}
        onPress={() => setUseTacticalView((prev) => !prev)}
        style={styles.modeToggle}
      >
        <MaterialIcons
          name={useTacticalView ? 'layers' : 'radar'}
          size={20}
          color={Colors.Signal}
        />
      </TouchableOpacity>

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

      {selectedHazard && (
        <View style={styles.hazardBanner}>
          <View style={styles.hazardBannerHeader}>
            <View style={[styles.hazardPill, { backgroundColor: getHazardColor(selectedHazard.severity) }]}>
              <Text style={styles.hazardPillText}>{selectedHazard.severity}</Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedHazard(null)}>
              <MaterialIcons name="close" size={18} color={Colors.Slate} />
            </TouchableOpacity>
          </View>
          <Text style={styles.hazardBannerTitle}>{selectedHazard.title}</Text>
          <Text style={styles.hazardBannerDesc} numberOfLines={2}>
            {selectedHazard.description}
          </Text>
        </View>
      )}

      {selectedDevice && (
        <View style={styles.hazardBanner}>
          <View style={styles.hazardBannerHeader}>
            <View style={[styles.hazardPill, { backgroundColor: Colors.Signal }]}>
              <Text style={styles.hazardPillText}>CONNECTED MESH DEVICE</Text>
            </View>
            <TouchableOpacity onPress={() => setSelectedDevice(null)}>
              <MaterialIcons name="close" size={18} color={Colors.Slate} />
            </TouchableOpacity>
          </View>
          <Text style={styles.hazardBannerTitle}>{selectedDevice.deviceName}</Text>
          <Text style={styles.hazardBannerDesc}>
            Owner: {selectedDevice.ownerName} · {selectedDevice.distanceMeters}m away · {selectedDevice.signalStrength}
          </Text>
          {selectedDevice.lastRelayedPacket && (
            <View style={{ marginTop: 6, padding: 8, backgroundColor: 'rgba(11, 15, 23, 0.9)', borderRadius: 8, borderWidth: 1, borderColor: Colors.Signal }}>
              <Text style={{ ...EyebrowStyle, fontSize: 9, color: Colors.Signal }}>RELAYED CUSTOM MESSAGE:</Text>
              <Text style={{ ...Typography.bodyMedium, color: '#FFFFFF', fontWeight: 'bold', fontStyle: 'italic', marginTop: 2 }}>
                "{selectedDevice.lastRelayedPacket.customMessage}"
              </Text>
              <Text style={{ ...Typography.bodySmall, fontSize: 10, color: Colors.Slate, marginTop: 4 }}>
                Transmission: {selectedDevice.lastRelayedPacket.relayPath.join(' ➔ ')}
              </Text>
            </View>
          )}
        </View>
      )}
    </View>
  );

};

const darkMapStyle = [
  { elementType: 'geometry', stylers: [{ color: '#090d16' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#090d16' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#746855' }] },
  {
    featureType: 'administrative.locality',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#8aa0be' }],
  },
  {
    featureType: 'poi',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#62728b' }],
  },
  {
    featureType: 'road',
    elementType: 'geometry',
    stylers: [{ color: '#161e2e' }],
  },
  {
    featureType: 'road',
    elementType: 'geometry.stroke',
    stylers: [{ color: '#212a3d' }],
  },
  {
    featureType: 'road.highway',
    elementType: 'geometry',
    stylers: [{ color: '#1e293b' }],
  },
  {
    featureType: 'water',
    elementType: 'geometry',
    stylers: [{ color: '#0b1626' }],
  },
  {
    featureType: 'water',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#35e0c8' }],
  },
];

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.Void,
  },
  tacticalContainer: {
    ...StyleSheet.absoluteFill,
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
  modeToggle: {
    position: 'absolute',
    top: Spacing.md,
    right: Spacing.md,
    width: 38,
    height: 38,
    borderRadius: 10,
    backgroundColor: 'rgba(11, 15, 23, 0.92)',
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    alignItems: 'center',
    justifyContent: 'center',
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
  hazardBanner: {
    position: 'absolute',
    bottom: 80,
    left: Spacing.md,
    right: Spacing.md,
    backgroundColor: Colors.InkRaisedHigh,
    borderRadius: Shapes.medium,
    borderWidth: 1,
    borderColor: Colors.HairlineGlow,
    padding: Spacing.md,
  },
  hazardBannerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.xxs,
  },
  hazardPill: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  hazardPillText: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#000000',
  },
  hazardBannerTitle: {
    ...Typography.titleMedium,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  hazardBannerDesc: {
    ...Typography.bodyMedium,
    color: Colors.Slate,
    marginTop: 2,
  },
});
