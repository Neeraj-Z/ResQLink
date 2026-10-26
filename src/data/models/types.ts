export type HazardType = 'FLOOD' | 'EARTHQUAKE' | 'FIRE' | 'CYCLONE' | 'LANDSLIDE' | 'INDUSTRIAL' | 'OTHER';

export type HazardSeverity = 'LOW' | 'MODERATE' | 'HIGH' | 'EXTREME';

export interface HazardZone {
  id: string;
  type: HazardType;
  severity: HazardSeverity;
  title: string;
  description: string;
  latitude: Double;
  longitude: Double;
  radiusMeters: number;
  issuedAtEpochMillis: number;
  source: string;
}

export type SosStatus = 'PENDING_SYNC' | 'SENT' | 'RELAYED_VIA_MESH' | 'ACKNOWLEDGED' | 'RESOLVED';
export type SosSeverity = 'CRITICAL' | 'HIGH' | 'MODERATE';

export interface SosAlert {
  id: string;
  userId: string;
  senderName?: string;
  senderDeviceName?: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  message: string;
  severity: SosSeverity;
  status: SosStatus;
  createdAtEpochMillis: number;
  isMeshRelayed: boolean;
  hopCount: number;
  relayPath?: string[];
  bloodGroup?: string | null;
  emergencyContact?: string | null;
  medicalNotes?: string | null;
}

export interface MeshRelayedPacket {
  id: string;
  senderId: string;
  senderName: string;
  senderPhone?: string;
  senderDeviceName: string;
  targetDeviceId: string;
  targetDeviceName: string;
  customMessage: string;
  severity: SosSeverity;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  bloodGroup?: string | null;
  emergencyContact?: string | null;
  medicalNotes?: string | null;
  createdAtEpochMillis: number;
  hopCount: number;
  relayPath: string[];
  transport: 'BLE_MESH' | 'WIFI_DIRECT' | 'LORA_HYBRID';
  deliveryStatus: 'DELIVERED' | 'RELAYING' | 'CONFIRMED';
  rssi?: string;
  direction?: 'BY_THIS_DEVICE' | 'THROUGH_THIS_DEVICE';
  acknowledgedAtEpochMillis?: number;
}

export interface ConnectedDevice {
  id: string;
  deviceName: string;
  ownerName: string;
  role: UserRole;
  signalStrength: string;
  distanceMeters: number;
  batteryPercent: number;
  status: 'CONNECTED' | 'RELAYING' | 'SYNCED' | 'DISCONNECTED';
  latitude: number;
  longitude: number;
  lastRelayedPacket?: MeshRelayedPacket | null;
  lastSeenEpochMillis: number;
}

export interface FirstAidGuide {
  id: string;
  title: string;
  category: string;
  summary: string;
  steps: string[];
  criticalWarning?: string | null;
  imageUrl?: string | null;
}

export type UserRole = 'CITIZEN' | 'VOLUNTEER' | 'RESCUE_TEAM' | 'ADMIN';

export interface User {
  id: string;
  name: string;
  phone: string;
  deviceName?: string;
  role: UserRole;
  bloodGroup?: string | null;
  emergencyContact?: string | null;
  medicalNotes?: string | null;
}

export type Double = number;

export type LocationStatus = 'IDLE' | 'LOCATING' | 'AVAILABLE' | 'UNAVAILABLE' | 'PERMISSION_DENIED';

export type MeshRelayState =
  | { type: 'Idle' }
  | { type: 'Starting' }
  | { type: 'Ready'; connectedPeerCount: number }
  | { type: 'Error'; message: string };

export type SosSendState = 'IDLE' | 'SENDING' | 'SENT_LOCALLY' | 'SYNCED';

