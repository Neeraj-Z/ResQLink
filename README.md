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

## 📱 Running ResQLink on Android (via Expo Go)

You can run and test ResQLink directly on any physical Android phone in **under 2 minutes** without needing Android Studio or APK installations:

### Quick 3-Step Setup with Expo Go:
1. **Install Expo Go on Android**:
   * Open the **Google Play Store** on your Android phone and install **[Expo Go](https://play.google.com/store/apps/details?id=host.exp.exponent)**.
2. **Start the Development Server on your PC**:
   * In the project root directory, run:
     ```bash
     npx expo start --tunnel
     ```
     *(The `--tunnel` flag creates a secure connection allowing your phone to connect over Wi-Fi or mobile data without complex network setup).*
3. **Scan the QR Code**:
   * A dynamic QR code will be generated directly in your terminal window.
   * Open the **Expo Go** app on your Android device, tap **"Scan QR code"**, and point your camera at the terminal screen.
   * ResQLink will instantly bundle and run with full touch controls, offline maps, and reactive state!

> 💡 **Quick Helper Script**: You can also run `node generate_qr.js` to launch the Expo tunnel directly, or open [qr.html](qr.html) in your browser for a quick visual walkthrough.

---

## 📖 How to Use ResQLink on Android (Field Guide)

ResQLink is built for high-stress disaster situations. Here is how each module functions on an Android device during an emergency:

### 1. 🚨 Triggering an Emergency SOS Beacon
* **One-Touch Activation**: Tap the prominent **SOS** tab. Press the large red **SEND SOS** button to initiate emergency broadcasting.
* **Safety Countdown**: A 5-second audible cancellation window prevents accidental triggers.
* **Critical Telemetry**: Automatically bundles your exact GPS coordinates (latitude/longitude), battery percentage, timestamp, and casualty tags.
* **Silent Distress Option**: Toggle "Silent Alarm" if situational danger requires sending distress signals covertly without sound or flashing screens.
* **Store-and-Forward**: If cellular/Wi-Fi is completely down, the alert is instantly written to your local offline database and handed to the peer-to-peer mesh engine.

### 2. 🗺️ Offline Interactive Tactical Map
* **Zero-Connection Geolocation**: Open the **Map** tab to view your current location against cached offline map tiles.
* **Hazard Danger Zones**: Red and amber danger circles illustrate active hazards (flood waters, structural collapses, active fires) with real-time radius estimates.
* **Safe Evacuation Havens**: Tap green sanctuary markers to view directions to verified relief centers, medical camps, and food distribution hubs.
* **Active SOS Geo-Pins**: Emergency beacons within your vicinity appear on the map with casualty urgency levels.

### 3. 📡 Autonomous Offline P2P Mesh Networking
* **No SIM or Internet Needed**: The app uses Bluetooth Low Energy and Wi-Fi Direct to form an ad-hoc local mesh with other nearby ResQLink devices.
* **Automatic Background Relay**: When you pass other people with ResQLink, your phone automatically forwards stored SOS alerts across devices up to 6 hops away.
* **Gateway Synchronization**: As soon as any single device in the mesh reaches an area with connectivity, all accumulated distress signals upload immediately to rescue headquarters.

### 4. 🩹 Offline First Aid Medical Knowledge Base
* **Immediate Local Search**: Switch to the **First Aid** tab to access triage protocols for CPR, severe hemorrhage, bone fractures, burns, hypothermia, and shock.
* **High-Legibility Layout**: Designed with high-contrast, distraction-free cards so instructions are easily readable in low-light and high-stress environments.

### 5. ⚠️ Real-Time Hazard Intelligence
* **Categorized Threat Streams**: Open the **Hazards** tab to monitor severity-coded updates (Critical, High, Moderate, Advisory).
* **Incident Details**: Read verified safety instructions, wind/water movement, and recommended evacuation routes.

### 6. 🪪 Offline Medical ID & Paramedic QR Code
* **Emergency Medical Card**: Access the **Settings / Profile** tab to configure your blood group, known allergies, chronic conditions, and emergency contacts.
* **Offline Paramedic Scan**: Generates an on-screen QR code containing your medical summary that first responders can scan with any phone even if you are unconscious.

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
