# ResQLink — Offline-First Disaster Management & Mesh Emergency Platform

[![Android](https://img.shields.io/badge/Platform-Android%20%7C%20Cross--Platform-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![React Native / Expo](https://img.shields.io/badge/React%20Native-Expo-black.svg)](https://expo.dev/)
[![Nearby Connections](https://img.shields.io/badge/Mesh%20Network-Nearby%20Connections-orange.svg)](https://developers.google.com/nearby/connections/overview)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**ResQLink** is a resilient, offline-first disaster response and emergency coordination platform designed for communication blackouts caused by earthquakes, floods, hurricanes, and infrastructure failures.

When cellular networks and power grids collapse, ResQLink turns surrounding smartphones into an ad-hoc, multi-hop **Peer-to-Peer (P2P) mesh network** using Bluetooth Low Energy (BLE) and Wi-Fi Direct. Critical SOS distress signals, medical triage data, hazard intelligence, and evacuation maps route autonomously across intermediate devices until reaching emergency responders or an active internet gateway.


---

## 📲 Direct Installation via QR Code (Android)

Install ResQLink directly onto any Android device in seconds without needing a computer or Android Studio:

<p align="center">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=https%3A%2F%2Fgithub.com%2FNeeraj-Z%2FResQLink%2Freleases%2Flatest%2Fdownload%2FResQLink.apk" alt="Direct APK Install QR Code" width="220" />
  <br>
  <b>Scan with your Phone Camera or Google Lens to Install</b>
  <br><br>
  <a href="https://github.com/Neeraj-Z/ResQLink/releases/latest/download/ResQLink.apk">
    <img src="https://img.shields.io/badge/Download-ResQLink%20Android%20APK%20(v1.0.0)-success?style=for-the-badge&logo=android" alt="Download APK" />
  </a>
</p>

### Quick 3-Step Setup:
1. **Scan the QR Code**: Point your Android device's **Camera** or **Google Lens** at the QR code above.
2. **Download & Open**: Tap the download prompt to download `ResQLink.apk`.
3. **Install & Launch**: Tap **Install** (allow *"Install unknown apps"* if prompted). Enable Bluetooth & Location for offline mesh networking!

> 💡 **Interactive Web Install Page**: Open [qr.html](qr.html) in your browser for a dual install portal (Direct APK + Live Expo Go preview).

---

## 🌟 Key Capabilities

### 1. Multi-Hop P2P Mesh SOS Relay
* **Zero-Pairing Ad-Hoc Mesh**: Automatically discovers, connects, and relays distress packets among nearby devices using Google Nearby Connections (`P2P_CLUSTER` topology) without requiring cellular data, internet access, or manual Bluetooth pairing.
* **Store-and-Forward Routing**: Devices cache incoming distress alerts locally in Room/SQLite and relay them to newly encountered peer nodes. The moment *any* node in the mesh chain gains cellular/Wi-Fi connectivity, pending packets automatically synchronize to central emergency servers.
* **Loop Prevention & Deduplication**: Packets carry unique collision-resistant UUIDs and hop-limit counters (TTL) to prevent broadcast storming and infinite loops.
* **Persistent Background Daemon**: Background service (`SosForegroundService`) keeps mesh relay active even when the screen is locked or the application is minimized.

### 2. Tactical Offline Maps & Hazard Zoning
* **100% Offline Geolocation**: Embeds local Leaflet rendering and map asset caches to display emergency zones without internet connectivity.
* **Dynamic Danger Buffers**: Visualizes hazard perimeters (floods, structural collapses, wildfires) with interactive severity levels (High, Moderate, Advisory).
* **SOS Beacon Tracking**: Plots distress signals with precise GPS coordinates, timestamps, and victim medical tags directly on the emergency map.

### 3. Immediate One-Touch SOS Triage
* **Fast-Path Broadcast**: Triggers rapid distress alerts with exact GPS telemetry, battery level, timestamp, and victim count.
* **Offline Medical Registry**: Generates an emergency QR identity code containing blood type, allergies, pre-existing conditions, and emergency contacts, scannable offline by field paramedics.

### 4. Indexed Offline First Aid Knowledge Base
* **Local Offline Search**: Full database of critical emergency guides (CPR, trauma triage, severe burns, fracture stabilization, water purification) fully functional without network access.
* **Step-by-Step Triage Instructions**: Designed with high-contrast, distraction-free typography for high-stress disaster conditions.

### 5. Tactical Command Dashboard & Field Bridge
* **Field Command Bridge (`laptop_bridge.js`)**: Lightweight gateway bridging field radios/tablets and coordinating rescue operations on local disaster headquarters laptops.
* **Master Disaster Dashboard (`master_dashboard.js`)**: Real-time visual monitoring console summarizing active SOS alerts, mesh relay density, and hazard status.

---

## 🏗️ Architecture & Technology Stack

ResQLink follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** design pattern with unidirectional data flow.

```
                      ┌────────────────────────────────────────┐
                      │             UI / Presentation          │
                      │   (Jetpack Compose M3 / React Native)   │
                      └───────────────────▲────────────────────┘
                                          │ State / Events
                      ┌───────────────────▼────────────────────┐
                      │          Domain Business Logic         │
                      │  (UseCases, Models, Relay Interfaces)   │
                      └─────────▲────────────────────▲─────────┘
                                │                    │
                ┌───────────────┴────┐          ─────┴──────────────────┐
                │ Local Data Source  │          │   Mesh & Remote Sync   │
                │  (Room SQLite DB)  │          │  (Nearby Mesh Manager) │
                └────────────────────┘          └────────────────────────┘
```

| Layer | Technologies |
| :--- | :--- |
| **Android Mobile** | Kotlin 2.0, Jetpack Compose, Material Design 3, Coroutines, StateFlow |
| **Cross-Platform / Web** | React Native, Expo, TypeScript |
| **Dependency Injection**| Dagger Hilt |
| **Local Persistence** | Android Room Database, DataStore Preferences, Async Storage |
| **Mesh Networking** | Google Nearby Connections API (Bluetooth LE + Wi-Fi Direct) |
| **Mapping & GIS** | Leaflet, OpenStreetMap offline tile engine |
| **Background Sync** | Android WorkManager (Expedited / Periodic SOS flush workers) |
| **Command & Control** | Node.js, Express, WebSockets, HTML5/CSS3 Dashboard |

---

## 📂 Repository Structure

```
ResQLink/
├── app/                              # Android Native Application
│   ├── src/main/java/com/resqlink/app/
│   │   ├── data/                     # Room DAOs, Entities, Mesh & Location Repositories
│   │   ├── domain/                   # Pure Kotlin business models & Use Cases
│   │   ├── presentation/             # Jetpack Compose Screens (Home, Map, SOS, Analytics...)
│   │   ├── di/                       # Dagger Hilt Dependency Injection Modules
│   │   ├── service/                  # SosForegroundService (Persistent Mesh Relay)
│   │   └── worker/                   # WorkManager background sync workers
│   └── src/main/assets/leaflet/      # Embedded offline Leaflet mapping engine
├── src/                              # React Native / Expo Application
│   ├── components/                   # Reusable UI widgets & map adapters
│   ├── context/                      # App state context & offline providers
│   ├── mesh/                         # P2P mesh network engine & packet handlers
│   ├── screens/                      # Mobile screens (SOS, Map, Hazards, First Aid, Analytics)
│   └── theme/                        # Design tokens, typography, and dark/light palettes
├── master_dashboard.js               # Tactical Incident Command Dashboard server
├── laptop_bridge.js                  # Field gateway bridge server
├── generate_qr.js                    # Offline Medical ID QR generator
└── firestore.rules                   # Backend security rules & access controls
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or **Koala (2024.1+)** with **JDK 17**.
* **Android SDK Platform 35** (Android 15+ compatible, 16 KB page alignment compliant).
* **Node.js 18+** and **npm** (for the cross-platform client and master dashboard).

---

### Running the Android Native App

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Neeraj-Z/ResQLink.git
   cd ResQLink
   ```

2. **Open in Android Studio:**
   * Select **Open an Existing Project** and choose the root `ResQLink` folder.
   * Allow Gradle to sync dependencies.

3. **Configure Environment:**
   * Provide a valid Google Maps API key (optional if using Leaflet offline maps) in your environment:
     ```bash
     export MAPS_API_KEY="your_api_key_here"
     ```
   * Add `google-services.json` inside the `app/` directory for cloud sync, or build directly with offline mock data.

4. **Build and Run:**
   * Connect a physical Android device (Bluetooth and Wi-Fi enabled for mesh testing).
   * Run the `:app` target.

---

### Running the Cross-Platform Mobile / Web App

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Start the Expo development server:**
   ```bash
   npm start
   ```

3. **Launch the application:**
   * Press `w` to open the interactive web preview.
   * Press `a` to run on a connected Android device or emulator.
   * Scan the terminal QR code with the **Expo Go** app on your phone.

---

### Launching the Incident Command Dashboard & Bridge

To run the incident command center for local emergency coordinators:

1. **Start the Master Dashboard:**
   ```bash
   node master_dashboard.js
   ```
   Access the dashboard at `http://localhost:3000`.

2. **Start the Field Bridge:**
   ```bash
   node laptop_bridge.js
   ```

---

## 📡 Mesh Networking Specifications

* **Topology**: Star-Mesh Hybrid (`Strategy.P2P_CLUSTER`).
* **Relay Strategy**: Store-and-Forward epidemic routing with hop-count ceiling (`MAX_HOPS = 6`).
* **Packet Schema**:
  ```json
  {
    "packetId": "UUID-v4",
    "senderId": "DEVICE-ID",
    "timestamp": 1719483000000,
    "payloadType": "SOS_ALERT",
    "latitude": 28.6139,
    "longitude": 77.2090,
    "severity": "CRITICAL",
    "hopCount": 1,
    "data": "Medical emergency, rescue required"
  }
  ```
* **Deduplication**: Incoming packets are hashed against local SQLite storage. Packets with existing message IDs or expired TTL are dropped immediately to avoid channel saturation.

---

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
