import { ConnectedDevice, MeshRelayedPacket, SosAlert, User } from '../data/models/types';

export type MeshPacketType = 'ANNOUNCE' | 'ANNOUNCE_RESPONSE' | 'SOS_BROADCAST' | 'SOS_ACK' | 'DISCONNECT';

export interface MeshWireMessage {
  type: MeshPacketType;
  senderDeviceId: string;
  senderDeviceName: string;
  ownerName: string;
  role: string;
  latitude: number;
  longitude: number;
  batteryPercent: number;
  timestamp: number;
  targetDeviceId?: string;
  sosPayload?: {
    alertId: string;
    customMessage: string;
    severity: string;
    latitude: number;
    longitude: number;
    accuracyMeters: number;
    bloodGroup?: string | null;
    emergencyContact?: string | null;
    medicalNotes?: string | null;
    hopCount: number;
    relayPath: string[];
    createdAtEpochMillis: number;
  };
  ackForAlertId?: string;
}

export type PeerUpdateCallback = (peers: ConnectedDevice[]) => void;
export type SosReceivedCallback = (packet: MeshRelayedPacket, alert: SosAlert) => void;
export type SosAckCallback = (alertId: string, peerId: string) => void;

class MeshNetworkEngine {
  private channel: BroadcastChannel | null = null;
  private instanceId: string;
  private deviceName: string;
  private activePeers: Map<string, ConnectedDevice> = new Map();
  private peerCallbacks: Set<PeerUpdateCallback> = new Set();
  private sosCallbacks: Set<SosReceivedCallback> = new Set();
  private ackCallbacks: Set<SosAckCallback> = new Set();
  private heartbeatInterval: any = null;
  private cleanupInterval: any = null;
  private currentUser: User | null = null;
  private currentLat: number = 28.6139;
  private currentLng: number = 77.2090;

  constructor() {
    this.instanceId = `node-${Math.random().toString(36).substring(2, 8)}`;
    this.deviceName = this.detectDeviceName();
    this.initChannel();
  }

  private detectDeviceName(): string {
    if (typeof window !== 'undefined' && window.navigator) {
      const ua = window.navigator.userAgent;
      if (/Android/i.test(ua)) return `Android Device (${this.instanceId.slice(-4)})`;
      if (/iPhone|iPad/i.test(ua)) return `Apple iOS Node (${this.instanceId.slice(-4)})`;
      if (/Chrome/i.test(ua)) return `Chrome Mesh Node (${this.instanceId.slice(-4)})`;
      if (/Firefox/i.test(ua)) return `Firefox Node (${this.instanceId.slice(-4)})`;
      if (/Edge/i.test(ua)) return `Edge Node (${this.instanceId.slice(-4)})`;
      return `Web Node (${this.instanceId.slice(-4)})`;
    }
    return `ResQLink Device (${this.instanceId.slice(-4)})`;
  }

  public getDeviceId(): string {
    return this.instanceId;
  }

  public getDeviceName(): string {
    return this.deviceName;
  }

  public setDeviceName(name: string) {
    if (name.trim()) {
      this.deviceName = name.trim();
      this.sendAnnounce();
    }
  }

  public updateUserAndLocation(user: User, lat: number | null, lng: number | null) {
    this.currentUser = user;
    if (user.deviceName) {
      this.deviceName = user.deviceName;
    }
    if (lat !== null) this.currentLat = lat;
    if (lng !== null) this.currentLng = lng;
  }

  private initChannel() {
    if (typeof BroadcastChannel !== 'undefined') {
      try {
        this.channel = new BroadcastChannel('resqlink_mesh_cluster_v1');
        this.channel.onmessage = (event) => {
          this.handleIncomingWireMessage(event.data);
        };
      } catch (e) {
        console.warn('BroadcastChannel not available', e);
      }
    }

    if (typeof window !== 'undefined' && window.addEventListener) {
      window.addEventListener('storage', (event) => {
        if (event.key === 'resqlink_mesh_broadcast' && event.newValue) {
          try {
            const data: MeshWireMessage = JSON.parse(event.newValue);
            this.handleIncomingWireMessage(data);
          } catch (err) {}
        }
      });

      window.addEventListener('beforeunload', () => {
        this.sendDisconnect();
      });
    }

    this.startHeartbeat();
  }

  private startHeartbeat() {
    this.sendAnnounce();

    this.heartbeatInterval = setInterval(() => {
      this.sendAnnounce();
    }, 2500);

    this.cleanupInterval = setInterval(() => {
      const now = Date.now();
      let changed = false;
      for (const [id, peer] of this.activePeers.entries()) {
        if (now - peer.lastSeenEpochMillis > 6500) {
          this.activePeers.delete(id);
          changed = true;
        }
      }
      if (changed) {
        this.notifyPeerListeners();
      }
    }, 3000);
  }

  private postMessage(msg: MeshWireMessage) {
    if (this.channel) {
      try {
        this.channel.postMessage(msg);
      } catch (e) {}
    }

    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem('resqlink_mesh_broadcast', JSON.stringify({ ...msg, _t: Date.now() }));
      } catch (e) {}
    }
  }

  private sendAnnounce(type: MeshPacketType = 'ANNOUNCE') {
    const msg: MeshWireMessage = {
      type,
      senderDeviceId: this.instanceId,
      senderDeviceName: this.deviceName,
      ownerName: this.currentUser?.name || 'ResQLink User',
      role: this.currentUser?.role || 'CITIZEN',
      latitude: this.currentLat,
      longitude: this.currentLng,
      batteryPercent: 92,
      timestamp: Date.now(),
    };
    this.postMessage(msg);
  }

  private sendDisconnect() {
    const msg: MeshWireMessage = {
      type: 'DISCONNECT',
      senderDeviceId: this.instanceId,
      senderDeviceName: this.deviceName,
      ownerName: this.currentUser?.name || 'ResQLink User',
      role: this.currentUser?.role || 'CITIZEN',
      latitude: this.currentLat,
      longitude: this.currentLng,
      batteryPercent: 92,
      timestamp: Date.now(),
    };
    this.postMessage(msg);
  }

  private calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371e3;
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
      Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return Math.max(2, Math.round(R * c));
  }

  private handleIncomingWireMessage(msg: MeshWireMessage) {
    if (!msg || msg.senderDeviceId === this.instanceId) {
      return;
    }

    if (msg.type === 'DISCONNECT') {
      if (this.activePeers.has(msg.senderDeviceId)) {
        this.activePeers.delete(msg.senderDeviceId);
        this.notifyPeerListeners();
      }
      return;
    }

    if (msg.type === 'ANNOUNCE' || msg.type === 'ANNOUNCE_RESPONSE') {
      const distance = this.calculateDistance(this.currentLat, this.currentLng, msg.latitude, msg.longitude);
      const signal = distance < 25 ? '-48 dBm (Excellent)' : distance < 75 ? '-64 dBm (Good)' : '-78 dBm (Fair)';

      const existing = this.activePeers.get(msg.senderDeviceId);
      const peer: ConnectedDevice = {
        id: msg.senderDeviceId,
        deviceName: msg.senderDeviceName,
        ownerName: msg.ownerName,
        role: (msg.role as any) || 'CITIZEN',
        signalStrength: signal,
        distanceMeters: distance,
        batteryPercent: msg.batteryPercent || 85,
        status: 'CONNECTED',
        latitude: msg.latitude,
        longitude: msg.longitude,
        lastSeenEpochMillis: Date.now(),
        lastRelayedPacket: existing ? existing.lastRelayedPacket : null,
      };

      this.activePeers.set(msg.senderDeviceId, peer);
      this.notifyPeerListeners();

      if (msg.type === 'ANNOUNCE') {
        this.sendAnnounce('ANNOUNCE_RESPONSE');
      }
      return;
    }

    if (msg.type === 'SOS_BROADCAST' && msg.sosPayload) {
      const p = msg.sosPayload;
      const fullRoute = [...p.relayPath, this.deviceName];

      const relayedPacket: MeshRelayedPacket = {
        id: `relayed-${p.alertId}-${this.instanceId}`,
        senderId: msg.senderDeviceId,
        senderName: msg.ownerName,
        senderDeviceName: msg.senderDeviceName,
        targetDeviceId: this.instanceId,
        targetDeviceName: this.deviceName,
        customMessage: p.customMessage,
        severity: p.severity as any,
        latitude: p.latitude,
        longitude: p.longitude,
        accuracyMeters: p.accuracyMeters,
        bloodGroup: p.bloodGroup,
        emergencyContact: p.emergencyContact,
        medicalNotes: p.medicalNotes,
        createdAtEpochMillis: p.createdAtEpochMillis,
        hopCount: p.hopCount,
        relayPath: fullRoute,
        transport: 'BLE_MESH',
        deliveryStatus: 'DELIVERED',
        rssi: '-50 dBm',
      };

      const sosAlert: SosAlert = {
        id: p.alertId,
        userId: msg.senderDeviceId,
        senderName: msg.ownerName,
        senderDeviceName: msg.senderDeviceName,
        latitude: p.latitude,
        longitude: p.longitude,
        accuracyMeters: p.accuracyMeters,
        message: p.customMessage,
        severity: p.severity as any,
        status: 'RELAYED_VIA_MESH',
        createdAtEpochMillis: p.createdAtEpochMillis,
        isMeshRelayed: true,
        hopCount: p.hopCount,
        relayPath: fullRoute,
        bloodGroup: p.bloodGroup,
        emergencyContact: p.emergencyContact,
        medicalNotes: p.medicalNotes,
      };

      this.sosCallbacks.forEach((cb) => cb(relayedPacket, sosAlert));
      this.sendSosAck(p.alertId, msg.senderDeviceId);
      return;
    }

    if (msg.type === 'SOS_ACK' && msg.ackForAlertId) {
      this.ackCallbacks.forEach((cb) => cb(msg.ackForAlertId!, msg.senderDeviceId));
      return;
    }
  }

  public sendSosAck(alertId: string, targetDeviceId: string) {
    const msg: MeshWireMessage = {
      type: 'SOS_ACK',
      senderDeviceId: this.instanceId,
      senderDeviceName: this.deviceName,
      ownerName: this.currentUser?.name || 'ResQLink User',
      role: this.currentUser?.role || 'CITIZEN',
      latitude: this.currentLat,
      longitude: this.currentLng,
      batteryPercent: 92,
      timestamp: Date.now(),
      targetDeviceId,
      ackForAlertId: alertId,
    };
    this.postMessage(msg);
  }

  public broadcastSos(alert: SosAlert, customMessage: string): MeshRelayedPacket[] {
    const senderDev = this.deviceName;
    const nowEpoch = alert.createdAtEpochMillis || Date.now();

    const msg: MeshWireMessage = {
      type: 'SOS_BROADCAST',
      senderDeviceId: this.instanceId,
      senderDeviceName: senderDev,
      ownerName: this.currentUser?.name || 'ResQLink User',
      role: this.currentUser?.role || 'CITIZEN',
      latitude: alert.latitude,
      longitude: alert.longitude,
      batteryPercent: 92,
      timestamp: nowEpoch,
      sosPayload: {
        alertId: alert.id,
        customMessage: customMessage,
        severity: alert.severity,
        latitude: alert.latitude,
        longitude: alert.longitude,
        accuracyMeters: alert.accuracyMeters || 10,
        bloodGroup: alert.bloodGroup,
        emergencyContact: alert.emergencyContact,
        medicalNotes: alert.medicalNotes,
        hopCount: 1,
        relayPath: [senderDev],
        createdAtEpochMillis: nowEpoch,
      },
    };

    this.postMessage(msg);

    const outgoingPackets: MeshRelayedPacket[] = Array.from(this.activePeers.values()).map((peer) => ({
      id: `mesh-pkt-${nowEpoch}-${peer.id}`,
      senderId: this.instanceId,
      senderName: this.currentUser?.name || 'ResQLink User',
      senderPhone: this.currentUser?.phone || '',
      senderDeviceName: senderDev,
      targetDeviceId: peer.id,
      targetDeviceName: peer.deviceName,
      customMessage: customMessage,
      severity: alert.severity,
      latitude: alert.latitude,
      longitude: alert.longitude,
      accuracyMeters: alert.accuracyMeters || 10,
      bloodGroup: alert.bloodGroup,
      emergencyContact: alert.emergencyContact,
      medicalNotes: alert.medicalNotes,
      createdAtEpochMillis: nowEpoch,
      hopCount: 1,
      relayPath: [senderDev, peer.deviceName],
      transport: 'BLE_MESH',
      deliveryStatus: 'DELIVERED',
      rssi: peer.signalStrength,
    }));

    outgoingPackets.forEach((pkt) => {
      const peer = this.activePeers.get(pkt.targetDeviceId);
      if (peer) {
        peer.lastRelayedPacket = pkt;
        peer.status = 'RELAYING';
      }
    });

    this.notifyPeerListeners();
    return outgoingPackets;
  }

  public onPeersChanged(callback: PeerUpdateCallback): () => void {
    this.peerCallbacks.add(callback);
    callback(Array.from(this.activePeers.values()));
    return () => {
      this.peerCallbacks.delete(callback);
    };
  }

  public onSosReceived(callback: SosReceivedCallback): () => void {
    this.sosCallbacks.add(callback);
    return () => {
      this.sosCallbacks.delete(callback);
    };
  }

  public onSosAck(callback: SosAckCallback): () => void {
    this.ackCallbacks.add(callback);
    return () => {
      this.ackCallbacks.delete(callback);
    };
  }

  private notifyPeerListeners() {
    const list = Array.from(this.activePeers.values());
    this.peerCallbacks.forEach((cb) => cb(list));
  }

  public getPeers(): ConnectedDevice[] {
    return Array.from(this.activePeers.values());
  }

  public destroy() {
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    if (this.cleanupInterval) clearInterval(this.cleanupInterval);
    this.sendDisconnect();
    if (this.channel) this.channel.close();
  }
}

export const MeshEngine = new MeshNetworkEngine();
