# ResQLink — Offline-First Disaster Management & Mesh Emergency Platform

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Nearby Connections](https://img.shields.io/badge/Mesh%20Network-Nearby%20Connections-orange.svg)](https://developers.google.com/nearby/connections/overview)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Latest%20Release-v1.0.0-blue.svg)](https://github.com/Neeraj-Z/ResQLink/releases/latest)

**ResQLink** is a production-grade, offline-first disaster response and emergency coordination platform engineered for complete communication blackouts caused by earthquakes, floods, hurricanes, and infrastructure collapses.

When cellular towers and internet backbones fail, ResQLink turns Android devices into an autonomous, multi-hop **Peer-to-Peer (P2P) mesh network** using Bluetooth Low Energy (BLE) and Wi-Fi Direct. Critical distress signals, triage vitals, hazard perimeters, and emergency maps propagate across intermediate devices until reaching first responders or an active internet gateway.

---

## 📥 Direct APK Installation (Android)

You can download and install the pre-compiled Android app directly on any Android device:

[![Download APK](https://img.shields.io/badge/Direct%20Download-ResQLink%20Android%20APK%20(v1.0.0)-success?style=for-the-badge&logo=android)](https://github.com/Neeraj-Z/ResQLink/releases/latest/download/ResQLink.apk)

* **Direct APK Download**: **[ResQLink.apk (v1.0.0)](https://github.com/Neeraj-Z/ResQLink/releases/latest/download/ResQLink.apk)**
* **All Releases & Notes**: [GitHub Releases](https://github.com/Neeraj-Z/ResQLink/releases)

### How to Install on Android:
1. Tap the download link above on your Android phone.
2. Open the downloaded `ResQLink.apk` file.
3. Tap **Install** (if prompted, allow *"Install unknown apps"* for your browser in Settings).
4. Launch **ResQLink** and grant Bluetooth and Location permissions for peer-to-peer mesh networking.

---

## 🌟 Core Features

### 1. 🚨 Immediate SOS Distress System
* **One-Touch Beacon**: Instantly broadcast distress signals with exact GPS coordinates (latitude, longitude, altitude), battery percentage, timestamp, and victim count.
* **Safety Countdown**: A 5-second audible cancellation window prevents accidental emergency broadcasts.
* **Silent Alarm Mode**: Covert distress broadcasting for hazardous situations where sirens or screen flashes could endanger the victim.
* **Local Store-and-Forward**: If completely disconnected, the alert is immediately persisted in local Room SQLite storage and queued for background mesh relay.

### 2. 🗺️ Offline Interactive Tactical Maps
* **100% Offline Geolocation**: Embedded Leaflet engine and offline cached tiles allow navigation and situational awareness without any cellular signal.
* **Dynamic Danger Buffers**: Interactive colored danger zones illustrate verified hazard perimeters (structural collapses, flash floods, active fires) with real-time radius estimates.
* **Evacuation Sanctuary Markers**: Direct routing to verified relief shelters, medical camps, and food/water distribution points.
* **Live Distress Geo-Pins**: Nearby SOS beacons are mapped in real time with casualty severity indicators.

### 3. 📡 Autonomous Multi-Hop P2P Mesh Relay
* **Zero-Configuration Ad-Hoc Mesh**: Automatically discovers and links nearby devices using Google Nearby Connections (`P2P_CLUSTER` topology) without requiring cellular data, internet access, or manual Bluetooth pairing.
* **Multi-Hop Store-and-Forward**: Devices forward distress alerts to newly encountered peer nodes (up to 6 hops). The moment *any* node in the mesh chain gains cellular/Wi-Fi connectivity, pending packets automatically synchronize to central emergency servers.
* **Broadcast Storm & Loop Protection**: Packets carry unique collision-resistant UUIDs and hop-limit counters (TTL) to prevent broadcast storming.
* **Persistent Background Daemon**: Background service (`SosForegroundService`) keeps mesh relay active even when the screen is locked or the app is minimized.

### 4. 🩹 Indexed Offline First Aid Knowledge Base
* **Local Instant Search**: Rapid search through emergency protocols (CPR, arterial bleeding, fractures, burns, shock, water purification) with zero network latency.
* **High-Legibility Triage Layout**: Designed with high-contrast cards optimized for low-light, high-stress disaster conditions.

### 5. ⚠️ Real-Time Hazard Intelligence
* **Severity-Categorized Threat Feeds**: Filter alerts by Critical, High, Moderate, and Advisory status.
* **Field Updates**: Step-by-step safety recommendations, water level forecasts, and evacuation guidance.

### 6. 🪪 Offline Medical ID & Paramedic QR
* **Emergency Medical Card**: Stores critical medical telemetry (blood group, allergies, chronic conditions, emergency contacts) locally.
* **Offline Paramedic Scan**: Generates an on-screen QR code that rescue workers and paramedics can scan with standard phone cameras even if the user is unconscious.

---

## 🏗️ Architecture & Technology Stack

ResQLink is built following **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** design pattern with unidirectional data flow.

```
                      ┌────────────────────────────────────────┐
                      │          Presentation Layer            │
                      │  (Jetpack Compose M3 + StateFlow + VM)  │
                      └───────────────────▲────────────────────┘
                                          │ State / Events
                      ┌───────────────────▼────────────────────┐
                      │             Domain Layer               │
                      │   (Use Cases, Domain Models, Mesh API) │
                      └─────────▲────────────────────▲─────────┘
                                │                    │
                ┌───────────────┴────┐          ─────┴──────────────────┐
                │     Data Layer     │          │   Mesh & Remote Sync   │
                │  (Room SQLite DB)  │          │  (Nearby Mesh Manager) │
                └────────────────────┘          └────────────────────────┘
```

| Layer | Technologies |
| :--- | :--- |
| **Language & Platform** | Kotlin 2.0 • Android SDK (minSdk 26, targetSdk 35) |
| **UI Framework** | Jetpack Compose • Material Design 3 |
| **Asynchronous Engine** | Kotlin Coroutines • StateFlow / SharedFlow |
| **Dependency Injection** | Dagger Hilt |
| **Local Persistence** | Room Database (SQLite) • DataStore Preferences |
| **Mesh Networking** | Google Nearby Connections API (`P2P_CLUSTER`) |
| **Mapping Engine** | Embedded Leaflet GIS • OpenStreetMap offline cache |
| **Background Processing** | Android WorkManager (Expedited / Periodic SOS flush workers) |
| **Incident Command** | Node.js Incident Operations Dashboard & Field Bridge |

---

## 📂 Repository Structure

```
ResQLink/
├── app/                              # Android Native Project Module
│   ├── src/main/java/com/resqlink/app/
│   │   ├── data/                     # Room DAOs, Entities, Mesh & Location Repositories
│   │   ├── domain/                   # Pure Kotlin business models & Use Cases
│   │   ├── presentation/             # Jetpack Compose Screens (Home, Map, SOS, Analytics...)
│   │   ├── di/                       # Dagger Hilt Dependency Injection Modules
│   │   ├── service/                  # SosForegroundService (Persistent Mesh Relay Daemon)
│   │   └── worker/                   # WorkManager background sync workers
│   ├── src/main/assets/leaflet/      # Embedded offline Leaflet mapping engine
│   └── build.gradle.kts              # App module dependencies & SDK configurations
├── build.gradle.kts                  # Root project Gradle build configuration
├── settings.gradle.kts               # Project settings & module repository catalog
├── master_dashboard.js               # Tactical Incident Command Dashboard server
├── laptop_bridge.js                  # Field gateway bridge server
├── firestore.rules                   # Backend security rules & access controls
└── README.md                         # Project documentation
```

---

## 🛠️ Building & Running from Source

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or **Koala (2024.1+)**
* **JDK 17**
* **Android SDK Platform 35** (Android 15+ compatible, 16 KB memory page aligned)

### Step 1: Clone the Repository
```bash
git clone https://github.com/Neeraj-Z/ResQLink.git
cd ResQLink
```

### Step 2: Open in Android Studio
1. Launch Android Studio.
2. Select **Open an Existing Project** and choose the root `ResQLink` folder.
3. Allow Gradle to download dependencies and sync the project.

### Step 3: Run on Physical Device
> ⚠️ **Note on Mesh Testing:** Google's Nearby Connections API requires physical hardware radios (Bluetooth and Wi-Fi) to form peer-to-peer mesh networks. For full mesh testing, run on physical Android devices rather than emulators.

1. Enable **Developer Options** and **USB Debugging** on your Android phone.
2. Connect your phone to your PC via USB.
3. In Android Studio, select your device from the target selector.
4. Click **Run 'app'** (`Shift + F10`) or execute in your terminal:
   ```bash
   ./gradlew installDebug
   ```

---

## 💻 Incident Command Dashboard & Field Bridge (Optional)

For local disaster coordination teams operating from command vehicles or base camps:

1. **Start the Master Rescue Operations Dashboard:**
   ```bash
   node master_dashboard.js
   ```
   Open `http://localhost:9000` in any web browser to view aggregated mesh alerts, multi-hop packet traces, and live victim statuses.

2. **Start the Field Radio Gateway Bridge:**
   ```bash
   node laptop_bridge.js
   ```
   Opens a local UDP/HTTP bridge on port `8988` to connect field radios with laptop coordination software.

---

## 📡 Mesh Networking Protocol Specifications

* **Topology**: Decentralized Star-Mesh Hybrid (`Strategy.P2P_CLUSTER`).
* **Routing Strategy**: Store-and-forward epidemic routing with hop-count ceiling (`MAX_HOPS = 6`).
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
    "data": "Medical emergency, trapped victim"
  }
  ```
* **Deduplication**: Incoming packets are hashed against local SQLite storage. Packets with existing message IDs or expired TTL are dropped immediately to avoid channel saturation.

---

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
